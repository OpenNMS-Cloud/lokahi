/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.opennms.horizon.minion.icmp.shell;

import com.google.common.base.Strings;
import com.google.protobuf.Any;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.opennms.horizon.minion.plugin.api.registries.ScannerRegistry;
import org.opennms.horizon.snmp.api.SnmpConfiguration;
import org.opennms.inventory.types.ServiceType;
import org.opennms.node.scan.contract.DetectRequest;
import org.opennms.node.scan.contract.NodeScanRequest;
import org.opennms.node.scan.contract.NodeScanResult;

@Command(scope = "opennms", name = "node-scan", description = "NodeScan for a given IP Address")
@Service
public class NodeScanCommand implements Action {

    @Reference
    private ScannerRegistry scannerRegistry;

    @Argument(index = 0, name = "ipAddress", description = "IP Address for Scan", required = true, multiValued = false)
    String ipAddress;

    @Argument(
            index = 1,
            name = "Snmp community String",
            description = "Snmp Communitry String to be used",
            required = false,
            multiValued = false)
    String communityString;

    @Override
    public Object execute() throws Exception {

        var scannerManager = scannerRegistry.getService("NodeScanner");
        var scanner = scannerManager.create();

        var scanRequestBuilder = NodeScanRequest.newBuilder()
                .setPrimaryIp(ipAddress)
                .addDetector(
                        DetectRequest.newBuilder().setService(ServiceType.ICMP).build())
                .addDetector(
                        DetectRequest.newBuilder().setService(ServiceType.SNMP).build());

        if (!Strings.isNullOrEmpty(communityString)) {
            scanRequestBuilder.addSnmpConfigs(SnmpConfiguration.newBuilder()
                    .setReadCommunity(communityString)
                    .build());
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
