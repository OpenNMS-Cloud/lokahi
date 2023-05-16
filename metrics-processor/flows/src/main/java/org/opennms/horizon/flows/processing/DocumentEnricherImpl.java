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

import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.opennms.dataplatform.flows.document.FlowDocument;
import org.opennms.dataplatform.flows.document.Locality;
import org.opennms.dataplatform.flows.document.NodeInfo;
import org.opennms.horizon.flows.classification.ClassificationEngine;
import org.opennms.horizon.flows.classification.ClassificationRequest;
import org.opennms.horizon.flows.classification.persistence.api.Protocols;
import org.opennms.horizon.flows.grpc.client.InventoryClient;
import org.opennms.horizon.inventory.dto.IpInterfaceDTO;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.google.protobuf.UInt64Value;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public class DocumentEnricherImpl {
    private static final Logger LOG = LoggerFactory.getLogger(DocumentEnricherImpl.class);

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

    public List<FlowDocument> enrich(final Collection<org.opennms.dataplatform.flows.document.FlowDocument> flows, final String tenantId) {
        if (flows.isEmpty()) {
            LOG.info("Nothing to enrich.");
            return Collections.emptyList();
        }

        return flows.stream().flatMap(flow -> {
            final var document = FlowDocument.newBuilder(flow);

            if (document == null) {
                return Stream.empty();
            }

            document.setTenantId(tenantId);

            // Node data
            getNodeInfo(flow.getLocation(), flow.getExporterAddress(), tenantId).ifPresent(document::setExporterNode);
            if (flow.getDstAddress() != null) {
                getNodeInfo(flow.getLocation(), flow.getDstAddress(), tenantId).ifPresent(document::setSrcNode);
            }
            if (flow.getSrcAddress() != null) {
                getNodeInfo(flow.getLocation(), flow.getSrcAddress(), tenantId).ifPresent(document::setDestNode);
            }

            // Locality
            if (flow.getSrcAddress() != null) {
                document.setSrcLocality(isPrivateAddress(flow.getSrcAddress()) ? Locality.PRIVATE : Locality.PUBLIC);
            }
            if (flow.getDstAddress() != null) {
                document.setDstLocality(isPrivateAddress(flow.getDstAddress()) ? Locality.PRIVATE : Locality.PUBLIC);
            }

            if (Locality.PUBLIC.equals(document.getDstLocality()) || Locality.PUBLIC.equals(document.getSrcLocality())) {
                document.setFlowLocality(Locality.PUBLIC);
            } else if (Locality.PRIVATE.equals(document.getDstLocality()) || Locality.PRIVATE.equals(document.getSrcLocality())) {
                document.setFlowLocality(Locality.PRIVATE);
            }

            final ClassificationRequest classificationRequest = createClassificationRequest(document);

            // Check whether classification is possible
            if (classificationRequest.isClassifiable()) {
                // Apply Application mapping
                var application = classificationEngine.classify(classificationRequest);
                if (application != null) {
                    document.setApplication(application);
                }
            }

            // Fix skewed clock
            // If received time and export time differ too much, correct all timestamps by the difference
            if (this.clockSkewCorrectionThreshold > 0) {
                final var skew = Duration.between(Instant.ofEpochMilli(flow.getReceivedAt()), Instant.ofEpochMilli(flow.getTimestamp()));
                if (skew.abs().toMillis() >= this.clockSkewCorrectionThreshold) {
                    // The applied correction is the negative skew
                    document.setClockCorrection(skew.negated().toMillis());

                    // Fix the skew on all timestamps of the flow
                    document.setTimestamp(Instant.ofEpochMilli(flow.getTimestamp()).minus(skew).toEpochMilli());
                    document.setFirstSwitched(UInt64Value.of(Instant.ofEpochMilli(flow.getFirstSwitched().getValue()).minus(skew).toEpochMilli()));
                    document.setDeltaSwitched(UInt64Value.of(Instant.ofEpochMilli(flow.getDeltaSwitched().getValue()).minus(skew).toEpochMilli()));
                    document.setLastSwitched(UInt64Value.of(Instant.ofEpochMilli(flow.getLastSwitched().getValue()).minus(skew).toEpochMilli()));
                }
            }

            return Stream.of(document.build());
        }).collect(Collectors.toList());
    }

    private static boolean isPrivateAddress(String ipAddress) {
        final InetAddress inetAddress = InetAddressUtils.addr(ipAddress);
        return inetAddress.isLoopbackAddress() || inetAddress.isLinkLocalAddress() || inetAddress.isSiteLocalAddress();
    }

    private Optional<NodeInfo> getNodeInfo(final String location, final String ipAddress,
                                           final String tenantId) {

        final IpInterfaceDTO iface;
        try {
            iface = inventoryClient.getIpInterfaceFromQuery(tenantId, ipAddress, location);
        } catch (StatusRuntimeException e) {
            if (Status.NOT_FOUND.getCode().equals(e.getStatus().getCode())) {
                return Optional.empty();
            } else {
                throw e;
            }
        }

        if (iface == null) {
            return Optional.empty();
        }
        return Optional.of(NodeInfo.newBuilder()
            .setNodeId(iface.getNodeId())
            .setInterfaceId(iface.getId())
            .setForeignId(iface.getHostname()) // temp until we have better solution
            .build());
    }

    public static ClassificationRequest createClassificationRequest(FlowDocument.Builder document) {
        final ClassificationRequest request = new ClassificationRequest();
        if (document.hasProtocol()) {
            request.setProtocol(Protocols.getProtocol(document.getProtocol().getValue()));
        }
        request.setLocation(document.getLocation());
        request.setExporterAddress(document.getHost());
        request.setDstAddress(document.getDstAddress());
        if (document.hasDstPort()) {
            request.setDstPort(document.getDstPort().getValue());
        }
        request.setSrcAddress(document.getSrcAddress());
        if (document.hasSrcPort()) {
            request.setSrcPort(document.getSrcPort().getValue());
        }

        return request;
    }
}
