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
package org.opennms.horizon.flows.processing;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Objects;
import org.opennms.horizon.flows.document.TenantLocationSpecificFlowDocumentLog;
import org.opennms.horizon.flows.integration.FlowException;
import org.opennms.horizon.flows.integration.FlowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        this.persisters.put(
                pid, new Persister(repository, this.metricRegistry.timer(MetricRegistry.name("logPersisting", pid))));
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
