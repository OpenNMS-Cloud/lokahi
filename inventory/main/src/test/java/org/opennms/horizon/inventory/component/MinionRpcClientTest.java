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
import static org.junit.Assert.assertEquals;
import static org.opennms.horizon.inventory.TestConstants.PRIMARY_TENANT_ID;

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
import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcResponseProto;
import org.opennms.cloud.grpc.minion_gateway.GatewayRpcRequestProto;
import org.opennms.cloud.grpc.minion_gateway.GatewayRpcResponseProto;
import org.opennms.cloud.grpc.minion_gateway.MinionIdentity;
import org.opennms.cloud.grpc.minion_gateway.RpcRequestServiceGrpc;
import org.opennms.horizon.grpc.echo.contract.EchoRequest;
import org.opennms.horizon.grpc.echo.contract.EchoResponse;
import org.opennms.horizon.shared.constants.GrpcConstants;

import com.google.protobuf.Any;

import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;

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
            public void request(GatewayRpcRequestProto request, StreamObserver<GatewayRpcResponseProto> responseObserver) {
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
                        .setPayload(Any.pack(EchoResponse.newBuilder().setTime(echoRequest.getTime()).build()))
                        .build()
                    );
                    responseObserver.onCompleted();
                } catch (Exception e) {
                    responseObserver.onError(new RuntimeException(e));
                }
            }
        }
        ;

        grpcCleanup.register(InProcessServerBuilder.forName(MinionRpcClientTest.class.getName())
            .addService(testRequestService)
            .directExecutor().build().start());
        ManagedChannel channel = grpcCleanup.register(InProcessChannelBuilder.forName(MinionRpcClientTest.class.getName()).directExecutor().build());
        client = new MinionRpcClient(channel, (ctx) -> Optional.ofNullable(GrpcConstants.TENANT_ID_CONTEXT_KEY.get()), 5000);
        client.init();

        receivedRequests = new LinkedList<>();
    }

    @AfterEach
    public void afterTest() {
        client.shutdown();
    }

    @Test
    void testSentRpcRequest() throws Exception {
        EchoRequest echoRequest = EchoRequest.newBuilder().setTime(System.currentTimeMillis()).build();

        MinionIdentity minionIdentity =
            MinionIdentity.newBuilder()
                .setTenant(PRIMARY_TENANT_ID)
                .setLocation("test-location")
                .setSystemId("test-system")
                .build();

        GatewayRpcRequestProto request = GatewayRpcRequestProto.newBuilder()
            .setIdentity(minionIdentity)
            .setModuleId("test-rpc")
            .setRpcId(UUID.randomUUID().toString())
            .setPayload(Any.pack(echoRequest))
            .build();

        GatewayRpcResponseProto response = client.sendRpcRequest(PRIMARY_TENANT_ID, request).get();
        assertEquals(1, receivedRequests.size());
        assertThat(response.getIdentity().getTenant()).isEqualTo(request.getIdentity().getTenant());
        assertThat(response.getIdentity().getSystemId()).isEqualTo(request.getIdentity().getSystemId());
        assertThat(response.getIdentity().getLocation()).isEqualTo(request.getIdentity().getLocation());
        assertThat(response.getModuleId()).isEqualTo(request.getModuleId());
        assertThat(response.getRpcId()).isEqualTo(request.getRpcId());
        EchoResponse echoResponse = response.getPayload().unpack(EchoResponse.class);
        assertThat(echoResponse.getTime()).isEqualTo(echoRequest.getTime());
        assertThat(System.currentTimeMillis() - echoResponse.getTime()).isPositive();
    }
}
