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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.opennms.horizon.alerts.proto.AlertRequest;
import org.opennms.horizon.alerts.proto.AlertResponse;
import org.opennms.horizon.alerts.proto.AlertServiceGrpc;
import org.opennms.horizon.alerts.proto.DeleteAlertResponse;
import org.opennms.horizon.alerts.proto.ListAlertsRequest;
import org.opennms.horizon.alerts.proto.ListAlertsResponse;
import org.opennms.horizon.server.mapper.alert.MonitorPolicyMapper;
import org.opennms.horizon.server.model.alerts.TimeRange;
import org.opennms.horizon.shared.constants.GrpcConstants;

import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;

public class AlertsClientTest {
    @Rule
    public static final GrpcCleanupRule grpcCleanUp = new GrpcCleanupRule();

    private static MonitorPolicyMapper policyMapper;
    private static AlertsClient client;
    private static MockServerInterceptor mockInterceptor;
    private static AlertServiceGrpc.AlertServiceImplBase mockAlertService;
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

        grpcCleanUp.register(InProcessServerBuilder.forName("AlertsClientTest").intercept(mockInterceptor)
            .addService(mockAlertService).directExecutor().build().start());
        ManagedChannel channel = grpcCleanUp.register(InProcessChannelBuilder.forName("AlertsClientTest").directExecutor().build());
        policyMapper = Mappers.getMapper(MonitorPolicyMapper.class);
        client = new AlertsClient(channel, 5000, policyMapper);
        client.initialStubs();
    }

    @AfterEach
    public void afterTest() {
        verifyNoMoreInteractions(mockAlertService);
        reset(mockAlertService);
        mockInterceptor.reset();
    }

    @Test
    public void testListAlerts() {
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
    public void testAcknowledgeAlert() {
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
    public void testUnacknowledgeAlert() {
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
    public void testClearAlert() {
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
    public void testEscalateAlert() {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        ArgumentCaptor<AlertRequest> captor = ArgumentCaptor.forClass(AlertRequest.class);
        AlertResponse result = client.escalateAlert(List.of(1L), accessToken + methodName);
        assertThat(result).isNotNull();
        verify(mockAlertService).escalateAlert(captor.capture(), any());
        assertThat(captor.getValue()).isNotNull();
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
