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
package org.opennms.horizon.inventory.grpc.discovery;

import com.google.rpc.Code;
import io.grpc.Context;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.opennms.horizon.inventory.dto.AzureActiveDiscoveryCreateDTO;
import org.opennms.horizon.inventory.dto.AzureActiveDiscoveryDTO;
import org.opennms.horizon.inventory.exception.LocationNotFoundException;
import org.opennms.horizon.inventory.grpc.TenantLookup;
import org.opennms.horizon.inventory.service.discovery.active.AzureActiveDiscoveryService;

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

        testAzureActiveDiscoveryCreateDTO = AzureActiveDiscoveryCreateDTO.newBuilder()
                .setName("x-active-discovery-create-x")
                .build();

        target = new AzureActiveDiscoveryGrpcService(mockTenantLookup, mockAzureActiveDiscoveryService);
    }

    @Test
    void testCreateDiscovery() {
        //
        // Setup Test Data and Interactions
        //
        var testDiscovery = AzureActiveDiscoveryDTO.newBuilder()
                .setName("x-active-discovery-x")
                .build();

        prepareCommonTenantLookup();
        StreamObserver<AzureActiveDiscoveryDTO> mockStreamObserver = Mockito.mock(StreamObserver.class);
        Mockito.when(mockAzureActiveDiscoveryService.createActiveDiscovery(
                        TEST_TENANT_ID, testAzureActiveDiscoveryCreateDTO))
                .thenReturn(testDiscovery);

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
        Mockito.when(mockAzureActiveDiscoveryService.createActiveDiscovery(
                        TEST_TENANT_ID, testAzureActiveDiscoveryCreateDTO))
                .thenThrow(testException);

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
        Mockito.when(mockAzureActiveDiscoveryService.createActiveDiscovery(
                        TEST_TENANT_ID, testAzureActiveDiscoveryCreateDTO))
                .thenThrow(testException);

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

    // ========================================
    // Internals
    // ----------------------------------------

    private void prepareCommonTenantLookup() {
        Mockito.when(mockTenantLookup.lookupTenantId(Mockito.any(Context.class)))
                .thenReturn(Optional.of(TEST_TENANT_ID));
    }

    private void prepareTenantLookupOnMissingTenant() {
        Mockito.when(mockTenantLookup.lookupTenantId(Mockito.any(Context.class)))
                .thenReturn(Optional.empty());
    }

    private ArgumentMatcher<Exception> prepareStatusExceptionMatcher(int expectedCode, String expectedMessage) {
        return argument -> ((argument instanceof StatusRuntimeException)
                && (((StatusRuntimeException) argument).getStatus().getCode().value() == expectedCode)
                && argument.getMessage().contains(expectedMessage));
    }
}
