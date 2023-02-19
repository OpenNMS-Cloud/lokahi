package org.opennms.horizon.flows.integration;

import org.opennms.horizon.flows.processing.EnrichedFlow;
import org.opennms.horizon.flows.processing.PipelineImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class FlowRepositoryImpl implements FlowRepository {
    private static final Logger LOG = LoggerFactory.getLogger(PipelineImpl.class);

    @Override
    public void persist(Collection<? extends EnrichedFlow> enrichedFlows) throws FlowException {
        LOG.info("Persisting flow data: {}", enrichedFlows);
        // implement in HS-925
    }
}
