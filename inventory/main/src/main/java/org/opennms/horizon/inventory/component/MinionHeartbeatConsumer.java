/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.horizon.inventory.component;

import com.google.common.base.Strings;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcResponseProto;
import org.opennms.horizon.grpc.echo.contract.EchoRequest;
import org.opennms.horizon.grpc.echo.contract.EchoResponse;
import org.opennms.horizon.grpc.heartbeat.contract.HeartbeatMessage;
import org.opennms.horizon.inventory.exception.InventoryRuntimeException;
import org.opennms.horizon.inventory.service.MonitoringSystemService;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.opennms.taskset.contract.MonitorResponse;
import org.opennms.taskset.contract.MonitorType;
import org.opennms.taskset.contract.TaskResult;
import org.opennms.taskset.contract.TenantedTaskSetResults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
@Component
@Slf4j
@PropertySource("classpath:application.yml")
public class MinionHeartbeatConsumer {

    protected static final String DEFAULT_TASK_RESULTS_TOPIC = "task-set.results";
    private static final int DEFAULT_MESSAGE_SIZE = 1024;
    private static final long ECHO_TIMEOUT = 30_000;
    private static final int MONITOR_PERIOD = 30_000 - 2000; // We expect heartbeats every 30 secs,
    // we should still process heartbeats received closer to 30secs interval, so 2secs prior arrival should still be processed.
    private final MinionRpcClient rpcClient;
    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    @Value("${kafka.topics.task-set-results:" + DEFAULT_TASK_RESULTS_TOPIC + "}")
    private String kafkaTopic;
    private final MonitoringSystemService service;

    private final Map<String, Long> rpcMaps = new ConcurrentHashMap<>();

    @KafkaListener(topics = "${kafka.topics.minion-heartbeat}", concurrency = "1")
    public void receiveMessage(@Payload byte[] data, @Headers Map<String, Object> headers) {
        try {
            HeartbeatMessage message = HeartbeatMessage.parseFrom(data);
            String tenantId = Optional.ofNullable(headers.get(GrpcConstants.TENANT_ID_KEY)).map(o -> new String((byte[])o))
                .orElseThrow(()->new InventoryRuntimeException("Missing tenant id"));
            log.info("Received heartbeat message for minion with tenant id: {}; id: {}; location: {}", tenantId, message.getIdentity().getSystemId(), message.getIdentity().getLocation());
            service.addMonitoringSystemFromHeartbeat(message, tenantId);
            String systemId = message.getIdentity().getSystemId();
            String location = message.getIdentity().getLocation();
            String key = tenantId + "_" + location + "-" + systemId;
            Long lastRun = rpcMaps.get(key);
            if (lastRun == null || (getSystemTimeInMsec() > (lastRun + MONITOR_PERIOD))) { //prevent run too many rpc calls
                CompletableFuture.runAsync(() -> runRpcMonitor(systemId, message.getIdentity().getLocation(), tenantId));
                rpcMaps.put(key, getSystemTimeInMsec());
            }
        } catch (Exception e) {
            log.error("Error while processing heartbeat message: ", e);
        }
    }

    @PreDestroy
    public void shutDown() {
        if(rpcClient != null) {
            rpcClient.shutdown();
        }
    }

    private void runRpcMonitor(String systemId, String location, String tenantId) {
        log.info("Sending RPC request for minion {} with location {}", systemId, location);
        EchoRequest echoRequest = EchoRequest.newBuilder()
            .setTime(System.nanoTime())
            .setMessage(Strings.repeat("*", DEFAULT_MESSAGE_SIZE))
            .build();
        RpcRequestProto request = RpcRequestProto.newBuilder()
            .setLocation(location)
            .setSystemId(systemId)
            .setModuleId("Echo")
            .setExpirationTime(System.currentTimeMillis() + ECHO_TIMEOUT)
            .setRpcId(UUID.randomUUID().toString())
            .setPayload(Any.pack(echoRequest))
            .build();
        rpcClient.sendRpcRequest(tenantId, request).thenApply(RpcResponseProto::getPayload)
            .thenApply(payload -> {
                try {
                    return payload.unpack(EchoResponse.class);
                } catch (InvalidProtocolBufferException e) {
                    throw new RuntimeException(e);
                }
            })
            .whenComplete((echoResponse, error) -> {
                    if (error != null) {
                        log.error("Unable to complete echo request for monitoring system {} with tenant {}, location {}",
                            systemId, tenantId, location, error);
                        return;
                    }
                    long responseTime = (System.nanoTime() - echoResponse.getTime()) / 1000000;
                    publishResult(systemId, location, tenantId, responseTime);
                    log.info("Response time for minion {} is {} msecs", systemId, responseTime);
                }
            );
    }

    private void publishResult(String systemId, String location, String tenantId, long responseTime) {
        // TODO: use a separate structure from TaskSetResult - this is not the result of processing a TaskSet
        MonitorResponse monitorResponse = MonitorResponse.newBuilder()
            .setStatus("UP")
            .setResponseTimeMs(responseTime)
            .setMonitorType(MonitorType.ECHO)
            .setIpAddress(systemId) //for minion only
            .setTimestamp(System.currentTimeMillis())
            .build();
        TaskResult result = TaskResult.newBuilder()
            .setId("monitor-"+systemId)
            .setSystemId(systemId)
            .setLocation(location)
            .setMonitorResponse(monitorResponse)
            .build();
        TenantedTaskSetResults results = TenantedTaskSetResults.newBuilder()
            .setTenantId(tenantId)
            .addResults(result)
            .build();

        ProducerRecord<String, byte[]> producerRecord = new ProducerRecord<>(kafkaTopic, results.toByteArray());
        kafkaTemplate.send(producerRecord);
    }

    protected long getSystemTimeInMsec() {
        return System.currentTimeMillis();
    }
}
