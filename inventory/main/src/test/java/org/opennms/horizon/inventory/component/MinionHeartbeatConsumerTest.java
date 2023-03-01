/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcResponseProto;
import org.opennms.horizon.grpc.echo.contract.EchoResponse;
import org.opennms.horizon.grpc.heartbeat.contract.HeartbeatMessage;
import org.opennms.horizon.inventory.service.MonitoringSystemService;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.protobuf.Any;

@ExtendWith(MockitoExtension.class)
public class MinionHeartbeatConsumerTest {
    @Mock
    private MinionRpcClient rpcClient;
    @Mock
    private KafkaTemplate<String, byte[]> kafkaTemplate;
    @Mock
    private MonitoringSystemService service;
    @InjectMocks
    MinionHeartbeatConsumer messageConsumer;

    private final String tenantId = "test-tenant";
    private Map<String, Object> headers;
    private HeartbeatMessage heartbeat;

    @BeforeAll
    static void prepareTests(){
        MinionHeartbeatConsumer.MONITOR_PERIOD = 1000;
    }

    @BeforeEach
    void beforeTest() {
        String systemId = "test-system123";
        String location = "test-location";

        heartbeat = HeartbeatMessage.newBuilder()
            .setIdentity(Identity.newBuilder().setLocation(location).setSystemId(systemId).build()).build();
        headers = new HashMap<>();
        headers.put(GrpcConstants.TENANT_ID_KEY, tenantId.getBytes(StandardCharsets.UTF_8));
        EchoResponse response = EchoResponse.newBuilder().setTime(System.nanoTime()).build();
        RpcResponseProto rpcResponse = RpcResponseProto.newBuilder().setPayload(Any.pack(response)).build();
        doReturn(CompletableFuture.completedFuture(rpcResponse)).when(rpcClient).sendRpcRequest(eq(tenantId), any(RpcRequestProto.class));
        ReflectionTestUtils.setField(messageConsumer, "kafkaTopic", "test-topic");
    }

    @Test
    void testAcceptHeartbeats() throws InterruptedException {
        messageConsumer.receiveMessage(heartbeat.toByteArray(), headers);
        messageConsumer.receiveMessage(heartbeat.toByteArray(), headers);
        Thread.sleep(1000);
        verify(service, times(2)).addMonitoringSystemFromHeartbeat(any(HeartbeatMessage.class), eq(tenantId));
        verify(rpcClient).sendRpcRequest(eq(tenantId), any(RpcRequestProto.class));
        verify(kafkaTemplate).send(any(ProducerRecord.class));
    }

    @Test
    void testAcceptHeartbeatsDelay() throws InterruptedException {
        messageConsumer.receiveMessage(heartbeat.toByteArray(), headers);
        Thread.sleep(1000);
        messageConsumer.receiveMessage(heartbeat.toByteArray(), headers);
        Thread.sleep(100);
        verify(service, times(2)).addMonitoringSystemFromHeartbeat(any(HeartbeatMessage.class), eq(tenantId));
        verify(rpcClient, times(2)).sendRpcRequest(eq(tenantId), any(RpcRequestProto.class));
        verify(kafkaTemplate, times(2)).send(any(ProducerRecord.class));
    }
}
