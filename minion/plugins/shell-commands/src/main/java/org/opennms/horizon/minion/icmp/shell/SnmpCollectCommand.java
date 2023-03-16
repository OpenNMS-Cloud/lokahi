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
import org.opennms.horizon.minion.plugin.api.CollectorRequestImpl;
import org.opennms.horizon.minion.plugin.api.registries.CollectorRegistry;
import org.opennms.horizon.snmp.api.SnmpConfiguration;
import org.opennms.horizon.snmp.api.SnmpResponseMetric;
import org.opennms.snmp.contract.SnmpCollectorRequest;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Command(scope = "opennms", name = "snmp-collect", description = "Snmp Collection for a given IP Address")
@Service
public class SnmpCollectCommand implements Action {

    @Reference
    private CollectorRegistry collectorRegistry;

    @Argument(index = 0, name = "ipAddress", description = "Host Address for Collector", required = true, multiValued = false)
    String ipAddress;

    @Argument(index = 1, name = "Snmp community String", description = "Snmp Communitry String to be used", required = false, multiValued = false)
    String communityString;

    @Override
    public Object execute() throws Exception {
        var snmpCollectorManager = collectorRegistry.getService("SNMPCollector");
        var snmpCollector = snmpCollectorManager.create();

        var requestBuilder = SnmpCollectorRequest.newBuilder().setHost(ipAddress);
        if (!Strings.isNullOrEmpty(communityString)) {
           requestBuilder.setAgentConfig(SnmpConfiguration.newBuilder().setReadCommunity(communityString).build());
        }
        CollectorRequestImpl collectorRequest = CollectorRequestImpl.builder().ipAddress(ipAddress)
                .nodeId(1L).build();
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
