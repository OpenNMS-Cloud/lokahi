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
package org.opennms.horizon.inventory.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.protobuf.Int64Value;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.opennms.horizon.alerts.proto.MonitorPolicyProto;
import org.opennms.horizon.alerts.proto.MonitorPolicyServiceGrpc;

class AlertClientTest {
    public static final GrpcCleanupRule grpcCleanUp = new GrpcCleanupRule();

    private static AlertClient client;
    private static MonitorPolicyServiceGrpc.MonitorPolicyServiceImplBase mockMonitorPolicyService;

    @BeforeAll
    public static void startGrpc() throws IOException {
        mockMonitorPolicyService = mock(
                MonitorPolicyServiceGrpc.MonitorPolicyServiceImplBase.class,
                delegatesTo(new MonitorPolicyServiceGrpc.MonitorPolicyServiceImplBase() {
                    @Override
                    public void getPolicyById(Int64Value request, StreamObserver<MonitorPolicyProto> responseObserver) {
                        responseObserver.onNext(MonitorPolicyProto.newBuilder()
                                .setId(request.getValue())
                                .build());
                        responseObserver.onCompleted();
                    }
                }));

        when(mockMonitorPolicyService.bindService()).thenCallRealMethod();

        grpcCleanUp.register(InProcessServerBuilder.forName("AlertClientTest")
                .addService(mockMonitorPolicyService)
                .directExecutor()
                .build()
                .start());
        ManagedChannel channel = grpcCleanUp.register(InProcessChannelBuilder.forName("AlertClientTest")
                .directExecutor()
                .build());
        client = new AlertClient(channel, 5000);
        client.initialStubs();
    }

    @AfterEach
    public void afterTest() {
        verifyNoMoreInteractions(mockMonitorPolicyService);
        reset(mockMonitorPolicyService);
    }

    @Test
    void testGetPolicyById() {
        long policyId = 100L;
        ArgumentCaptor<Int64Value> captor = ArgumentCaptor.forClass(Int64Value.class);
        MonitorPolicyProto result = client.getPolicyById(policyId, "tenantId");
        assertThat(result).isNotNull();
        verify(mockMonitorPolicyService).bindService();
        verify(mockMonitorPolicyService).getPolicyById(captor.capture(), any());
        assertThat(captor.getValue().getValue()).isEqualTo(policyId);
    }
}
