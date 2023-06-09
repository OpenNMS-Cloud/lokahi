package org.opennms.horizon.flows.integration;


import io.grpc.ManagedChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.opennms.dataplatform.flows.ingester.v1.IngesterGrpc;
import org.opennms.dataplatform.flows.ingester.v1.StoreFlowDocumentsResponse;
import org.opennms.horizon.flows.document.FlowDocument;
import org.opennms.horizon.flows.document.TenantLocationSpecificFlowDocument;
import org.opennms.horizon.flows.document.TenantLocationSpecificFlowDocumentLog;
import org.opennms.horizon.flows.grpc.client.IngestorClient;
import org.springframework.retry.support.RetryTemplate;

import java.util.Collections;
import java.util.List;


class FlowRepositoryImplTest {

    private FlowRepositoryImpl flowRepository;
    private final IngesterGrpc.IngesterBlockingStub ingesterBlockingStub = Mockito.mock(IngesterGrpc.IngesterBlockingStub.class);
    private final ManagedChannel managedChannel = Mockito.mock(ManagedChannel.class);
    private final IngestorClient ingestorClient = new IngestorClient(managedChannel, 1000, new RetryTemplate());


    @BeforeEach
    public void setUp() {
        flowRepository = new FlowRepositoryImpl(ingestorClient);
        ingestorClient.setIngesterBlockingStub(ingesterBlockingStub);
        Mockito.when(ingesterBlockingStub.withDeadlineAfter(Mockito.anyLong(), Mockito.any())).thenReturn(ingesterBlockingStub);
        Mockito.when(ingesterBlockingStub.withInterceptors(Mockito.any())).thenReturn(ingesterBlockingStub);

        try (MockedStatic<IngesterGrpc> mockedIngester = Mockito.mockStatic(IngesterGrpc.class)) {
            mockedIngester.when(() -> IngesterGrpc.newBlockingStub(managedChannel)).thenReturn(ingesterBlockingStub);
        }
        Mockito.when(ingesterBlockingStub
                .storeFlowDocuments(Mockito.any()))
            .thenThrow(RuntimeException.class).thenThrow(RuntimeException.class)
            .thenReturn(StoreFlowDocumentsResponse.newBuilder().build());
    }

    @Test
    void testCorrectNumberOfInteractionsWithIngesterStub() {
        // Given
        var flowsLog =
                TenantLocationSpecificFlowDocumentLog.newBuilder()
                    .setTenantId("any-tenant-id")
                    .addMessage(FlowDocument.newBuilder());

        // When
        flowRepository.persist(flowsLog.build());

        // Then
        Mockito.verify(ingesterBlockingStub, Mockito.times(3)).storeFlowDocuments(Mockito.any());
    }
}
