/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2017-2023 The OpenNMS Group, Inc.
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

import com.codahale.metrics.MetricRegistry;
import org.opennms.horizon.flows.classification.ClassificationEngine;
import org.opennms.horizon.flows.classification.ClassificationRequest;
import org.opennms.horizon.flows.classification.persistence.api.Protocols;
import org.opennms.horizon.flows.grpc.client.InventoryClient;
import org.opennms.horizon.grpc.flows.contract.ContextKey;
import org.opennms.horizon.grpc.flows.contract.FlowDocument;
import org.opennms.horizon.grpc.flows.contract.FlowSource;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DocumentEnricherImpl {
    private static final Logger LOG = LoggerFactory.getLogger(DocumentEnricherImpl.class);

    private static final String NODE_METADATA_CACHE = "flows.node.metadata";

    private InventoryClient inventoryClient;

    private final ClassificationEngine classificationEngine;

    private final long clockSkewCorrectionThreshold;

    public DocumentEnricherImpl(final MetricRegistry metricRegistry,
                                final InventoryClient inventoryClient,
                                final ClassificationEngine classificationEngine,
                                final long clockSkewCorrectionThreshold) {
        this.inventoryClient = Objects.requireNonNull(inventoryClient);
        this.classificationEngine = Objects.requireNonNull(classificationEngine);

        this.clockSkewCorrectionThreshold = clockSkewCorrectionThreshold;
    }

    public List<EnrichedFlow> enrich(final Collection<FlowDocument> flows, final FlowSource source, final String tenantId) {
        if (flows.isEmpty()) {
            LOG.info("Nothing to enrich.");
            return Collections.emptyList();
        }

        return flows.stream().flatMap(flow -> {
            final EnrichedFlow document = EnrichedFlow.from(flow);
            if (document == null) {
                return Stream.empty();
            }

            // Metadata from message
            document.setHost(source.getSourceAddress());
            document.setLocation(source.getLocation());

            // Node data
            getNodeInfo(source.getLocation(), source.getSourceAddress(), source.getContextKey(), flow.getExporterIdentifier(), tenantId).ifPresent(document::setExporterNodeInfo);
            if (flow.getDstAddress() != null) {
                getNodeInfo(source.getLocation(), flow.getDstAddress(), null, null, tenantId).ifPresent(document::setSrcNodeInfo);
            }
            if (flow.getSrcAddress() != null) {
                getNodeInfo(source.getLocation(), flow.getSrcAddress(), null, null, tenantId).ifPresent(document::setDstNodeInfo);
            }

            // Locality
            if (flow.getSrcAddress() != null) {
                document.setSrcLocality(isPrivateAddress(flow.getSrcAddress()) ? EnrichedFlow.Locality.PRIVATE : EnrichedFlow.Locality.PUBLIC);
            }
            if (flow.getDstAddress() != null) {
                document.setDstLocality(isPrivateAddress(flow.getDstAddress()) ? EnrichedFlow.Locality.PRIVATE : EnrichedFlow.Locality.PUBLIC);
            }

            if (EnrichedFlow.Locality.PUBLIC.equals(document.getDstLocality()) || EnrichedFlow.Locality.PUBLIC.equals(document.getSrcLocality())) {
                document.setFlowLocality(EnrichedFlow.Locality.PUBLIC);
            } else if (EnrichedFlow.Locality.PRIVATE.equals(document.getDstLocality()) || EnrichedFlow.Locality.PRIVATE.equals(document.getSrcLocality())) {
                document.setFlowLocality(EnrichedFlow.Locality.PRIVATE);
            }

            final ClassificationRequest classificationRequest = createClassificationRequest(document);

            // Check whether classification is possible
            if (classificationRequest.isClassifiable()) {
                // Apply Application mapping
                document.setApplication(classificationEngine.classify(classificationRequest));
            }

            // Fix skewed clock
            // If received time and export time differ too much, correct all timestamps by the difference
            if (this.clockSkewCorrectionThreshold > 0) {
                if (flow.getClockCorrection() >= this.clockSkewCorrectionThreshold) {

                    // Fix the skew on all timestamps of the flow
                    document.setTimestamp(Instant.ofEpochMilli(flow.getTimestamp() - flow.getClockCorrection()));
                    document.setFirstSwitched(Instant.ofEpochMilli(flow.getFirstSwitched().getValue() - flow.getClockCorrection()));
                    document.setDeltaSwitched(Instant.ofEpochMilli(flow.getDeltaSwitched().getValue() - flow.getClockCorrection()));
                    document.setLastSwitched(Instant.ofEpochMilli(flow.getLastSwitched().getValue() - flow.getClockCorrection()));
                }
            }

            return Stream.of(document);
        }).collect(Collectors.toList());
    }

    private static boolean isPrivateAddress(String ipAddress) {
        final InetAddress inetAddress = InetAddressUtils.addr(ipAddress);
        return inetAddress.isLoopbackAddress() || inetAddress.isLinkLocalAddress() || inetAddress.isSiteLocalAddress();
    }

    private Optional<NodeInfo> getNodeInfo(final String location, final String ipAddress,
                                                    final ContextKey contextKey, final String value,
                                                    final String tenantId) {
        // TODO: HS-961
//        if (contextKey != null && !Strings.isNullOrEmpty(value)) {
//            final NodeMetadataKey metadataKey = new NodeMetadataKey(contextKey, value);
//            try {
//                nodeDocument = this.nodeMetadataCache.get(metadataKey);
//            } catch (ExecutionException e) {
//                LOG.error("Error while retrieving NodeDocument from NodeMetadataCache: {}.", e.getMessage(), e);
//                throw new RuntimeException(e);
//            }
//            if(nodeDocument.isPresent()) {
//                return nodeDocument;
//            }
//        }
        final var iface = inventoryClient.getIpInterfaceFromQuery(tenantId, ipAddress, location);
        if(iface == null){
            return Optional.empty();
        }

        var nodeInfo = new NodeInfo();
        nodeInfo.setNodeId(iface.getNodeId());
        nodeInfo.setInterfaceId(iface.getId());
        return Optional.of(nodeInfo);
    }

    public static ClassificationRequest createClassificationRequest(EnrichedFlow document) {
        final ClassificationRequest request = new ClassificationRequest();
        request.setProtocol(Protocols.getProtocol(document.getProtocol()));
        request.setLocation(document.getLocation());
        request.setExporterAddress(document.getHost());
        request.setDstAddress(document.getDstAddr());
        request.setDstPort(document.getDstPort());
        request.setSrcAddress(document.getSrcAddr());
        request.setSrcPort(document.getSrcPort());

        return request;
    }
}
