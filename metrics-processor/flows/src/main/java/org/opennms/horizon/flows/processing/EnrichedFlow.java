/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
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

import org.opennms.horizon.grpc.flows.contract.FlowDocument;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public class EnrichedFlow {
    enum NetflowVersion {
        V5,
        V9,
        IPFIX,
        SFLOW,
    }

    enum Direction {
        INGRESS,
        EGRESS,
        UNKNOWN,
    }

    enum SamplingAlgorithm {
        Unassigned,
        SystematicCountBasedSampling,
        SystematicTimeBasedSampling,
        RandomNOutOfNSampling,
        UniformProbabilisticSampling,
        PropertyMatchFiltering,
        HashBasedFiltering,
        FlowStateDependentIntermediateFlowSelectionProcess;
    }

    public enum Locality {
        PUBLIC, PRIVATE
    }

    private Instant receivedAt;
    private Instant timestamp;
    private Long bytes;
    private Direction direction;
    private String dstAddr;
    private String dstAddrHostname;
    private Long dstAs;
    private Integer dstMaskLen;
    private Integer dstPort;
    private Integer engineId;
    private Integer engineType;
    private Instant deltaSwitched;
    private Instant firstSwitched;
    private int flowRecords;
    private long flowSeqNum;
    private Integer inputSnmp;
    private Integer ipProtocolVersion;
    private Instant lastSwitched;
    private String nextHop;
    private String nextHopHostname;
    private Long outputSnmp;
    private Long packets;
    private Integer protocol;
    private SamplingAlgorithm samplingAlgorithm;
    private Double samplingInterval;
    private String srcAddr;
    private String srcAddrHostname;
    private Long srcAs;
    private Integer srcMaskLen;
    private Integer srcPort;
    private Integer tcpFlags;
    private Integer tos;
    private Integer dscp;
    private Integer ecn;
    private NetflowVersion netflowVersion;
    private Integer vlan;

    private String application;
    private String host;
    private String location;
    private Locality srcLocality;
    private Locality dstLocality;
    private Locality flowLocality;
    private NodeInfo srcNodeInfo;
    private NodeInfo dstNodeInfo;
    private NodeInfo exporterNodeInfo;
    private Duration clockCorrection;

    public EnrichedFlow() {
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }


    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }


    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }


    public Locality getSrcLocality() {
        return srcLocality;
    }

    public void setSrcLocality(Locality srcLocality) {
        this.srcLocality = srcLocality;
    }


    public Locality getDstLocality() {
        return dstLocality;
    }

    public void setDstLocality(Locality dstLocality) {
        this.dstLocality = dstLocality;
    }


    public Locality getFlowLocality() {
        return flowLocality;
    }

    public void setFlowLocality(Locality flowLocality) {
        this.flowLocality = flowLocality;
    }


    public NodeInfo getSrcNodeInfo() {
        return srcNodeInfo;
    }

    public void setSrcNodeInfo(NodeInfo srcNodeInfo) {
        this.srcNodeInfo = srcNodeInfo;
    }

    public NodeInfo getDstNodeInfo() {
        return dstNodeInfo;
    }

    public void setDstNodeInfo(NodeInfo dstNodeInfo) {
        this.dstNodeInfo = dstNodeInfo;
    }

    public NodeInfo getExporterNodeInfo() {
        return exporterNodeInfo;
    }

    public void setExporterNodeInfo(NodeInfo exporterNodeInfo) {
        this.exporterNodeInfo = exporterNodeInfo;
    }


    public Duration getClockCorrection() {
        return this.clockCorrection;
    }

    public void setClockCorrection(final Duration clockCorrection) {
        this.clockCorrection = clockCorrection;
    }


    public Instant getTimestamp() {
        return this.timestamp;
    }


    public Instant getFirstSwitched() {
        return this.firstSwitched;
    }


    public Instant getDeltaSwitched() {
        return this.deltaSwitched;
    }


    public Instant getLastSwitched() {
        return this.lastSwitched;
    }


    public Instant getReceivedAt() {
        return this.receivedAt;
    }


    public Long getBytes() {
        return this.bytes;
    }


    public Direction getDirection() {
        return this.direction;
    }


    public String getDstAddr() {
        return this.dstAddr;
    }


    public Optional<String> getDstAddrHostname() {
        return Optional.ofNullable(this.dstAddrHostname);
    }


    public Long getDstAs() {
        return this.dstAs;
    }


    public Integer getDstMaskLen() {
        return this.dstMaskLen;
    }


    public Integer getDstPort() {
        return this.dstPort;
    }


    public Integer getEngineId() {
        return this.engineId;
    }


    public Integer getEngineType() {
        return this.engineType;
    }

    public void setDeltaSwitched(final Instant deltaSwitched) {
        this.deltaSwitched = deltaSwitched;
    }

    public void setFirstSwitched(final Instant firstSwitched) {
        this.firstSwitched = firstSwitched;
    }


    public int getFlowRecords() {
        return this.flowRecords;
    }


    public long getFlowSeqNum() {
        return this.flowSeqNum;
    }


    public Integer getInputSnmp() {
        return this.inputSnmp;
    }


    public Integer getIpProtocolVersion() {
        return this.ipProtocolVersion;
    }

    public void setLastSwitched(final Instant lastSwitched) {
        this.lastSwitched = lastSwitched;
    }


    public String getNextHop() {
        return this.nextHop;
    }


    public Optional<String> getNextHopHostname() {
        return Optional.ofNullable(this.nextHopHostname);
    }


    public Long getOutputSnmp() {
        return this.outputSnmp;
    }


    public Long getPackets() {
        return this.packets;
    }


    public Integer getProtocol() {
        return this.protocol;
    }


    public SamplingAlgorithm getSamplingAlgorithm() {
        return this.samplingAlgorithm;
    }


    public Double getSamplingInterval() {
        return this.samplingInterval;
    }


    public String getSrcAddr() {
        return this.srcAddr;
    }


    public Optional<String> getSrcAddrHostname() {
        return Optional.ofNullable(this.srcAddrHostname);
    }


    public Long getSrcAs() {
        return this.srcAs;
    }


    public Integer getSrcMaskLen() {
        return this.srcMaskLen;
    }


    public Integer getSrcPort() {
        return this.srcPort;
    }


    public Integer getTcpFlags() {
        return this.tcpFlags;
    }


    public Integer getTos() {
        return this.tos;
    }


    public NetflowVersion getNetflowVersion() {
        return this.netflowVersion;
    }


    public Integer getVlan() {
        return this.vlan;
    }

    public void setReceivedAt(final Instant receivedAt) {
        this.receivedAt = receivedAt;
    }

    public void setTimestamp(final Instant timestamp) {
        this.timestamp = timestamp;
    }

    public void setBytes(final Long bytes) {
        this.bytes = bytes;
    }

    public void setDirection(final Direction direction) {
        this.direction = direction;
    }

    public void setDstAddr(final String dstAddr) {
        this.dstAddr = dstAddr;
    }

    public void setDstAddrHostname(final String dstAddrHostname) {
        this.dstAddrHostname = dstAddrHostname;
    }

    public void setDstAs(final Long dstAs) {
        this.dstAs = dstAs;
    }

    public void setDstMaskLen(final Integer dstMaskLen) {
        this.dstMaskLen = dstMaskLen;
    }

    public void setDstPort(final Integer dstPort) {
        this.dstPort = dstPort;
    }

    public void setEngineId(final Integer engineId) {
        this.engineId = engineId;
    }

    public void setEngineType(final Integer engineType) {
        this.engineType = engineType;
    }

    public void setFlowRecords(final int flowRecords) {
        this.flowRecords = flowRecords;
    }

    public void setFlowSeqNum(final long flowSeqNum) {
        this.flowSeqNum = flowSeqNum;
    }

    public void setInputSnmp(final Integer inputSnmp) {
        this.inputSnmp = inputSnmp;
    }

    public void setIpProtocolVersion(final Integer ipProtocolVersion) {
        this.ipProtocolVersion = ipProtocolVersion;
    }

    public void setNextHop(final String nextHop) {
        this.nextHop = nextHop;
    }

    public void setNextHopHostname(final String nextHopHostname) {
        this.nextHopHostname = nextHopHostname;
    }

    public void setOutputSnmp(final Long outputSnmp) {
        this.outputSnmp = outputSnmp;
    }

    public void setPackets(final Long packets) {
        this.packets = packets;
    }

    public void setProtocol(final Integer protocol) {
        this.protocol = protocol;
    }

    public void setSamplingAlgorithm(final SamplingAlgorithm samplingAlgorithm) {
        this.samplingAlgorithm = samplingAlgorithm;
    }

    public void setSamplingInterval(final Double samplingInterval) {
        this.samplingInterval = samplingInterval;
    }

    public void setSrcAddr(final String srcAddr) {
        this.srcAddr = srcAddr;
    }

    public void setSrcAddrHostname(final String srcAddrHostname) {
        this.srcAddrHostname = srcAddrHostname;
    }

    public void setSrcAs(final Long srcAs) {
        this.srcAs = srcAs;
    }

    public void setSrcMaskLen(final Integer srcMaskLen) {
        this.srcMaskLen = srcMaskLen;
    }

    public void setSrcPort(final Integer srcPort) {
        this.srcPort = srcPort;
    }

    public void setTcpFlags(final Integer tcpFlags) {
        this.tcpFlags = tcpFlags;
    }

    public void setTos(final Integer tos) {
        this.tos = tos;
    }


    public Integer getDscp() {
        return this.dscp;
    }

    public void setDscp(final Integer dscp) {
        this.dscp = dscp;
    }


    public Integer getEcn() {
        return this.ecn;
    }

    public void setEcn(final Integer ecn) {
        this.ecn = ecn;
    }

    public void setNetflowVersion(final NetflowVersion netflowVersion) {
        this.netflowVersion = netflowVersion;
    }

    public void setVlan(final Integer vlan) {
        this.vlan = vlan;
    }


    public String getConvoKey() {
        return ConversationKeyUtils.getConvoKeyAsJsonString(this.getLocation(),
                                                            this.getProtocol(),
                                                            this.getSrcAddr(),
                                                            this.getDstAddr(),
                                                            this.getApplication());
    }

    public static EnrichedFlow from(final FlowDocument flow) {
        final var enriched = new EnrichedFlow();

        enriched.setReceivedAt(Instant.ofEpochMilli(flow.getTimestamp()));
        enriched.setTimestamp(Instant.ofEpochMilli(flow.getTimestamp()));
        enriched.setDirection(Direction.valueOf(flow.getDirection().name()));
        enriched.setDstAddr(flow.getDstAddress());
        enriched.setDstAddrHostname(flow.getDstHostname());
        enriched.setDstAs(flow.getDstAs().getValue());
        enriched.setDstMaskLen(flow.getDstMaskLen().getValue());
        enriched.setDstPort(flow.getDstPort().getValue());
        enriched.setEngineId(flow.getEngineId().getValue());
        enriched.setEngineType(flow.getEngineType().getValue());
        enriched.setDeltaSwitched(Instant.ofEpochMilli(flow.getDeltaSwitched().getValue()));
        enriched.setFirstSwitched(Instant.ofEpochMilli(flow.getFirstSwitched().getValue()));
        enriched.setFlowRecords(flow.getNumFlowRecords().getValue());
        enriched.setFlowSeqNum(flow.getFlowSeqNum().getValue());
        enriched.setInputSnmp(flow.getInputSnmpIfindex().getValue());
        enriched.setIpProtocolVersion(flow.getIpProtocolVersion().getValue());
        enriched.setLastSwitched(Instant.ofEpochMilli(flow.getLastSwitched().getValue()));
        enriched.setNextHop(flow.getNextHopAddress());
        enriched.setNextHopHostname(flow.getNextHopHostname());
        // temp cast to Long (notified cloud storage InputSnmp & OutputSnmp are not the same datatype)
        enriched.setOutputSnmp(Long.valueOf(flow.getOutputSnmpIfindex().getValue()));
        enriched.setPackets(flow.getNumPackets().getValue());
        enriched.setProtocol(flow.getProtocol().getValue());
        enriched.setSamplingAlgorithm(convertSamplingAlgorithm(flow.getSamplingAlgorithm()));
        enriched.setSamplingInterval(flow.getSamplingInterval().getValue());
        enriched.setSrcAddr(flow.getSrcAddress());
        enriched.setSrcAddrHostname(flow.getSrcHostname());
        enriched.setSrcAs(flow.getSrcAs().getValue());
        enriched.setSrcMaskLen(flow.getSrcMaskLen().getValue());
        enriched.setSrcPort(flow.getSrcPort().getValue());
        enriched.setTcpFlags(flow.getTcpFlags().getValue());
        enriched.setTos(flow.getTos().getValue());
        enriched.setDscp(flow.getDscp().getValue());
        enriched.setEcn(flow.getEcn().getValue());
        enriched.setNetflowVersion(NetflowVersion.valueOf(flow.getNetflowVersion().name()));
        enriched.setVlan(flow.getVlan().getValue());

        return enriched;
    }

    static SamplingAlgorithm convertSamplingAlgorithm(org.opennms.horizon.grpc.flows.contract.SamplingAlgorithm samplingAlgorithm){
        switch(samplingAlgorithm){
            case UNASSIGNED:
                return SamplingAlgorithm.Unassigned;
            case SYSTEMATIC_COUNT_BASED_SAMPLING:
                return SamplingAlgorithm.SystematicCountBasedSampling;
            case SYSTEMATIC_TIME_BASED_SAMPLING:
                return SamplingAlgorithm.SystematicTimeBasedSampling;
            case RANDOM_N_OUT_OF_N_SAMPLING:
                return SamplingAlgorithm.RandomNOutOfNSampling;
            case UNIFORM_PROBABILISTIC_SAMPLING:
                return SamplingAlgorithm.UniformProbabilisticSampling;
            case PROPERTY_MATCH_FILTERING:
                return SamplingAlgorithm.PropertyMatchFiltering;
            case HASH_BASED_FILTERING:
                return SamplingAlgorithm.HashBasedFiltering;
            case FLOW_STATE_DEPENDENT_INTERMEDIATE_FLOW_SELECTION_PROCESS:
                return SamplingAlgorithm.FlowStateDependentIntermediateFlowSelectionProcess;
            default:
                return SamplingAlgorithm.Unassigned;
        }
    }
}
