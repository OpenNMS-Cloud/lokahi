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
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.opennms.horizon.inventory.TestConstants.PRIMARY_TENANT_ID;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcRequestServiceGrpc;
import org.opennms.cloud.grpc.minion.RpcResponseProto;
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
    private RpcRequestServiceGrpc.RpcRequestServiceImplBase mockRequestService;

    @BeforeEach
    public void setUp() throws IOException {
        mockRequestService = mock(RpcRequestServiceGrpc.RpcRequestServiceImplBase.class, delegatesTo(
            new RpcRequestServiceGrpc.RpcRequestServiceImplBase(){
                @Override
                public void request(RpcRequestProto request, StreamObserver<RpcResponseProto> responseObserver) {
                    try {
                        EchoRequest echoRequest = request.getPayload().unpack(EchoRequest.class);
                        responseObserver.onNext(RpcResponseProto.newBuilder()
                            .setLocation(request.getLocation())
                            .setRpcId(request.getRpcId())
                            .setModuleId(request.getModuleId())
                            .setSystemId(request.getSystemId())
                            .setPayload(Any.pack(EchoResponse.newBuilder().setTime(echoRequest.getTime()).build()))
                            .build()
                        );
                        responseObserver.onCompleted();
                    } catch (Exception e) {
                        responseObserver.onError(new RuntimeException(e));
                    }
                }
            }));
        grpcCleanup.register(InProcessServerBuilder.forName(MinionRpcClientTest.class.getName())
            .addService(mockRequestService)
            .directExecutor().build().start());
        ManagedChannel channel = grpcCleanup.register(InProcessChannelBuilder.forName(MinionRpcClientTest.class.getName()).directExecutor().build());
        client = new MinionRpcClient(channel, (ctx) -> Optional.ofNullable(GrpcConstants.TENANT_ID_CONTEXT_KEY.get()));
        client.init();
    }

    @AfterEach
    public void afterTest() {
        verifyNoMoreInteractions(mockRequestService);
        client.shutdown();
    }

    @Test
    void testSentRpcRequest() throws Exception {
        EchoRequest echoRequest = EchoRequest.newBuilder().setTime(System.currentTimeMillis()).build();
        RpcRequestProto request = RpcRequestProto.newBuilder()
            .setSystemId("test-system")
            .setLocation("test-location")
            .setModuleId("test-rpc")
            .setRpcId(UUID.randomUUID().toString())
            .setPayload(Any.pack(echoRequest))
            .build();
        RpcResponseProto response = client.sendRpcRequest(PRIMARY_TENANT_ID, request).get();
        verify(mockRequestService).request(any(), any());
        assertThat(response.getSystemId()).isEqualTo(request.getSystemId());
        assertThat(response.getLocation()).isEqualTo(request.getLocation());
        assertThat(response.getModuleId()).isEqualTo(request.getModuleId());
        assertThat(response.getRpcId()).isEqualTo(request.getRpcId());
        EchoResponse echoResponse = response.getPayload().unpack(EchoResponse.class);
        assertThat(echoResponse.getTime()).isEqualTo(echoRequest.getTime());
        assertThat(System.currentTimeMillis() - echoResponse.getTime()).isPositive();
    }
}
