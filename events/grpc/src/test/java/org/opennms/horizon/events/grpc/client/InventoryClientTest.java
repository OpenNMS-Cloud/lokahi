/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2023 The OpenNMS Group; Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group; Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group; Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation; either version 3 of the License;
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful;
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not; see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.horizon.events.grpc.client;

import com.google.protobuf.Int64Value;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.inventory.dto.NodeServiceGrpc;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class InventoryClientTest {
    public static final GrpcCleanupRule grpcCleanUp = new GrpcCleanupRule();

    private static InventoryClient client;
    private static NodeServiceGrpc.NodeServiceImplBase mockNodeService;

    private final String accessToken = "test-token";

    @BeforeAll
    public static void startGrpc() throws IOException {
        mockNodeService = mock(NodeServiceGrpc.NodeServiceImplBase.class, delegatesTo(
            new NodeServiceGrpc.NodeServiceImplBase() {
                @Override
                public void getNodeById(Int64Value request, StreamObserver<NodeDTO> responseObserver) {
                    responseObserver.onNext(NodeDTO.newBuilder()
                        .setId(request.getValue()).build());
                    responseObserver.onCompleted();
                }
            }));

        grpcCleanUp.register(InProcessServerBuilder.forName("InventoryClientTest")
            .addService(mockNodeService)
            .directExecutor().build().start());
        ManagedChannel channel = grpcCleanUp.register(InProcessChannelBuilder.forName("InventoryClientTest").directExecutor().build());
        client = new InventoryClient(channel, 5000);
        client.initialStubs();
    }

    @AfterEach
    public void afterTest() {
        verifyNoMoreInteractions(mockNodeService);
        reset(mockNodeService);
    }

    @Test
    void testGetNodeById() {
        long nodeId = 100L;
        ArgumentCaptor<Int64Value> captor = ArgumentCaptor.forClass(Int64Value.class);
        NodeDTO result = client.getNodeById("tenantId", nodeId);
        assertThat(result).isNotNull();
        verify(mockNodeService).getNodeById(captor.capture(), any());
        assertThat(captor.getValue().getValue()).isEqualTo(nodeId);
    }
}
