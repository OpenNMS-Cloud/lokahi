/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022-2023 The OpenNMS Group, Inc.
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

package org.opennms.horizon.flows.integration;


import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * A network flow.
 *
 * Represents statistics about the communication between two systems.
 *
 * @since 1.1.0
 */
@Model
public interface Flow {
    /**
     * The application as deduced by the classification engine.
     *
     * @return the application name
     */
    String getApplication();

    /**
     * The IP address of the source exporting this flow.
     *
     * @return the IP address
     */
    String getHost();

    /**
     * The location in which the flow was received.
     *
     * @return the location name
     */
    String getLocation();

    Locality getSrcLocality();

    Locality getDstLocality();

    Locality getFlowLocality();

    NodeInfo getSrcNodeInfo();

    NodeInfo getDstNodeInfo();

    NodeInfo getExporterNodeInfo();

    Duration getClockCorrection();

    Instant getTimestamp();

    Instant getFirstSwitched();

    Instant getDeltaSwitched();

    Instant getLastSwitched();

    Instant getReceivedAt();

    Long getBytes();

    Direction getDirection();

    String getDstAddr();

    Optional<String> getDstAddrHostname();

    Long getDstAs();

    Integer getDstMaskLen();

    Integer getDstPort();

    Integer getEngineId();

    Integer getEngineType();

    int getFlowRecords();

    long getFlowSeqNum();

    Integer getInputSnmp();

    Integer getIpProtocolVersion();

    String getNextHop();

    Optional<String> getNextHopHostname();

    Long getOutputSnmp();

    Long getPackets();

    Integer getProtocol();

    SamplingAlgorithm getSamplingAlgorithm();

    Double getSamplingInterval();

    String getSrcAddr();

    Optional<String> getSrcAddrHostname();

    Long getSrcAs();

    Integer getSrcMaskLen();

    Integer getSrcPort();

    Integer getTcpFlags();

    Integer getTos();

    NetflowVersion getNetflowVersion();

    Integer getVlan();

    Integer getDscp();

    Integer getEcn();

    String getConvoKey();

    @Model
    public enum Locality {
        PUBLIC, PRIVATE
    }

    @Model
    public interface NodeInfo {
        long getInterfaceId();

        long getNodeId();

        String getForeignId();

        String getForeignSource();

        List<String> getCategories();
    }

    @Model
    enum NetflowVersion {
        V5,
        V9,
        IPFIX,
        SFLOW,
    }

    @Model
    enum Direction {
        INGRESS,
        EGRESS,
        UNKNOWN,
    }

    @Model
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
}
