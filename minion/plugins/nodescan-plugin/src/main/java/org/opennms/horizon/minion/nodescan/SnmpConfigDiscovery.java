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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.opennms.horizon.shared.snmp.SnmpAgentConfig;
import org.opennms.horizon.shared.snmp.SnmpHelper;
import org.opennms.horizon.shared.snmp.SnmpObjId;
import org.opennms.horizon.shared.snmp.SnmpValue;

public class SnmpConfigDiscovery {

    private final SnmpHelper snmpHelper;

    public SnmpConfigDiscovery(SnmpHelper snmpHelper) {
        this.snmpHelper = snmpHelper;
    }

    public List<SnmpAgentConfig> detectSNMP(List<SnmpAgentConfig> configs) {
        List<CompletableFuture<SnmpValue[]>> futures = new ArrayList<>();

        // Start a CompletableFuture for each SnmpAgentConfig to detect SNMP service
        for (SnmpAgentConfig config : configs) {
            CompletableFuture<SnmpValue[]> future = snmpHelper.getAsync(config, new SnmpObjId[]{SnmpObjId.get(SnmpHelper.SYSTEM_OID)})
                .completeOnTimeout(new SnmpValue[0], 5000, TimeUnit.MILLISECONDS); // timeout after 5 seconds
            futures.add(future);
        }

        // Wait for all detection CompletableFutures to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        List<SnmpAgentConfig> detectedConfigs = new ArrayList<>();

        // Add the SnmpAgentConfigs where SnmpValues is not empty to the detectedConfigs list
        for (int i = 0; i < configs.size(); i++) {
            SnmpValue[] values = futures.get(i).join();
            if (values != null && values.length > 0 && values[0] != null) {
                detectedConfigs.add(configs.get(i));
            }
        }

        return detectedConfigs;
    }
}
