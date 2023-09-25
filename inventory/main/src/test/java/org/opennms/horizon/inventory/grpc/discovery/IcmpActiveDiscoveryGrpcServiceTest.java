package org.opennms.horizon.inventory.grpc.discovery;

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
import org.opennms.horizon.inventory.grpc.TenantLookup;
import org.opennms.horizon.inventory.service.discovery.active.IcmpActiveDiscoveryService;
import org.opennms.horizon.inventory.service.taskset.ScannerTaskSetService;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class IcmpActiveDiscoveryGrpcServiceTest {

    public static final String TEST_TENANT_ID = "x-tenant-id-x";

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

        testIcmpActiveDiscoveryCreateDTO =
            IcmpActiveDiscoveryCreateDTO.newBuilder()
                .setName("x-active-discovery-create-x")
                .build();

        target = new IcmpActiveDiscoveryGrpcService(mockTenantLookup, mockIcmpActiveDiscoveryService, mockScannerTaskSetService);
    }

    @Test
    void testCreateDiscovery() {
        //
        // Setup Test Data and Interactions
        //
        var testDiscovery =
            IcmpActiveDiscoveryDTO.newBuilder()
                .setName("x-active-discovery-x")
                .build();

        prepareCommonTenantLookup();
        StreamObserver<IcmpActiveDiscoveryDTO> mockStreamObserver = Mockito.mock(StreamObserver.class);
        Mockito.when(mockIcmpActiveDiscoveryService.createActiveDiscovery(testIcmpActiveDiscoveryCreateDTO, TEST_TENANT_ID)).thenReturn(testDiscovery);

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
        Mockito.when(mockIcmpActiveDiscoveryService.createActiveDiscovery(testIcmpActiveDiscoveryCreateDTO, TEST_TENANT_ID)).thenThrow(testException);

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
        Mockito.when(mockIcmpActiveDiscoveryService.getActiveDiscoveries(TEST_TENANT_ID)).thenReturn(testDiscoveries);

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
        Mockito.when(mockIcmpActiveDiscoveryService.getDiscoveryById(1313, TEST_TENANT_ID)).thenReturn(Optional.of(testDiscovery));

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
        Mockito.when(mockIcmpActiveDiscoveryService.getDiscoveryById(1313, TEST_TENANT_ID)).thenReturn(Optional.empty());

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
    void testUpsertDiscoveryForOutOfRangeIpAddresses() {

        prepareCommonTenantLookup();
        String ipRange = "192.168.1.0-192.169.1.0";
        var discoveryCreateDTO =
            IcmpActiveDiscoveryCreateDTO.newBuilder()
                .setName("test-out-of-range")
                .addIpAddresses(ipRange)
                .build();
        StreamObserver<IcmpActiveDiscoveryDTO> mockStreamObserver = Mockito.mock(StreamObserver.class);
        target.upsertActiveDiscovery(discoveryCreateDTO, mockStreamObserver);
        var matcher = prepareStatusExceptionMatcher(Code.INVALID_ARGUMENT_VALUE, "Ip Address range is too large " + ipRange);
        Mockito.verify(mockStreamObserver).onError(Mockito.argThat(matcher));
    }

//========================================
// Internals
//----------------------------------------

    private void prepareCommonTenantLookup() {
        Mockito.when(mockTenantLookup.lookupTenantId(Mockito.any(Context.class))).thenReturn(Optional.of(TEST_TENANT_ID));
    }

    private void prepareTenantLookupOnMissingTenant() {
        Mockito.when(mockTenantLookup.lookupTenantId(Mockito.any(Context.class))).thenReturn(Optional.empty());
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
