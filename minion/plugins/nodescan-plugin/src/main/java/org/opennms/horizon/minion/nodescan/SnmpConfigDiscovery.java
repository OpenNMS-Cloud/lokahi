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
package org.opennms.horizon.minion.nodescan;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.opennms.horizon.shared.snmp.SnmpAgentConfig;
import org.opennms.horizon.shared.snmp.SnmpHelper;
import org.opennms.horizon.shared.snmp.SnmpObjId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        // Join all the results.
        CompletableFuture<List<Optional<SnmpAgentConfig>>> results = allFutures.thenApply(
                agentConfig -> futures.stream().map(CompletableFuture::join).collect(Collectors.toList()));
        try {
            var timeout = findMaxTimeout(configs);
            var validConfigs = results.get(timeout, TimeUnit.MILLISECONDS);
            detectedConfigs.addAll(
                    validConfigs.stream().flatMap(Optional::stream).toList());
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
        var configWithMaxTimeout = configs.stream()
                .max(Comparator.comparing(agentConfig ->
                        agentConfig.getTimeout() * agentConfig.getRetries() > 0 ? agentConfig.getRetries() : 1))
                .orElse(new SnmpAgentConfig(null, SnmpAgentConfig.DEFAULTS));
        return (configWithMaxTimeout.getRetries() > 0 ? configWithMaxTimeout.getRetries() : 1)
                        * configWithMaxTimeout.getTimeout()
                + 2000; // Add 2 secs buffer.
    }

    private CompletableFuture<Optional<SnmpAgentConfig>> detectConfig(SnmpAgentConfig agentConfig) {
        CompletableFuture<Optional<SnmpAgentConfig>> future = new CompletableFuture<>();
        try {
            LOG.debug("Validating AgentConfig {}", agentConfig);
            var snmpFuture =
                    snmpHelper.getAsync(agentConfig, new SnmpObjId[] {SnmpObjId.get(SnmpHelper.SYS_OBJECTID_INSTANCE)});
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
