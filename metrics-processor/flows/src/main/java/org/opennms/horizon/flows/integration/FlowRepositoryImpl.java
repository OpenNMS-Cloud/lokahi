package org.opennms.horizon.flows.integration;

import lombok.RequiredArgsConstructor;
import org.opennms.dataplatform.flows.document.FlowDocument;
import org.opennms.dataplatform.flows.document.NodeInfo;
import org.opennms.dataplatform.flows.ingester.v1.StoreFlowDocumentsRequest;
import org.opennms.horizon.flows.document.TenantLocationSpecificFlowDocumentLog;
import org.opennms.horizon.flows.grpc.client.IngestorClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.List;

@RequiredArgsConstructor
public class FlowRepositoryImpl implements FlowRepository {
    private static final Logger LOG = LoggerFactory.getLogger(FlowRepositoryImpl.class);
    private final IngestorClient ingestorClient;

    @Override
    public void persist(TenantLocationSpecificFlowDocumentLog enrichedFlowsLog) {
        LOG.info("Persisting flow data: {}", enrichedFlowsLog);
        var enrichedFlows = enrichedFlowsLog.getMessageList();

        if (CollectionUtils.isEmpty(enrichedFlows)) {
            LOG.trace("No EnrichedFlow present, skipping flow data persisting step. ");
            return;
        }

        List<FlowDocument> listDataPlatformFlowDocuments =
            enrichedFlows.stream().map(this::mapFlowDocument).toList();

        StoreFlowDocumentsRequest storeFlowDocumentsRequest = StoreFlowDocumentsRequest.newBuilder()
            .addAllDocuments(listDataPlatformFlowDocuments)
            .build();

        ingestorClient.sendData(
            storeFlowDocumentsRequest,
            enrichedFlowsLog.getTenantId()
        );
    }

//========================================
// Internals
//----------------------------------------

    // NOTE: this will hopefully be simplified in the future
    private FlowDocument mapFlowDocument(org.opennms.horizon.flows.document.FlowDocument flowDocument) {
        FlowDocument result =
            FlowDocument.newBuilder()
                .setTimestamp(flowDocument.getTimestamp())
                .setNumBytes(flowDocument.getNumBytes())
                .setDirectionValue(flowDocument.getDirection().getNumber())
                .setDstAddress(flowDocument.getDstAddress())
                .setDstHostname(flowDocument.getDstHostname())
                .setDstAs(flowDocument.getDstAs())
                .setDstMaskLen(flowDocument.getDstMaskLen())
                .setDstPort(flowDocument.getDstPort())
                .setEngineId(flowDocument.getEngineId())
                .setEngineType(flowDocument.getEngineType())
                .setDeltaSwitched(flowDocument.getDeltaSwitched())
                .setFirstSwitched(flowDocument.getFirstSwitched())
                .setLastSwitched(flowDocument.getLastSwitched())
                .setNumFlowRecords(flowDocument.getNumFlowRecords())
                .setNumPackets(flowDocument.getNumPackets())
                .setFlowSeqNum(flowDocument.getFlowSeqNum())
                .setInputSnmpIfindex(flowDocument.getInputSnmpIfindex())
                .setOutputSnmpIfindex(flowDocument.getOutputSnmpIfindex())
                .setIpProtocolVersion(flowDocument.getIpProtocolVersion())
                .setNextHopAddress(flowDocument.getNextHopAddress())
                .setNextHopHostname(flowDocument.getNextHopHostname())
                .setProtocol(flowDocument.getProtocol())
                .setSamplingAlgorithmValue(flowDocument.getSamplingAlgorithm().getNumber())
                .setSamplingInterval(flowDocument.getSamplingInterval())
                .setSrcAddress(flowDocument.getSrcAddress())
                .setSrcHostname(flowDocument.getSrcHostname())
                .setSrcAs(flowDocument.getSrcAs())
                .setSrcMaskLen(flowDocument.getSrcMaskLen())
                .setSrcPort(flowDocument.getSrcPort())
                .setTcpFlags(flowDocument.getTcpFlags())
                .setTos(flowDocument.getTos())
                .setNetflowVersionValue(flowDocument.getNetflowVersion().getNumber())
                .setVlan(flowDocument.getVlan())
                .setSrcNode(
                    mapNodeInfoToDataPlatform(flowDocument.getSrcNode())
                )
                .setExporterNode(
                    mapNodeInfoToDataPlatform(flowDocument.getExporterNode())
                )
                .setDestNode(
                    mapNodeInfoToDataPlatform(flowDocument.getDestNode())
                )
                .setApplication(flowDocument.getApplication())
                .setHost(flowDocument.getHost())
                .setSrcLocalityValue(flowDocument.getSrcLocality().getNumber())
                .setDstLocalityValue(flowDocument.getDstLocality().getNumber())
                .setFlowLocalityValue(flowDocument.getFlowLocality().getNumber())
                .setClockCorrection(flowDocument.getClockCorrection())
                .setDscp(flowDocument.getDscp())
                .setEcn(flowDocument.getEcn())
                .setExporterAddress(flowDocument.getExporterAddress())
                .setExporterPort(flowDocument.getExporterPort())
                .setExporterIdentifier(flowDocument.getExporterIdentifier())
                .setReceivedAt(flowDocument.getReceivedAt())
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
