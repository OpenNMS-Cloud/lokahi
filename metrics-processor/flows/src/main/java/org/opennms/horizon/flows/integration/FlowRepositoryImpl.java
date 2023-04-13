package org.opennms.horizon.flows.integration;

import org.apache.commons.lang3.StringUtils;
import org.opennms.dataplatform.flows.document.FlowDocument;
import org.opennms.dataplatform.flows.document.NodeInfo;
import org.opennms.horizon.flows.document.TenantLocationSpecificFlowDocument;
import org.opennms.dataplatform.flows.ingester.v1.StoreFlowDocumentsRequest;

import org.opennms.horizon.flows.grpc.client.IngestorClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FlowRepositoryImpl implements FlowRepository {
    private static final Logger LOG = LoggerFactory.getLogger(FlowRepositoryImpl.class);
    private final IngestorClient ingestorClient;

    @Override
    public void persist(Collection<TenantLocationSpecificFlowDocument> enrichedFlows) {
        LOG.info("Persisting flow data: {}", enrichedFlows.toString());

        if (CollectionUtils.isEmpty(enrichedFlows)) {
            LOG.trace("No EnrichedFlow present, skipping flow data persisting step. ");
            return;
        }

        List<FlowDocument> listDataPlatformFlowDocuments =
            enrichedFlows.stream().map(this::mapFlowDocument).collect(Collectors.toList());

        StoreFlowDocumentsRequest storeFlowDocumentsRequest = StoreFlowDocumentsRequest.newBuilder()
            .addAllDocuments(listDataPlatformFlowDocuments)
            .build();

        ingestorClient.sendData(
            storeFlowDocumentsRequest,
            StringUtils.join(enrichedFlows.stream().map(TenantLocationSpecificFlowDocument::getTenantId), ",")
        );
    }

//========================================
// Internals
//----------------------------------------

    // NOTE: this will hopefully be simplified in the future
    private FlowDocument mapFlowDocument(TenantLocationSpecificFlowDocument tenantLocationSpecificFlowDocument) {
        FlowDocument result =
            FlowDocument.newBuilder()
                .setTimestamp(tenantLocationSpecificFlowDocument.getTimestamp())
                .setNumBytes(tenantLocationSpecificFlowDocument.getNumBytes())
                .setDirectionValue(tenantLocationSpecificFlowDocument.getDirection().getNumber())
                .setDstAddress(tenantLocationSpecificFlowDocument.getDstAddress())
                .setDstHostname(tenantLocationSpecificFlowDocument.getDstHostname())
                .setDstAs(tenantLocationSpecificFlowDocument.getDstAs())
                .setDstMaskLen(tenantLocationSpecificFlowDocument.getDstMaskLen())
                .setDstPort(tenantLocationSpecificFlowDocument.getDstPort())
                .setEngineId(tenantLocationSpecificFlowDocument.getEngineId())
                .setEngineType(tenantLocationSpecificFlowDocument.getEngineType())
                .setDeltaSwitched(tenantLocationSpecificFlowDocument.getDeltaSwitched())
                .setFirstSwitched(tenantLocationSpecificFlowDocument.getFirstSwitched())
                .setLastSwitched(tenantLocationSpecificFlowDocument.getLastSwitched())
                .setNumFlowRecords(tenantLocationSpecificFlowDocument.getNumFlowRecords())
                .setNumPackets(tenantLocationSpecificFlowDocument.getNumPackets())
                .setFlowSeqNum(tenantLocationSpecificFlowDocument.getFlowSeqNum())
                .setInputSnmpIfindex(tenantLocationSpecificFlowDocument.getInputSnmpIfindex())
                .setOutputSnmpIfindex(tenantLocationSpecificFlowDocument.getOutputSnmpIfindex())
                .setIpProtocolVersion(tenantLocationSpecificFlowDocument.getIpProtocolVersion())
                .setNextHopAddress(tenantLocationSpecificFlowDocument.getNextHopAddress())
                .setNextHopHostname(tenantLocationSpecificFlowDocument.getNextHopHostname())
                .setProtocol(tenantLocationSpecificFlowDocument.getProtocol())
                .setSamplingAlgorithmValue(tenantLocationSpecificFlowDocument.getSamplingAlgorithm().getNumber())
                .setSamplingInterval(tenantLocationSpecificFlowDocument.getSamplingInterval())
                .setSrcAddress(tenantLocationSpecificFlowDocument.getSrcAddress())
                .setSrcHostname(tenantLocationSpecificFlowDocument.getSrcHostname())
                .setSrcAs(tenantLocationSpecificFlowDocument.getSrcAs())
                .setSrcMaskLen(tenantLocationSpecificFlowDocument.getSrcMaskLen())
                .setSrcPort(tenantLocationSpecificFlowDocument.getSrcPort())
                .setTcpFlags(tenantLocationSpecificFlowDocument.getTcpFlags())
                .setTos(tenantLocationSpecificFlowDocument.getTos())
                .setNetflowVersionValue(tenantLocationSpecificFlowDocument.getNetflowVersion().getNumber())
                .setVlan(tenantLocationSpecificFlowDocument.getVlan())
                .setSrcNode(
                    mapNodeInfoToDataPlatform(tenantLocationSpecificFlowDocument.getSrcNode())
                )
                .setExporterNode(
                    mapNodeInfoToDataPlatform(tenantLocationSpecificFlowDocument.getExporterNode())
                )
                .setDestNode(
                    mapNodeInfoToDataPlatform(tenantLocationSpecificFlowDocument.getDestNode())
                )
                .setApplication(tenantLocationSpecificFlowDocument.getApplication())
                .setHost(tenantLocationSpecificFlowDocument.getHost())
                .setSrcLocalityValue(tenantLocationSpecificFlowDocument.getSrcLocality().getNumber())
                .setDstLocalityValue(tenantLocationSpecificFlowDocument.getDstLocality().getNumber())
                .setFlowLocalityValue(tenantLocationSpecificFlowDocument.getFlowLocality().getNumber())
                .setClockCorrection(tenantLocationSpecificFlowDocument.getClockCorrection())
                .setDscp(tenantLocationSpecificFlowDocument.getDscp())
                .setEcn(tenantLocationSpecificFlowDocument.getEcn())
                .setExporterAddress(tenantLocationSpecificFlowDocument.getExporterAddress())
                .setExporterPort(tenantLocationSpecificFlowDocument.getExporterPort())
                .setExporterIdentifier(tenantLocationSpecificFlowDocument.getExporterIdentifier())
                .setReceivedAt(tenantLocationSpecificFlowDocument.getReceivedAt())
                .build();

        return result;
    }

    private NodeInfo mapNodeInfoToDataPlatform(org.opennms.horizon.flows.document.NodeInfo src) {
        NodeInfo result =
            NodeInfo.newBuilder()
                .setForeignSource(src.getForeignSource())
                .setForeignId(src.getForeignId())
                .setNodeId(src.getNodeId())
                .addAllCategories(src.getCategoriesList())
                .setInterfaceId(src.getInterfaceId())
                .build();

        return result;
    }
}
