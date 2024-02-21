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

import com.google.protobuf.UInt64Value;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.opennms.horizon.flows.classification.ClassificationEngine;
import org.opennms.horizon.flows.classification.ClassificationRequest;
import org.opennms.horizon.flows.document.FlowDocument;
import org.opennms.horizon.flows.document.Locality;
import org.opennms.horizon.flows.document.NodeInfo;
import org.opennms.horizon.flows.document.TenantLocationSpecificFlowDocumentLog;
import org.opennms.horizon.flows.grpc.client.InventoryClient;
import org.opennms.horizon.inventory.dto.IpInterfaceDTO;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentEnricherImpl {
    private static final Logger LOG = LoggerFactory.getLogger(DocumentEnricherImpl.class);

    private InventoryClient inventoryClient;

    private final ClassificationEngine classificationEngine;

    private final long clockSkewCorrectionThreshold;
    private final FlowDocumentClassificationRequestMapper flowDocumentBuilderClassificationRequestMapper;

    public DocumentEnricherImpl(
            InventoryClient inventoryClient,
            ClassificationEngine classificationEngine,
            FlowDocumentClassificationRequestMapper flowDocumentClassificationRequestMapper,
            long clockSkewCorrectionThreshold) {

        this.inventoryClient = Objects.requireNonNull(inventoryClient);
        this.classificationEngine = Objects.requireNonNull(classificationEngine);
        this.flowDocumentBuilderClassificationRequestMapper = flowDocumentClassificationRequestMapper;

        this.clockSkewCorrectionThreshold = clockSkewCorrectionThreshold;
    }

    public List<FlowDocument> enrich(TenantLocationSpecificFlowDocumentLog flowsLog) {
        var flows = flowsLog.getMessageList();
        if (flows.isEmpty()) {
            LOG.info("Nothing to enrich for tenant-id={}", flowsLog.getTenantId());
            return Collections.emptyList();
        }

        return flows.stream()
                .map(f -> this.enrichOne(f, flowsLog.getTenantId(), flowsLog.getLocationId()))
                .toList();
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
            if (!Status.NOT_FOUND.getCode().equals(e.getStatus().getCode())) {
                LOG.warn(
                        "Fail to get NodeInfo ipAddress: {} location: {} unknown error: {}",
                        ipAddress,
                        location,
                        e.getStatus());
            }
            return null;
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
    private FlowDocument enrichOne(FlowDocument flow, String tenantId, String location) {
        var document = FlowDocument.newBuilder(flow); // Can never return null

        // Node data
        Optional.ofNullable(getNodeInfo(location, flow.getExporterAddress(), tenantId))
                .ifPresent(document::setExporterNode);
        Optional.ofNullable(getNodeInfo(location, flow.getSrcAddress(), tenantId))
                .ifPresent(document::setSrcNode);
        Optional.ofNullable(getNodeInfo(location, flow.getDstAddress(), tenantId))
                .ifPresent(document::setDestNode);

        // Locality
        document.setSrcLocality(isPrivateAddress(flow.getSrcAddress()) ? Locality.PRIVATE : Locality.PUBLIC);
        document.setDstLocality(isPrivateAddress(flow.getDstAddress()) ? Locality.PRIVATE : Locality.PUBLIC);

        if (Locality.PUBLIC.equals(document.getDstLocality()) || Locality.PUBLIC.equals(document.getSrcLocality())) {
            document.setFlowLocality(Locality.PUBLIC);
        } else if (Locality.PRIVATE.equals(document.getDstLocality())
                || Locality.PRIVATE.equals(document.getSrcLocality())) {
            document.setFlowLocality(Locality.PRIVATE);
        }

        ClassificationRequest classificationRequest =
                flowDocumentBuilderClassificationRequestMapper.createClassificationRequest(document.build(), location);

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
            final var skew = Duration.between(
                    Instant.ofEpochMilli(flow.getReceivedAt()), Instant.ofEpochMilli(flow.getTimestamp()));
            if (skew.abs().toMillis() >= this.clockSkewCorrectionThreshold) {
                // The applied correction is the negative skew
                document.setClockCorrection(skew.negated().toMillis());

                // Fix the skew on all timestamps of the flow
                document.setTimestamp(
                        Instant.ofEpochMilli(flow.getTimestamp()).minus(skew).toEpochMilli());
                document.setFirstSwitched(UInt64Value.of(
                        Instant.ofEpochMilli(flow.getFirstSwitched().getValue())
                                .minus(skew)
                                .toEpochMilli()));
                document.setDeltaSwitched(UInt64Value.of(
                        Instant.ofEpochMilli(flow.getDeltaSwitched().getValue())
                                .minus(skew)
                                .toEpochMilli()));
                document.setLastSwitched(UInt64Value.of(
                        Instant.ofEpochMilli(flow.getLastSwitched().getValue())
                                .minus(skew)
                                .toEpochMilli()));
            }
        }

        return document.build();
    }
}
