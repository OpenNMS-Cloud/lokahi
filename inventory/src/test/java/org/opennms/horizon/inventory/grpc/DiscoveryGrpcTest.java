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

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.common.VerificationException;
import org.opennms.horizon.inventory.dto.DiscoveryRequest;
import org.opennms.horizon.inventory.dto.DiscoveryServiceGrpc;
import org.opennms.horizon.inventory.service.taskset.ScannerTaskSetService;

import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.stub.MetadataUtils;

public class DiscoveryGrpcTest extends AbstractGrpcUnitTest {

    private ScannerTaskSetService mockScannerTaskService;
    private DiscoveryServiceGrpc.DiscoveryServiceBlockingStub stub;
    private ManagedChannel channel;

    @BeforeEach
    public void beforeTest() throws VerificationException, IOException {
        mockScannerTaskService = mock(ScannerTaskSetService.class);
        DiscoveryGrpcService grpcService = new DiscoveryGrpcService(tenantLookup, mockScannerTaskService);
        startServer(grpcService);
        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
        stub = DiscoveryServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    public void afterTest() throws InterruptedException {
        verifyNoMoreInteractions(mockScannerTaskService);
        channel.shutdownNow();
        channel.awaitTermination(10, TimeUnit.SECONDS);
        stopServer();
    }

    @Test
    void testDiscoverServices() {
        doNothing().when(mockScannerTaskService).sendDiscoveryScannerTask(anyList(), anyString(), anyString(), anyString());
        List<String> values = List.of("127.0.0.1", "127.0.0.2");
        DiscoveryRequest request = DiscoveryRequest.newBuilder()
            .setRequisitionName("requisition")
            .setLocation("location")
            .addAllIpAddresses(values)
            .build();
        stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createHeaders())).discoverServices(request);
        verify(mockScannerTaskService).sendDiscoveryScannerTask(eq(values), eq("location"), eq("test-tenant"), eq("requisition"));
    }
}
