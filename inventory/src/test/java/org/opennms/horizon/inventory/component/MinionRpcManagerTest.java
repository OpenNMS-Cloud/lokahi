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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.opennms.horizon.inventory.TestConstants.PRIMARY_TENANT_ID;
import static org.opennms.horizon.inventory.TestConstants.SECONDARY_TENANT_ID;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcResponseProto;
import org.opennms.horizon.grpc.echo.contract.EchoRequest;
import org.opennms.horizon.grpc.echo.contract.EchoResponse;
import org.opennms.horizon.inventory.Constants;
import org.opennms.horizon.inventory.model.MonitoringSystemBean;
import org.opennms.horizon.inventory.service.MonitoringSystemService;
import org.opennms.taskset.contract.MonitorResponse;
import org.opennms.taskset.contract.MonitorType;
import org.opennms.taskset.contract.TaskResult;
import org.opennms.taskset.contract.TaskSetResults;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;

import nl.altindag.log.LogCaptor;

class MinionRpcManagerTest {
    private MinionRpcManager rpcManager;
    private MinionRpcClient mockClient;
    private MonitoringSystemService mockService;
    private KafkaTemplate<String, byte[]> mockTemplate;

    private MonitoringSystemBean system1, system2;
    private RpcResponseProto rpcResponse;
    private LogCaptor logCaptor;

    @BeforeEach
    public void prepare() {
        logCaptor = LogCaptor.forClass(MinionRpcManager.class);
        mockClient = mock(MinionRpcClient.class);
        mockService = mock(MonitoringSystemService.class);
        mockTemplate = mock(KafkaTemplate.class);
        rpcManager = new MinionRpcManager(mockClient, mockService, mockTemplate);
        ReflectionTestUtils.setField(rpcManager, "initialDelay", 0);
        ReflectionTestUtils.setField(rpcManager, "kafkaTopic", "test-topic");
        system1 = new MonitoringSystemBean("test-system1", PRIMARY_TENANT_ID, "test-location1");
        system2 = new MonitoringSystemBean("test-system2", SECONDARY_TENANT_ID, "test-location2");
        EchoResponse echoResponse = EchoResponse.newBuilder().setTime(System.nanoTime()).build();
        rpcResponse = RpcResponseProto.newBuilder().setPayload(Any.pack(echoResponse)).build();
    }

    @AfterEach
    public void cleanUp() throws InterruptedException {
        rpcManager.shutDown();
        verify(mockClient).shutdown();
        verifyNoMoreInteractions(mockClient);
        verifyNoMoreInteractions(mockService);
        verifyNoMoreInteractions(mockTemplate);
    }

    @Test
    void testStartUp() throws InvalidProtocolBufferException {
        List<MonitoringSystemBean> systemList = Arrays.asList(system1, system2);
        ArgumentCaptor<RpcRequestProto> primaryTenantCaptor = ArgumentCaptor.forClass(RpcRequestProto.class);
        ArgumentCaptor<RpcRequestProto> secondaryTenantCaptor = ArgumentCaptor.forClass(RpcRequestProto.class);
        doReturn(Arrays.asList(system1, system2)).when(mockService).listAllForMonitoring();
        doReturn(CompletableFuture.completedFuture(rpcResponse)).when(mockClient).sendRpcRequest(eq(PRIMARY_TENANT_ID), any(RpcRequestProto.class));
        doReturn(CompletableFuture.completedFuture(rpcResponse)).when(mockClient).sendRpcRequest(eq(SECONDARY_TENANT_ID), any(RpcRequestProto.class));
        ArgumentCaptor<ProducerRecord<String, byte[]>> producerCaptor = ArgumentCaptor.forClass(ProducerRecord.class);
        rpcManager.startRpc();
        await().atMost(Duration.ofMillis(1000)).until(() ->
            logCaptor.getInfoLogs().stream().anyMatch(l->l.startsWith("Response time for minion")));
        verify(mockService).listAllForMonitoring();
        verify(mockClient, times(1)).sendRpcRequest(eq(PRIMARY_TENANT_ID), primaryTenantCaptor.capture());
        verify(mockClient, times(1)).sendRpcRequest(eq(SECONDARY_TENANT_ID), secondaryTenantCaptor.capture());
        List<RpcRequestProto> requests = primaryTenantCaptor.getAllValues();
        assertRequests(Collections.singletonList(system1), requests);
        requests = secondaryTenantCaptor.getAllValues();
        assertRequests(Collections.singletonList(system2), requests);
        verify(mockTemplate, times(2)).send(producerCaptor.capture());
        List<ProducerRecord<String, byte[]>> records = producerCaptor.getAllValues();
        assertProducerRecords(systemList, records);
    }

    @Test
    void testAddSystem() throws InvalidProtocolBufferException {
        long id = 1L;
        doReturn(Collections.emptyList()).when(mockService).listAllForMonitoring();
        doReturn(Optional.of(system1)).when(mockService).getByIdForMonitoring(id);
        doReturn(CompletableFuture.completedFuture(rpcResponse)).when(mockClient).sendRpcRequest(eq(PRIMARY_TENANT_ID), any(RpcRequestProto.class));
        rpcManager.startRpc();
        rpcManager.addSystem(id);
        await().atMost(Duration.ofMillis(5000)).until(() ->
            logCaptor.getInfoLogs().stream().anyMatch(l -> l.startsWith("Response time for minion")));
        ArgumentCaptor<RpcRequestProto> requestCaptor = ArgumentCaptor.forClass(RpcRequestProto.class);
        ArgumentCaptor<ProducerRecord<String, byte[]>> producerCaptor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(mockService).listAllForMonitoring();
        verify(mockService).getByIdForMonitoring(id);
        verify(mockClient).sendRpcRequest(eq(PRIMARY_TENANT_ID), requestCaptor.capture());
        assertRequests(Collections.singletonList(system1), Collections.singletonList(requestCaptor.getValue()));
        verify(mockTemplate).send(producerCaptor.capture());
        assertProducerRecords(Collections.singletonList(system1), Collections.singletonList(producerCaptor.getValue()));
    }

    private void assertProducerRecords(List<MonitoringSystemBean> systems, List<ProducerRecord<String, byte[]>> records) throws InvalidProtocolBufferException {
        assertThat(records).asList().hasSameSizeAs(systems);
        for (ProducerRecord<String, byte[]> record : records) {
            TaskSetResults results = TaskSetResults.parseFrom(record.value());
            assertThat(results.getResultsList()).asList().hasSize(1);
            TaskResult result = results.getResults(0);
            MonitorResponse response = result.getMonitorResponse();
            assertThat(response.getStatus()).isEqualTo("UP");
            assertThat(response.getMonitorType()).isEqualTo(MonitorType.ECHO);
            assertThat(response.getResponseTimeMs()).isPositive();
            assertThat(System.nanoTime() - response.getResponseTimeMs()).isPositive();

            MonitoringSystemBean system = systems.stream().filter(s->s.getSystemId().equals(result.getSystemId())).findFirst().orElseThrow();
            assertThat(result.getId()).isEqualTo("monitor-" + system.getSystemId());
            assertThat(result.getSystemId()).isEqualTo(system.getSystemId());
            assertThat(result.getLocation()).isEqualTo(system.getLocation());
            assertThat(response.getIpAddress()).isEqualTo(system.getSystemId());
            Header[] headers = record.headers().toArray();
            Header tenantHeader = Arrays.stream(headers).filter(h->h.key().equals(Constants.TENANT_ID_KEY)).findFirst().orElseThrow();
            assertThat(new String(tenantHeader.value())).isEqualTo(system.getTenantId());
        }
    }

    private void assertRequests(List<MonitoringSystemBean> systemList, List<RpcRequestProto> requests) throws InvalidProtocolBufferException {
        assertThat(requests).asList().hasSameSizeAs(systemList);
        List<String> systemIds = systemList.stream().map(MonitoringSystemBean::getSystemId).collect(Collectors.toList());
        List<String> locations = systemList.stream().map(MonitoringSystemBean::getLocation).collect(Collectors.toList());

        for (RpcRequestProto r :requests) {
            assertThat(systemIds).asList().contains(r.getSystemId());
            assertThat(locations).asList().contains(r.getLocation());
            assertThat(r.getModuleId()).isEqualTo("Echo");
            assertThat(r.getRpcId()).isNotNull();
            EchoRequest echoRequest = r.getPayload().unpack(EchoRequest.class);
            assertThat(echoRequest.getTime()).isPositive();
            assertThat(System.nanoTime() - echoRequest.getTime()).isPositive();
        }
    }
}
