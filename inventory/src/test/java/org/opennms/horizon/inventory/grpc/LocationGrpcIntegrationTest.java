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

package org.opennms.horizon.inventory.grpc;

import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptors;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.MetadataUtils;
import io.grpc.testing.GrpcCleanupRule;
import lombok.extern.slf4j.Slf4j;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.common.VerificationException;
import org.opennms.horizon.inventory.Constants;
import org.opennms.horizon.inventory.dto.MonitoringLocationDTO;
import org.opennms.horizon.inventory.dto.MonitoringLocationList;
import org.opennms.horizon.inventory.dto.MonitoringLocationServiceGrpc;
import org.opennms.horizon.inventory.service.MonitoringLocationService;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

//This is an example of gRPC integration tests underline mock services.
@Slf4j
public class LocationGrpcIntegrationTest {
    @Rule
    public static final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();
    private  static Server server;
    private  static ManagedChannel channel;
    private static MonitoringLocationServiceGrpc.MonitoringLocationServiceBlockingStub stub;
    private static MonitoringLocationGrpcService grpcService;
    private static MonitoringLocationService mockLocationService;
    private static TenantLookup tenantLookup;
    private static InventoryServerInterceptor spyInterceptor;

    private final String tenantId = "test-tenant";
    private final String authHeader = "Bearer esgs12345";
    private final String headerWithoutTenant = "Bearer esgs12345invalid";
    private final String differentTenantHeader = "Bearer esgs12345different";

    private MonitoringLocationDTO location1, location2;



    @BeforeAll
    public static void startServer() throws IOException {
        tenantLookup = new GrpcTenantLookupImpl();
        mockLocationService = mock(MonitoringLocationService.class);
        spyInterceptor = spy(new InventoryServerInterceptor(mock(KeycloakDeployment.class)));
        grpcService = new MonitoringLocationGrpcService(mockLocationService, tenantLookup);
        String serverName = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(serverName)
            .addService(ServerInterceptors.intercept(grpcService, spyInterceptor)).directExecutor().build();
        server.start();
        grpcCleanup.register(server);
        log.info("Server {} was started.", serverName);
        stub = MonitoringLocationServiceGrpc.newBlockingStub(grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build()));
    }

    @BeforeEach
    public void prepareTest() throws VerificationException {
        doReturn(Optional.of(tenantId)).when(spyInterceptor).verifyAccessToken(authHeader);
        doReturn(Optional.of("invalid-tenant")).when(spyInterceptor).verifyAccessToken(differentTenantHeader);
        doReturn(Optional.empty()).when(spyInterceptor).verifyAccessToken(headerWithoutTenant);
        doThrow(new VerificationException()).when(spyInterceptor).verifyAccessToken(null);
        location1 = MonitoringLocationDTO.newBuilder().build();
        location2 = MonitoringLocationDTO.newBuilder().build();
    }

    @AfterEach
    public void afterTest() {
        verifyNoMoreInteractions(mockLocationService);
        verifyNoMoreInteractions(spyInterceptor);
        reset(mockLocationService, spyInterceptor);
    }


    @Test
    public void testListLocations() throws VerificationException {
        doReturn(Arrays.asList(location1, location2)).when(mockLocationService).findByTenantId(tenantId);
        Metadata headers = new Metadata();
        headers.put(Constants.AUTHORIZATION_METADATA_KEY, authHeader);
        MonitoringLocationList result = stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers)).listLocations(Empty.newBuilder().build());
        assertThat(result.getLocationsList().size()).isEqualTo(2);
        verify(mockLocationService).findByTenantId(tenantId);
        verify(spyInterceptor).verifyAccessToken(authHeader);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }
}
