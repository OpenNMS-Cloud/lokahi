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

import java.util.UUID;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcResponseProto;
import org.opennms.horizon.grpc.echo.contract.EchoRequest;
import org.opennms.horizon.grpc.echo.contract.EchoResponse;
import org.opennms.horizon.inventory.model.MonitoringSystemBean;
import org.opennms.horizon.inventory.service.MonitoringSystemService;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.opennms.taskset.contract.MonitorResponse;
import org.opennms.taskset.contract.MonitorType;
import org.opennms.taskset.contract.TaskResult;
import org.opennms.taskset.contract.TaskSetResults;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MinionRpcManager {
    private static final String DEFAULT_TASK_RESULTS_TOPIC = "task-set.results";
    private static final int DEFAULT_MESSAGE_SIZE = 1024;
    private static final int MONITOR_INITIAL_DELAY = 3_000;
    private static final int MONITOR_PERIOD = 30_000;
    private static final long ECHO_TIMEOUT = 30_000;
    private final MinionRpcClient rpcClient;
    private final MonitoringSystemService service;
    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    private int initialDelay = MONITOR_INITIAL_DELAY;
    @Value("${kafka.topics.results:" + DEFAULT_TASK_RESULTS_TOPIC + "}")
    private String kafkaTopic;

    public MinionRpcManager(MinionRpcClient rpcClient, MonitoringSystemService service,
                            @Qualifier("byteArrayTemplate") KafkaTemplate<String, byte[]> kafkaTemplate) {
        this.rpcClient = rpcClient;
        this.service = service;
        this.kafkaTemplate = kafkaTemplate;
    }

    private ScheduledThreadPoolExecutor executor;

    @PostConstruct
    public void startRpc() {
        log.info("Start RPC");
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
            .setNameFormat("monitoring-system-monitor-runner-%d")
                .build();
        executor = new ScheduledThreadPoolExecutor(10, threadFactory);
        service.listAllForMonitoring().forEach(this::startMonitoring);
    }

    public void addSystem(long id) {
        service.getByIdForMonitoring(id).ifPresent(this::startMonitoring);
    }

    private void startMonitoring(MonitoringSystemBean systemDTO) {
        executor.scheduleAtFixedRate(() -> runRpcMonitor(systemDTO.getSystemId(),
            systemDTO.getLocation(), systemDTO.getTenantId()), initialDelay, MONITOR_PERIOD, TimeUnit.MILLISECONDS);
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
                    log.error("Unable to complete echo request", error);
                    return;
                }
                long responseTime = (System.nanoTime() - echoResponse.getTime()) / 1000000;
                publishResult(systemId, location, tenantId, responseTime);
                log.info("Response time for minion {} is {} msecs", systemId, responseTime);
            }
        );
    }

    private void publishResult(String systemId, String location, String tenantId, long responseTime) {
        MonitorResponse monitorResponse = MonitorResponse.newBuilder()
            .setStatus("UP")
            .setResponseTimeMs(responseTime)
            .setMonitorType(MonitorType.ECHO)
            .setIpAddress(systemId) //for minion only
            .build();
        TaskResult result = TaskResult.newBuilder()
            .setId("monitor-"+systemId)
            .setSystemId(systemId)
            .setLocation(location)
            .setMonitorResponse(monitorResponse)
            .build();
        TaskSetResults results = TaskSetResults.newBuilder()
            .addResults(result)
            .build();
        ProducerRecord<String, byte[]> producerRecord = new ProducerRecord<>(kafkaTopic, results.toByteArray());
        producerRecord.headers().add(new RecordHeader(GrpcConstants.TENANT_ID_KEY, tenantId.getBytes()));
        kafkaTemplate.send(producerRecord);
    }

    @PreDestroy
    public void shutDown() throws InterruptedException {
        if(executor !=null && !executor.isShutdown()) {
            executor.shutdown();
            executor.awaitTermination(1000, TimeUnit.MILLISECONDS);
        }
        if(rpcClient != null) {
            rpcClient.shutdown();
        }
    }
}
