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

package org.opennms.horizon.minion.icmp.shell;

import com.google.common.base.Strings;
import com.google.protobuf.Any;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.opennms.horizon.minion.plugin.api.registries.ScannerRegistry;
import org.opennms.horizon.snmp.api.SnmpConfiguration;
import org.opennms.node.scan.contract.NodeScanRequest;
import org.opennms.node.scan.contract.NodeScanResult;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Command(scope = "opennms", name = "node-scan", description = "NodeScan for a given IP Address")
@Service
public class NodeScanCommand implements Action {

    @Reference
    private ScannerRegistry scannerRegistry;

    @Argument(index = 0, name = "ipAddress", description = "IP Address for Scan", required = true, multiValued = false)
    String ipAddress;

    @Argument(index = 1, name = "Snmp community String", description = "Snmp Communitry String to be used", required = false, multiValued = false)
    String communityString;


    @Override
    public Object execute() throws Exception {

        var scannerManager = scannerRegistry.getService("NodeScanner");
        var scanner = scannerManager.create();

        var scanRequestBuilder = NodeScanRequest.newBuilder()
            .setPrimaryIp(ipAddress);
        if (!Strings.isNullOrEmpty(communityString)) {
            scanRequestBuilder.addSnmpConfigs(SnmpConfiguration.newBuilder().setReadCommunity(communityString).build());
        }
        var config = Any.pack(scanRequestBuilder.build());
        var future = scanner.scan(config);
        while (true) {
            try {
                try {
                    var response = future.get(1, TimeUnit.SECONDS);
                    Any result = Any.pack(response.getResults());
                    NodeScanResult nodeScanResult = result.unpack(NodeScanResult.class);

                    System.out.printf("Node Scan result : \n  %s ", nodeScanResult.toString());
                } catch (InterruptedException e) {
                    System.out.println("\n\nInterrupted.");
                } catch (ExecutionException e) {
                    System.out.printf("\n\n Node Scan failed with: %s\n", e);
                }
                break;
            } catch (TimeoutException e) {
                // pass
            }
            System.out.print(".");
        }
        return null;
    }
}
