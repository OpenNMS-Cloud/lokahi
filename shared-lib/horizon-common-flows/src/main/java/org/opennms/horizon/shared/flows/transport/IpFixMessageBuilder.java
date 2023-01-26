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

package org.opennms.horizon.shared.flows.transport;

import java.net.InetAddress;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;


import com.google.common.primitives.UnsignedLong;
import com.google.protobuf.UInt32Value;

import org.opennms.horizon.shared.flows.Direction;
import org.opennms.horizon.shared.flows.FlowMessage;
import org.opennms.horizon.shared.flows.NetflowVersion;
import org.opennms.horizon.shared.flows.RecordEnrichment;
import org.opennms.horizon.shared.flows.SamplingAlgorithm;
import org.opennms.horizon.shared.flows.values.Value;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class IpFixMessageBuilder implements MessageBuilder {

    @Override
    public FlowMessage.Builder buildMessage(final Iterable<Value<?>> values, final RecordEnrichment enrichment) {
        final FlowMessage.Builder builder = FlowMessage.newBuilder();

        Long exportTime = null;
        Long octetDeltaCount = null;
        Long postOctetDeltaCount = null;
        Long layer2OctetDeltaCount = null;
        Long postLayer2OctetDeltaCount = null;
        Long transportOctetDeltaCount = null;
        InetAddress destinationIPv6Address = null;
        InetAddress destinationIPv4Address = null;
        Long destinationIPv6PrefixLength = null;
        Long destinationIPv4PrefixLength = null;
        Instant flowStartSeconds = null;
        Instant flowStartMilliseconds = null;
        Instant flowStartMicroseconds = null;
        Instant flowStartNanoseconds = null;
        Long flowStartDeltaMicroseconds = null;
        Long flowStartSysUpTime = null;
        Instant systemInitTimeMilliseconds = null;
        Instant flowEndSeconds = null;
        Instant flowEndMilliseconds = null;
        Instant flowEndMicroseconds = null;
        Instant flowEndNanoseconds = null;
        Long flowEndDeltaMicroseconds = null;
        Long flowEndSysUpTime = null;
        InetAddress ipNextHopIPv6Address = null;
        InetAddress ipNextHopIPv4Address = null;
        InetAddress bgpNextHopIPv6Address = null;
        InetAddress bgpNextHopIPv4Address = null;
        Long packetDeltaCount = null;
        Long postPacketDeltaCount = null;
        Long transportPacketDeltaCount = null;
        Long samplingAlgorithm = null;
        Long samplerMode = null;
        Long selectorAlgorithm = null;
        Long samplingInterval = null;
        Long samplerRandomInterval = null;
        Long samplingFlowInterval = null;
        Long samplingFlowSpacing = null;
        Long flowSamplingTimeInterval = null;
        Long samplingSize = null;
        Long samplingPopulation = null;
        Long samplingProbability = null;
        Long hashSelectedRangeMin = null;
        Long hashSelectedRangeMax = null;
        Long hashOutputRangeMin = null;
        Long hashOutputRangeMax = null;
        InetAddress sourceIPv6Address = null;
        InetAddress sourceIPv4Address = null;
        Long sourceIPv6PrefixLength = null;
        Long sourceIPv4PrefixLength = null;
        Long vlanId = null;
        Long postVlanId = null;
        Long dot1qVlanId = null;
        Long dot1qCustomerVlanId = null;
        Long postDot1qVlanId = null;
        Long postDot1qCustomerVlanId = null;
        Long flowActiveTimeout = null;
        Long flowInactiveTimeout = null;
        UInt32Value ingressPhysicalInterface = null;
        UInt32Value egressPhysicalInterface = null;
        UInt32Value inputSnmp = null;
        UInt32Value outputSnmp = null;


        for (Value<?> value : values) {
            switch (value.getName()) {
                case "@exportTime":
                    exportTime = MessageUtils.getLongValue(value);
                    break;
                case "octetDeltaCount":
                    octetDeltaCount = MessageUtils.getLongValue(value);
                    break;
                case "postOctetDeltaCount":
                    postOctetDeltaCount = MessageUtils.getLongValue(value);
                    break;
                case "layer2OctetDeltaCount":
                    layer2OctetDeltaCount = MessageUtils.getLongValue(value);
                    break;
                case "postLayer2OctetDeltaCount":
                    postLayer2OctetDeltaCount = MessageUtils.getLongValue(value);
                    break;
                case "transportOctetDeltaCount":
                    transportOctetDeltaCount = MessageUtils.getLongValue(value);
                    break;
                case "flowDirection":
                    Long directionValue = MessageUtils.getLongValue(value);
                    Direction direction = Direction.UNKNOWN;
                    direction = handleDirectionValue(directionValue, direction);
                    builder.setDirection(direction);
                    break;
                case "destinationIPv6Address":
                    destinationIPv6Address = MessageUtils.getInetAddress(value);
                    break;
                case "destinationIPv4Address":
                    destinationIPv4Address = MessageUtils.getInetAddress(value);
                    break;
                case "bgpDestinationAsNumber":
                    MessageUtils.getUInt64Value(value).ifPresent(builder::setDstAs);
                    break;
                case "destinationIPv6PrefixLength":
                    destinationIPv6PrefixLength = MessageUtils.getLongValue(value);
                    break;
                case "destinationIPv4PrefixLength":
                    destinationIPv4PrefixLength = MessageUtils.getLongValue(value);
                    break;
                case "destinationTransportPort":
                    MessageUtils.getUInt32Value(value).ifPresent(builder::setDstPort);
                    break;
                case "engineId":
                    MessageUtils.getUInt32Value(value).ifPresent(builder::setEngineId);
                    break;
                case "engineType":
                    MessageUtils.getUInt32Value(value).ifPresent(builder::setEngineType);
                    break;
                case "@recordCount":
                    MessageUtils.getUInt32Value(value).ifPresent(builder::setNumFlowRecords);
                    break;
                case "@sequenceNumber":
                    MessageUtils.getUInt64Value(value).ifPresent(builder::setFlowSeqNum);
                    break;
                case "ingressInterface":
                    inputSnmp = MessageUtils.getUInt32Value(value).orElse(null);
                    break;
                case "ipVersion":
                    Long ipVersion = MessageUtils.getLongValue(value);
                    if (ipVersion != null) {
                        builder.setIpProtocolVersion(MessageUtils.setIntValue(ipVersion.intValue()));
                    }
                    break;
                case "egressInterface":
                    outputSnmp = MessageUtils.getUInt32Value(value).orElse(null);
                    break;
                case "protocolIdentifier":
                    MessageUtils.getUInt32Value(value).ifPresent(builder::setProtocol);
                    break;
                case "tcpControlBits":
                    MessageUtils.getUInt32Value(value).ifPresent(builder::setTcpFlags);
                    break;
                case "ipClassOfService":
                    MessageUtils.getUInt32Value(value).ifPresent(builder::setTos);
                    break;
                case "@observationDomainId":
                    Long observationDomainId = MessageUtils.getLongValue(value);
                    if (observationDomainId != null) {
                        builder.setNodeIdentifier(String.valueOf(observationDomainId));
                    }
                    break;

                case "flowStartSeconds":
                    flowStartSeconds = MessageUtils.getTime(value);
                    break;
                case "flowStartMilliseconds":
                    flowStartMilliseconds = MessageUtils.getTime(value);
                    break;
                case "flowStartMicroseconds":
                    flowStartMicroseconds = MessageUtils.getTime(value);
                    break;
                case "flowStartNanoseconds":
                    flowStartNanoseconds = MessageUtils.getTime(value);
                    break;
                case "flowStartDeltaMicroseconds":
                    flowStartDeltaMicroseconds = MessageUtils.getLongValue(value);
                    break;
                case "flowStartSysUpTime":
                    flowStartSysUpTime = MessageUtils.getLongValue(value);
                    break;
                case "systemInitTimeMilliseconds":
                    systemInitTimeMilliseconds = MessageUtils.getTime(value);
                    break;
                case "flowEndSeconds":
                    flowEndSeconds = MessageUtils.getTime(value);
                    break;
                case "flowEndMilliseconds":
                    flowEndMilliseconds = MessageUtils.getTime(value);
                    break;
                case "flowEndMicroseconds":
                    flowEndMicroseconds = MessageUtils.getTime(value);
                    break;
                case "flowEndNanoseconds":
                    flowEndNanoseconds = MessageUtils.getTime(value);
                    break;
                case "flowEndDeltaMicroseconds":
                    flowEndDeltaMicroseconds = MessageUtils.getLongValue(value);
                    break;
                case "flowEndSysUpTime":
                    flowEndSysUpTime = MessageUtils.getLongValue(value);
                    break;
                case "ipNextHopIPv6Address":
                    ipNextHopIPv6Address = MessageUtils.getInetAddress(value);
                    break;
                case "ipNextHopIPv4Address":
                    ipNextHopIPv4Address = MessageUtils.getInetAddress(value);
                    break;
                case "bgpNextHopIPv6Address":
                    bgpNextHopIPv6Address = MessageUtils.getInetAddress(value);
                    break;
                case "bgpNextHopIPv4Address":
                    bgpNextHopIPv4Address = MessageUtils.getInetAddress(value);
                    break;
                case "packetDeltaCount":
                    packetDeltaCount = MessageUtils.getLongValue(value);
                    break;
                case "postPacketDeltaCount":
                    postPacketDeltaCount = MessageUtils.getLongValue(value);
                    break;
                case "transportPacketDeltaCount":
                    transportPacketDeltaCount = MessageUtils.getLongValue(value);
                    break;
                case "samplingAlgorithm":
                    samplingAlgorithm = MessageUtils.getLongValue(value);
                    break;
                case "samplerMode":
                    samplerMode = MessageUtils.getLongValue(value);
                    break;
                case "selectorAlgorithm":
                    selectorAlgorithm = MessageUtils.getLongValue(value);
                    break;
                case "samplingInterval":
                    samplingInterval = MessageUtils.getLongValue(value);
                    break;
                case "samplerRandomInterval":
                    samplerRandomInterval = MessageUtils.getLongValue(value);
                    break;
                case "samplingFlowInterval":
                    samplingFlowInterval = MessageUtils.getLongValue(value);
                    break;
                case "samplingFlowSpacing":
                    samplingFlowSpacing = MessageUtils.getLongValue(value);
                    break;
                case "flowSamplingTimeInterval":
                    flowSamplingTimeInterval = MessageUtils.getLongValue(value);
                    break;
                case "samplingSize":
                    samplingSize = MessageUtils.getLongValue(value);
                    break;
                case "samplingPopulation":
                    samplingPopulation = MessageUtils.getLongValue(value);
                    break;
                case "samplingProbability":
                    samplingProbability = MessageUtils.getLongValue(value);
                    break;
                case "hashSelectedRangeMin":
                    hashSelectedRangeMin = MessageUtils.getLongValue(value);
                    break;
                case "hashSelectedRangeMax":
                    hashSelectedRangeMax = MessageUtils.getLongValue(value);
                    break;
                case "hashOutputRangeMin":
                    hashOutputRangeMin = MessageUtils.getLongValue(value);
                    break;
                case "hashOutputRangeMax":
                    hashOutputRangeMax = MessageUtils.getLongValue(value);
                    break;
                case "sourceIPv6Address":
                    sourceIPv6Address = MessageUtils.getInetAddress(value);
                    break;
                case "sourceIPv4Address":
                    sourceIPv4Address = MessageUtils.getInetAddress(value);
                    break;
                case "sourceIPv6PrefixLength":
                    sourceIPv6PrefixLength = MessageUtils.getLongValue(value);
                    break;
                case "sourceIPv4PrefixLength":
                    sourceIPv4PrefixLength = MessageUtils.getLongValue(value);
                    break;
                case "sourceTransportPort":
                    MessageUtils.getUInt32Value(value).ifPresent(builder::setSrcPort);
                    break;
                case "vlanId":
                    vlanId = MessageUtils.getLongValue(value);
                    break;
                case "postVlanId":
                    postVlanId = MessageUtils.getLongValue(value);
                    break;
                case "dot1qVlanId":
                    dot1qVlanId = MessageUtils.getLongValue(value);
                    break;
                case "dot1qCustomerVlanId":
                    dot1qCustomerVlanId = MessageUtils.getLongValue(value);
                    break;
                case "postDot1qVlanId":
                    postDot1qVlanId = MessageUtils.getLongValue(value);
                    break;
                case "postDot1qCustomerVlanId":
                    postDot1qCustomerVlanId = MessageUtils.getLongValue(value);
                    break;
                case "flowActiveTimeout":
                    flowActiveTimeout = MessageUtils.getLongValue(value);
                    break;
                case "flowInactiveTimeout":
                    flowInactiveTimeout = MessageUtils.getLongValue(value);
                    break;
                case "ingressPhysicalInterface":
                    ingressPhysicalInterface = MessageUtils.getUInt32Value(value).orElse(null);
                    break;
                case "egressPhysicalInterface":
                    egressPhysicalInterface = MessageUtils.getUInt32Value(value).orElse(null);
                    break;
            }
        }

        // Set input interface
        MessageUtils.first(ingressPhysicalInterface, inputSnmp).ifPresent(builder::setInputSnmpIfindex);

        // Set output interface
        MessageUtils.first(egressPhysicalInterface, outputSnmp).ifPresent(builder::setOutputSnmpIfindex);

        MessageUtils.first(octetDeltaCount, postOctetDeltaCount, layer2OctetDeltaCount, postLayer2OctetDeltaCount,
                transportOctetDeltaCount)
                .ifPresent(bytes ->
                    builder.setNumBytes(MessageUtils.setLongValue(bytes))
                );

        MessageUtils.first(destinationIPv6Address,
                destinationIPv4Address).ifPresent(ipAddress -> {
            builder.setDstAddress(ipAddress.getHostAddress());
            enrichment.getHostnameFor(ipAddress).ifPresent(builder::setDstHostname);
        });

        MessageUtils.first(destinationIPv6PrefixLength,
                destinationIPv4PrefixLength)
                .ifPresent(prefixLen -> builder.setDstMaskLen(MessageUtils.setIntValue(prefixLen.intValue())));


        MessageUtils.first(ipNextHopIPv6Address,
                ipNextHopIPv4Address,
                bgpNextHopIPv6Address,
                bgpNextHopIPv4Address).ifPresent(ipAddress -> {
            builder.setNextHopAddress(ipAddress.getHostAddress());
            enrichment.getHostnameFor(ipAddress).ifPresent(builder::setNextHopHostname);
        });

        MessageUtils.first(sourceIPv6Address,
                sourceIPv4Address).ifPresent(ipAddress -> {
            builder.setSrcAddress(ipAddress.getHostAddress());
            enrichment.getHostnameFor(ipAddress).ifPresent(builder::setSrcHostname);
        });

        MessageUtils.first(sourceIPv6PrefixLength,
                sourceIPv4PrefixLength)
                .ifPresent(prefixLen -> builder.setSrcMaskLen(MessageUtils.setIntValue(prefixLen.intValue())));

        MessageUtils.first(vlanId, postVlanId, dot1qVlanId, dot1qCustomerVlanId, postDot1qVlanId, postDot1qCustomerVlanId)
                .ifPresent(vlan -> builder.setVlan(MessageUtils.setIntValue(vlan.intValue())));

        long timeStamp = exportTime  != null ? exportTime * 1000 : 0;
        builder.setTimestamp(timeStamp);

        // Set first switched
        Long flowStartDelta = flowStartDeltaMicroseconds != null ?
                flowStartDeltaMicroseconds + timeStamp : null;
        Long systemInitTime = systemInitTimeMilliseconds != null ?
                systemInitTimeMilliseconds.toEpochMilli() : null;
        Long flowStart = flowStartSysUpTime != null && systemInitTime != null ?
                flowStartSysUpTime + systemInitTime : null;

        Optional<Long> firstSwitchedInMilli = MessageUtils.first(flowStartSeconds,
                flowStartMilliseconds,
                flowStartMicroseconds,
                flowStartNanoseconds).map(Instant::toEpochMilli);
        if (firstSwitchedInMilli.isPresent()) {
            builder.setFirstSwitched(MessageUtils.setLongValue(firstSwitchedInMilli.get()));
        } else {
            MessageUtils.first(flowStartDelta,
                    flowStart).ifPresent(firstSwitched -> builder.setFirstSwitched(MessageUtils.setLongValue(firstSwitched))
            );
        }

        // Set lastSwitched
        Long flowEndDelta = flowEndDeltaMicroseconds != null ?
                flowEndDeltaMicroseconds + timeStamp : null;
        Long flowEnd = flowEndSysUpTime != null && systemInitTime != null ?
                flowEndSysUpTime + systemInitTime : null;

        Optional<Long> lastSwitchedInMilli = MessageUtils.first(flowEndSeconds,
                flowEndMilliseconds,
                flowEndMicroseconds,
                flowEndNanoseconds).map(Instant::toEpochMilli);

        if(lastSwitchedInMilli.isPresent()) {
            builder.setLastSwitched(MessageUtils.setLongValue(lastSwitchedInMilli.get()));
        } else {
            MessageUtils.first(flowEndDelta,
                    flowEnd).ifPresent(lastSwitchedValue -> builder.setLastSwitched(MessageUtils.setLongValue(lastSwitchedValue)));
        }

        MessageUtils.first(packetDeltaCount, postPacketDeltaCount, transportPacketDeltaCount).ifPresent(packets ->
            builder.setNumPackets(MessageUtils.setLongValue(packets)));

        SamplingAlgorithm sampling = SamplingAlgorithm.UNASSIGNED;
        final Integer deprecatedSamplingAlgorithm = MessageUtils.first(samplingAlgorithm, samplerMode)
                .map(Long::intValue).orElse(null);
        if (deprecatedSamplingAlgorithm != null) {
            if (deprecatedSamplingAlgorithm == 1) {
                sampling = SamplingAlgorithm.SYSTEMATIC_COUNT_BASED_SAMPLING;
            }
            if (deprecatedSamplingAlgorithm == 2) {
                sampling = SamplingAlgorithm.RANDOM_N_OUT_OF_N_SAMPLING;
            }
        }

        if (selectorAlgorithm != null) {
            switch (selectorAlgorithm.intValue()) {
                case 0:
                    sampling = SamplingAlgorithm.UNASSIGNED;
                    break;
                case 1:
                    sampling = SamplingAlgorithm.SYSTEMATIC_COUNT_BASED_SAMPLING;
                    break;
                case 2:
                    sampling = SamplingAlgorithm.SYSTEMATIC_TIME_BASED_SAMPLING;
                    break;
                case 3:
                    sampling = SamplingAlgorithm.RANDOM_N_OUT_OF_N_SAMPLING;
                    break;
                case 4:
                    sampling = SamplingAlgorithm.UNIFORM_PROBABILISTIC_SAMPLING;
                    break;
                case 5:
                    sampling = SamplingAlgorithm.PROPERTY_MATCH_FILTERING;
                    break;
                case 6:
                case 7:
                case 8:
                    sampling = SamplingAlgorithm.HASH_BASED_FILTERING;
                    break;
                case 9:
                    sampling = SamplingAlgorithm.FLOW_STATE_DEPENDENT_INTERMEDIATE_FLOW_SELECTION_PROCESS;
                    break;
            }
        }
        builder.setSamplingAlgorithm(sampling);

        final Double deprecatedSamplingInterval = MessageUtils.first(samplingInterval, samplerRandomInterval)
                .map(Long::doubleValue).orElse(null);

        if (deprecatedSamplingInterval != null) {
            builder.setSamplingInterval(MessageUtils.setDoubleValue(deprecatedSamplingInterval));
        } else {
            if (selectorAlgorithm != null) {
                switch (selectorAlgorithm.intValue()) {
                    case 0:
                        break;
                    case 1: {
                        double interval = samplingFlowInterval != null ?
                                          samplingFlowInterval.doubleValue() : 1.0;
                        double spacing = samplingFlowSpacing != null ?
                                         samplingFlowSpacing.doubleValue() : 0.0;
                        double samplingIntervalValue = interval + spacing / interval;
                        builder.setSamplingInterval(MessageUtils.setDoubleValue(samplingIntervalValue));
                        break;
                    }
                    case 2: {
                        double interval = flowSamplingTimeInterval != null ?
                                          flowSamplingTimeInterval.doubleValue() : 1.0;
                        double samplingIntervalValue = interval + 1.0;
                        builder.setSamplingInterval(MessageUtils.setDoubleValue(samplingIntervalValue));
                        break;
                    }
                    case 3: {
                        double size = samplingSize != null ? samplingSize.doubleValue() : 1.0;
                        double population = samplingPopulation != null ? samplingPopulation.doubleValue() : 1.0;
                        double samplingIntervalValue = population / size;
                        builder.setSamplingInterval(MessageUtils.setDoubleValue(samplingIntervalValue));
                        break;
                    }
                    case 4: {
                        double probability = samplingProbability != null ? samplingProbability.doubleValue() : 1.0;
                        builder.setSamplingInterval(MessageUtils.setDoubleValue(1.0 / probability));
                        break;
                    }
                    case 5:
                    case 6:
                    case 7: {
                        UnsignedLong selectedRangeMin = hashSelectedRangeMin != null ? UnsignedLong.fromLongBits(hashSelectedRangeMin) : UnsignedLong.ZERO;
                        UnsignedLong selectedRangeMax = hashSelectedRangeMax != null ? UnsignedLong.fromLongBits(hashSelectedRangeMax) : UnsignedLong.MAX_VALUE;
                        UnsignedLong outputRangeMin = hashOutputRangeMin != null ? UnsignedLong.fromLongBits(hashOutputRangeMin) : UnsignedLong.ZERO;
                        UnsignedLong outputRangeMax = hashOutputRangeMax != null ? UnsignedLong.fromLongBits(hashOutputRangeMax) : UnsignedLong.MAX_VALUE;
                        double samplingIntervalValue = (outputRangeMax.minus(outputRangeMin)).dividedBy(selectedRangeMax.minus(selectedRangeMin)).doubleValue();
                        builder.setSamplingInterval(MessageUtils.setDoubleValue(samplingIntervalValue));
                        break;
                    }
                    case 8:
                    case 9:
                    default:
                        builder.setSamplingInterval(MessageUtils.setDoubleValue(Double.NaN));
                }
            } else {
                builder.setSamplingInterval(MessageUtils.setDoubleValue(1.0));
            }
        }

        buildDeltaSwitched(builder, flowActiveTimeout, flowInactiveTimeout);

        builder.setNetflowVersion(NetflowVersion.IPFIX);
        return builder;
    }

    static Direction handleDirectionValue(Long directionValue, Direction direction) {
        if (Objects.nonNull(directionValue)) {
            switch (directionValue.intValue()) {
                case 0:
                    direction = Direction.INGRESS;
                    break;
                case 1:
                    direction = Direction.EGRESS;
                    break;
            }
        }
        return direction;
    }

    static void buildDeltaSwitched(FlowMessage.Builder builder, Long flowActiveTimeout, Long flowInactiveTimeout) {
        Timeout timeout = new Timeout(flowActiveTimeout, flowInactiveTimeout);
        timeout.setFirstSwitched(builder.hasFirstSwitched() ? builder.getFirstSwitched().getValue() : null);
        timeout.setLastSwitched(builder.hasLastSwitched() ? builder.getLastSwitched().getValue() : null);
        timeout.setNumBytes(builder.getNumBytes().getValue());
        timeout.setNumPackets(builder.getNumPackets().getValue());
        Long deltaSwitched = timeout.getDeltaSwitched();
        MessageUtils.getUInt64Value(deltaSwitched).ifPresent(builder::setDeltaSwitched);
    }
}
