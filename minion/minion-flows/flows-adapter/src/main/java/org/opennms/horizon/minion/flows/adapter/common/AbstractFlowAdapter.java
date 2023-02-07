/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2017-2017 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2017 The OpenNMS Group, Inc.
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

package org.opennms.horizon.minion.flows.adapter.common;

import static com.codahale.metrics.MetricRegistry.name;

import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.opennms.horizon.minion.flows.adapter.imported.ContextKey;
import org.opennms.horizon.minion.flows.adapter.imported.Flow;
import org.opennms.horizon.minion.flows.adapter.imported.FlowSource;
import org.opennms.horizon.minion.flows.adapter.imported.Pipeline;
import org.opennms.horizon.minion.flows.adapter.imported.ProcessingOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.google.common.base.Strings;

public abstract class AbstractFlowAdapter<P> implements Adapter {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractFlowAdapter.class);

    private String metaDataNodeLookup;
    private ContextKey contextKey;

    /**
     * Time taken to parse a log
     */
    private final Timer logParsingTimer;

    /**
     * Number of packets per log
     */
    private final Histogram packetsPerLogHistogram;

    private final Meter entriesReceived;

    private final Meter entriesParsed;

    private final Meter entriesConverted;

    private boolean applicationThresholding;
    private boolean applicationDataCollection;

    private final List<? extends PackageDefinition> packages;

    private final TelemetryRegistry telemetryRegistry;

    public AbstractFlowAdapter(final AdapterDefinition adapterConfig, final TelemetryRegistry telemetryRegistry) {
        Objects.requireNonNull(adapterConfig);
        Objects.requireNonNull(telemetryRegistry.getMetricRegistry());
        Objects.requireNonNull(telemetryRegistry);

        this.logParsingTimer = telemetryRegistry.getMetricRegistry().timer(name("adapters", adapterConfig.getFullName(), "logParsing"));
        this.packetsPerLogHistogram = telemetryRegistry.getMetricRegistry().histogram(name("adapters", adapterConfig.getFullName(), "packetsPerLog"));
        this.entriesReceived = telemetryRegistry.getMetricRegistry().meter(name("adapters", adapterConfig.getFullName(), "entriesReceived"));
        this.entriesParsed = telemetryRegistry.getMetricRegistry().meter(name("adapters", adapterConfig.getFullName(), "entriesParsed"));
        this.entriesConverted = telemetryRegistry.getMetricRegistry().meter(name("adapters", adapterConfig.getFullName(), "entriesConverted"));
        this.telemetryRegistry = telemetryRegistry;
        this.packages = Objects.requireNonNull(adapterConfig.getPackages());
    }

    @Override
    public void handleMessageLog(TelemetryMessageLog messageLog) {
        LOG.debug("Received {} telemetry messages", messageLog.getMessageList().size());

        int flowPackets = 0;

        final List<Flow> flows = new LinkedList<>();
        try (Timer.Context ctx = logParsingTimer.time()) {
            for (TelemetryMessageLogEntry eachMessage : messageLog.getMessageList()) {
                this.entriesReceived.mark();

                LOG.trace("Parsing packet: {}", eachMessage);
                final P flowPacket = parse(eachMessage);
                if (flowPacket != null) {
                    this.entriesParsed.mark();

                    flowPackets += 1;

                    final List<Flow> converted = this.convert(flowPacket, Instant.ofEpochMilli(eachMessage.getTimestamp()));
                    flows.addAll(converted);

                    this.entriesConverted.mark(converted.size());
                }
            }
            packetsPerLogHistogram.update(flowPackets);
        }

        LOG.debug("Sending {} packets, {} flows to metrics-processor for enrichment step. ", flowPackets, flows.size());
        final FlowSource source = new FlowSource(messageLog.getLocation(), messageLog.getSourceAddress(), contextKey);

        // TODO: verify that the message is sent correctly
        telemetryRegistry.getDispatcher().send(TelemetryMessageProtoCreator.createMessage(source, flows,
            this.applicationThresholding, this.applicationDataCollection, this.packages));
        LOG.debug("Packets and flows successfully sent to metrics-processor. ");

        LOG.debug("Completed processing {} telemetry messages.", messageLog.getMessageList().size());
    }

    protected abstract P parse(TelemetryMessageLogEntry message);

    protected abstract List<Flow> convert(final P packet, final Instant receivedAt);

    public String getMetaDataNodeLookup() {
        return metaDataNodeLookup;
    }

    public void setMetaDataNodeLookup(String metaDataNodeLookup) {
        this.metaDataNodeLookup = metaDataNodeLookup;

        if (!Strings.isNullOrEmpty(this.metaDataNodeLookup)) {
            this.contextKey = new ContextKey(metaDataNodeLookup);
        } else {
            this.contextKey = null;
        }
    }

    public boolean isApplicationThresholding() {
        return this.applicationThresholding;
    }

    public void setApplicationThresholding(final boolean applicationThresholding) {
        this.applicationThresholding = applicationThresholding;
    }

    public boolean isApplicationDataCollection() {
        return this.applicationDataCollection;
    }

    public void setApplicationDataCollection(final boolean applicationDataCollection) {
        this.applicationDataCollection = applicationDataCollection;
    }
}
