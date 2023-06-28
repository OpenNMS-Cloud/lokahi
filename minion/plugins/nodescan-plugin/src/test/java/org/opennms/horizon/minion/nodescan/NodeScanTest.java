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
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.shared.snmp.ServiceBasedStrategyResolver;
import org.opennms.horizon.shared.snmp.SnmpAgentConfig;
import org.opennms.horizon.shared.snmp.SnmpConfiguration;
import org.opennms.horizon.shared.snmp.SnmpHelper;
import org.opennms.horizon.shared.snmp.SnmpHelperImpl;
import org.opennms.horizon.shared.snmp.SnmpWalker;
import org.opennms.horizon.shared.snmp.StrategyResolver;
import org.opennms.horizon.shared.snmp.snmp4j.Snmp4JStrategy;
import org.opennms.node.scan.contract.IpInterfaceResult;
import org.opennms.node.scan.contract.SnmpInterfaceResult;


//This is development tool
@Disabled
public class NodeScanTest {

    private SnmpAgentConfig agentConfig;
    private SnmpHelper snmpHelper;

    @BeforeEach
    void prepare() throws UnknownHostException {
        Snmp4JStrategy snmp4JStrategy = new Snmp4JStrategy();
        StrategyResolver strategyResolver = new ServiceBasedStrategyResolver(snmp4JStrategy);
        snmpHelper = new SnmpHelperImpl(strategyResolver);
        SnmpConfiguration configuration = SnmpConfiguration.DEFAULTS;
        configuration.setVersion(SnmpConfiguration.VERSION2C);
        agentConfig = new SnmpAgentConfig(InetAddress.getByName("127.0.0.1"), configuration);
    }

    @Test
    void testIpTableTracker() throws InterruptedException {
        List<IpInterfaceResult> list = new ArrayList<>();

        IPAddrTracker tracker = new IPAddrTracker() {
            @Override
            public void processIPInterfaceRow(IPInterfaceRow row) {
                row.createInterfaceFromRow().ifPresent(list::add);
            }
        };

        try(SnmpWalker walker = snmpHelper.createWalker(agentConfig, "ipTable", tracker)) {
            walker.start();
            walker.waitFor(3 * 60 * 1000);
        }

        IPAddressTableTracker newTracker = new IPAddressTableTracker() {
            @Override
            public void processIPAddressRow(IPAddressRow row) {
                row.createInterfaceFromRow().ifPresent(list::add);
            }
        };
        try (SnmpWalker newWalker = snmpHelper.createWalker(agentConfig, "ipAddressTable", newTracker)) {
            newWalker.start();
            newWalker.waitFor();
        }
        System.out.println(list);
    }

    @Test
    void testSystemWalker() throws InterruptedException {
        SystemGroupTracker tracker = new SystemGroupTracker(agentConfig.getAddress());

        try(SnmpWalker walker = snmpHelper.createWalker(agentConfig, "systemGroup", tracker)) {
            walker.start();
            walker.waitFor();
        }

        if(tracker.isFinished()) {
            System.out.println(tracker.createNodeInfo());
        }
    }

    @Test
    void testSNMPInterfaceScan() throws InterruptedException {
        List<SnmpInterfaceResult> list = new ArrayList<>();
        SNMPInterfaceTableTracker tracker = new SNMPInterfaceTableTracker() {
            @Override
            public void processPhysicalInterfaceRow(PhysicalInterfaceRow row) {
                row.createInterfaceFromRow().ifPresent(list::add);
            }
        };
        try (SnmpWalker walker = snmpHelper.createWalker(agentConfig, "snmpInterfaceTable", tracker)) {
            walker.start();
            walker.waitFor();
        }
        System.out.println("SNMP Interfaces: " + list);
    }
}
