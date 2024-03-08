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
package org.opennms.horizon.flows.integration;

import io.grpc.ManagedChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.opennms.dataplatform.flows.ingester.v1.IngesterGrpc;
import org.opennms.dataplatform.flows.ingester.v1.StoreFlowDocumentsRequest;
import org.opennms.dataplatform.flows.ingester.v1.StoreFlowDocumentsResponse;
import org.opennms.horizon.flows.document.FlowDocument;
import org.opennms.horizon.flows.document.TenantLocationSpecificFlowDocumentLog;
import org.opennms.horizon.flows.grpc.client.IngestorClient;
import org.springframework.retry.support.RetryTemplate;

class FlowRepositoryImplTest {

    private final String tenantId = "any-tenant-id";

    private FlowRepositoryImpl flowRepository;
    private final IngesterGrpc.IngesterBlockingStub ingesterBlockingStub =
            Mockito.mock(IngesterGrpc.IngesterBlockingStub.class);
    private final ManagedChannel managedChannel = Mockito.mock(ManagedChannel.class);
    private final IngestorClient ingestorClient = new IngestorClient(managedChannel, 1000, new RetryTemplate());

    @BeforeEach
    public void setUp() {
        flowRepository = new FlowRepositoryImpl(ingestorClient);
        ingestorClient.setIngesterBlockingStub(ingesterBlockingStub);
        Mockito.when(ingesterBlockingStub.withDeadlineAfter(Mockito.anyLong(), Mockito.any()))
                .thenReturn(ingesterBlockingStub);
        Mockito.when(ingesterBlockingStub.withInterceptors(Mockito.any())).thenReturn(ingesterBlockingStub);

        try (MockedStatic<IngesterGrpc> mockedIngester = Mockito.mockStatic(IngesterGrpc.class)) {
            mockedIngester
                    .when(() -> IngesterGrpc.newBlockingStub(managedChannel))
                    .thenReturn(ingesterBlockingStub);
        }
        Mockito.when(ingesterBlockingStub.storeFlowDocuments(Mockito.any()))
                .thenThrow(RuntimeException.class)
                .thenThrow(RuntimeException.class)
                .thenReturn(StoreFlowDocumentsResponse.newBuilder().build());
    }

    @Test
    void testCorrectNumberOfInteractionsWithIngesterStub() {
        // Given
        var flowsLog = TenantLocationSpecificFlowDocumentLog.newBuilder()
                .setTenantId(tenantId)
                .addMessage(FlowDocument.newBuilder());

        // When
        flowRepository.persist(flowsLog.build());

        // Then
        class FlowDocumentArgumentMatcher implements ArgumentMatcher<StoreFlowDocumentsRequest> {
            @Override
            public boolean matches(StoreFlowDocumentsRequest right) {
                return right.getDocumentsList().stream().allMatch(d -> tenantId.equals(d.getTenantId()));
            }
        }

        Mockito.verify(ingesterBlockingStub, Mockito.times(3))
                .storeFlowDocuments(Mockito.argThat(new FlowDocumentArgumentMatcher()));
    }
}
