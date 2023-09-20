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

package org.opennms.horizon.alertservice.grpc;

import com.google.protobuf.BoolValue;
import com.google.protobuf.Int64Value;
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
import org.opennms.horizon.alerts.proto.MonitorPolicyServiceGrpc;
import org.opennms.horizon.alertservice.db.entity.Alert;
import org.opennms.horizon.alertservice.db.entity.AlertCondition;
import org.opennms.horizon.alertservice.db.entity.MonitorPolicy;
import org.opennms.horizon.alertservice.db.entity.PolicyRule;
import org.opennms.horizon.alertservice.db.repository.AlertConditionRepository;
import org.opennms.horizon.alertservice.db.repository.AlertDefinitionRepository;
import org.opennms.horizon.alertservice.db.repository.AlertRepository;
import org.opennms.horizon.alertservice.db.repository.EventDefinitionRepository;
import org.opennms.horizon.alertservice.db.repository.MonitorPolicyRepository;
import org.opennms.horizon.alertservice.db.repository.PolicyRuleRepository;
import org.opennms.horizon.alertservice.db.repository.TagRepository;
import org.opennms.horizon.alertservice.db.tenant.GrpcTenantLookupImpl;
import org.opennms.horizon.alertservice.db.tenant.TenantLookup;
import org.opennms.horizon.alertservice.mapper.EventDefinitionMapper;
import org.opennms.horizon.alertservice.mapper.MonitorPolicyMapper;
import org.opennms.horizon.alertservice.service.MonitorPolicyService;
import org.opennms.horizon.alertservice.service.routing.TagOperationProducer;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class MonitoringPolicyGrpcTest extends AbstractGrpcUnitTest {
    private MonitorPolicyServiceGrpc.MonitorPolicyServiceBlockingStub stub;
    private MonitorPolicyService spyMonitorPolicyService;
    private MonitorPolicyMapper mockPolicyMapper;
    private MonitorPolicyRepository mockMonitorPolicyRepository;
    private PolicyRuleRepository mockPolicyRuleRepository;
    private AlertDefinitionRepository mockAlertDefinitionRepo;
    private AlertRepository mockAlertRepository;
    private TagRepository mockTagRepository;
    private TagOperationProducer mockTagOperationProducer;
    private Alert alert1, alert2;
    private ManagedChannel channel;


    protected TenantLookup tenantLookup = new GrpcTenantLookupImpl();


    @BeforeEach
    public void prepareTest() throws VerificationException, IOException {
        mockPolicyMapper = mock(MonitorPolicyMapper.class);
        mockMonitorPolicyRepository = mock(MonitorPolicyRepository.class);
        mockPolicyRuleRepository = mock(PolicyRuleRepository.class);
        mockAlertDefinitionRepo = mock(AlertDefinitionRepository.class);
        mockAlertRepository = mock(AlertRepository.class);
        mockTagRepository = mock(TagRepository.class);
        mockTagOperationProducer = mock(TagOperationProducer.class);

        spyMonitorPolicyService = spy(new MonitorPolicyService(mockPolicyMapper, mockMonitorPolicyRepository,
            mockPolicyRuleRepository, mockAlertDefinitionRepo, mockAlertRepository, mockTagRepository, mockTagOperationProducer));
        MonitorPolicyGrpc grpcService = new MonitorPolicyGrpc(spyMonitorPolicyService, tenantLookup);
        startServer(grpcService);
        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
        stub = MonitorPolicyServiceGrpc.newBlockingStub(channel);
    }

    private Alert generateAlert(String ruleName, String policyName) {
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
        verifyNoMoreInteractions(spyMonitorPolicyService, mockPolicyRuleRepository, mockMonitorPolicyRepository, spyInterceptor);

        reset(spyMonitorPolicyService, spyInterceptor);
        channel.shutdownNow();
        channel.awaitTermination(10, TimeUnit.SECONDS);
        stopServer();
    }

    @Test
    void testDeleteAlertByPolicy() throws VerificationException {
        alert1 = generateAlert("rule1", "policy1");
        alert2 = generateAlert("rule2", "policy1");
        List<Alert> alerts = List.of(alert1, alert2);
        doReturn(alerts).when(mockAlertRepository).findByPolicyIdAndTenantId(10L, tenantId);

        BoolValue result = stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createHeaders()))
            .deletePolicyById(Int64Value.of(10));

        assertThat(result.getValue()).isTrue();
        verify(spyMonitorPolicyService).deletePolicyById(10L, tenantId);
        verify(mockAlertRepository).findByPolicyIdAndTenantId(10L, tenantId);
        verify(mockAlertRepository).deleteAll(alerts);
        verify(mockMonitorPolicyRepository).deleteByIdAndTenantId(10L, tenantId);

        verify(spyInterceptor).verifyAccessToken(authHeader);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    @Test
    void testDeleteAlertByRule() throws VerificationException {
        alert1 = generateAlert("rule1", "policy1");
        alert2 = generateAlert("rule2", "policy1");
        List<Alert> alerts = List.of(alert1, alert2);
        doReturn(alerts).when(mockAlertRepository).findByRuleIdAndTenantId(10L, tenantId);

        BoolValue result = stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createHeaders()))
            .deleteRuleById(Int64Value.of(10));

        assertThat(result.getValue()).isTrue();
        verify(spyMonitorPolicyService).deleteRuleById(10L, tenantId);
        verify(mockAlertRepository).findByRuleIdAndTenantId(10L, tenantId);
        verify(mockAlertRepository).deleteAll(alerts);
        verify(mockPolicyRuleRepository).deleteByIdAndTenantId(10L, tenantId);

        verify(spyInterceptor).verifyAccessToken(authHeader);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    @Test
    void testCountAlertByPolicy() throws VerificationException {
        doReturn(1L).when(mockAlertRepository).countByPolicyIdAndTenantId(10, tenantId);

        Int64Value result = stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createHeaders()))
            .countAlertByPolicyId(Int64Value.of(10));
        assertThat(result.getValue()).isEqualTo(1);
        verify(spyMonitorPolicyService).countAlertByPolicyId(10, tenantId);
        verify(spyInterceptor).verifyAccessToken(authHeader);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    @Test
    void testCountAlertByRule() throws VerificationException {
        doReturn(1L).when(mockAlertRepository).countByRuleIdAndTenantId(10, tenantId);

        Int64Value result = stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createHeaders()))
            .countAlertByRuleId(Int64Value.of(10));
        assertThat(result.getValue()).isEqualTo(1);
        verify(spyMonitorPolicyService).countAlertByRuleId(10, tenantId);
        verify(spyInterceptor).verifyAccessToken(authHeader);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }
}
