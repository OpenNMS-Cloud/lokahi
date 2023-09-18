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

package org.opennms.horizon.alertservice.grpc;

import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.stub.MetadataUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.common.VerificationException;
import org.mockito.ArgumentCaptor;
import org.opennms.horizon.alerts.proto.AlertServiceGrpc;
import org.opennms.horizon.alerts.proto.Filter;
import org.opennms.horizon.alerts.proto.ListAlertsRequest;
import org.opennms.horizon.alerts.proto.ListAlertsResponse;
import org.opennms.horizon.alertservice.api.AlertService;
import org.opennms.horizon.alertservice.db.entity.Alert;
import org.opennms.horizon.alertservice.db.entity.AlertCondition;
import org.opennms.horizon.alertservice.db.entity.MonitorPolicy;
import org.opennms.horizon.alertservice.db.entity.Node;
import org.opennms.horizon.alertservice.db.entity.PolicyRule;
import org.opennms.horizon.alertservice.db.repository.AlertRepository;
import org.opennms.horizon.alertservice.db.repository.LocationRepository;
import org.opennms.horizon.alertservice.db.repository.NodeRepository;
import org.opennms.horizon.alertservice.db.tenant.GrpcTenantLookupImpl;
import org.opennms.horizon.alertservice.db.tenant.TenantLookup;
import org.opennms.horizon.alertservice.mapper.AlertMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class AlertGrpcServiceTest extends AbstractGrpcUnitTest {
    private AlertServiceGrpc.AlertServiceBlockingStub stub;
    private AlertService mockAlertService;
    private Alert alert1, alert2;
    private org.opennms.horizon.alerts.proto.Alert alertProto1, alertProto2;
    private ManagedChannel channel;
    private AlertMapper mockAlertMapper;
    private AlertRepository mockAlertRepository;
    private NodeRepository mockNodeRepository;

    private LocationRepository mockLocationRepository;

    protected TenantLookup tenantLookup = new GrpcTenantLookupImpl();


    @BeforeEach
    public void prepareTest() throws VerificationException, IOException {
        mockAlertService = mock(AlertService.class);
        mockAlertRepository = mock(AlertRepository.class);
        mockNodeRepository = mock(NodeRepository.class);
        mockAlertMapper = mock(AlertMapper.class);
        mockLocationRepository = mock(LocationRepository.class);
        AlertGrpcService grpcService = new AlertGrpcService(mockAlertMapper, mockAlertRepository, mockNodeRepository, mockLocationRepository, mockAlertService, tenantLookup);
        startServer(grpcService);
        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
        stub = AlertServiceGrpc.newBlockingStub(channel);
        alert1 = generateAlert("rule1", "policy1");
        alert2 = generateAlert("rule2", "policy2");
        alertProto1 = org.opennms.horizon.alerts.proto.Alert.newBuilder().build();
        alertProto2 = org.opennms.horizon.alerts.proto.Alert.newBuilder().build();
    }

    private Alert generateAlert(String ruleName, String policyName){
        var alert = new Alert();
        var alertCondition = new AlertCondition();
        var rule = new PolicyRule();
        var policy = new MonitorPolicy();
        alert.setAlertCondition(alertCondition);
        alertCondition.setRule(rule);
        rule.setName(ruleName);
        policy.setName(policyName);
        rule.setPolicy(policy);
        return alert;
    }

    @AfterEach
    public void afterTest() throws InterruptedException {
        verifyNoMoreInteractions(mockAlertService);
        verifyNoMoreInteractions(spyInterceptor);
        reset(mockAlertService, spyInterceptor);
        channel.shutdownNow();
        channel.awaitTermination(10, TimeUnit.SECONDS);
        stopServer();
    }

    @Test
    void testListAlerts() throws VerificationException {
        Page<org.opennms.horizon.alertservice.db.entity.Alert> page = mock(Page.class);
        doReturn(Arrays.asList(alert1, alert2)).when(page).getContent();
        doReturn(page).when(mockAlertRepository).findBySeverityInAndLastEventTimeBetweenAndTenantId(any(), any(), any(), any(), any());
        when(mockAlertMapper.toProto(any(org.opennms.horizon.alertservice.db.entity.Alert.class))).thenReturn(alertProto1, alertProto2);
        when(mockAlertMapper.toProto(any(org.opennms.horizon.alertservice.db.entity.Alert.class))).thenReturn(alertProto1, alertProto2);
        ListAlertsResponse result = stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createHeaders()))
            .listAlerts(ListAlertsRequest.newBuilder().build());
        assertThat(result.getAlertsList().size()).isEqualTo(2);
        ArgumentCaptor<PageRequest> pageRequestCaptor = ArgumentCaptor.forClass(PageRequest.class);
        verify(mockAlertRepository).findBySeverityInAndLastEventTimeBetweenAndTenantId(any(), any(), any(), pageRequestCaptor.capture(), any());
        verify(spyInterceptor).verifyAccessToken(authHeader);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));

        assertThat(pageRequestCaptor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(pageRequestCaptor.getValue().getPageSize()).isEqualTo(10);
        assertThat(pageRequestCaptor.getValue().getSort().getOrderFor("id").getDirection()).isEqualTo(org.springframework.data.domain.Sort.Direction.DESC);
    }

    @Test
    void testListAlertsWithNodes() throws VerificationException {
        Page<org.opennms.horizon.alertservice.db.entity.Alert> page = mock(Page.class);
        doReturn(Arrays.asList(alert1, alert2)).when(page).getContent();
        doReturn(page).when(mockAlertRepository).findBySeverityInAndLastEventTimeBetweenAndManagedObjectTypeAndManagedObjectInstanceInAndTenantId(any(), any(), any(), any(), any(), any(), any());
        doReturn(Arrays.asList(mock(Node.class))).when(mockNodeRepository).findAllByNodeLabelAndTenantId(any(), any());
        when(mockAlertMapper.toProto(any(org.opennms.horizon.alertservice.db.entity.Alert.class))).thenReturn(alertProto1, alertProto2);
        when(mockAlertMapper.toProto(any(org.opennms.horizon.alertservice.db.entity.Alert.class))).thenReturn(alertProto1, alertProto2);
        ListAlertsResponse result = stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createHeaders()))
            .listAlerts(ListAlertsRequest.newBuilder().addFilters(Filter.newBuilder().setNodeLabel("label").build()).build());
        assertThat(result.getAlertsList().size()).isEqualTo(2);
        ArgumentCaptor<PageRequest> pageRequestCaptor = ArgumentCaptor.forClass(PageRequest.class);
        verify(mockAlertRepository).findBySeverityInAndLastEventTimeBetweenAndManagedObjectTypeAndManagedObjectInstanceInAndTenantId(any(), any(), any(), any(), any(), pageRequestCaptor.capture(), any());
        verify(spyInterceptor).verifyAccessToken(authHeader);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));

        assertThat(pageRequestCaptor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(pageRequestCaptor.getValue().getPageSize()).isEqualTo(10);
        assertThat(pageRequestCaptor.getValue().getSort().getOrderFor("id").getDirection()).isEqualTo(org.springframework.data.domain.Sort.Direction.DESC);
    }
}
