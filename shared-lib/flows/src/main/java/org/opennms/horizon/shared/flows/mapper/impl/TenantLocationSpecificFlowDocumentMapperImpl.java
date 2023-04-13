package org.opennms.horizon.shared.flows.mapper.impl;

import org.opennms.horizon.flows.document.FlowDocument;
import org.opennms.horizon.flows.document.TenantLocationSpecificFlowDocument;
import org.opennms.horizon.shared.flows.mapper.TenantLocationSpecificFlowDocumentMapper;

/**
 * WARNING: due to the way protobuf objects work, and the simple set(get()) approach to copying fields here, missing
 * fields in the source messages will result in default, non-missing fields in the copy.
 */
public class TenantLocationSpecificFlowDocumentMapperImpl implements TenantLocationSpecificFlowDocumentMapper {
    @Override
    public TenantLocationSpecificFlowDocument mapBareToTenanted(String tenantId, String location, FlowDocument flowDocument) {
        TenantLocationSpecificFlowDocument result =
            TenantLocationSpecificFlowDocument.newBuilder()
                .setTenantId(tenantId)
                .setLocation(location)

                .setTimestamp(flowDocument.getTimestamp())
                .setNumBytes(flowDocument.getNumBytes())
                .setDirection(flowDocument.getDirection())
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
                .setSamplingAlgorithm(flowDocument.getSamplingAlgorithm())
                .setSamplingInterval(flowDocument.getSamplingInterval())
                .setSrcAddress(flowDocument.getSrcAddress())
                .setSrcHostname(flowDocument.getSrcHostname())
                .setSrcAs(flowDocument.getSrcAs())
                .setSrcMaskLen(flowDocument.getSrcMaskLen())
                .setSrcPort(flowDocument.getSrcPort())
                .setTcpFlags(flowDocument.getTcpFlags())
                .setTos(flowDocument.getTos())
                .setNetflowVersion(flowDocument.getNetflowVersion())
                .setVlan(flowDocument.getVlan())
                .setSrcNode(flowDocument.getSrcNode())
                .setExporterNode(flowDocument.getExporterNode())
                .setDestNode(flowDocument.getDestNode())
                .setApplication(flowDocument.getApplication())
                .setHost(flowDocument.getHost())
                .setSrcLocality(flowDocument.getSrcLocality())
                .setDstLocality(flowDocument.getDstLocality())
                .setFlowLocality(flowDocument.getFlowLocality())
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

    @Override
    public FlowDocument mapTenantedToBare(TenantLocationSpecificFlowDocument tenantLocationSpecificFlowDocument) {
        FlowDocument result =
            FlowDocument.newBuilder()
                .setTimestamp(tenantLocationSpecificFlowDocument.getTimestamp())
                .setNumBytes(tenantLocationSpecificFlowDocument.getNumBytes())
                .setDirection(tenantLocationSpecificFlowDocument.getDirection())
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
                .setSamplingAlgorithm(tenantLocationSpecificFlowDocument.getSamplingAlgorithm())
                .setSamplingInterval(tenantLocationSpecificFlowDocument.getSamplingInterval())
                .setSrcAddress(tenantLocationSpecificFlowDocument.getSrcAddress())
                .setSrcHostname(tenantLocationSpecificFlowDocument.getSrcHostname())
                .setSrcAs(tenantLocationSpecificFlowDocument.getSrcAs())
                .setSrcMaskLen(tenantLocationSpecificFlowDocument.getSrcMaskLen())
                .setSrcPort(tenantLocationSpecificFlowDocument.getSrcPort())
                .setTcpFlags(tenantLocationSpecificFlowDocument.getTcpFlags())
                .setTos(tenantLocationSpecificFlowDocument.getTos())
                .setNetflowVersion(tenantLocationSpecificFlowDocument.getNetflowVersion())
                .setVlan(tenantLocationSpecificFlowDocument.getVlan())
                .setSrcNode(tenantLocationSpecificFlowDocument.getSrcNode())
                .setExporterNode(tenantLocationSpecificFlowDocument.getExporterNode())
                .setDestNode(tenantLocationSpecificFlowDocument.getDestNode())
                .setApplication(tenantLocationSpecificFlowDocument.getApplication())
                .setHost(tenantLocationSpecificFlowDocument.getHost())
                .setSrcLocality(tenantLocationSpecificFlowDocument.getSrcLocality())
                .setDstLocality(tenantLocationSpecificFlowDocument.getDstLocality())
                .setFlowLocality(tenantLocationSpecificFlowDocument.getFlowLocality())
                .setClockCorrection(tenantLocationSpecificFlowDocument.getClockCorrection())
                .setDscp(tenantLocationSpecificFlowDocument.getDscp())
                .setEcn(tenantLocationSpecificFlowDocument.getEcn())
                .setExporterAddress(tenantLocationSpecificFlowDocument.getExporterAddress())
                .setExporterPort(tenantLocationSpecificFlowDocument.getExporterPort())
                .setExporterIdentifier(tenantLocationSpecificFlowDocument.getExporterIdentifier())
                .setReceivedAt(tenantLocationSpecificFlowDocument.getReceivedAt())
                .build()
                ;

        return result;
    }
}
