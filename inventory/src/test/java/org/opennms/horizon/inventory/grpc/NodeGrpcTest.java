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

package org.opennms.horizon.inventory.grpc;

import com.google.protobuf.Int64Value;
import com.google.rpc.Code;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.MetadataUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.common.VerificationException;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.inventory.dto.NodeServiceGrpc;
import org.opennms.horizon.inventory.mapper.NodeMapper;
import org.opennms.horizon.inventory.service.IpInterfaceService;
import org.opennms.horizon.inventory.service.NodeService;
import org.opennms.horizon.inventory.service.taskset.DetectorTaskSetService;
import org.opennms.horizon.inventory.service.taskset.ScannerTaskSetService;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

public class NodeGrpcTest extends AbstractGrpcUnitTest {

    private NodeService mockNodeService;
    private IpInterfaceService mockIpInterfaceService;
    private NodeMapper mockNodeMapper;
    private DetectorTaskSetService mockTaskSetService;
    private ScannerTaskSetService mockScannerTaskService;
    private NodeServiceGrpc.NodeServiceBlockingStub stub;
    private ManagedChannel channel;

    @BeforeEach
    public void beforeTest() throws VerificationException, IOException {
        mockNodeService = mock(NodeService.class);
        mockIpInterfaceService = mock(IpInterfaceService.class);
        mockNodeMapper = mock(NodeMapper.class);
        mockTaskSetService = mock(DetectorTaskSetService.class);
        NodeGrpcService grpcService = new NodeGrpcService(mockNodeService, mockIpInterfaceService, mockNodeMapper,
            tenantLookup, mockTaskSetService, mockScannerTaskService);
        startServer(grpcService);
        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
        stub = NodeServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    public void afterTest() throws InterruptedException {
        verifyNoMoreInteractions(mockNodeService);
        channel.shutdownNow();
        channel.awaitTermination(10, TimeUnit.SECONDS);
        stopServer();
    }

    @Test
    void testDeleteNode() {
        long id = 100L;
        NodeDTO nodeDTO = NodeDTO.newBuilder().setId(id).build();
        doReturn(Optional.of(nodeDTO)).when(mockNodeService).getByIdAndTenantId(id, tenantId);
        assertThat(stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createHeaders())).deleteNode(Int64Value.of(id)).getValue()).isTrue();
        verify(mockNodeService).getByIdAndTenantId(id, tenantId);
        verify(mockNodeService).deleteNode(id);
    }

    @Test
    void testDeleteNodeNotFound() {
        long id = 100L;
        doReturn(Optional.empty()).when(mockNodeService).getByIdAndTenantId(id, tenantId);
        StatusRuntimeException exception = Assertions.assertThrows(StatusRuntimeException.class, () -> stub
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createHeaders())).deleteNode(Int64Value.of(id)));
        assertThat(StatusProto.fromThrowable(exception).getCode()).isEqualTo(Code.NOT_FOUND_VALUE);
        verify(mockNodeService).getByIdAndTenantId(id, tenantId);
    }

    @Test
    void testDeleteNodeException() {
        long id = 100L;
        NodeDTO nodeDTO = NodeDTO.newBuilder().setId(id).build();
        doReturn(Optional.of(nodeDTO)).when(mockNodeService).getByIdAndTenantId(id, tenantId);
        doThrow(new RuntimeException("bad request")).when(mockNodeService).deleteNode(id);
        StatusRuntimeException exception = Assertions.assertThrows(StatusRuntimeException.class, () -> stub
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createHeaders())).deleteNode(Int64Value.of(id)));
        assertThat(StatusProto.fromThrowable(exception).getCode()).isEqualTo(Code.INTERNAL_VALUE);
        verify(mockNodeService).getByIdAndTenantId(id, tenantId);
        verify(mockNodeService).deleteNode(id);
    }
}
