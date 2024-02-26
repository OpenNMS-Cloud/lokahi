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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.google.protobuf.Empty;
import com.google.protobuf.Int64Value;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.stub.MetadataUtils;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.common.VerificationException;
import org.mockito.Mockito;
import org.opennms.horizon.inventory.dto.IdList;
import org.opennms.horizon.inventory.dto.MonitoringLocationDTO;
import org.opennms.horizon.inventory.dto.MonitoringLocationList;
import org.opennms.horizon.inventory.dto.MonitoringLocationServiceGrpc;
import org.opennms.horizon.inventory.service.ConfigUpdateService;
import org.opennms.horizon.inventory.service.MonitoringLocationService;
import org.springframework.test.annotation.DirtiesContext;

// This is an example of gRPC integration tests underline mock services.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class LocationGrpcTest extends AbstractGrpcUnitTest {
    private MonitoringLocationServiceGrpc.MonitoringLocationServiceBlockingStub stub;
    private MonitoringLocationService mockLocationService;
    private MonitoringLocationDTO location1, location2;
    private ManagedChannel channel;

    @BeforeEach
    public void prepareTest() throws VerificationException, IOException {
        mockLocationService = mock(MonitoringLocationService.class);
        MonitoringLocationGrpcService grpcService = new MonitoringLocationGrpcService(
                mockLocationService, tenantLookup, Mockito.mock(ConfigUpdateService.class));
        startServer(grpcService);
        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
        stub = MonitoringLocationServiceGrpc.newBlockingStub(channel);
        location1 = MonitoringLocationDTO.newBuilder().build();
        location2 = MonitoringLocationDTO.newBuilder().build();
    }

    @AfterEach
    public void afterTest() throws InterruptedException {
        verifyNoMoreInteractions(mockLocationService);
        verifyNoMoreInteractions(spyInterceptor);
        reset(mockLocationService, spyInterceptor);
        channel.shutdownNow();
        channel.awaitTermination(10, TimeUnit.SECONDS);
        stopServer();
    }

    @Test
    void testListLocations() throws VerificationException {
        doReturn(Arrays.asList(location1, location2)).when(mockLocationService).findByTenantId(TENANT_ID);
        MonitoringLocationList result = stub.withInterceptors(
                        MetadataUtils.newAttachHeadersInterceptor(createHeaders()))
                .listLocations(Empty.newBuilder().build());
        assertThat(result.getLocationsList().size()).isEqualTo(2);
        verify(mockLocationService).findByTenantId(TENANT_ID);
        verify(spyInterceptor).verifyAccessToken(AUTH_HEADER);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    @Test
    void testListLocationsByIds() throws VerificationException {
        List<Long> ids = Arrays.asList(1L, 2L);
        doReturn(Arrays.asList(location1, location2)).when(mockLocationService).findByLocationIds(ids);
        MonitoringLocationList result = stub.withInterceptors(
                        MetadataUtils.newAttachHeadersInterceptor(createHeaders()))
                .listLocationsByIds(IdList.newBuilder()
                        .addAllIds(ids.stream().map(Int64Value::of).collect(Collectors.toList()))
                        .build());
        assertThat(result.getLocationsList().size()).isEqualTo(2);
        verify(mockLocationService).findByLocationIds(ids);
        verify(spyInterceptor).verifyAccessToken(AUTH_HEADER);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }
}
