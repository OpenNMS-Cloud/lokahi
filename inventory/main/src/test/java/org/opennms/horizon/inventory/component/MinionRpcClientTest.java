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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertEquals;
import static org.opennms.horizon.inventory.TestConstants.PRIMARY_TENANT_ID;

import com.google.protobuf.Any;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opennms.cloud.grpc.minion_gateway.GatewayRpcRequestProto;
import org.opennms.cloud.grpc.minion_gateway.GatewayRpcResponseProto;
import org.opennms.cloud.grpc.minion_gateway.MinionIdentity;
import org.opennms.cloud.grpc.minion_gateway.RpcRequestServiceGrpc;
import org.opennms.horizon.grpc.echo.contract.EchoRequest;
import org.opennms.horizon.grpc.echo.contract.EchoResponse;
import org.opennms.horizon.shared.constants.GrpcConstants;

class MinionRpcClientTest {

    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    private MinionRpcClient client;
    private RpcRequestServiceGrpc.RpcRequestServiceImplBase testRequestService;
    private List<Pair<GatewayRpcRequestProto, StreamObserver<GatewayRpcResponseProto>>> receivedRequests;

    @BeforeEach
    public void setUp() throws IOException {
        testRequestService = new RpcRequestServiceGrpc.RpcRequestServiceImplBase() {
            @Override
            public void request(
                    GatewayRpcRequestProto request, StreamObserver<GatewayRpcResponseProto> responseObserver) {
                //     super.request(request, responseObserver);
                // }
                //
                // @Override
                // public void request(RpcRequestProto request, StreamObserver<RpcResponseProto> responseObserver) {
                receivedRequests.add(Pair.of(request, responseObserver));
                try {
                    EchoRequest echoRequest = request.getPayload().unpack(EchoRequest.class);
                    responseObserver.onNext(GatewayRpcResponseProto.newBuilder()
                            .setIdentity(request.getIdentity())
                            .setRpcId(request.getRpcId())
                            .setModuleId(request.getModuleId())
                            .setPayload(Any.pack(EchoResponse.newBuilder()
                                    .setTime(echoRequest.getTime())
                                    .build()))
                            .build());
                    responseObserver.onCompleted();
                } catch (Exception e) {
                    responseObserver.onError(new RuntimeException(e));
                }
            }
        };

        grpcCleanup.register(InProcessServerBuilder.forName(MinionRpcClientTest.class.getName())
                .addService(testRequestService)
                .directExecutor()
                .build()
                .start());
        ManagedChannel channel =
                grpcCleanup.register(InProcessChannelBuilder.forName(MinionRpcClientTest.class.getName())
                        .directExecutor()
                        .build());
        client = new MinionRpcClient(
                channel, (ctx) -> Optional.ofNullable(GrpcConstants.TENANT_ID_CONTEXT_KEY.get()), 5000);
        client.init();

        receivedRequests = new LinkedList<>();
    }

    @AfterEach
    public void afterTest() {
        client.shutdown();
    }

    @Test
    void testSentRpcRequest() throws Exception {
        EchoRequest echoRequest =
                EchoRequest.newBuilder().setTime(System.currentTimeMillis()).build();

        MinionIdentity minionIdentity = MinionIdentity.newBuilder()
                .setTenantId(PRIMARY_TENANT_ID)
                .setLocationId("test-location")
                .setSystemId("test-system")
                .build();

        GatewayRpcRequestProto request = GatewayRpcRequestProto.newBuilder()
                .setIdentity(minionIdentity)
                .setModuleId("test-rpc")
                .setRpcId(UUID.randomUUID().toString())
                .setPayload(Any.pack(echoRequest))
                .build();

        GatewayRpcResponseProto response =
                client.sendRpcRequest(PRIMARY_TENANT_ID, request).get();
        assertEquals(1, receivedRequests.size());
        assertThat(response.getIdentity().getTenantId())
                .isEqualTo(request.getIdentity().getTenantId());
        assertThat(response.getIdentity().getSystemId())
                .isEqualTo(request.getIdentity().getSystemId());
        assertThat(response.getIdentity().getLocationId())
                .isEqualTo(request.getIdentity().getLocationId());
        assertThat(response.getModuleId()).isEqualTo(request.getModuleId());
        assertThat(response.getRpcId()).isEqualTo(request.getRpcId());
        EchoResponse echoResponse = response.getPayload().unpack(EchoResponse.class);
        assertThat(echoResponse.getTime()).isEqualTo(echoRequest.getTime());
        assertThat(System.currentTimeMillis() - echoResponse.getTime()).isPositive();
    }
}
