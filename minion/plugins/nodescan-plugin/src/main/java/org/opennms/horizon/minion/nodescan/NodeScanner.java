/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2023 The OpenNMS Group, Inc.
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

package org.opennms.horizon.minion.nodescan;


import com.google.common.base.Strings;
import com.google.protobuf.Any;
import org.opennms.horizon.minion.plugin.api.ScanResultsResponse;
import org.opennms.horizon.minion.plugin.api.ScanResultsResponseImpl;
import org.opennms.horizon.minion.plugin.api.Scanner;
import org.opennms.horizon.shared.snmp.SnmpAgentConfig;
import org.opennms.horizon.shared.snmp.SnmpConfiguration;
import org.opennms.horizon.shared.snmp.SnmpHelper;
import org.opennms.horizon.shared.snmp.SnmpWalker;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.opennms.horizon.snmp.api.Version;
import org.opennms.node.scan.contract.IpInterfaceResult;
import org.opennms.node.scan.contract.NodeInfoResult;
import org.opennms.node.scan.contract.NodeScanRequest;
import org.opennms.node.scan.contract.NodeScanResult;
import org.opennms.node.scan.contract.SnmpInterfaceResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;



public class NodeScanner implements Scanner {
    private static final Logger LOG = LoggerFactory.getLogger(NodeScanner.class);
    private final SnmpHelper snmpHelper;
    private final SnmpConfigDiscovery snmpConfigDiscovery;

    public NodeScanner(SnmpHelper snmpHelper) {
        this.snmpHelper = snmpHelper;
        snmpConfigDiscovery = new SnmpConfigDiscovery(snmpHelper);
    }

    @Override
    public CompletableFuture<ScanResultsResponse> scan(Any config) {
        LOG.info("Received node scan config {}", config);

        if (!config.is(NodeScanRequest.class)) {
            throw new IllegalArgumentException("Task config must be a NodeScanRequest, this is wrong type: " + config.getTypeUrl());
        }

        try {
            NodeScanRequest scanRequest = config.unpack(NodeScanRequest.class);
            InetAddress primaryIpAddress = InetAddressUtils.getInetAddress(scanRequest.getPrimaryIp());
            // Assign default agent config.
            SnmpAgentConfig agentConfig = new SnmpAgentConfig(primaryIpAddress, SnmpConfiguration.DEFAULTS);
            // Derive configs from request
            Set<SnmpAgentConfig> configs = deriveSnmpConfigs(scanRequest.getSnmpConfigsList(), primaryIpAddress);
            List<SnmpAgentConfig> snmpAgentConfigs = snmpConfigDiscovery.getDiscoveredConfig(configs.stream().toList());
            if (!snmpAgentConfigs.isEmpty()) {
                // Get first matching config
                agentConfig = snmpAgentConfigs.get(0);
            } else {
                LOG.warn("No matching agentConfig found from Snmp Config discovery, default config will be used");
            }

            NodeInfoResult nodeInfo = scanSystem(agentConfig);
            List<IpInterfaceResult> ipInterfaceResults = scanIpAddrTable(agentConfig);
            List<SnmpInterfaceResult> snmpInterfaceResults = scanSnmpInterface(agentConfig);

            return CompletableFuture.completedFuture(ScanResultsResponseImpl.builder().results(NodeScanResult.newBuilder()
                .setNodeId(scanRequest.getNodeId())
                .setNodeInfo(nodeInfo)
                .addAllIpInterfaces(ipInterfaceResults)
                .addAllSnmpInterfaces(snmpInterfaceResults)
                .setSnmpConfig(mapSnmpAgentConfig(agentConfig))
                .addAllSnmpInterfaces(snmpInterfaceResults).build())
                .build());
        } catch (Exception e) {
            LOG.error("Error while node scan", e);
            return CompletableFuture.failedFuture(e);
        }

    }

    private org.opennms.horizon.snmp.api.SnmpConfiguration mapSnmpAgentConfig(SnmpAgentConfig agentConfig) {
        return org.opennms.horizon.snmp.api.SnmpConfiguration.newBuilder()
            .setVersion(Version.forNumber(agentConfig.getVersion()))
            .setAddress(InetAddressUtils.str(agentConfig.getAddress()))
            .setPort(agentConfig.getPort())
            .setRetries(agentConfig.getRetries())
            .setTimeout(agentConfig.getTimeout())
            .setMaxVarsPerPdu(agentConfig.getMaxVarsPerPdu())
            .setMaxRepetitions(agentConfig.getMaxRepetitions())
            .setMaxRequestSize(agentConfig.getMaxRequestSize())
            .setReadCommunity(agentConfig.getReadCommunity())
            .setWriteCommunity(agentConfig.getWriteCommunity())
            // Skip V3 for now
            .build();
    }

    Set<SnmpAgentConfig> deriveSnmpConfigs(List<org.opennms.horizon.snmp.api.SnmpConfiguration> configsFromRequest,
                                           InetAddress primaryIpAddress) {
        Set<SnmpAgentConfig> configs = new HashSet<>();
        List<SnmpAgentConfig> configsForReadCommunity = new ArrayList<>();
        configsFromRequest.forEach(snmpConfig -> {
            var readCommunity = snmpConfig.getReadCommunity();
            if (!Strings.isNullOrEmpty(readCommunity) &&
                !SnmpConfiguration.DEFAULT_READ_COMMUNITY.equals(readCommunity)) {
                SnmpAgentConfig agentConfig = new SnmpAgentConfig(primaryIpAddress, SnmpConfiguration.DEFAULTS);
                agentConfig.setReadCommunity(readCommunity);
                configsForReadCommunity.add(agentConfig);
            }
        });
        // Add default config
        configsForReadCommunity.add(new SnmpAgentConfig(primaryIpAddress, SnmpConfiguration.DEFAULTS));

        List<SnmpAgentConfig> configsForPort = new ArrayList<>();
        configsFromRequest.forEach(snmpConfig -> {
            var port = snmpConfig.getPort();
            if (port != 0 && SnmpConfiguration.DEFAULT_PORT != port) {
                SnmpAgentConfig agentConfig = new SnmpAgentConfig(primaryIpAddress, SnmpConfiguration.DEFAULTS);
                agentConfig.setPort(port);
                configsForPort.add(agentConfig);
            }
        });
        configsForPort.add(new SnmpAgentConfig(primaryIpAddress, SnmpConfiguration.DEFAULTS));

        configsForReadCommunity.forEach(config -> {
            configsForPort.forEach(portConfig -> {
                SnmpAgentConfig agentConfig = new SnmpAgentConfig(primaryIpAddress, SnmpConfiguration.DEFAULTS);
                agentConfig.setPort(portConfig.getPort());
                agentConfig.setReadCommunity(config.getReadCommunity());
                configs.add(agentConfig);
            });
        });

        configsForPort.forEach(config -> {
            configsForReadCommunity.forEach(readCommunityConfig -> {
                config.setReadCommunity(readCommunityConfig.getReadCommunity());
                SnmpAgentConfig agentConfig = new SnmpAgentConfig(primaryIpAddress, SnmpConfiguration.DEFAULTS);
                agentConfig.setReadCommunity(readCommunityConfig.getReadCommunity());
                agentConfig.setPort(config.getPort());
                configs.add(agentConfig);
            });
        });
        return configs;
    }

    private List<SnmpInterfaceResult> scanSnmpInterface(SnmpAgentConfig agentConfig) throws InterruptedException {
        List<SnmpInterfaceResult> results = new ArrayList<>();
        SNMPInterfaceTableTracker tracker = new SNMPInterfaceTableTracker() {
            @Override
            public void processPhysicalInterfaceRow(PhysicalInterfaceRow row) {
                results.add(row.createInterfaceFromRow());
            }
        };
        try(SnmpWalker walker = snmpHelper.createWalker(agentConfig, "snmpInterfaceTable", tracker)) {
            walker.start();
            walker.waitFor();
        }
        return results;
    }

    private List<IpInterfaceResult> scanIpAddrTable(SnmpAgentConfig agentConfig) throws InterruptedException {
        List<IpInterfaceResult> results = new ArrayList<>();
        IPAddrTracker tracker = new IPAddrTracker() {
            @Override
            public void processIPInterfaceRow(IPInterfaceRow row) {
                row.createInterfaceFromRow().ifPresent(results::add);
            }
        };
        try(var walker = snmpHelper.createWalker(agentConfig, "ipAddrEntry", tracker)) {
            walker.start();
            walker.waitFor();
        }
        IPAddressTableTracker ipAddressTableTracker = new IPAddressTableTracker() {
            @Override
            public void processIPAddressRow(IPAddressRow row) {
                row.createInterfaceFromRow().ifPresent(results::add);
            }
        };
        try(var walker = snmpHelper.createWalker(agentConfig, "ipAddressTableEntry", ipAddressTableTracker)) {
            walker.start();
            walker.waitFor();
        }
        return results;
    }

    private NodeInfoResult scanSystem(SnmpAgentConfig agentConfig) throws InterruptedException {
        SystemGroupTracker tracker = new SystemGroupTracker(agentConfig.getAddress());
        try(var walker = snmpHelper.createWalker(agentConfig, "systemGroup", tracker)) {
            walker.start();
            walker.waitFor();
        }
        return tracker.createNodeInfo();
    }
}
