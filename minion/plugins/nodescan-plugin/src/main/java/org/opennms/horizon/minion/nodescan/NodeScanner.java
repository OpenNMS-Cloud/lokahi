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

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.opennms.horizon.minion.plugin.api.ScanResultsResponse;
import org.opennms.horizon.minion.plugin.api.ScanResultsResponseImpl;
import org.opennms.horizon.minion.plugin.api.Scanner;
import org.opennms.horizon.shared.snmp.SnmpAgentConfig;
import org.opennms.horizon.shared.snmp.SnmpConfiguration;
import org.opennms.horizon.shared.snmp.SnmpHelper;
import org.opennms.horizon.shared.snmp.SnmpWalker;
import org.opennms.node.scan.contract.IpInterfaceResult;
import org.opennms.node.scan.contract.NodeInfoResult;
import org.opennms.node.scan.contract.NodeScanRequest;
import org.opennms.node.scan.contract.NodeScanResult;
import org.opennms.node.scan.contract.SnmpInterfaceResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Any;

public class NodeScanner implements Scanner {
    private static final Logger LOGGER = LoggerFactory.getLogger(NodeScanner.class);
    private final SnmpHelper snmpHelper;

    public NodeScanner(SnmpHelper snmpHelper) {
        this.snmpHelper = snmpHelper;
    }

    @Override
    public CompletableFuture<ScanResultsResponse> scan(Any config) {
        LOGGER.info("Received node scan config {}", config);

        if(!config.is(NodeScanRequest.class)) {
            throw new IllegalArgumentException("Task config must be a NodeScanRequest, this is wrong type: " + config.getTypeUrl());
        }

        return CompletableFuture.supplyAsync(() ->{
            try {
                NodeScanRequest scanRequest = config.unpack(NodeScanRequest.class);
                SnmpAgentConfig agentConfig = new SnmpAgentConfig(InetAddress.getByName(scanRequest.getPrimaryIp()), SnmpConfiguration.DEFAULTS);
                agentConfig.setVersion(SnmpConfiguration.VERSION2C);
                NodeInfoResult nodeInfo = scanSystem(agentConfig);


                List<IpInterfaceResult> ipInterfaceResults = scanIpAddrTable(agentConfig);
                List<SnmpInterfaceResult> snmpInterfaceResults = scanSnmpInterface(agentConfig);
                NodeScanResult scanResult = NodeScanResult.newBuilder()
                    .setNodeId(scanRequest.getNodeId())
                    .setNodeInfo(nodeInfo)
                    .addAllIpInterfaces(ipInterfaceResults)
                    .addAllSnmpInterfaces(snmpInterfaceResults)
                    .build();
                return ScanResultsResponseImpl.builder().results(scanResult).build();
            } catch (Exception e) {
                LOGGER.error("Error while node scan", e);
                throw new RuntimeException(e);
            }
        });
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
