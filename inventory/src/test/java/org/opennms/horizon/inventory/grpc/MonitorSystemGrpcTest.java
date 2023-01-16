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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.IOException;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.common.VerificationException;
import org.opennms.horizon.inventory.dto.MonitoringSystemDTO;
import org.opennms.horizon.inventory.dto.MonitoringSystemServiceGrpc;
import org.opennms.horizon.inventory.service.MonitoringSystemService;

import com.google.protobuf.StringValue;
import com.google.rpc.Code;

import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.MetadataUtils;

public class MonitorSystemGrpcTest extends AbstractGrpcUnitTest {
    private MonitoringSystemService mockService;
    private MonitoringSystemServiceGrpc.MonitoringSystemServiceBlockingStub stub;
    private final String systemId = "test-system";

    @BeforeEach
    void beforeTest() throws IOException, VerificationException {
        mockService = mock(MonitoringSystemService.class);
        MonitoringSystemGrpcService grpcService = new MonitoringSystemGrpcService(mockService, tenantLookup);
        startServer(grpcService);
        stub = MonitoringSystemServiceGrpc.newBlockingStub(grpCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build()));
    }

    @AfterEach
    void afterTest() throws InterruptedException {
        verifyNoMoreInteractions(mockService);
        stopServer();
    }

    @Test
    void testDeleteSystem() {
        long id = 1L;
        MonitoringSystemDTO systemDTO = MonitoringSystemDTO.newBuilder()
                .setSystemId(systemId).setId(id).build();
        doReturn(Optional.of(systemDTO)).when(mockService).findBySystemId(systemId, tenantId);
        assertThat(stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createHeaders())).deleteMonitoringSystem(StringValue.of(systemId)).getValue());
        verify(mockService).findBySystemId(systemId, tenantId);
        verify(mockService).deleteMonitoringSystem(id);
    }

    @Test
    void testDeleteSystemNotFound() {
        doReturn(Optional.empty()).when(mockService).findBySystemId(systemId, tenantId);
        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, () -> stub
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createHeaders())).deleteMonitoringSystem(StringValue.of(systemId)));
        assertThat(StatusProto.fromThrowable(exception).getCode()).isEqualTo(Code.NOT_FOUND_VALUE);
        verify(mockService).findBySystemId(systemId, tenantId);
    }

    @Test
    void testDeleteSystemException() {
        long id = 1L;
        MonitoringSystemDTO systemDTO = MonitoringSystemDTO.newBuilder()
            .setSystemId(systemId).setId(id).build();
        doReturn(Optional.of(systemDTO)).when(mockService).findBySystemId(systemId, tenantId);
        doThrow(new RuntimeException("bad request")).when(mockService).deleteMonitoringSystem(id);
        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, () -> stub
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createHeaders())).deleteMonitoringSystem(StringValue.of(systemId)));
        assertThat(StatusProto.fromThrowable(exception).getCode()).isEqualTo(Code.INTERNAL_VALUE);
        verify(mockService).findBySystemId(systemId, tenantId);
        verify(mockService).deleteMonitoringSystem(id);
    }

}
