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
package org.opennms.horizon.server.service.grpc;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.opennms.horizon.alerts.proto.AlertServiceGrpc;
import org.opennms.horizon.alerts.proto.ListAlertsRequest;
import org.opennms.horizon.alerts.proto.ListAlertsResponse;
import org.opennms.horizon.server.mapper.alert.AlertEventDefinitionMapper;
import org.opennms.horizon.server.mapper.alert.AlertsCountMapper;
import org.opennms.horizon.server.mapper.alert.EventDefinitionByVendorMapper;
import org.opennms.horizon.server.mapper.alert.MonitorPolicyMapper;
import org.opennms.horizon.server.model.alerts.TimeRange;
import org.opennms.horizon.shared.constants.GrpcConstants;

public class AlertsClientDeadlineTest {
    @Rule
    public static final GrpcCleanupRule grpcCleanUp = new GrpcCleanupRule();

    private static MonitorPolicyMapper monitorPolicyMapper;

    private static AlertEventDefinitionMapper alertEventDefinitionMapper;
    private static AlertsClient client;
    private static MockServerInterceptor mockInterceptor;
    private static AlertServiceGrpc.AlertServiceImplBase mockAlertService;
    private final String accessToken = "test-token";
    private static AlertsCountMapper alertsCountMapper;

    private static EventDefinitionByVendorMapper eventDefinitionByVendorMapper;

    @BeforeAll
    public static void startGrpc() throws IOException {
        mockInterceptor = new MockServerInterceptor();

        mockAlertService = mock(
                AlertServiceGrpc.AlertServiceImplBase.class, delegatesTo(new AlertServiceGrpc.AlertServiceImplBase() {
                    @Override
                    public void listAlerts(
                            ListAlertsRequest request, StreamObserver<ListAlertsResponse> responseObserver) {
                        CompletableFuture.delayedExecutor(2000, TimeUnit.MILLISECONDS)
                                .execute(() -> {
                                    responseObserver.onNext(
                                            ListAlertsResponse.newBuilder().build());
                                    responseObserver.onCompleted();
                                });
                    }
                }));

        grpcCleanUp.register(InProcessServerBuilder.forName("AlertsClientDeadlineTest")
                .intercept(mockInterceptor)
                .addService(mockAlertService)
                .directExecutor()
                .build()
                .start());
        ManagedChannel channel = grpcCleanUp.register(InProcessChannelBuilder.forName("AlertsClientDeadlineTest")
                .directExecutor()
                .build());
        monitorPolicyMapper = Mappers.getMapper(MonitorPolicyMapper.class);
        client = new AlertsClient(
                channel,
                1000,
                monitorPolicyMapper,
                alertEventDefinitionMapper,
                alertsCountMapper,
                eventDefinitionByVendorMapper);
        client.initialStubs();
    }

    @AfterEach
    public void afterTest() {
        verifyNoMoreInteractions(mockAlertService);
        reset(mockAlertService);
        mockInterceptor.reset();
    }

    @Test
    void testListAlerts() {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        ArgumentCaptor<ListAlertsRequest> captor = ArgumentCaptor.forClass(ListAlertsRequest.class);
        StatusRuntimeException thrown = assertThrows(
                StatusRuntimeException.class,
                () -> client.listAlerts(
                        5,
                        0,
                        Collections.singletonList("CRITICAL"),
                        TimeRange.TODAY,
                        "tenantId",
                        true,
                        "node",
                        accessToken + methodName),
                "Expected listLocations() to throw, but it didn't");
        assertThat(thrown.getStatus().getCode()).isEqualTo(Status.Code.DEADLINE_EXCEEDED);
        verify(mockAlertService).listAlerts(captor.capture(), any());
        assertThat(captor.getValue()).isNotNull();
        assertThat(mockInterceptor.getAuthHeader()).isEqualTo(accessToken + methodName);
    }

    private static class MockServerInterceptor implements ServerInterceptor {
        private String authHeader;

        @Override
        public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
                ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
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
