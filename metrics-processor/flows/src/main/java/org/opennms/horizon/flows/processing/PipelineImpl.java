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

package org.opennms.horizon.flows.processing;

import java.util.Map;
import java.util.Objects;

import org.opennms.horizon.flows.document.TenantLocationSpecificFlowDocumentLog;
import org.opennms.horizon.flows.integration.FlowException;
import org.opennms.horizon.flows.integration.FlowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.collect.Maps;

public class PipelineImpl implements Pipeline {

    public static final String REPOSITORY_ID = "flows.repository.id";

    private static final Logger LOG = LoggerFactory.getLogger(PipelineImpl.class);

    /**
     * Time taken to enrich the flows in a log
     */
    private final Timer logEnrichementTimer;

    /**
     * Number of flows in a log
     */
    private final Histogram flowsPerLog;

    /**
     * Number of logs without a flow
     */
    private final Counter emptyFlows;

    private final MetricRegistry metricRegistry;

    private final DocumentEnricherImpl documentEnricher;

    private final Map<String, Persister> persisters = Maps.newConcurrentMap();

    public PipelineImpl(final MetricRegistry metricRegistry, final DocumentEnricherImpl documentEnricher) {
        this.metricRegistry = Objects.requireNonNull(metricRegistry);
        this.documentEnricher = Objects.requireNonNull(documentEnricher);

        this.emptyFlows = metricRegistry.counter("emptyFlows");
        this.flowsPerLog = metricRegistry.histogram("flowsPerLog");
        this.logEnrichementTimer = metricRegistry.timer("logEnrichment");
    }

    public void process(TenantLocationSpecificFlowDocumentLog flowsLog) throws FlowException {
        var flows = flowsLog.getMessageList();
        // Track the number of flows per call
        this.flowsPerLog.update(flows.size());

        // Filter empty logs
        if (flows.isEmpty()) {
            this.emptyFlows.inc();
            LOG.info("Received empty flows for tenant-id={}. Nothing to do.", flowsLog.getTenantId());
            return;
        }

        // Enrich with model data
        LOG.debug("Enriching {} flow documents.", flows.size());
        var enrichedFlowsLog = TenantLocationSpecificFlowDocumentLog.newBuilder()
            .setTenantId(flowsLog.getTenantId())
            .setLocationId(flowsLog.getLocationId())
            .setSystemId(flowsLog.getSystemId());
        try (Timer.Context ctx = this.logEnrichementTimer.time()) {
            var enrichedFlows = documentEnricher.enrich(flowsLog);
            enrichedFlowsLog.addAllMessage(enrichedFlows);
        } catch (Exception e) {
            throw new FlowException("Failed to enrich one or more flows.", e);
        }

        // TODO: DC-543 (Mark nodes and interfaces as having associated flows)

        // Push flows to persistence
        for (final var persister : this.persisters.entrySet()) {
            persister.getValue().persist(enrichedFlowsLog.build());
        }
    }

    @SuppressWarnings("rawtypes")
    public synchronized void onBind(final FlowRepository repository, final Map properties) {
        if (properties.get(REPOSITORY_ID) == null) {
            LOG.error("Flow repository {} has no repository ID defined. Ignoring...", repository);
            return;
        }

        final String pid = Objects.toString(properties.get(REPOSITORY_ID));
        this.persisters.put(pid, new Persister(repository,
            this.metricRegistry.timer(MetricRegistry.name("logPersisting", pid))));
    }

    @SuppressWarnings("rawtypes")
    public synchronized void onUnbind(final FlowRepository repository, final Map properties) {
        if (properties.get(REPOSITORY_ID) == null) {
            LOG.error("Flow repository {} has no repository ID defined. Ignoring...", repository);
            return;
        }

        final String pid = Objects.toString(properties.get(REPOSITORY_ID));
        this.persisters.remove(pid);
    }

    private static class Persister {
        public final FlowRepository repository;
        public final Timer logTimer;

        public Persister(final FlowRepository repository, final Timer logTimer) {
            this.repository = Objects.requireNonNull(repository);
            this.logTimer = Objects.requireNonNull(logTimer);
        }

        public void persist(final TenantLocationSpecificFlowDocumentLog flowsLog) throws FlowException {
            try (final var ctx = this.logTimer.time()) {
                this.repository.persist(flowsLog);
            }
        }
    }
}
