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
import com.google.protobuf.Empty;
import com.google.protobuf.Int64Value;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.stub.MetadataUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.common.VerificationException;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opennms.horizon.alerts.proto.MonitorPolicyProto;
import org.opennms.horizon.alerts.proto.MonitorPolicyServiceGrpc;
import org.opennms.horizon.alertservice.db.entity.Alert;
import org.opennms.horizon.alertservice.db.entity.AlertCondition;
import org.opennms.horizon.alertservice.db.entity.MonitorPolicy;
import org.opennms.horizon.alertservice.db.entity.PolicyRule;
import org.opennms.horizon.alertservice.db.entity.SystemPolicyTag;
import org.opennms.horizon.alertservice.db.entity.Tag;
import org.opennms.horizon.alertservice.db.repository.AlertDefinitionRepository;
import org.opennms.horizon.alertservice.db.repository.AlertRepository;
import org.opennms.horizon.alertservice.db.repository.MonitorPolicyRepository;
import org.opennms.horizon.alertservice.db.repository.PolicyRuleRepository;
import org.opennms.horizon.alertservice.db.repository.SystemPolicyTagRepository;
import org.opennms.horizon.alertservice.db.repository.TagRepository;
import org.opennms.horizon.alertservice.db.tenant.GrpcTenantLookupImpl;
import org.opennms.horizon.alertservice.db.tenant.TenantLookup;
import org.opennms.horizon.alertservice.mapper.MonitorPolicyMapper;
import org.opennms.horizon.alertservice.mapper.MonitorPolicyMapperImpl;
import org.opennms.horizon.alertservice.service.MonitorPolicyService;
import org.opennms.horizon.alertservice.service.routing.TagOperationProducer;
import org.opennms.horizon.shared.common.tag.proto.Operation;
import org.opennms.horizon.shared.common.tag.proto.TagOperationList;
import org.opennms.horizon.shared.common.tag.proto.TagOperationProto;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.opennms.horizon.alertservice.service.MonitorPolicyService.DEFAULT_POLICY;
import static org.opennms.horizon.alertservice.service.MonitorPolicyService.SYSTEM_TENANT;

@ExtendWith(MockitoExtension.class)
class MonitoringPolicyGrpcTest extends AbstractGrpcUnitTest {
    private MonitorPolicyServiceGrpc.MonitorPolicyServiceBlockingStub stub;
    private MonitorPolicyService spyMonitorPolicyService;
    private MonitorPolicyMapper policyMapper = new MonitorPolicyMapperImpl();
    private MonitorPolicyRepository mockMonitorPolicyRepository;
    private SystemPolicyTagRepository mockSystemPolicyTagRepository;
    private PolicyRuleRepository mockPolicyRuleRepository;
    private AlertDefinitionRepository mockAlertDefinitionRepo;
    private AlertRepository mockAlertRepository;
    private TagRepository mockTagRepository;
    private TagOperationProducer mockTagOperationProducer;
    private Alert alert1, alert2;
    private ManagedChannel channel;
    protected TenantLookup tenantLookup = new GrpcTenantLookupImpl();

    @Captor
    private ArgumentCaptor<Tag> tagCaptor;
    @Captor
    private ArgumentCaptor<SystemPolicyTag.RelationshipId> systemPolicyTagIdCaptor;
    @Captor
    ArgumentCaptor<SystemPolicyTag> systemPolicyTagCaptor;
    @Captor
    ArgumentCaptor<TagOperationList> tagOperationListCaptor;

    @BeforeEach
    void prepareTest() throws VerificationException, IOException {
        mockMonitorPolicyRepository = mock(MonitorPolicyRepository.class);
        mockPolicyRuleRepository = mock(PolicyRuleRepository.class);
        mockAlertDefinitionRepo = mock(AlertDefinitionRepository.class);
        mockAlertRepository = mock(AlertRepository.class);
        mockTagRepository = mock(TagRepository.class);
        mockTagOperationProducer = mock(TagOperationProducer.class);
        mockSystemPolicyTagRepository = mock(SystemPolicyTagRepository.class);

        spyMonitorPolicyService = spy(new MonitorPolicyService(policyMapper, mockMonitorPolicyRepository,
            mockSystemPolicyTagRepository, mockPolicyRuleRepository, mockAlertDefinitionRepo, mockAlertRepository, mockTagRepository, mockTagOperationProducer));
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
        verifyNoMoreInteractions(spyMonitorPolicyService, mockPolicyRuleRepository, mockMonitorPolicyRepository, spyInterceptor
            , mockSystemPolicyTagRepository, mockTagOperationProducer);

        reset(spyMonitorPolicyService, spyInterceptor);
        channel.shutdownNow();
        channel.awaitTermination(10, TimeUnit.SECONDS);
        stopServer();
    }

    @Test
    void testGetDefaultPolicyTagWithoutCustomTag() throws VerificationException {
        // prepare
        long policyId = 1L;
        var defaultPolicy = generateDefaultPolicy(policyId, List.of("default"));
        doReturn(Optional.of(defaultPolicy)).when(mockMonitorPolicyRepository).findByNameAndTenantId(DEFAULT_POLICY, SYSTEM_TENANT);
        doReturn(new HashSet<>()).when(mockSystemPolicyTagRepository).findByTenantIdAndPolicyId(tenantId, policyId);

        // execute
        var result = stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createHeaders()))
            .getDefaultPolicy(Empty.newBuilder().build());

        // check
        verify(spyMonitorPolicyService, times(1)).getDefaultPolicyProto(tenantId);
        assertThat(result.getTagsCount()).isEqualTo(1);
        assertThat(result.getTagsList().get(0)).isEqualTo("default");
        verify(spyInterceptor).verifyAccessToken(authHeader);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    @Test
    void testGetDefaultPolicyTagWithCustomTag() throws VerificationException {
        // prepare
        long policyId = 2L;
        var defaultPolicy = generateDefaultPolicy(policyId, List.of("default"));
        var tag1 = new Tag();
        tag1.setName("tag1");
        tag1.setTenantId(tenantId);
        var systemPolicyTags = new HashSet<SystemPolicyTag>();
        systemPolicyTags.add(new SystemPolicyTag(tenantId, policyId, tag1));

        doReturn(Optional.of(defaultPolicy)).when(mockMonitorPolicyRepository).findByNameAndTenantId(DEFAULT_POLICY, SYSTEM_TENANT);
        doReturn(systemPolicyTags).when(mockSystemPolicyTagRepository).findByTenantIdAndPolicyId(tenantId, policyId);

        // execute
        var result = stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createHeaders()))
            .getDefaultPolicy(Empty.newBuilder().build());

        // check
        verify(spyMonitorPolicyService, times(1)).getDefaultPolicyProto(tenantId);
        assertThat(result.getTagsCount()).isEqualTo(1);
        assertThat(result.getTagsList()).hasSameElementsAs(List.of("tag1"));
        verify(spyInterceptor).verifyAccessToken(authHeader);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    @Test
    void testUpdateDefaultPolicyTagWithDefaultTag() throws VerificationException {
        // prepare
        final long policyId = 10L;
        final String defaultTagName = "default";

        var defaultPolicy = generateDefaultPolicy(policyId, List.of(defaultTagName));

        doReturn(Optional.of(defaultPolicy)).when(mockMonitorPolicyRepository).findByNameAndTenantId(DEFAULT_POLICY, SYSTEM_TENANT);

        var tag = new Tag();
        tag.setName("tag");
        tag.setTenantId(tenantId);
        var customDefaultTag = new Tag();
        customDefaultTag.setName(defaultTagName);
        customDefaultTag.setTenantId(tenantId);

        var systemPolicyTag = new SystemPolicyTag(tenantId, policyId, tag);
        var systemPolicyDefaultTag = new SystemPolicyTag(tenantId, policyId, customDefaultTag);
        doReturn(new HashSet<>()).when(mockSystemPolicyTagRepository).findByTenantIdAndPolicyId(tenantId, policyId);
        when(mockSystemPolicyTagRepository.save(any(SystemPolicyTag.class))).thenAnswer(i -> {
            var inPolicyTag = (SystemPolicyTag) i.getArgument(0);
            if (systemPolicyTag.getTag().getName().equals(inPolicyTag.getTag().getName())) {
                return systemPolicyTag;
            } else if (inPolicyTag.getTag().getName().equals(defaultTagName)) {
                return systemPolicyDefaultTag;
            } else {
                return null;
            }
        });

        doReturn(Optional.empty()).when(mockTagRepository).findByTenantIdAndName(any(), any());

        when(mockTagRepository.save(any(Tag.class))).thenAnswer(i -> {
            var inTag = (Tag) i.getArgument(0);
            if (tag.getName().equals(inTag.getName())) {
                return tag;
            } else if ("default".equals(inTag.getName())) {
                return customDefaultTag;
            } else {
                return null;
            }
        });

        // execute
        var requestPolicy = MonitorPolicyProto.newBuilder()
            .setName(DEFAULT_POLICY)
            .setId(1L)
            .addTags("tag")
            .addTags(defaultTagName)
            .build();
        var result = stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createHeaders()))
            .createPolicy(requestPolicy);

        // check
        verify(spyMonitorPolicyService).createPolicy(requestPolicy, tenantId);
        assertThat(result.getTagsCount()).isEqualTo(2);
        assertThat(result.getTagsList()).hasSameElementsAs(List.of(defaultTagName, tag.getName()));

        verify(mockTagRepository, times(2)).save(tagCaptor.capture());
        var savedTags = tagCaptor.getAllValues();
        assertThat(savedTags.get(0).getName()).isEqualTo(tag.getName());
        assertThat(savedTags.get(0).getTenantId()).isEqualTo(tag.getTenantId());
        assertThat(savedTags.get(1).getName()).isEqualTo(defaultTagName);
        assertThat(savedTags.get(1).getTenantId()).isEqualTo(tag.getTenantId());

        var operation = TagOperationList.newBuilder()
            .addTags(TagOperationProto.newBuilder().setTenantId(tenantId).setTagName("tag"))
            .build();
        verify(mockSystemPolicyTagRepository).deleteEmptyTagByTenantIdAndPolicyId(tenantId, defaultPolicy.getId());
        verify(mockTagOperationProducer).sendTagUpdate(tagOperationListCaptor.capture());
        assertThat(tagOperationListCaptor.getValue().getTagsList()).hasSameElementsAs(operation.getTagsList());
        verify(spyInterceptor).verifyAccessToken(authHeader);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    @Test
    void testDeleteDefaultPolicyTagWithDefaultTag() throws VerificationException {
        // prepare
        final long policyId = 10L;
        final String defaultTagName = "default";

        var defaultPolicy = generateDefaultPolicy(policyId, List.of(defaultTagName));

        doReturn(Optional.of(defaultPolicy)).when(mockMonitorPolicyRepository).findByNameAndTenantId(DEFAULT_POLICY, SYSTEM_TENANT);
        doReturn(new HashSet<>()).when(mockSystemPolicyTagRepository).findByTenantIdAndPolicyId(tenantId, policyId);

        // execute
        var requestPolicy = MonitorPolicyProto.newBuilder()
            .setName(DEFAULT_POLICY)
            .build();
        var result = stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createHeaders()))
            .createPolicy(requestPolicy);

        // check
        verify(spyMonitorPolicyService).createPolicy(requestPolicy, tenantId);
        assertThat(result.getTagsList()).isEmpty();

        var operation = TagOperationList.newBuilder()
            .addTags(TagOperationProto.newBuilder().setTenantId(tenantId).setTagName("default").setOperation(Operation.REMOVE_TAG))
            .build();
        verify(mockSystemPolicyTagRepository).save(systemPolicyTagCaptor.capture());
        assertThat(systemPolicyTagCaptor.getValue().getPolicyId()).isEqualTo(defaultPolicy.getId());
        assertThat(systemPolicyTagCaptor.getValue().getTenantId()).isEqualTo(tenantId);
        assertThat(systemPolicyTagCaptor.getValue().getTag()).isNull();
        verify(mockTagOperationProducer).sendTagUpdate(operation);
        verify(spyInterceptor).verifyAccessToken(authHeader);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    @Test
    void testUpdateDefaultPolicyTag() throws VerificationException {
        // prepare
        long policyId = 10L;
        var defaultPolicy = generateDefaultPolicy(policyId, new ArrayList<>(0));
        var tags = new HashSet<Tag>();
        var tag1 = new Tag();
        tag1.setName("tag1");
        tag1.setTenantId(tenantId);
        var tag2 = new Tag();
        tag2.setName("tag2");
        tag2.setTenantId(tenantId);
        tags.add(tag1);
        tags.add(tag2);
        defaultPolicy.setTags(tags);

        doReturn(Optional.of(defaultPolicy)).when(mockMonitorPolicyRepository).findByNameAndTenantId(DEFAULT_POLICY, SYSTEM_TENANT);
        var systemPolicyTags = new HashSet<SystemPolicyTag>();
        systemPolicyTags.add(new SystemPolicyTag(tenantId, policyId, tag1));
        systemPolicyTags.add(new SystemPolicyTag(tenantId, policyId, tag2));

        var tag3 = new Tag();
        tag3.setName("tag3");
        tag3.setTenantId(tenantId);
        var systemPolicyTag3 = new SystemPolicyTag(tenantId, policyId, tag3);
        doReturn(systemPolicyTags).when(mockSystemPolicyTagRepository).findByTenantIdAndPolicyId(tenantId, policyId);
        when(mockSystemPolicyTagRepository.save(any(SystemPolicyTag.class))).thenAnswer(i -> {
            var inPolicyTag = (SystemPolicyTag) i.getArgument(0);
            if (systemPolicyTag3.getTag().getName().equals(inPolicyTag.getTag().getName()))
                return systemPolicyTag3;
            else
                return null;
        });

        doReturn(Optional.of(tag1)).when(mockTagRepository).findByTenantIdAndName(tenantId, tag1.getName());
        doReturn(Optional.empty()).when(mockTagRepository).findByTenantIdAndName(tenantId, tag3.getName());

        when(mockTagRepository.save(any(Tag.class))).thenAnswer(i -> {
            var inTag = (Tag) i.getArgument(0);
            if (tag3.getName().equals(inTag.getName())) {
                return tag3;
            } else {
                return null;
            }
        });

        // execute
        var requestPolicy = MonitorPolicyProto.newBuilder()
            .setName(DEFAULT_POLICY)
            .addTags("tag1")
            .addTags("tag3")
            .build();
        var result = stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createHeaders()))
            .createPolicy(requestPolicy);

        // check
        verify(spyMonitorPolicyService).createPolicy(requestPolicy, tenantId);
        assertThat(result.getTagsCount()).isEqualTo(2);
        assertThat(result.getTagsList()).hasSameElementsAs(requestPolicy.getTagsList());

        verify(mockTagRepository).save(tagCaptor.capture());
        assertThat(tagCaptor.getValue().getName()).isEqualTo(tag3.getName());
        assertThat(tagCaptor.getValue().getTenantId()).isEqualTo(tag3.getTenantId());
        verify(mockSystemPolicyTagRepository).deleteById(systemPolicyTagIdCaptor.capture());
        assertThat(systemPolicyTagIdCaptor.getValue().getPolicyId()).isEqualTo(policyId);
        assertThat(systemPolicyTagIdCaptor.getValue().getTag().getName()).isEqualTo(tag2.getName());
        assertThat(systemPolicyTagIdCaptor.getValue().getTag().getTenantId()).isEqualTo(tag2.getTenantId());

        var operation = TagOperationList.newBuilder().addTags(TagOperationProto.newBuilder().setTenantId(tenantId).setTagName("tag3"))
            .addTags(TagOperationProto.newBuilder().setTenantId(tenantId).setTagName("tag2").setOperation(Operation.REMOVE_TAG))
            .build();
        verify(mockSystemPolicyTagRepository).deleteEmptyTagByTenantIdAndPolicyId(tenantId, defaultPolicy.getId());
        verify(mockTagOperationProducer).sendTagUpdate(operation);
        verify(spyInterceptor).verifyAccessToken(authHeader);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }


    @Test
    void testInvalidPolicyId() throws VerificationException {
        // execute
        var requestPolicy = MonitorPolicyProto.newBuilder()
            .setId(100)
            .setTenantId(tenantId)
            .build();
        StatusRuntimeException thrown = Assertions.assertThrows(StatusRuntimeException.class, () -> {
            stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createHeaders()))
                .createPolicy(requestPolicy);
        });

        Assertions.assertEquals(String.format("INVALID_ARGUMENT: policy not found by id %s for tenant %s", 100, tenantId), thrown.getMessage());
        verify(spyMonitorPolicyService, times(1)).createPolicy(any(), eq(tenantId));
        verify(mockMonitorPolicyRepository, times(1)).findByIdAndTenantId(any(),eq(tenantId));
        verify(spyInterceptor).verifyAccessToken(authHeader);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
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
    void testDeleteDefaultPolicy() throws VerificationException {
        var stubWithInterceptors = stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createHeaders(authHeaderSystem)));
        var policyId = Int64Value.of(10);
        StatusRuntimeException thrown = Assertions.assertThrows(StatusRuntimeException.class, () -> {
            stubWithInterceptors.deletePolicyById(policyId);
        });

        Assertions.assertEquals(String.format("INTERNAL: Policy with tenantId %s is not allowed to delete.",
            SYSTEM_TENANT), thrown.getMessage());
        verify(spyMonitorPolicyService).deletePolicyById(10L, SYSTEM_TENANT);
        verify(spyInterceptor).verifyAccessToken(authHeaderSystem);
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
    void testDeleteDefaultPolicyRule() throws VerificationException {
        var stubWithInterceptors = stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createHeaders(authHeaderSystem)));
        var policyId = Int64Value.of(10);
        StatusRuntimeException thrown = Assertions.assertThrows(StatusRuntimeException.class, () -> {
            stubWithInterceptors.deleteRuleById(policyId);
        });

        Assertions.assertEquals(String.format("INTERNAL: Rule with tenantId %s is not allowed to delete.",
            SYSTEM_TENANT), thrown.getMessage());
        verify(spyMonitorPolicyService).deleteRuleById(10L, SYSTEM_TENANT);
        verify(spyInterceptor).verifyAccessToken(authHeaderSystem);
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


    MonitorPolicy generateDefaultPolicy(long policyId, List<String> tagNames) {
        Objects.requireNonNull(tagNames);
        var defaultPolicy = new MonitorPolicy();
        var tags = new HashSet<Tag>();
        tagNames.forEach(tagName -> {
            var tag = new Tag();
            tag.setName(tagName);
            tag.setTenantId(SYSTEM_TENANT);
            tags.add(tag);
        });
        defaultPolicy.setName(DEFAULT_POLICY);
        defaultPolicy.setId(policyId);
        defaultPolicy.setTags(tags);
        return defaultPolicy;
    }
}
