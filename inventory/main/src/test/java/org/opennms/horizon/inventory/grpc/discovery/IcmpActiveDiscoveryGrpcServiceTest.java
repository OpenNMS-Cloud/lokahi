/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022-2023 The OpenNMS Group, Inc.
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
package org.opennms.horizon.inventory.grpc.discovery;

import com.google.protobuf.BoolValue;
import com.google.protobuf.Empty;
import com.google.protobuf.Int64Value;
import com.google.rpc.Code;
import io.grpc.Context;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.opennms.horizon.inventory.discovery.IcmpActiveDiscoveryCreateDTO;
import org.opennms.horizon.inventory.discovery.IcmpActiveDiscoveryDTO;
import org.opennms.horizon.inventory.discovery.IcmpActiveDiscoveryList;
import org.opennms.horizon.inventory.dto.MonitoringLocationDTO;
import org.opennms.horizon.inventory.exception.LocationNotFoundException;
import org.opennms.horizon.inventory.grpc.TenantLookup;
import org.opennms.horizon.inventory.service.discovery.active.IcmpActiveDiscoveryService;
import org.opennms.horizon.inventory.service.taskset.ScannerTaskSetService;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

public class IcmpActiveDiscoveryGrpcServiceTest {

    public static final String TEST_TENANT_ID = "x-tenant-id-x";

    public static final long TEST_LOCATION_ID = 10L;

    public static final String TEST_LOCATION = "x-location-x";

    private TenantLookup mockTenantLookup;
    private IcmpActiveDiscoveryService mockIcmpActiveDiscoveryService;
    private ScannerTaskSetService mockScannerTaskSetService;

    private IcmpActiveDiscoveryCreateDTO testIcmpActiveDiscoveryCreateDTO;


    private IcmpActiveDiscoveryGrpcService target;

    @BeforeEach
    public void setUp() {
        mockTenantLookup = Mockito.mock(TenantLookup.class);
        mockIcmpActiveDiscoveryService = Mockito.mock(IcmpActiveDiscoveryService.class);
        mockScannerTaskSetService = Mockito.mock(ScannerTaskSetService.class);

        MonitoringLocationDTO location = MonitoringLocationDTO.newBuilder().setLocation(TEST_LOCATION).setId(TEST_LOCATION_ID).setTenantId(TEST_TENANT_ID).build();

        testIcmpActiveDiscoveryCreateDTO =
            IcmpActiveDiscoveryCreateDTO.newBuilder()
                .setName("x-active-discovery-create-x")
                .setLocationId(String.valueOf(TEST_LOCATION_ID))
                .build();

        target = new IcmpActiveDiscoveryGrpcService(mockTenantLookup, mockIcmpActiveDiscoveryService,
            mockScannerTaskSetService);
    }

    @Test
    void testCreateDiscovery() {
        //
        // Setup Test Data and Interactions
        //
        var testDiscovery =
            IcmpActiveDiscoveryDTO.newBuilder()
                .setLocationId(String.valueOf(TEST_LOCATION_ID))
                .setName("x-active-discovery-x")
                .build();

        prepareCommonTenantLookup();
        StreamObserver<IcmpActiveDiscoveryDTO> mockStreamObserver = Mockito.mock(StreamObserver.class);
        when(mockIcmpActiveDiscoveryService.createActiveDiscovery(testIcmpActiveDiscoveryCreateDTO, TEST_TENANT_ID)).thenReturn(testDiscovery);

        //
        // Execute
        //
        target.createDiscovery(testIcmpActiveDiscoveryCreateDTO, mockStreamObserver);

        //
        // Verify the Results
        //
        Mockito.verify(mockStreamObserver).onNext(testDiscovery);
        Mockito.verify(mockStreamObserver).onCompleted();
    }

    @Test
    void testCreateDiscoveryException() {
        //
        // Setup Test Data and Interactions
        //
        var testException = new RuntimeException("x-test-exception-x");
        prepareCommonTenantLookup();
        StreamObserver<IcmpActiveDiscoveryDTO> mockStreamObserver = Mockito.mock(StreamObserver.class);
        when(mockIcmpActiveDiscoveryService.createActiveDiscovery(testIcmpActiveDiscoveryCreateDTO, TEST_TENANT_ID)).thenThrow(testException);

        //
        // Execute
        //
        target.createDiscovery(testIcmpActiveDiscoveryCreateDTO, mockStreamObserver);

        //
        // Verify the Results
        //
        var matcher = prepareStatusExceptionMatcher(Code.INVALID_ARGUMENT_VALUE, "Invalid request");
        Mockito.verify(mockStreamObserver).onError(Mockito.argThat(matcher));
    }

    @Test
    void testCreateDiscoveryNotFoundException() {
        //
        // Setup Test Data and Interactions
        //
        var testException = new LocationNotFoundException("x-test-exception-x");
        prepareCommonTenantLookup();
        StreamObserver<IcmpActiveDiscoveryDTO> mockStreamObserver = Mockito.mock(StreamObserver.class);
        when(mockIcmpActiveDiscoveryService.createActiveDiscovery(testIcmpActiveDiscoveryCreateDTO, TEST_TENANT_ID)).thenThrow(testException);

        //
        // Execute
        //
        target.createDiscovery(testIcmpActiveDiscoveryCreateDTO, mockStreamObserver);

        //
        // Verify the Results
        //
        var matcher = prepareStatusExceptionMatcher(Code.INVALID_ARGUMENT_VALUE, "x-test-exception-x");
        Mockito.verify(mockStreamObserver).onError(Mockito.argThat(matcher));
    }

    @Test
    void testCreateDiscoveryHasId() {
        //
        // Setup Test Data and Interactions
        //
        var icmpActiveDiscoveryCreateDTO = IcmpActiveDiscoveryCreateDTO.newBuilder().setId(1).build();
        prepareTenantLookupOnMissingTenant();
        StreamObserver<IcmpActiveDiscoveryDTO> mockStreamObserver = Mockito.mock(StreamObserver.class);

        //
        // Execute
        //
        target.createDiscovery(icmpActiveDiscoveryCreateDTO, mockStreamObserver);

        //
        // Verify the Results
        //
        var matcher = prepareStatusExceptionMatcher(Code.INVALID_ARGUMENT_VALUE, "createDiscovery should not set id");
        Mockito.verify(mockStreamObserver).onError(Mockito.argThat(matcher));
    }

    @Test
    void testCreateDiscoveryMissingTenant() {
        //
        // Setup Test Data and Interactions
        //
        prepareTenantLookupOnMissingTenant();
        StreamObserver<IcmpActiveDiscoveryDTO> mockStreamObserver = Mockito.mock(StreamObserver.class);

        //
        // Execute
        //
        target.createDiscovery(testIcmpActiveDiscoveryCreateDTO, mockStreamObserver);

        //
        // Verify the Results
        //
        var matcher = prepareStatusExceptionMatcher(Code.INVALID_ARGUMENT_VALUE, "Missing tenantId");
        Mockito.verify(mockStreamObserver).onError(Mockito.argThat(matcher));
    }

    @Test
    void testListDiscoveries() {
        //
        // Setup Test Data and Interactions
        //
        var testDiscoveries =
            List.of(
                IcmpActiveDiscoveryDTO.newBuilder()
                    .setName("x-active-discovery-x")
                    .build()
            );

        prepareCommonTenantLookup();
        StreamObserver<IcmpActiveDiscoveryList> mockStreamObserver = Mockito.mock(StreamObserver.class);
        when(mockIcmpActiveDiscoveryService.getActiveDiscoveries(TEST_TENANT_ID)).thenReturn(testDiscoveries);

        //
        // Execute
        //
        target.listDiscoveries(Empty.getDefaultInstance(), mockStreamObserver);

        //
        // Verify the Results
        //
        Mockito.verify(mockStreamObserver).onNext(
            Mockito.argThat(
                (argument) -> Objects.equals(argument.getDiscoveriesList(), testDiscoveries)
            )
        );
        Mockito.verify(mockStreamObserver).onCompleted();
    }

    @Test
    void testListDiscoveriesMissingTenant() {
        //
        // Setup Test Data and Interactions
        //
        prepareTenantLookupOnMissingTenant();
        StreamObserver<IcmpActiveDiscoveryList> mockStreamObserver = Mockito.mock(StreamObserver.class);

        //
        // Execute
        //
        target.listDiscoveries(Empty.getDefaultInstance(), mockStreamObserver);

        //
        // Verify the Results
        //
        var matcher = prepareStatusExceptionMatcher(Code.INVALID_ARGUMENT_VALUE, "Missing tenantId");
        Mockito.verify(mockStreamObserver).onError(Mockito.argThat(matcher));
    }

    @Test
    void testGetDiscoveryById() {
        //
        // Setup Test Data and Interactions
        //
        var testDiscovery =
            IcmpActiveDiscoveryDTO.newBuilder()
                .setName("x-active-discovery-x")
                .build();

        prepareCommonTenantLookup();
        StreamObserver<IcmpActiveDiscoveryDTO> mockStreamObserver = Mockito.mock(StreamObserver.class);
        when(mockIcmpActiveDiscoveryService.getDiscoveryById(1313, TEST_TENANT_ID)).thenReturn(Optional.of(testDiscovery));

        //
        // Execute
        //
        target.getDiscoveryById(Int64Value.of(1313), mockStreamObserver);

        //
        // Verify the Results
        //
        Mockito.verify(mockStreamObserver).onNext(testDiscovery);
        Mockito.verify(mockStreamObserver).onCompleted();
    }

    @Test
    void testGetDiscoveryByIdNotFound() {
        //
        // Setup Test Data and Interactions
        //
        var testDiscovery =
            IcmpActiveDiscoveryDTO.newBuilder()
                .setName("x-active-discovery-x")
                .build();

        prepareCommonTenantLookup();
        StreamObserver<IcmpActiveDiscoveryDTO> mockStreamObserver = Mockito.mock(StreamObserver.class);
        when(mockIcmpActiveDiscoveryService.getDiscoveryById(1313, TEST_TENANT_ID)).thenReturn(Optional.empty());

        //
        // Execute
        //
        target.getDiscoveryById(Int64Value.of(1313), mockStreamObserver);

        //
        // Verify the Results
        //
        var matcher = prepareStatusExceptionMatcher(Code.NOT_FOUND_VALUE, "Can't find discovery config for name:");
        Mockito.verify(mockStreamObserver).onError(Mockito.argThat(matcher));
    }

    @Test
    void testGetDiscoveryByIdMissingTenant() {
        //
        // Setup Test Data and Interactions
        //
        prepareTenantLookupOnMissingTenant();
        StreamObserver<IcmpActiveDiscoveryDTO> mockStreamObserver = Mockito.mock(StreamObserver.class);

        //
        // Execute
        //
        target.getDiscoveryById(Int64Value.of(1313), mockStreamObserver);

        //
        // Verify the Results
        //
        var matcher = prepareStatusExceptionMatcher(Code.INVALID_ARGUMENT_VALUE, "Missing tenantId");
        Mockito.verify(mockStreamObserver).onError(Mockito.argThat(matcher));
    }

    @Test
    void testUpsertDiscoveryForInvalidRangeIpAddresses() {
        prepareCommonTenantLookup();
        String ipRange = "ABC-DEF";
        var discoveryCreateDTO =
            IcmpActiveDiscoveryCreateDTO.newBuilder()
                .setName("invalid-range")
                .addIpAddresses(ipRange)
                .setLocationId(String.valueOf(TEST_LOCATION_ID))
                .build();
        StreamObserver<IcmpActiveDiscoveryDTO> mockStreamObserver = Mockito.mock(StreamObserver.class);
        target.upsertActiveDiscovery(discoveryCreateDTO, mockStreamObserver);
        var matcher = prepareStatusExceptionMatcher(Code.INVALID_ARGUMENT_VALUE, "Invalid Ip Address entry " + ipRange);
        Mockito.verify(mockStreamObserver).onError(Mockito.argThat(matcher));
    }

    @Test
    void testUpsertDiscoveryForOutOfRangeIpAddresses() {
        prepareCommonTenantLookup();
        String ipRange = "192.168.1.0-192.169.1.0";
        var discoveryCreateDTO =
            IcmpActiveDiscoveryCreateDTO.newBuilder()
                .setName("test-out-of-range")
                .addIpAddresses(ipRange)
                .setLocationId(String.valueOf(TEST_LOCATION_ID))
                .build();
        StreamObserver<IcmpActiveDiscoveryDTO> mockStreamObserver = Mockito.mock(StreamObserver.class);
        target.upsertActiveDiscovery(discoveryCreateDTO, mockStreamObserver);
        var matcher = prepareStatusExceptionMatcher(Code.INVALID_ARGUMENT_VALUE, "Ip Address range is too large " + ipRange);
        Mockito.verify(mockStreamObserver).onError(Mockito.argThat(matcher));
    }

    @Test
    void testUpsertDiscovery() {
        // prepare
        var discoveryCreateDTO = IcmpActiveDiscoveryCreateDTO.newBuilder().setId(10).setName("update")
            .setLocationId(String.valueOf(TEST_LOCATION_ID)).addIpAddresses("192.168.0.1")
            .build();
        IcmpActiveDiscoveryDTO icmpActiveDiscoveryDTO = IcmpActiveDiscoveryDTO.newBuilder()
            .setId(10).setName("name")
            .setLocationId(String.valueOf(TEST_LOCATION_ID)).addIpAddresses("192.168.0.1").build();

        prepareCommonTenantLookup();
        StreamObserver<IcmpActiveDiscoveryDTO> mockStreamObserver = Mockito.mock(StreamObserver.class);
        Mockito.when(mockIcmpActiveDiscoveryService.getDiscoveryById(10L, TEST_TENANT_ID)).thenReturn(Optional.of(icmpActiveDiscoveryDTO));
        Mockito.when(mockIcmpActiveDiscoveryService.upsertActiveDiscovery(discoveryCreateDTO, TEST_TENANT_ID))
            .thenReturn(IcmpActiveDiscoveryDTO.newBuilder(icmpActiveDiscoveryDTO).setName("update").build());

        // execute
        target.upsertActiveDiscovery(discoveryCreateDTO, mockStreamObserver);

        // verify
        Mockito.verify(mockIcmpActiveDiscoveryService, times(1)).upsertActiveDiscovery(
            discoveryCreateDTO, TEST_TENANT_ID);
        Mockito.verify(mockScannerTaskSetService, times(1)).sendDiscoveryScannerTask(
            discoveryCreateDTO.getIpAddressesList(), Long.valueOf(discoveryCreateDTO.getLocationId()), TEST_TENANT_ID, discoveryCreateDTO.getId()
        );
        Mockito.verify(mockStreamObserver, times(1)).onNext(any(IcmpActiveDiscoveryDTO.class));
    }


    @Test
    void testDeleteDiscovery() {
        // prepare
        IcmpActiveDiscoveryDTO icmpActiveDiscoveryDTO = IcmpActiveDiscoveryDTO.newBuilder()
            .setId(10).setName("delete")
            .setLocationId(String.valueOf(TEST_LOCATION_ID))
            .setTenantId(TEST_TENANT_ID).addIpAddresses("192.168.0.1").build();

        prepareCommonTenantLookup();
        StreamObserver<BoolValue> mockStreamObserver = Mockito.mock(StreamObserver.class);
        Mockito.when(mockIcmpActiveDiscoveryService.getDiscoveryById(10L, TEST_TENANT_ID)).thenReturn(Optional.of(icmpActiveDiscoveryDTO));
        Mockito.when(mockIcmpActiveDiscoveryService.deleteActiveDiscovery(10L, TEST_TENANT_ID)).thenReturn(true);

        // execute
        target.deleteActiveDiscovery(Int64Value.of(10), mockStreamObserver);

        // verify
        Mockito.verify(mockIcmpActiveDiscoveryService, times(1)).deleteActiveDiscovery(
            10, TEST_TENANT_ID);
        Mockito.verify(mockScannerTaskSetService, times(1)).removeDiscoveryScanTask(
            Long.valueOf(icmpActiveDiscoveryDTO.getLocationId()), icmpActiveDiscoveryDTO.getId(), icmpActiveDiscoveryDTO.getTenantId()
        );
        Mockito.verify(mockStreamObserver, times(1)).onNext(BoolValue.of(true));
    }

//========================================
// Internals
//----------------------------------------

    private void prepareCommonTenantLookup() {
        when(mockTenantLookup.lookupTenantId(any(Context.class))).thenReturn(Optional.of(TEST_TENANT_ID));
    }

    private void prepareTenantLookupOnMissingTenant() {
        when(mockTenantLookup.lookupTenantId(any(Context.class))).thenReturn(Optional.empty());
    }

    private ArgumentMatcher<Exception> prepareStatusExceptionMatcher(int expectedCode, String expectedMessage) {
        return argument ->
            (
                (argument instanceof StatusRuntimeException) &&
                (((StatusRuntimeException) argument).getStatus().getCode().value() == expectedCode)  &&
                argument.getMessage().contains(expectedMessage)
            );
    }
}
