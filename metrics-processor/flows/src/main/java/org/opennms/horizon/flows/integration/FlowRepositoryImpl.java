package org.opennms.horizon.flows.integration;

import org.apache.commons.lang3.StringUtils;
import org.opennms.dataplatform.flows.document.FlowDocument;
import org.opennms.dataplatform.flows.ingester.v1.StoreFlowDocumentsRequest;

import org.opennms.horizon.flows.grpc.client.IngestorClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.Collection;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FlowRepositoryImpl implements FlowRepository {
    private static final Logger LOG = LoggerFactory.getLogger(FlowRepositoryImpl.class);
    private final IngestorClient ingestorClient;

    @Override
    public void persist(Collection<FlowDocument> enrichedFlows) {
        LOG.info("Persisting flow data: {}", enrichedFlows.toString());

        if (CollectionUtils.isEmpty(enrichedFlows)) {
            LOG.trace("No EnrichedFlow present, skipping flow data persisting step. ");
            return;
        }

        StoreFlowDocumentsRequest storeFlowDocumentsRequest = StoreFlowDocumentsRequest.newBuilder()
            .addAllDocuments(enrichedFlows)
            .build();

        ingestorClient.sendData(storeFlowDocumentsRequest, StringUtils.join(enrichedFlows.stream().map(FlowDocument::getTenantId), ","));
    }
}
