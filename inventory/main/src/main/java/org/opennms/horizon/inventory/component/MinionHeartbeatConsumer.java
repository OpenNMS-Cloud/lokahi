/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.opennms.horizon.inventory.component;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;
import javax.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.opennms.cloud.grpc.minion_gateway.GatewayRpcRequestProto;
import org.opennms.cloud.grpc.minion_gateway.GatewayRpcResponseProto;
import org.opennms.cloud.grpc.minion_gateway.MinionIdentity;
import org.opennms.horizon.grpc.echo.contract.EchoRequest;
import org.opennms.horizon.grpc.echo.contract.EchoResponse;
import org.opennms.horizon.grpc.heartbeat.contract.TenantLocationSpecificHeartbeatMessage;
import org.opennms.horizon.inventory.dto.MonitoringLocationDTO;
import org.opennms.horizon.inventory.service.MonitoringLocationService;
import org.opennms.horizon.inventory.service.MonitoringSystemService;
import org.opennms.horizon.inventory.util.Clock;
import org.opennms.taskset.contract.MonitorResponse;
import org.opennms.taskset.contract.MonitorType;
import org.opennms.taskset.contract.TaskResult;
import org.opennms.taskset.contract.TenantLocationSpecificTaskSetResults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
@PropertySource("classpath:application.yml")
public class MinionHeartbeatConsumer {

    protected static final String DEFAULT_TASK_RESULTS_TOPIC = "task-set.results";
    private static final int DEFAULT_MESSAGE_SIZE = 1024;
    private static final long ECHO_TIMEOUT = 30_000;
    private static final int MONITOR_PERIOD = 30_000 - 2000; // We expect heartbeats every 30 secs,
    // we should still process heartbeats received closer to 30secs interval, so 2secs prior arrival should still be
    // processed.
    private final MinionRpcClient rpcClient;
    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private final ThreadFactory threadFactory = new ThreadFactoryBuilder()
            .setNameFormat("handle-minion-heartbeat-%d")
            .build();
    private final ExecutorService executorService = Executors.newFixedThreadPool(100, threadFactory);

    @Value("${kafka.topics.task-set-results:" + DEFAULT_TASK_RESULTS_TOPIC + "}")
    private String kafkaTopic;

    // Testability
    @Setter
    private Consumer<Runnable> rpcMonitorRunner = CompletableFuture::runAsync;

    private final MonitoringSystemService monitoringSystemService;
    private final MonitoringLocationService locationService;
    private final Clock clock;
    private final Map<String, Long> rpcMaps = new ConcurrentHashMap<>();

    @KafkaListener(topics = "${kafka.topics.minion-heartbeat}", concurrency = "${kafka.concurrency.minion-heartbeat}")
    public void receiveMessage(@Payload byte[] data) {

        try {
            TenantLocationSpecificHeartbeatMessage message = TenantLocationSpecificHeartbeatMessage.parseFrom(data);
            executorService.execute(() -> processHeartbeat(message));
        } catch (InvalidProtocolBufferException e) {
            log.error("Error while processing heartbeat message: ", e);
        }
    }

    public void processHeartbeat(TenantLocationSpecificHeartbeatMessage message) {
        try {
            String tenantId = message.getTenantId();
            String locationId = message.getLocationId();

            Span.current().setAttribute("user", tenantId);
            Span.current().setAttribute("location-id", locationId);
            Span.current().setAttribute("system-id", message.getIdentity().getSystemId());

            Optional<MonitoringLocationDTO> location =
                    locationService.getByIdAndTenantId(Long.parseLong(locationId), tenantId);
            if (location.isEmpty()) {
                log.info(
                        "Received heartbeat message for orphaned minion: tenantId={}; locationId={}; systemId={}",
                        tenantId,
                        locationId,
                        message.getIdentity().getSystemId());
                return;
            }
            log.info(
                    "Received heartbeat message for minion: tenantId={}; locationId={}; systemId={}",
                    tenantId,
                    locationId,
                    message.getIdentity().getSystemId());
            monitoringSystemService.addMonitoringSystemFromHeartbeat(message);

            String systemId = message.getIdentity().getSystemId();
            String key = tenantId + "_" + locationId + "-" + systemId;

            Long lastRun = rpcMaps.get(key);

            // WARNING: this uses wall-clock.  If a system's time is changed, this logic will be impacted.
            // TODO: consider changing to System.nanoTime() which is not affected by wall-clock changes
            long currentTimeMs = clock.getCurrentTimeMs();
            if (lastRun == null || (currentTimeMs > (lastRun + MONITOR_PERIOD))) { // prevent run too many rpc calls
                rpcMonitorRunner.accept(() -> runRpcMonitor(tenantId, locationId, systemId));
                rpcMaps.put(key, currentTimeMs);
            }

            Span.current().setStatus(StatusCode.OK);
        } catch (Exception e) {
            log.error("Error while processing heartbeat message: ", e);
            Span.current().recordException(e);
        }
    }

    @PreDestroy
    public void shutDown() {
        if (rpcClient != null) {
            rpcClient.shutdown();
        }
    }

    private void runRpcMonitor(String tenantId, String locationId, String systemId) {
        log.info("Sending RPC request for tenantId={}; locationId={}; systemId={}", tenantId, locationId, systemId);
        EchoRequest echoRequest = EchoRequest.newBuilder()
                .setTime(System.nanoTime())
                .setMessage(Strings.repeat("*", DEFAULT_MESSAGE_SIZE))
                .build();

        MinionIdentity minionIdentity = MinionIdentity.newBuilder()
                .setTenantId(tenantId)
                .setLocationId(locationId)
                .setSystemId(systemId)
                .build();

        GatewayRpcRequestProto request = GatewayRpcRequestProto.newBuilder()
                .setIdentity(minionIdentity)
                .setModuleId("Echo")
                .setExpirationTime(System.currentTimeMillis() + ECHO_TIMEOUT)
                .setRpcId(UUID.randomUUID().toString())
                .setPayload(Any.pack(echoRequest))
                .build();

        rpcClient
                .sendRpcRequest(tenantId, request)
                .thenApply(GatewayRpcResponseProto::getPayload)
                .thenApply(payload -> {
                    try {
                        return payload.unpack(EchoResponse.class);
                    } catch (InvalidProtocolBufferException e) {
                        throw new RuntimeException(e);
                    }
                })
                .whenComplete((echoResponse, error) -> {
                    if (error != null) {
                        log.error(
                                "Unable to complete echo request for monitoring with tenantId={}; locationId={}; systemId={}",
                                tenantId,
                                locationId,
                                systemId,
                                error);
                        return;
                    }
                    long responseTime = (System.nanoTime() - echoResponse.getTime()) / 1000000;
                    publishResult(systemId, locationId, tenantId, responseTime);
                    log.info("Response time for minion {} is {} msecs", systemId, responseTime);
                });
    }

    private void publishResult(String systemId, String locationId, String tenantId, long responseTime) {
        // TODO: use a separate structure from TaskSetResult - this is not the result of processing a TaskSet
        org.opennms.taskset.contract.Identity identity = org.opennms.taskset.contract.Identity.newBuilder()
                .setSystemId(systemId)
                .build();
        MonitorResponse monitorResponse = MonitorResponse.newBuilder()
                .setStatus("UP")
                .setResponseTimeMs(responseTime)
                .setMonitorType(MonitorType.ECHO)
                .setIpAddress(systemId) // for minion only
                .setTimestamp(System.currentTimeMillis())
                .build();
        TaskResult result = TaskResult.newBuilder()
                .setId("monitor-" + systemId)
                .setIdentity(identity)
                .setMonitorResponse(monitorResponse)
                .build();
        TenantLocationSpecificTaskSetResults results = TenantLocationSpecificTaskSetResults.newBuilder()
                .setTenantId(tenantId)
                .setLocationId(locationId)
                .addResults(result)
                .build();

        ProducerRecord<String, byte[]> producerRecord = new ProducerRecord<>(kafkaTopic, results.toByteArray());
        kafkaTemplate.send(producerRecord);
    }
}
