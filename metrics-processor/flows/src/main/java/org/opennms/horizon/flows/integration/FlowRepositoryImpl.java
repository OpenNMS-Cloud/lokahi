package org.opennms.horizon.flows.integration;

import org.opennms.dataplatform.flows.document.FlowDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class FlowRepositoryImpl implements FlowRepository {
    private static final Logger LOG = LoggerFactory.getLogger(FlowRepositoryImpl.class);

    @Override
    public void persist(Collection<FlowDocument> enrichedFlows) throws FlowException {
        LOG.info("Persisting flow data: {}", enrichedFlows.toString());
        // implement in HS-925
    }
}
