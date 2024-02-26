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
import org.opennms.horizon.minion.plugin.api.CollectorRequestImpl;
import org.opennms.horizon.minion.plugin.api.registries.CollectorRegistry;
import org.opennms.horizon.snmp.api.SnmpConfiguration;
import org.opennms.horizon.snmp.api.SnmpResponseMetric;
import org.opennms.snmp.contract.SnmpCollectorRequest;

@Command(scope = "opennms", name = "snmp-collect", description = "Snmp Collection for a given IP Address")
@Service
public class SnmpCollectCommand implements Action {

    @Reference
    private CollectorRegistry collectorRegistry;

    @Argument(
            index = 0,
            name = "ipAddress",
            description = "Host Address for Collector",
            required = true,
            multiValued = false)
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
        var snmpCollectorManager = collectorRegistry.getService("SNMPCollector");
        var snmpCollector = snmpCollectorManager.create();

        var requestBuilder = SnmpCollectorRequest.newBuilder().setHost(ipAddress);
        if (!Strings.isNullOrEmpty(communityString)) {
            requestBuilder.setAgentConfig(SnmpConfiguration.newBuilder()
                    .setReadCommunity(communityString)
                    .build());
        }
        CollectorRequestImpl collectorRequest =
                CollectorRequestImpl.builder().ipAddress(ipAddress).nodeId(1L).build();
        var future = snmpCollector.collect(collectorRequest, Any.pack(requestBuilder.build()));
        while (true) {
            try {
                try {
                    var response = future.get(1, TimeUnit.SECONDS);
                    Any result = Any.pack(response.getResults());
                    var collectionResults = result.unpack(SnmpResponseMetric.class);
                    System.out.println(collectionResults);
                } catch (InterruptedException e) {
                    System.out.println("\n\nInterrupted.");
                } catch (ExecutionException e) {
                    System.out.printf("\n\n Snmp Collection failed with: %s\n", e);
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
