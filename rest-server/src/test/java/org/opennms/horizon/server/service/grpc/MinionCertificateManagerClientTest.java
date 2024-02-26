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
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import java.io.IOException;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.opennms.horizon.minioncertmanager.proto.EmptyResponse;
import org.opennms.horizon.minioncertmanager.proto.GetMinionCertificateResponse;
import org.opennms.horizon.minioncertmanager.proto.MinionCertificateManagerGrpc;
import org.opennms.horizon.minioncertmanager.proto.MinionCertificateRequest;
import org.opennms.horizon.shared.constants.GrpcConstants;

public class MinionCertificateManagerClientTest {
    @Rule
    public static final GrpcCleanupRule grpcCleanUp = new GrpcCleanupRule();

    private static MinionCertificateManagerClient client;
    private static MockServerInterceptor mockInterceptor;
    private static MinionCertificateManagerGrpc.MinionCertificateManagerImplBase mockMCMService;
    private final String accessToken = "test-token";

    @BeforeAll
    public static void startGrpc() throws IOException {
        mockInterceptor = new MockServerInterceptor();

        mockMCMService = mock(
                MinionCertificateManagerGrpc.MinionCertificateManagerImplBase.class,
                delegatesTo(new MinionCertificateManagerGrpc.MinionCertificateManagerImplBase() {
                    @Override
                    public void getMinionCert(
                            MinionCertificateRequest request,
                            StreamObserver<GetMinionCertificateResponse> responseObserver) {
                        responseObserver.onNext(GetMinionCertificateResponse.newBuilder()
                                .setCertificate(ByteString.copyFromUtf8("test"))
                                .setPassword("password")
                                .build());
                        responseObserver.onCompleted();
                    }

                    @Override
                    public void revokeMinionCert(
                            MinionCertificateRequest request, StreamObserver<EmptyResponse> responseObserver) {
                        responseObserver.onNext(EmptyResponse.newBuilder().build());
                        responseObserver.onCompleted();
                    }
                }));

        grpcCleanUp.register(InProcessServerBuilder.forName("MinionCertificateManagerClientTest")
                .intercept(mockInterceptor)
                .addService(mockMCMService)
                .directExecutor()
                .build()
                .start());
        ManagedChannel channel =
                grpcCleanUp.register(InProcessChannelBuilder.forName("MinionCertificateManagerClientTest")
                        .directExecutor()
                        .build());
        client = new MinionCertificateManagerClient(channel, 1000L);
        client.initialStubs();
    }

    @AfterEach
    public void afterTest() {
        verifyNoMoreInteractions(mockMCMService);
        reset(mockMCMService);
        mockInterceptor.reset();
    }

    @Test
    void testGetMinionCert() {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        ArgumentCaptor<MinionCertificateRequest> captor = ArgumentCaptor.forClass(MinionCertificateRequest.class);
        GetMinionCertificateResponse result = client.getMinionCert("tenantId", 333L, accessToken + methodName);
        Assertions.assertFalse(result.getPassword().isEmpty());
        verify(mockMCMService).getMinionCert(captor.capture(), any());
        assertThat(captor.getValue()).isNotNull();
        assertThat(mockInterceptor.getAuthHeader()).isEqualTo(accessToken + methodName);
    }

    @Test
    void testRevoke() {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        ArgumentCaptor<MinionCertificateRequest> captor = ArgumentCaptor.forClass(MinionCertificateRequest.class);
        client.revokeCertificate("tenantId", 333L, accessToken + methodName);
        verify(mockMCMService).revokeMinionCert(captor.capture(), any());
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
