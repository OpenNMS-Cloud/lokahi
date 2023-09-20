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

import org.opennms.horizon.shared.snmp.SnmpAgentConfig;
import org.opennms.horizon.shared.snmp.SnmpHelper;
import org.opennms.horizon.shared.snmp.SnmpObjId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class SnmpConfigDiscovery {
    private static final Logger LOG = LoggerFactory.getLogger(SnmpConfigDiscovery.class);
    private final SnmpHelper snmpHelper;

    public SnmpConfigDiscovery(SnmpHelper snmpHelper) {
        this.snmpHelper = snmpHelper;
    }

    public List<SnmpAgentConfig> getDiscoveredConfig(List<SnmpAgentConfig> configs) {

        List<SnmpAgentConfig> detectedConfigs = new ArrayList<>();
        var futures = configs.stream().map(this::detectConfig).toList();
        var allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        //Join all the results.
        CompletableFuture<List<Optional<SnmpAgentConfig>>> results = allFutures.thenApply(agentConfig ->
            futures.stream().map(CompletableFuture::join).collect(Collectors.toList()));
        try {
            var timeout = findMaxTimeout(configs);
            var validConfigs = results.get(timeout, TimeUnit.MILLISECONDS);
            detectedConfigs.addAll(validConfigs.stream().flatMap(Optional::stream).toList());
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Exception while executing config discovery", e);
            } else {
                LOG.error("Exception while executing config discovery {}", e.getMessage());
            }
        }

        return detectedConfigs;
    }

    private int findMaxTimeout(List<SnmpAgentConfig> configs) {
        var configWithMaxTimeout = configs.stream().max(Comparator.comparing(agentConfig ->
                agentConfig.getTimeout() *
                    agentConfig.getRetries() > 0 ? agentConfig.getRetries() : 1))
            .orElse(new SnmpAgentConfig(null, SnmpAgentConfig.DEFAULTS));
        return (configWithMaxTimeout.getRetries() > 0 ? configWithMaxTimeout.getRetries() : 1) *
            configWithMaxTimeout.getTimeout()
            + 2000; // Add 2 secs buffer.
    }

    private CompletableFuture<Optional<SnmpAgentConfig>> detectConfig(SnmpAgentConfig agentConfig) {
        CompletableFuture<Optional<SnmpAgentConfig>> future = new CompletableFuture<>();
        try {
            LOG.debug("Validating AgentConfig {}", agentConfig);
            var snmpFuture = snmpHelper.getAsync(agentConfig, new SnmpObjId[]{SnmpObjId.get(SnmpHelper.SYS_OBJECTID_INSTANCE)});
            snmpFuture.whenComplete(((snmpValues, throwable) -> {
                if (snmpValues != null) {
                    var snmpValue = snmpValues.length > 0 ? snmpValues[0] : null;
                    if (snmpValue != null && !snmpValue.isError()) {
                        future.complete(Optional.of(agentConfig));
                        return;
                    }
                }
                future.complete(Optional.empty());
            }));
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Exception while doing snmp get with agentConfig {}", agentConfig);
            }
            future.complete(Optional.empty());
        }
        return future;
    }
}
