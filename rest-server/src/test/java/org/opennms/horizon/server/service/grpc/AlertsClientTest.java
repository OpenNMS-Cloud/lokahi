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

package org.opennms.horizon.server.service.grpc;

import com.google.protobuf.BoolValue;
import com.google.protobuf.Int64Value;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.opennms.horizon.alerts.proto.AlertEventDefinitionServiceGrpc;
import org.opennms.horizon.alerts.proto.AlertRequest;
import org.opennms.horizon.alerts.proto.AlertResponse;
import org.opennms.horizon.alerts.proto.AlertServiceGrpc;
import org.opennms.horizon.alerts.proto.DeleteAlertResponse;
import org.opennms.horizon.alerts.proto.EventType;
import org.opennms.horizon.alerts.proto.ListAlertEventDefinitionsRequest;
import org.opennms.horizon.alerts.proto.ListAlertEventDefinitionsResponse;
import org.opennms.horizon.alerts.proto.ListAlertsRequest;
import org.opennms.horizon.alerts.proto.ListAlertsResponse;
import org.opennms.horizon.alerts.proto.MonitorPolicyServiceGrpc;
import org.opennms.horizon.server.mapper.alert.AlertEventDefinitionMapper;
import org.opennms.horizon.server.mapper.alert.AlertsCountMapper;
import org.opennms.horizon.server.mapper.alert.MonitorPolicyMapper;
import org.opennms.horizon.server.model.alerts.AlertEventDefinition;
import org.opennms.horizon.server.model.alerts.TimeRange;
import org.opennms.horizon.shared.constants.GrpcConstants;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class AlertsClientTest {
    @Rule
    public static final GrpcCleanupRule grpcCleanUp = new GrpcCleanupRule();

    private static MonitorPolicyMapper monitorPolicyMapper;

    private static AlertEventDefinitionMapper alertEventDefinitionMapper;
    private static AlertsCountMapper alertsCountMapper;
    private static AlertsClient client;
    private static MockServerInterceptor mockInterceptor;
    private static AlertServiceGrpc.AlertServiceImplBase mockAlertService;
    private static MonitorPolicyServiceGrpc.MonitorPolicyServiceImplBase mockMonitoringPolicyService;
    private static AlertEventDefinitionServiceGrpc.AlertEventDefinitionServiceImplBase mockAlertEventDefinitionService;
    private final String accessToken = "test-token";

    @BeforeAll
    public static void startGrpc() throws IOException {
        mockInterceptor = new MockServerInterceptor();

        mockAlertService = mock(AlertServiceGrpc.AlertServiceImplBase.class, delegatesTo(
            new AlertServiceGrpc.AlertServiceImplBase() {
                @Override
                public void listAlerts(ListAlertsRequest request, StreamObserver<ListAlertsResponse> responseObserver) {
                    responseObserver.onNext(ListAlertsResponse.newBuilder().build());
                    responseObserver.onCompleted();
                }

                @Override
                public void acknowledgeAlert(AlertRequest alertRequest, StreamObserver<AlertResponse> responseObserver) {
                    responseObserver.onNext(AlertResponse.newBuilder().build());
                    responseObserver.onCompleted();
                }

                @Override
                public void unacknowledgeAlert(AlertRequest alertRequest, StreamObserver<AlertResponse> responseObserver) {
                    responseObserver.onNext(AlertResponse.newBuilder().build());
                    responseObserver.onCompleted();
                }

                @Override
                public void clearAlert(AlertRequest alertRequest, StreamObserver<AlertResponse> responseObserver) {
                    responseObserver.onNext(AlertResponse.newBuilder().build());
                    responseObserver.onCompleted();
                }

                @Override
                public void escalateAlert(AlertRequest alertRequest, StreamObserver<AlertResponse> responseObserver) {
                    responseObserver.onNext(AlertResponse.newBuilder().build());
                    responseObserver.onCompleted();
                }

                @Override
                public void deleteAlert(AlertRequest alertRequest, StreamObserver<DeleteAlertResponse> responseObserver) {
                    responseObserver.onNext(DeleteAlertResponse.newBuilder().build());
                    responseObserver.onCompleted();
                }
            }));

        mockAlertEventDefinitionService = mock(AlertEventDefinitionServiceGrpc.AlertEventDefinitionServiceImplBase.class, delegatesTo(
            new AlertEventDefinitionServiceGrpc.AlertEventDefinitionServiceImplBase() {
                @Override
                public void listAlertEventDefinitions(ListAlertEventDefinitionsRequest request, StreamObserver<ListAlertEventDefinitionsResponse> responseObserver) {
                    responseObserver.onNext(ListAlertEventDefinitionsResponse.newBuilder().build());
                    responseObserver.onCompleted();
                }
            }));

        mockMonitoringPolicyService = mock(MonitorPolicyServiceGrpc.MonitorPolicyServiceImplBase.class, delegatesTo((
            new MonitorPolicyServiceGrpc.MonitorPolicyServiceImplBase(){
                @Override
                public void countAlertByPolicyId(Int64Value id, StreamObserver<Int64Value> responseObserver) {
                    responseObserver.onNext(Int64Value.of(10));
                    responseObserver.onCompleted();
                }

                @Override
                public void countAlertByRuleId(Int64Value id, StreamObserver<Int64Value> responseObserver) {
                    responseObserver.onNext(Int64Value.of(11));
                    responseObserver.onCompleted();
                }

                @Override
                public void deletePolicyById(Int64Value id, StreamObserver<BoolValue> responseObserver) {
                    responseObserver.onNext(BoolValue.of(true));
                    responseObserver.onCompleted();
                }

                @Override
                public void deleteRuleById(Int64Value id, StreamObserver<BoolValue> responseObserver) {
                    responseObserver.onNext(BoolValue.of(true));
                    responseObserver.onCompleted();
                }
            }
        )));

        grpcCleanUp.register(InProcessServerBuilder.forName("AlertsClientTest").intercept(mockInterceptor)
            .addService(mockAlertService).addService(mockAlertEventDefinitionService)
            .addService(mockMonitoringPolicyService).directExecutor().build().start());
        ManagedChannel channel = grpcCleanUp.register(InProcessChannelBuilder.forName("AlertsClientTest").directExecutor().build());
        monitorPolicyMapper = Mappers.getMapper(MonitorPolicyMapper.class);
        alertEventDefinitionMapper = Mappers.getMapper(AlertEventDefinitionMapper.class);
        client = new AlertsClient(channel, 5000, monitorPolicyMapper, alertEventDefinitionMapper, alertsCountMapper);
        client.initialStubs();
    }

    @AfterEach
    public void afterTest() {
        verifyNoMoreInteractions(mockAlertService, mockAlertEventDefinitionService);
        reset(mockAlertService, mockAlertEventDefinitionService);
        mockInterceptor.reset();
    }

    @Test
    void testListAlerts() {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        ArgumentCaptor<ListAlertsRequest> captor = ArgumentCaptor.forClass(ListAlertsRequest.class);
        ListAlertsResponse result = client.listAlerts(5, 0, Collections.emptyList(), TimeRange.TODAY, "tenantId", true, "node", accessToken + methodName);
        assertThat(result.getAlertsList().isEmpty()).isTrue();
        verify(mockAlertService).listAlerts(captor.capture(), any());
        assertThat(captor.getValue()).isNotNull();
        assertThat(mockInterceptor.getAuthHeader()).isEqualTo(accessToken + methodName);
    }

    @Test
    void testListAlertEventDefinitions() {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        ArgumentCaptor<ListAlertEventDefinitionsRequest> captor = ArgumentCaptor.forClass(ListAlertEventDefinitionsRequest.class);
        List<AlertEventDefinition> result = client.listAlertEventDefinitions(EventType.SNMP_TRAP, accessToken + methodName);
        assertThat(result.isEmpty()).isTrue();
        verify(mockAlertEventDefinitionService).listAlertEventDefinitions(captor.capture(), any());
        assertThat(captor.getValue()).isNotNull();
        assertThat(mockInterceptor.getAuthHeader()).isEqualTo(accessToken + methodName);
    }

    @Test
    void testAcknowledgeAlert() {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        ArgumentCaptor<AlertRequest> captor = ArgumentCaptor.forClass(AlertRequest.class);
        AlertResponse result = client.acknowledgeAlert(List.of(1L), accessToken + methodName);
        assertThat(result).isNotNull();
        verify(mockAlertService).acknowledgeAlert(captor.capture(), any());
        assertThat(captor.getValue()).isNotNull();
        assertThat(mockInterceptor.getAuthHeader()).isEqualTo(accessToken + methodName);
    }

    @Test
    void testUnacknowledgeAlert() {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        ArgumentCaptor<AlertRequest> captor = ArgumentCaptor.forClass(AlertRequest.class);
        AlertResponse result = client.unacknowledgeAlert(List.of(1L), accessToken + methodName);
        assertThat(result).isNotNull();
        verify(mockAlertService).unacknowledgeAlert(captor.capture(), any());
        assertThat(captor.getValue()).isNotNull();
        assertThat(mockInterceptor.getAuthHeader()).isEqualTo(accessToken + methodName);
    }

    @Test
    void testClearAlert() {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        ArgumentCaptor<AlertRequest> captor = ArgumentCaptor.forClass(AlertRequest.class);
        AlertResponse result = client.clearAlert(List.of(1L), accessToken + methodName);
        assertThat(result).isNotNull();
        verify(mockAlertService).clearAlert(captor.capture(), any());
        assertThat(captor.getValue()).isNotNull();
        assertThat(mockInterceptor.getAuthHeader()).isEqualTo(accessToken + methodName);
    }

    @Test
    void testEscalateAlert() {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        ArgumentCaptor<AlertRequest> captor = ArgumentCaptor.forClass(AlertRequest.class);
        AlertResponse result = client.escalateAlert(List.of(1L), accessToken + methodName);
        assertThat(result).isNotNull();
        verify(mockAlertService).escalateAlert(captor.capture(), any());
        assertThat(captor.getValue()).isNotNull();
        assertThat(mockInterceptor.getAuthHeader()).isEqualTo(accessToken + methodName);
    }

    @Test
    void testDeletePolicyById() {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        var captor = ArgumentCaptor.forClass(Int64Value.class);
        var result = client.deletePolicyById(1L, accessToken + methodName);
        assertThat(result).isTrue();
        verify(mockMonitoringPolicyService).deletePolicyById(captor.capture(), any());
        assertThat(captor.getValue().getValue()).isEqualTo(1L);
        assertThat(mockInterceptor.getAuthHeader()).isEqualTo(accessToken + methodName);
    }

    @Test
    void testDeleteRuleById() {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        var captor = ArgumentCaptor.forClass(Int64Value.class);
        var result = client.deleteRuleById(1L, accessToken + methodName);
        assertThat(result).isTrue();
        verify(mockMonitoringPolicyService).deleteRuleById(captor.capture(), any());
        assertThat(captor.getValue().getValue()).isEqualTo(1L);
        assertThat(mockInterceptor.getAuthHeader()).isEqualTo(accessToken + methodName);
    }

    @Test
    void testCountAlertByPolicyId() {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        var captor = ArgumentCaptor.forClass(Int64Value.class);
        var result = client.countAlertByPolicyId(1L, accessToken + methodName);
        assertThat(result).isEqualTo(10);
        verify(mockMonitoringPolicyService).countAlertByPolicyId(captor.capture(), any());
        assertThat(captor.getValue().getValue()).isEqualTo(1L);
        assertThat(mockInterceptor.getAuthHeader()).isEqualTo(accessToken + methodName);
    }

    @Test
    void testCountAlertByRuleId() {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        var captor = ArgumentCaptor.forClass(Int64Value.class);
        var result = client.countAlertByRuleId(2L, accessToken + methodName);
        assertThat(result).isEqualTo(11);
        verify(mockMonitoringPolicyService).countAlertByRuleId(captor.capture(), any());
        assertThat(captor.getValue().getValue()).isEqualTo(2L);
        assertThat(mockInterceptor.getAuthHeader()).isEqualTo(accessToken + methodName);
    }

    private static class MockServerInterceptor implements ServerInterceptor {
        private String authHeader;

        @Override
        public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
            authHeader = headers.get(GrpcConstants.AUTHORIZATION_METADATA_KEY);
            return next.startCall(call, headers);
        }

        public String getAuthHeader() {
            return authHeader;
        }

        public void reset() {
            authHeader = null;
        }
    }

}
