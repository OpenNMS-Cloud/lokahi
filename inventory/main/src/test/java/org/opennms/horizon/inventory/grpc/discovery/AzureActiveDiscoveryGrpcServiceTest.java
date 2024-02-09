package org.opennms.horizon.inventory.grpc.discovery;

import com.google.rpc.Code;
import io.grpc.Context;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.opennms.horizon.inventory.dto.AzureActiveDiscoveryCreateDTO;
import org.opennms.horizon.inventory.dto.AzureActiveDiscoveryDTO;
import org.opennms.horizon.inventory.exception.LocationNotFoundException;
import org.opennms.horizon.inventory.grpc.TenantLookup;
import org.opennms.horizon.inventory.service.discovery.active.AzureActiveDiscoveryService;

import java.util.Optional;

public class AzureActiveDiscoveryGrpcServiceTest {

    public static final String TEST_TENANT_ID = "x-tenant-id-x";

    private TenantLookup mockTenantLookup;
    private AzureActiveDiscoveryService mockAzureActiveDiscoveryService;

    private AzureActiveDiscoveryCreateDTO testAzureActiveDiscoveryCreateDTO;

    private AzureActiveDiscoveryGrpcService target;

    @BeforeEach
    public void setUp() {
        mockTenantLookup = Mockito.mock(TenantLookup.class);
        mockAzureActiveDiscoveryService = Mockito.mock(AzureActiveDiscoveryService.class);

        testAzureActiveDiscoveryCreateDTO =
            AzureActiveDiscoveryCreateDTO.newBuilder()
                .setName("x-active-discovery-create-x")
                .build();

        target = new AzureActiveDiscoveryGrpcService(mockTenantLookup, mockAzureActiveDiscoveryService);
    }

    @Test
    void testCreateDiscovery() {
        //
        // Setup Test Data and Interactions
        //
        var testDiscovery =
            AzureActiveDiscoveryDTO.newBuilder()
                .setName("x-active-discovery-x")
                .build();

        prepareCommonTenantLookup();
        StreamObserver<AzureActiveDiscoveryDTO> mockStreamObserver = Mockito.mock(StreamObserver.class);
        Mockito.when(mockAzureActiveDiscoveryService.createActiveDiscovery(TEST_TENANT_ID, testAzureActiveDiscoveryCreateDTO)).thenReturn(testDiscovery);

        //
        // Execute
        //
        target.createDiscovery(testAzureActiveDiscoveryCreateDTO, mockStreamObserver);

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
        StreamObserver<AzureActiveDiscoveryDTO> mockStreamObserver = Mockito.mock(StreamObserver.class);
        Mockito.when(mockAzureActiveDiscoveryService.createActiveDiscovery(TEST_TENANT_ID, testAzureActiveDiscoveryCreateDTO)).thenThrow(testException);

        //
        // Execute
        //
        target.createDiscovery(testAzureActiveDiscoveryCreateDTO, mockStreamObserver);

        //
        // Verify the Results
        //
        var matcher = prepareStatusExceptionMatcher(Code.INTERNAL_VALUE, "x-test-exception-x");
        Mockito.verify(mockStreamObserver).onError(Mockito.argThat(matcher));
    }

    @Test
    void testCreateDiscoveryMissingTenant() {
        //
        // Setup Test Data and Interactions
        //
        prepareTenantLookupOnMissingTenant();
        StreamObserver<AzureActiveDiscoveryDTO> mockStreamObserver = Mockito.mock(StreamObserver.class);

        //
        // Execute
        //
        target.createDiscovery(testAzureActiveDiscoveryCreateDTO, mockStreamObserver);

        //
        // Verify the Results
        //
        var matcher = prepareStatusExceptionMatcher(Code.INVALID_ARGUMENT_VALUE, "Tenant Id can't be empty");
        Mockito.verify(mockStreamObserver).onError(Mockito.argThat(matcher));
    }

    @Test
    void testCreateDiscoveryLocationNotFound() {
        //
        // Setup Test Data and Interactions
        //
        var testException = new LocationNotFoundException("x-test-exception-x");
        prepareCommonTenantLookup();
        StreamObserver<AzureActiveDiscoveryDTO> mockStreamObserver = Mockito.mock(StreamObserver.class);
        Mockito.when(mockAzureActiveDiscoveryService.createActiveDiscovery(TEST_TENANT_ID, testAzureActiveDiscoveryCreateDTO)).thenThrow(testException);

        //
        // Execute
        //
        target.createDiscovery(testAzureActiveDiscoveryCreateDTO, mockStreamObserver);

        //
        // Verify the Results
        //
        var matcher = prepareStatusExceptionMatcher(Code.INVALID_ARGUMENT_VALUE, "x-test-exception-x");
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
