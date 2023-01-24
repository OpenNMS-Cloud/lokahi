/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2020 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2020 The OpenNMS Group, Inc.
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

package org.opennms.horizon.flows.adapter.common;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import org.opennms.horizon.flows.processing.enrichment.Flow;
import org.opennms.horizon.flows.processing.enrichment.Locality;
import org.opennms.horizon.flows.processing.enrichment.NodeInfo;
import org.opennms.horizon.minion.flows.parser.flowmessage.Direction;
import org.opennms.horizon.minion.flows.parser.flowmessage.FlowMessage;
import org.opennms.horizon.minion.flows.parser.flowmessage.NetflowVersion;
import org.opennms.horizon.minion.flows.parser.flowmessage.SamplingAlgorithm;

import com.google.common.base.Strings;

public class NetflowMessage implements Flow {

    private final FlowMessage flowMessageProto;
    private final Instant receivedAt;

    public NetflowMessage(FlowMessage flowMessageProto, final Instant receivedAt) {
        this.flowMessageProto = flowMessageProto;
        this.receivedAt = Objects.requireNonNull(receivedAt);
    }

    @Override
    public Duration getClockCorrection() {
        return null;
    }

    @Override
    public NodeInfo getExporterNodeInfo() {
        return null;
    }

    @Override
    public NodeInfo getDstNodeInfo() {
        return null;
    }

    @Override
    public NodeInfo getSrcNodeInfo() {
        return null;
    }

    @Override
    public Locality getFlowLocality() {
        return null;
    }

    @Override
    public Locality getDstLocality() {
        return null;
    }

    @Override
    public Locality getSrcLocality() {
        return null;
    }

    @Override
    public String getLocation() {
        return null;
    }

    @Override
    public String getHost() {
        return null;
    }

    @Override
    public String getApplication() {
        return null;
    }

    @Override
    public Instant getReceivedAt() {
        return this.receivedAt;
    }

    @Override
    public Instant getTimestamp() {
        return Instant.ofEpochMilli(flowMessageProto.getTimestamp());
    }

    @Override
    public Long getBytes() {
        return flowMessageProto.hasNumBytes() ? flowMessageProto.getNumBytes().getValue() : null;
    }

    @Override
    public Direction getDirection() {
        switch (flowMessageProto.getDirection()) {
            case INGRESS:
                return Direction.INGRESS;
            case EGRESS:
                return Direction.EGRESS;
            default:
                return Direction.UNKNOWN;
        }
    }

    @Override
    public String getDstAddr() {

        if (!Strings.isNullOrEmpty(flowMessageProto.getDstAddress())) {
            return flowMessageProto.getDstAddress();
        }
        return null;
    }

    @Override
    public Optional<String> getDstAddrHostname() {
        if (!Strings.isNullOrEmpty(flowMessageProto.getDstHostname())) {
            return Optional.of(flowMessageProto.getDstHostname());
        }
        return Optional.empty();
    }

    @Override
    public Long getDstAs() {
        return flowMessageProto.hasDstAs() ? flowMessageProto.getDstAs().getValue() : null;
    }

    @Override
    public Integer getDstMaskLen() {
        return flowMessageProto.hasDstMaskLen() ? flowMessageProto.getDstMaskLen().getValue() : null;
    }

    @Override
    public Integer getDstPort() {
        return flowMessageProto.hasDstPort() ? flowMessageProto.getDstPort().getValue() : null;
    }

    @Override
    public Integer getEngineId() {
        return flowMessageProto.hasEngineId() ? flowMessageProto.getEngineId().getValue() : null;
    }

    @Override
    public Integer getEngineType() {
        return flowMessageProto.hasEngineType() ? flowMessageProto.getEngineType().getValue() : null;
    }

    @Override
    public Instant getDeltaSwitched() {
        return flowMessageProto.hasDeltaSwitched() ? Instant.ofEpochMilli(flowMessageProto.getDeltaSwitched().getValue()) : getFirstSwitched();
    }

    @Override
    public Instant getFirstSwitched() {
        return flowMessageProto.hasFirstSwitched() ? Instant.ofEpochMilli(flowMessageProto.getFirstSwitched().getValue()) : null;
    }

    @Override
    public int getFlowRecords() {
        return flowMessageProto.hasNumFlowRecords() ? flowMessageProto.getNumFlowRecords().getValue() : 0;
    }

    @Override
    public long getFlowSeqNum() {
        return flowMessageProto.hasFlowSeqNum() ? flowMessageProto.getFlowSeqNum().getValue() : 0L;
    }

    @Override
    public Integer getInputSnmp() {
        return flowMessageProto.hasInputSnmpIfindex() ? flowMessageProto.getInputSnmpIfindex().getValue() : null;
    }

    @Override
    public Integer getIpProtocolVersion() {
        return flowMessageProto.hasIpProtocolVersion() ? flowMessageProto.getIpProtocolVersion().getValue() : null;
    }

    @Override
    public Instant getLastSwitched() {
        return flowMessageProto.hasLastSwitched() ? Instant.ofEpochMilli(flowMessageProto.getLastSwitched().getValue()) : null;
    }

    @Override
    public String getNextHop() {
        if (!Strings.isNullOrEmpty(flowMessageProto.getNextHopAddress())) {
            return flowMessageProto.getNextHopAddress();
        }
        return null;
    }

    @Override
    public Optional<String> getNextHopHostname() {
        if (!Strings.isNullOrEmpty(flowMessageProto.getNextHopHostname())) {
            return Optional.of(flowMessageProto.getNextHopHostname());
        }
        return Optional.empty();
    }

    @Override
    public Integer getOutputSnmp() {
        return flowMessageProto.hasOutputSnmpIfindex() ? flowMessageProto.getOutputSnmpIfindex().getValue() : null;
    }

    @Override
    public Long getPackets() {
        return flowMessageProto.hasNumPackets() ? flowMessageProto.getNumPackets().getValue() : null;
    }

    @Override
    public Integer getProtocol() {
        return flowMessageProto.hasProtocol() ? flowMessageProto.getProtocol().getValue() : null;
    }

    @Override
    public SamplingAlgorithm getSamplingAlgorithm() {
        return flowMessageProto.getSamplingAlgorithm();
    }

    @Override
    public Double getSamplingInterval() {
        return flowMessageProto.hasSamplingInterval() ? flowMessageProto.getSamplingInterval().getValue() : null;
    }

    @Override
    public String getSrcAddr() {
        if (!Strings.isNullOrEmpty(flowMessageProto.getSrcAddress())) {
            return flowMessageProto.getSrcAddress();
        }
        return null;
    }

    @Override
    public Optional<String> getSrcAddrHostname() {
        if (!Strings.isNullOrEmpty(flowMessageProto.getSrcHostname())) {
            return Optional.of(flowMessageProto.getSrcHostname());
        }
        return Optional.empty();
    }

    @Override
    public Long getSrcAs() {
        return flowMessageProto.hasSrcAs() ? flowMessageProto.getSrcAs().getValue() : null;
    }

    @Override
    public Integer getSrcMaskLen() {
        return flowMessageProto.hasSrcMaskLen() ? flowMessageProto.getSrcMaskLen().getValue() : null;
    }

    @Override
    public Integer getSrcPort() {
        return flowMessageProto.hasSrcPort() ? flowMessageProto.getSrcPort().getValue() : null;
    }

    @Override
    public Integer getTcpFlags() {
        return flowMessageProto.hasTcpFlags() ? flowMessageProto.getTcpFlags().getValue() : null;
    }

    @Override
    public Integer getTos() {
        return flowMessageProto.hasTos() ? flowMessageProto.getTos().getValue() : null;
    }

    @Override
    public NetflowVersion getNetflowVersion() {
        switch (flowMessageProto.getNetflowVersion()) {
            case V5:
                return NetflowVersion.V5;
            case V9:
                return NetflowVersion.V9;
            case IPFIX:
                return NetflowVersion.IPFIX;
            default:
                return NetflowVersion.V5;
        }
    }

    @Override
    public Integer getVlan() {
        return flowMessageProto.hasVlan() ? flowMessageProto.getVlan().getValue() : null;
    }

    @Override
    public String getNodeIdentifier() {
        if (!Strings.isNullOrEmpty(flowMessageProto.getNodeIdentifier())) {
            return flowMessageProto.getNodeIdentifier();
        }
        return null;
    }
}
