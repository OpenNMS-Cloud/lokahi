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
package org.opennms.horizon.minion.snmp;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;
import org.opennms.horizon.minion.plugin.api.CollectionRequest;
import org.opennms.horizon.minion.plugin.api.CollectionSet;
import org.opennms.horizon.minion.plugin.api.ServiceCollector;
import org.opennms.horizon.minion.plugin.api.ServiceCollectorResponseImpl;
import org.opennms.horizon.shared.snmp.AggregateTracker;
import org.opennms.horizon.shared.snmp.SnmpAgentConfig;
import org.opennms.horizon.shared.snmp.SnmpHelper;
import org.opennms.horizon.shared.snmp.SnmpWalkCallback;
import org.opennms.horizon.shared.snmp.SnmpWalker;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.opennms.horizon.snmp.api.SnmpConfiguration;
import org.opennms.horizon.snmp.api.SnmpResponseMetric;
import org.opennms.horizon.snmp.api.SnmpResultMetric;
import org.opennms.horizon.snmp.api.SnmpV3Configuration;
import org.opennms.snmp.contract.SnmpCollectorRequest;
import org.opennms.snmp.contract.SnmpInterfaceElement;
import org.opennms.taskset.contract.MonitorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SnmpCollector implements ServiceCollector {

    private final Logger LOG = LoggerFactory.getLogger(SnmpCollector.class);
    private final SnmpHelper snmpHelper;
    private final ThreadFactory threadFactory = new ThreadFactoryBuilder()
            .setNameFormat("snmp-collector-result-processor-%d")
            .build();
    private final ExecutorService executorService = Executors.newCachedThreadPool(threadFactory);

    private static final ExecutorService REAPER_EXECUTOR = Executors.newCachedThreadPool(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "SNMP-Proxy-RPC-Session-Reaper");
        }
    });

    public SnmpCollector(SnmpHelper snmpHelper) {
        this.snmpHelper = snmpHelper;
    }

    @Override
    public CompletableFuture<CollectionSet> collect(CollectionRequest request, Any config) {

        if (!config.is(SnmpCollectorRequest.class)) {
            throw new IllegalArgumentException("config must be an SnmpRequest; type-url=" + config.getTypeUrl());
        }
        var result = new CompletableFuture<CollectionSet>();

        try {
            final CompletableFuture<List<SnmpResultMetric>> future = new CompletableFuture<>();
            SnmpCollectorRequest snmpRequest = config.unpack(SnmpCollectorRequest.class);

            LOG.debug("SNMP Collector Request {}", snmpRequest);
            SnmpResponseMetric.Builder builder = SnmpResponseMetric.newBuilder();
            SnmpCollectionSet snmpCollectionSet = new SnmpCollectionSet(builder);

            String ipAddress = snmpRequest.getHost();
            snmpCollectionSet.addDefaultTrackers();

            for (final var part : snmpRequest.getPartList()) {
                if (part.hasScalar()) {
                    final var scalar = part.getScalar();
                    final var collectable = SnmpScalarCollector.forScalar(scalar, builder);

                    snmpCollectionSet.getTrackers().add(collectable);
                }

                if (part.hasTable()) {
                    final var table = part.getTable();
                    final var collectable = SnmpTableCollector.forTable(table, builder);

                    snmpCollectionSet.getTrackers().add(collectable);
                }
            }

            for (SnmpInterfaceElement element : snmpRequest.getSnmpInterfaceList()) {
                var interfaceMetricsTracker = new InterfaceMetricsTracker(
                        element.getIfIndex(), element.getIfName(), element.getIpAddress(), builder);
                snmpCollectionSet.getTrackers().add(interfaceMetricsTracker);
            }

            AggregateTracker aggregate = new AggregateTracker(snmpCollectionSet.getTrackers());

            long nodeId = request.getNodeId();
            try (final SnmpWalker walker = snmpHelper.createWalker(
                    mapAgent(snmpRequest.getAgentConfig(), ipAddress), "Snmp-Collector", aggregate)) {
                walker.setCallback(new SnmpWalkCallback() {
                    @Override
                    public void complete(SnmpWalker tracker, Throwable t) {
                        try {
                            if (t != null) {
                                future.completeExceptionally(t);
                            } else {
                                var responseList = builder.getResultsList();
                                future.complete(responseList);
                            }
                        } finally {
                            // Close the tracker using a separate thread
                            // This allows the SnmpWalker to clean up properly instead
                            // of interrupting execution as it's executing the callback
                            REAPER_EXECUTOR.submit(new Runnable() {
                                @Override
                                public void run() {
                                    tracker.close();
                                }
                            });
                        }
                    }
                });
                walker.start();
                walker.waitFor();
            }
            result = future.thenApplyAsync(
                    snmpResults -> mapSnmpValuesToResponse(snmpResults, ipAddress, nodeId), executorService);
        } catch (InvalidProtocolBufferException pbe) {
            LOG.debug("Error while mapping Snmp results to proto ", pbe);
            var response = generateFailureResponse(request);
            result.complete(response);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            LOG.debug("Interrupted while collecting metrics ", ie);
            var response = generateFailureResponse(request);
        } catch (Exception e) {
            LOG.debug("Error while collecting metrics ", e);
            var response = generateFailureResponse(request);
            result.complete(response);
        }
        return result;
    }

    private ServiceCollectorResponseImpl generateFailureResponse(CollectionRequest request) {
        return ServiceCollectorResponseImpl.builder()
                .nodeId(request.getNodeId())
                .monitorType(MonitorType.SNMP)
                .status(false)
                .ipAddress(request.getIpAddress())
                .build();
    }

    private ServiceCollectorResponseImpl mapSnmpValuesToResponse(
            List<SnmpResultMetric> snmpResults, String ipAddress, long nodeId) {

        var response =
                SnmpResponseMetric.newBuilder().addAllResults(snmpResults).build();
        LOG.debug("SNMP Collector Results {}", snmpResults);
        return ServiceCollectorResponseImpl.builder()
                .results(response)
                .nodeId(nodeId)
                .monitorType(MonitorType.SNMP)
                .status(true)
                .timeStamp(System.currentTimeMillis())
                .ipAddress(ipAddress)
                .build();
    }

    static SnmpAgentConfig mapAgent(SnmpConfiguration agent, String ipAddress) throws Exception {
        SnmpAgentConfig agentConfig =
                new SnmpAgentConfig(InetAddressUtils.getInetAddress(ipAddress), SnmpAgentConfig.DEFAULTS);
        if (agent.getVersion().getNumber() > 0) {
            agentConfig.setVersion(agent.getVersion().getNumber());
        } else {
            agentConfig.setVersion(SnmpAgentConfig.DEFAULT_VERSION);
        }
        if (agent.hasConfig()) {
            SnmpV3Configuration v3config = agent.getConfig();
            agentOption(v3config.hasSecurityLevel(), agentConfig::setSecurityLevel, v3config::getSecurityLevel);
            agentOption(v3config.hasSecurityName(), agentConfig::setSecurityName, v3config::getSecurityName);
            agentOption(v3config.hasAuthPassPhrase(), agentConfig::setAuthPassPhrase, v3config::getAuthPassPhrase);
            agentOption(v3config.hasAuthProtocol(), agentConfig::setAuthProtocol, v3config::getAuthProtocol);
            agentOption(v3config.hasPrivPassPhrase(), agentConfig::setPrivPassPhrase, v3config::getPrivPassPhrase);
            agentOption(v3config.hasPrivProtocol(), agentConfig::setPrivProtocol, v3config::getPrivProtocol);
            agentOption(v3config.hasContextName(), agentConfig::setContextName, v3config::getContextName);
            agentOption(v3config.hasEnterpriseId(), agentConfig::setEnterpriseId, v3config::getEnterpriseId);
            agentOption(v3config.hasContextEngineId(), agentConfig::setContextEngineId, v3config::getContextEngineId);
            agentOption(v3config.hasEngineId(), agentConfig::setEngineId, v3config::getEngineId);
        }
        agentOption(
                agent.hasProxyForAddress(),
                agentConfig::setProxyFor,
                () -> InetAddress.getByName(agent.getProxyForAddress()));
        agentOption(agent.hasPort(), agentConfig::setPort, agent::getPort);
        agentOption(agent.hasTimeout(), agentConfig::setTimeout, agent::getTimeout);
        agentOption(agent.hasRetries(), agentConfig::setRetries, agent::getRetries);
        agentOption(agent.hasMaxVarsPerPdu(), agentConfig::setMaxVarsPerPdu, agent::getMaxVarsPerPdu);
        agentOption(agent.hasMaxRepetitions(), agentConfig::setMaxRepetitions, agent::getMaxRepetitions);
        agentOption(agent.hasMaxRequestSize(), agentConfig::setMaxRequestSize, agent::getMaxRequestSize);
        agentOption(agent.hasTtl(), agentConfig::setTTL, agent::getTtl);
        agentOption(agent.hasReadCommunity(), agentConfig::setReadCommunity, agent::getReadCommunity);
        agentOption(agent.hasWriteCommunity(), agentConfig::setWriteCommunity, agent::getWriteCommunity);
        return agentConfig;
    }

    private static <T> void agentOption(boolean provided, Consumer<T> setter, Callable<T> getter) throws Exception {
        if (provided) {
            setter.accept(getter.call());
        }
    }
}
