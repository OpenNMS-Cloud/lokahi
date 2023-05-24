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

import com.google.protobuf.UInt64Value;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.opennms.horizon.flows.document.Locality;
import org.opennms.horizon.flows.document.NodeInfo;
import org.opennms.horizon.flows.classification.ClassificationEngine;
import org.opennms.horizon.flows.classification.ClassificationRequest;
import org.opennms.horizon.flows.document.TenantLocationSpecificFlowDocument;
import org.opennms.horizon.flows.grpc.client.InventoryClient;
import org.opennms.horizon.inventory.dto.IpInterfaceDTO;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class DocumentEnricherImpl {
    private static final Logger LOG = LoggerFactory.getLogger(DocumentEnricherImpl.class);

    private InventoryClient inventoryClient;

    private final ClassificationEngine classificationEngine;

    private final long clockSkewCorrectionThreshold;
    private final FlowDocumentClassificationRequestMapper flowDocumentBuilderClassificationRequestMapper;

    public DocumentEnricherImpl(InventoryClient inventoryClient,
                                ClassificationEngine classificationEngine,
                                FlowDocumentClassificationRequestMapper flowDocumentClassificationRequestMapper,
                                long clockSkewCorrectionThreshold) {

        this.inventoryClient = Objects.requireNonNull(inventoryClient);
        this.classificationEngine = Objects.requireNonNull(classificationEngine);
        this.flowDocumentBuilderClassificationRequestMapper = flowDocumentClassificationRequestMapper;

        this.clockSkewCorrectionThreshold = clockSkewCorrectionThreshold;
    }

    public List<TenantLocationSpecificFlowDocument> enrich(Collection<TenantLocationSpecificFlowDocument> flows) {
        if (flows.isEmpty()) {
            LOG.info("Nothing to enrich.");
            return Collections.emptyList();
        }

        return flows.stream().map(this::enrichOne).collect(Collectors.toList());
    }

    private boolean isPrivateAddress(String ipAddress) {
        final InetAddress inetAddress = InetAddressUtils.addr(ipAddress);
        return inetAddress.isLoopbackAddress() || inetAddress.isLinkLocalAddress() || inetAddress.isSiteLocalAddress();
    }

    private NodeInfo getNodeInfo(String location, String ipAddress, String tenantId) {

        IpInterfaceDTO iface;
        try {
            iface = inventoryClient.getIpInterfaceFromQuery(tenantId, ipAddress, location);
        } catch (StatusRuntimeException e) {
            if (Status.NOT_FOUND.getCode().equals(e.getStatus().getCode())) {
                return null;
            } else {
                throw e;
            }
        }

        if (iface == null) {
            return null;
        }
        return NodeInfo.newBuilder()
            .setNodeId(iface.getNodeId())
            .setInterfaceId(iface.getId())
            .setForeignId(iface.getHostname()) // temp until we have better solution
            .build();
    }

    // Note that protobuf semantics prevent nulls in many places here
    private TenantLocationSpecificFlowDocument enrichOne(TenantLocationSpecificFlowDocument flow) {
        var document = TenantLocationSpecificFlowDocument.newBuilder(flow);     // Can never return null

        String tenantId = flow.getTenantId();
        String location = flow.getLocation();

        // Node data
        Optional.ofNullable(getNodeInfo(location, flow.getExporterAddress(), tenantId)).ifPresent(document::setExporterNode);
        Optional.ofNullable(getNodeInfo(location, flow.getSrcAddress(), tenantId)).ifPresent(document::setSrcNode);
        Optional.ofNullable(getNodeInfo(location, flow.getDstAddress(), tenantId)).ifPresent(document::setDestNode);

        // Locality
        document.setSrcLocality(isPrivateAddress(flow.getSrcAddress()) ? Locality.PRIVATE : Locality.PUBLIC);
        document.setDstLocality(isPrivateAddress(flow.getDstAddress()) ? Locality.PRIVATE : Locality.PUBLIC);

        if (Locality.PUBLIC.equals(document.getDstLocality()) || Locality.PUBLIC.equals(document.getSrcLocality())) {
            document.setFlowLocality(Locality.PUBLIC);
        } else if (Locality.PRIVATE.equals(document.getDstLocality()) || Locality.PRIVATE.equals(document.getSrcLocality())) {
            document.setFlowLocality(Locality.PRIVATE);
        }

        ClassificationRequest classificationRequest =
            flowDocumentBuilderClassificationRequestMapper.createClassificationRequest(document.build());

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

        return document.build();
    }
}
