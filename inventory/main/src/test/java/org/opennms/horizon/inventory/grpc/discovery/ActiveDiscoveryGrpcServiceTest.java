package org.opennms.horizon.inventory.grpc.discovery;

import com.google.protobuf.BoolValue;
import com.google.protobuf.Empty;
import com.google.protobuf.Int64Value;
import com.google.rpc.Code;
import io.grpc.Context;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.opennms.horizon.inventory.dto.ActiveDiscoveryDTO;
import org.opennms.horizon.inventory.dto.ActiveDiscoveryList;
import org.opennms.horizon.inventory.grpc.TenantLookup;
import org.opennms.horizon.inventory.service.discovery.active.ActiveDiscoveryService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class ActiveDiscoveryGrpcServiceTest {

    public static final String TEST_TENANT_ID = "x-tenant-id-x";

    private TenantLookup mockTenantLookup;
    private ActiveDiscoveryService mockActiveDiscoveryService;

    private ActiveDiscoveryGrpcService target;

    @BeforeEach
    public void setUp() {
        mockTenantLookup = Mockito.mock(TenantLookup.class);
        mockActiveDiscoveryService = Mockito.mock(ActiveDiscoveryService.class);

        target = new ActiveDiscoveryGrpcService(mockTenantLookup, mockActiveDiscoveryService);
    }

    @Test
    void testListDiscoveries() {
        //
        // Setup Test Data and Interactions
        //
        var testActiveDiscoveryDTO1 = ActiveDiscoveryDTO.newBuilder().build();
        var testActiveDiscoveryDTO2 = ActiveDiscoveryDTO.newBuilder().build();
        var testActiveDiscoveryDTO3 = ActiveDiscoveryDTO.newBuilder().build();
        var testActiveDiscoveryList =
            List.of(
                testActiveDiscoveryDTO1,
                testActiveDiscoveryDTO2,
                testActiveDiscoveryDTO3
            );

        prepareCommonTenantLookup();
        StreamObserver<ActiveDiscoveryList> mockStreamObserver = Mockito.mock(StreamObserver.class);
        Mockito.when(mockActiveDiscoveryService.getActiveDiscoveries(TEST_TENANT_ID)).thenReturn(testActiveDiscoveryList);

        //
        // Execute
        //
        target.listDiscoveries(Empty.getDefaultInstance(), mockStreamObserver);

        //
        // Verify the Results
        //
        Mockito.verify(mockStreamObserver).onNext(
            Mockito.argThat(
                (argument) ->
                    (
                        (argument.getActiveDiscoveriesCount() == 3) &&
                        (argument.getActiveDiscoveries(0) == testActiveDiscoveryDTO1) &&
                        (argument.getActiveDiscoveries(1) == testActiveDiscoveryDTO2) &&
                        (argument.getActiveDiscoveries(2) == testActiveDiscoveryDTO3)
                    )
            )
        );
        Mockito.verify(mockStreamObserver).onCompleted();
    }

    @Test
    void testListDiscoveriesException() {
        //
        // Setup Test Data and Interactions
        //
        var testException = new RuntimeException("x-test-exception-x");
        prepareCommonTenantLookup();
        StreamObserver<ActiveDiscoveryList> mockStreamObserver = Mockito.mock(StreamObserver.class);
        Mockito.when(mockActiveDiscoveryService.getActiveDiscoveries(TEST_TENANT_ID)).thenThrow(testException);

        //
        // Execute
        //
        target.listDiscoveries(Empty.getDefaultInstance(), mockStreamObserver);

        //
        // Verify the Results
        //
        var matcher = prepareStatusExceptionMatcher(Code.INTERNAL_VALUE, "x-test-exception-x");
        Mockito.verify(mockStreamObserver).onError(Mockito.argThat(matcher));
    }

    @Test
    void testListDiscoveriesMissingTenant() {
        //
        // Setup Test Data and Interactions
        //
        prepareTenantLookupOnMissingTenant();
        StreamObserver<ActiveDiscoveryList> mockStreamObserver = Mockito.mock(StreamObserver.class);

        //
        // Execute
        //
        target.listDiscoveries(Empty.getDefaultInstance(), mockStreamObserver);

        //
        // Verify the Results
        //
        var matcher = prepareStatusExceptionMatcher(Code.INVALID_ARGUMENT_VALUE, "Tenant Id can't be empty");
        Mockito.verify(mockStreamObserver).onError(Mockito.argThat(matcher));
    }

    @Test
    void testDeleteDiscovery() {
        //
        // Setup Test Data and Interactions
        //
        prepareCommonTenantLookup();
        StreamObserver<BoolValue> mockStreamObserver = Mockito.mock(StreamObserver.class);

        //
        // Execute
        //
        target.deleteDiscovery(Int64Value.of(1717), mockStreamObserver);

        //
        // Verify the Results
        //
        Mockito.verify(mockActiveDiscoveryService).deleteActiveDiscovery(TEST_TENANT_ID, 1717);
        Mockito.verify(mockStreamObserver).onNext(BoolValue.of(true));
        Mockito.verify(mockStreamObserver).onCompleted();
    }

    @Test
    void testDeleteDiscoveryNotFoundException() {
        //
        // Setup Test Data and Interactions
        //
        var testException = new EntityNotFoundException("x-test-exception-x");
        prepareCommonTenantLookup();
        StreamObserver<BoolValue> mockStreamObserver = Mockito.mock(StreamObserver.class);
        Mockito.doThrow(testException).when(mockActiveDiscoveryService).deleteActiveDiscovery(TEST_TENANT_ID, 1717);

        //
        // Execute
        //
        target.deleteDiscovery(Int64Value.of(1717), mockStreamObserver);

        //
        // Verify the Results
        //
        var matcher = prepareStatusExceptionMatcher(Code.NOT_FOUND_VALUE, "x-test-exception-x");
        Mockito.verify(mockStreamObserver).onError(Mockito.argThat(matcher));
    }

    @Test
    void testDeleteDiscoveryException() {
        //
        // Setup Test Data and Interactions
        //
        var testException = new RuntimeException("x-test-exception-x");
        prepareCommonTenantLookup();
        StreamObserver<BoolValue> mockStreamObserver = Mockito.mock(StreamObserver.class);
        Mockito.doThrow(testException).when(mockActiveDiscoveryService).deleteActiveDiscovery(TEST_TENANT_ID, 1717);

        //
        // Execute
        //
        target.deleteDiscovery(Int64Value.of(1717), mockStreamObserver);

        //
        // Verify the Results
        //
        var matcher = prepareStatusExceptionMatcher(Code.INTERNAL_VALUE, "x-test-exception-x");
        Mockito.verify(mockStreamObserver).onError(Mockito.argThat(matcher));
    }

    @Test
    void testDeleteDiscoveryMissingTenant() {
        //
        // Setup Test Data and Interactions
        //
        prepareTenantLookupOnMissingTenant();
        StreamObserver<BoolValue> mockStreamObserver = Mockito.mock(StreamObserver.class);

        //
        // Execute
        //
        target.deleteDiscovery(Int64Value.of(1717), mockStreamObserver);

        //
        // Verify the Results
        //
        var matcher = prepareStatusExceptionMatcher(Code.INVALID_ARGUMENT_VALUE, "Tenant Id can't be empty");
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
