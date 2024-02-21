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
package org.opennms.horizon.inventory.grpc;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.google.protobuf.Empty;
import com.google.protobuf.Int64Value;
import com.google.rpc.Code;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.MetadataUtils;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.common.VerificationException;
import org.mockito.Mockito;
import org.opennms.horizon.inventory.dto.MonitoringSystemDTO;
import org.opennms.horizon.inventory.dto.MonitoringSystemQuery;
import org.opennms.horizon.inventory.dto.MonitoringSystemServiceGrpc;
import org.opennms.horizon.inventory.exception.LocationNotFoundException;
import org.opennms.horizon.inventory.service.MonitoringSystemService;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MonitoringSystemGrpcServiceTest extends AbstractGrpcUnitTest {
    private MonitoringSystemService mockService;
    private MonitoringSystemServiceGrpc.MonitoringSystemServiceBlockingStub stub;

    private final String systemId = "test-system";
    private ManagedChannel channel;

    @BeforeEach
    void beforeTest() throws IOException, VerificationException {
        mockService = mock(MonitoringSystemService.class);
        MonitoringSystemGrpcService grpcService = new MonitoringSystemGrpcService(mockService, tenantLookup);
        startServer(grpcService);
        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
        stub = MonitoringSystemServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void afterTest() throws InterruptedException {
        verifyNoMoreInteractions(mockService);
        channel.shutdownNow();
        channel.awaitTermination(10, TimeUnit.SECONDS);
        stopServer();
    }

    @Test
    void testListMonitoringSystem() {
        MonitoringSystemDTO systemDTO =
                MonitoringSystemDTO.newBuilder().setSystemId(systemId).build();
        doReturn(Collections.singletonList(systemDTO)).when(mockService).findByTenantId(TENANT_ID);
        assertThat(stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createHeaders()))
                        .listMonitoringSystem(Empty.newBuilder().build()))
                .isNotNull();
        verify(mockService).findByTenantId(TENANT_ID);
    }

    @Test
    void testListMonitoringSystemByLocationId() {
        long locationId = 1L;
        MonitoringSystemDTO systemDTO = MonitoringSystemDTO.newBuilder()
                .setSystemId(systemId)
                .setMonitoringLocationId(locationId)
                .build();
        doReturn(Collections.singletonList(systemDTO))
                .when(mockService)
                .findByMonitoringLocationIdAndTenantId(locationId, TENANT_ID);
        assertThat(stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createHeaders()))
                        .listMonitoringSystemByLocationId(Int64Value.of(locationId)))
                .isNotNull();
        verify(mockService).findByMonitoringLocationIdAndTenantId(locationId, TENANT_ID);
    }

    @Test
    void testListMonitoringSystemByLocationIdNoMinions() {
        long locationId = 1L;
        doReturn(Collections.emptyList())
                .when(mockService)
                .findByMonitoringLocationIdAndTenantId(locationId, TENANT_ID);

        var result = stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createHeaders()))
                .listMonitoringSystemByLocationId(Int64Value.of(locationId))
                .getSystemsList();

        Assertions.assertTrue(result.isEmpty());
        verify(mockService).findByMonitoringLocationIdAndTenantId(locationId, TENANT_ID);
    }

    @Test
    void testListMonitoringSystemByLocationIdLocationNotFound() {
        long locationId = 1L;
        var locationException = new LocationNotFoundException("Location not found for id: " + locationId);
        doThrow(locationException).when(mockService).findByMonitoringLocationIdAndTenantId(locationId, TENANT_ID);

        var exception = assertThrows(StatusRuntimeException.class, () -> stub.withInterceptors(
                        MetadataUtils.newAttachHeadersInterceptor(createHeaders()))
                .listMonitoringSystemByLocationId(Int64Value.of(locationId)));

        assertThat(StatusProto.fromThrowable(exception).getCode()).isEqualTo(Code.NOT_FOUND_VALUE);
        verify(mockService, times(1)).findByMonitoringLocationIdAndTenantId(locationId, TENANT_ID);
    }

    @Test
    void testGetMonitoringSystemByQuery() {
        long locationId = 1L;

        MonitoringSystemQuery query = MonitoringSystemQuery.newBuilder()
                .setLocation(String.valueOf(locationId))
                .setSystemId(systemId)
                .build();
        MonitoringSystemDTO systemDTO = MonitoringSystemDTO.newBuilder()
                .setSystemId(systemId)
                .setMonitoringLocationId(locationId)
                .build();
        doReturn(Optional.of(systemDTO))
                .when(mockService)
                .findByLocationAndSystemId(query.getLocation(), query.getSystemId(), TENANT_ID);

        assertThat(stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createHeaders()))
                        .getMonitoringSystemByQuery(query))
                .isNotNull();

        verify(mockService, times(1)).findByLocationAndSystemId(query.getLocation(), query.getSystemId(), TENANT_ID);
    }

    @Test
    void testGetMonitoringSystemByQueryNotFound() {
        MonitoringSystemQuery query = MonitoringSystemQuery.newBuilder().build();
        doReturn(Optional.empty())
                .when(mockService)
                .findByLocationAndSystemId(query.getLocation(), query.getSystemId(), TENANT_ID);

        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, () -> stub.withInterceptors(
                        MetadataUtils.newAttachHeadersInterceptor(createHeaders()))
                .getMonitoringSystemByQuery(MonitoringSystemQuery.newBuilder().build()));

        assertThat(StatusProto.fromThrowable(exception).getCode()).isEqualTo(Code.NOT_FOUND_VALUE);
        verify(mockService, times(1)).findByLocationAndSystemId(query.getLocation(), query.getSystemId(), TENANT_ID);
    }

    @Test
    void testGetMonitoringSystemByQueryMissTenant() throws VerificationException {
        Mockito.reset(spyInterceptor);
        doReturn(Optional.empty()).when(spyInterceptor).verifyAccessToken(AUTH_HEADER);

        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, () -> stub.withInterceptors(
                        MetadataUtils.newAttachHeadersInterceptor(createHeaders()))
                .getMonitoringSystemByQuery(MonitoringSystemQuery.newBuilder().build()));

        assertThat(StatusProto.fromThrowable(exception).getCode()).isEqualTo(Code.UNAUTHENTICATED_VALUE);
    }

    @Test
    void testDeleteSystem() {
        long id = 1L;
        MonitoringSystemDTO systemDTO =
                MonitoringSystemDTO.newBuilder().setSystemId(systemId).setId(id).build();
        doReturn(Optional.of(systemDTO)).when(mockService).findById(id, TENANT_ID);
        assertThat(stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createHeaders()))
                .deleteMonitoringSystem(Int64Value.newBuilder().setValue(id).build())
                .getValue());
        verify(mockService).findById(id, TENANT_ID);
        verify(mockService).deleteMonitoringSystem(id);
    }

    @Test
    void testDeleteSystemNotFound() {
        long id = 1L;
        doReturn(Optional.empty()).when(mockService).findById(id, TENANT_ID);
        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, () -> stub.withInterceptors(
                        MetadataUtils.newAttachHeadersInterceptor(createHeaders()))
                .deleteMonitoringSystem(Int64Value.newBuilder().setValue(id).build()));
        assertThat(StatusProto.fromThrowable(exception).getCode()).isEqualTo(Code.NOT_FOUND_VALUE);
        verify(mockService).findById(id, TENANT_ID);
    }

    @Test
    void testDeleteMonitoringSystemMissTenant() throws VerificationException {
        Mockito.reset(spyInterceptor);
        doReturn(Optional.empty()).when(spyInterceptor).verifyAccessToken(AUTH_HEADER);

        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, () -> stub.withInterceptors(
                        MetadataUtils.newAttachHeadersInterceptor(createHeaders()))
                .deleteMonitoringSystem(Int64Value.of(1)));

        assertThat(StatusProto.fromThrowable(exception).getCode()).isEqualTo(Code.UNAUTHENTICATED_VALUE);
    }

    @Test
    void testDeleteSystemException() {
        long id = 1L;
        MonitoringSystemDTO systemDTO =
                MonitoringSystemDTO.newBuilder().setSystemId(systemId).setId(id).build();
        doReturn(Optional.of(systemDTO)).when(mockService).findById(id, TENANT_ID);
        doThrow(new RuntimeException("bad request")).when(mockService).deleteMonitoringSystem(id);
        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, () -> stub.withInterceptors(
                        MetadataUtils.newAttachHeadersInterceptor(createHeaders()))
                .deleteMonitoringSystem(Int64Value.newBuilder().setValue(id).build()));
        assertThat(StatusProto.fromThrowable(exception).getCode()).isEqualTo(Code.INTERNAL_VALUE);
        verify(mockService).findById(id, TENANT_ID);
        verify(mockService).deleteMonitoringSystem(id);
    }

    @Test
    void testGetMonitoringSystemByIdMissTenant() throws VerificationException {
        Mockito.reset(spyInterceptor);
        doReturn(Optional.empty()).when(spyInterceptor).verifyAccessToken(AUTH_HEADER);

        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, () -> stub.withInterceptors(
                        MetadataUtils.newAttachHeadersInterceptor(createHeaders()))
                .getMonitoringSystemById(Int64Value.of(1)));

        assertThat(StatusProto.fromThrowable(exception).getCode()).isEqualTo(Code.UNAUTHENTICATED_VALUE);
    }
}
