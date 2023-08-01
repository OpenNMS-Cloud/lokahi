/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022-2023 The OpenNMS Group, Inc.
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

package org.opennms.horizon.minion.azure;

import com.google.protobuf.Any;
import org.opennms.azure.contract.AzureCollectorRequest;
import org.opennms.azure.contract.AzureCollectorResourcesRequest;
import org.opennms.horizon.azure.api.AzureResponseMetric;
import org.opennms.horizon.azure.api.AzureResultMetric;
import org.opennms.horizon.azure.api.AzureValueMetric;
import org.opennms.horizon.azure.api.AzureValueType;
import org.opennms.horizon.minion.plugin.api.CollectionRequest;
import org.opennms.horizon.minion.plugin.api.CollectionSet;
import org.opennms.horizon.minion.plugin.api.ServiceCollector;
import org.opennms.horizon.minion.plugin.api.ServiceCollectorResponseImpl;
import org.opennms.horizon.shared.azure.http.AzureHttpClient;
import org.opennms.horizon.shared.azure.http.AzureHttpException;
import org.opennms.horizon.shared.azure.http.dto.instanceview.AzureInstanceView;
import org.opennms.horizon.shared.azure.http.dto.login.AzureOAuthToken;
import org.opennms.horizon.shared.azure.http.dto.metrics.AzureMetrics;
import org.opennms.taskset.contract.MonitorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class AzureCollector implements ServiceCollector {
    private final Logger log = LoggerFactory.getLogger(AzureCollector.class);

    private static final String INTERVAL_PARAM = "interval";
    private static final String METRIC_NAMES_PARAM = "metricnames";

    private static final String TIMESPAN_PARAM = "timespan";

    // Reference inventory: TaskUtils.DEFAULT_SCHEDULE
    private static final int TIMESPAN_SECOND = 60;

    // Supported intervals: PT1M,PT5M,PT15M,PT30M,PT1H,PT6H,PT12H,P1D
    private static final String METRIC_INTERVAL = "PT1M";

    // Azure Metric Key - Metrics Processor Key
    private static final Map<String, String> AZURE_NODE_METRIC_TO_ALIAS = Map.of(
        "Network In Total", "network_in_total_bytes",
        "Network Out Total", "network_out_total_bytes"
    );

    // Valid metrics: BytesSentRate,BytesReceivedRate,PacketsSentRate,PacketsReceivedRate
    private static final Map<String, String> AZURE_INTERFACE_METRIC_TO_ALIAS = Map.of(
        "BytesReceivedRate", "bytes_received_rate",
        "BytesSentRate", "bytes_sent_rate"
    );

    // Valid metrics: PacketsInDDoS,PacketsDroppedDDoS,PacketsForwardedDDoS,TCPPacketsInDDoS,TCPPacketsDroppedDDoS,TCPPacketsForwardedDDoS,UDPPacketsInDDoS,UDPPacketsDroppedDDoS,UDPPacketsForwardedDDoS,BytesInDDoS,BytesDroppedDDoS,BytesForwardedDDoS,TCPBytesInDDoS,TCPBytesDroppedDDoS,TCPBytesForwardedDDoS,UDPBytesInDDoS,UDPBytesDroppedDDoS,UDPBytesForwardedDDoS,IfUnderDDoSAttack,DDoSTriggerTCPPackets,DDoSTriggerUDPPackets,DDoSTriggerSYNPackets,VipAvailability,ByteCount,PacketCount,SynCount
    private static final Map<String, String> AZURE_IPINTERFACE_METRIC_TO_ALIAS = Map.of(
        "ByteCount", "bytes_received"
    );

    private static final String AZURE_NODE_PREFIX = "azure-node-";
    private static final String METRIC_DELIMITER = ",";

    private final AzureHttpClient client;

    public AzureCollector(AzureHttpClient client) {
        this.client = client;
    }

    @Override
    public CompletableFuture<CollectionSet> collect(CollectionRequest collectionRequest, Any config) {
        CompletableFuture<CollectionSet> future = new CompletableFuture<>();

        try {
            if (!config.is(AzureCollectorRequest.class)) {
                throw new IllegalArgumentException("configuration must be an AzureCollectorRequest; type-url=" + config.getTypeUrl());
            }

            AzureCollectorRequest request = config.unpack(AzureCollectorRequest.class);

            AzureOAuthToken token = client.login(request.getDirectoryId(),
                request.getClientId(), request.getClientSecret(), request.getTimeoutMs(), request.getRetries());

            AzureInstanceView instanceView = client.getInstanceView(token, request.getSubscriptionId(),
                request.getResourceGroup(), request.getResource(), request.getTimeoutMs(), request.getRetries());

            if (instanceView.isUp()) {

                // host metrics
                List<AzureResultMetric> metricResults = collectNodeMetrics(request, token).entrySet().stream()
                    .map(nodeMetric -> mapNodeResult(request, nodeMetric))
                    .collect(Collectors.toCollection(ArrayList::new));

                // interface metrics
                for (var resources : request.getCollectorResourcesList()) {
                    metricResults.addAll(collectInterfaceMetrics(request, resources, token)
                        .entrySet().stream().map(interfaceMetric ->
                            mapInterfaceResult(request, resources.getResource(), resources.getType(), interfaceMetric))
                        .toList());
                }

                log.debug("AZURE COLLECTOR metricResults LIST: {}", metricResults);

                AzureResponseMetric results = AzureResponseMetric.newBuilder()
                    .addAllResults(metricResults)
                    .build();

                future.complete(ServiceCollectorResponseImpl.builder()
                    .results(results)
                    .nodeId(collectionRequest.getNodeId())
                    .monitorType(MonitorType.AZURE)
                    .status(true)
                    .timeStamp(System.currentTimeMillis())
                    .ipAddress(AZURE_NODE_PREFIX + collectionRequest.getNodeId())
                    .build());

            } else {
                future.complete(ServiceCollectorResponseImpl.builder()
                    .nodeId(collectionRequest.getNodeId())
                    .monitorType(MonitorType.AZURE)
                    .status(false)
                    .timeStamp(System.currentTimeMillis())
                    .ipAddress(AZURE_NODE_PREFIX + collectionRequest.getNodeId())
                    .build());
            }
        } catch (Exception e) {
            log.error("Failed to collect for azure resource", e);
            future.complete(ServiceCollectorResponseImpl.builder()
                .nodeId(collectionRequest.getNodeId())
                .monitorType(MonitorType.AZURE)
                .status(false)
                .timeStamp(System.currentTimeMillis())
                .ipAddress(AZURE_NODE_PREFIX + collectionRequest.getNodeId())
                .build());
        }
        return future;
    }

    private Map<String, Double> collectNodeMetrics(AzureCollectorRequest request, AzureOAuthToken token) throws AzureHttpException {
        Map<String, String> params = getMetricsParams(AZURE_NODE_METRIC_TO_ALIAS.keySet());

        AzureMetrics metrics = client.getMetrics(token, request.getSubscriptionId(),
            request.getResourceGroup(), request.getResource(), params, request.getTimeoutMs(), request.getRetries());

        return metrics.collect();
    }

    private Map<String, Double> collectInterfaceMetrics(AzureCollectorRequest request,
                                                        AzureCollectorResourcesRequest resource,
                                                        AzureOAuthToken token) throws AzureHttpException {
        try {
            var type = AzureHttpClient.ResourcesType.fromMetricName(resource.getType());
            Map<String, String> params =
                getMetricsParams(AzureHttpClient.ResourcesType.NETWORK_INTERFACES == type ?
                    AZURE_INTERFACE_METRIC_TO_ALIAS.keySet() : AZURE_IPINTERFACE_METRIC_TO_ALIAS.keySet());

            AzureMetrics metrics = client.getNetworkInterfaceMetrics(token, request.getSubscriptionId(),
                request.getResourceGroup(), resource.getType() + "/" + resource.getResource(), params,
                request.getTimeoutMs(), request.getRetries());

            return metrics.collect();
        } catch (IllegalArgumentException ex) {
            throw new AzureHttpException("Unknown type: " + resource.getType());
        }
    }

    private Map<String, String> getMetricsParams(Collection<String> metricNames) {
        Map<String, String> params = new HashMap<>();
        params.put(INTERVAL_PARAM, METRIC_INTERVAL);
        // limit cost, start time must be at least 1 min before
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS).plusSeconds(-60L);
        String toTime = now.toString();
        // at least 2 min range
        params.put(TIMESPAN_PARAM, now.plusSeconds(-TIMESPAN_SECOND - 60L).toString() + "/" + toTime);
        params.put(METRIC_NAMES_PARAM, String.join(METRIC_DELIMITER, metricNames));
        return params;
    }

    private AzureResultMetric mapNodeResult(AzureCollectorRequest request, Map.Entry<String, Double> metricData) {
        return AzureResultMetric.newBuilder()
            .setResourceGroup(request.getResourceGroup())
            .setResourceName(request.getResource())
            .setType("node")
            .setAlias(getNodeMetricAlias(metricData.getKey()))
            .setValue(
                AzureValueMetric.newBuilder()
                    .setType(AzureValueType.INT64)
                    .setUint64(metricData.getValue().longValue())
                    .build())
            .build();
    }

    private AzureResultMetric mapInterfaceResult(AzureCollectorRequest request,
                                                 String resourceName, String type,
                                                 Map.Entry<String, Double> metricData) {
        return AzureResultMetric.newBuilder()
            .setResourceGroup(request.getResourceGroup())
            .setResourceName(resourceName)
            .setType(type)
            .setAlias(getInterfaceMetricAlias(metricData.getKey()))
            .setValue(
                AzureValueMetric.newBuilder()
                    .setType(AzureValueType.INT64)
                    .setUint64(metricData.getValue().longValue())
                    .build())
            .build();
    }

    private String getNodeMetricAlias(String metricName) {
        if (AZURE_NODE_METRIC_TO_ALIAS.containsKey(metricName)) {
            return AZURE_NODE_METRIC_TO_ALIAS.get(metricName);
        }
        throw new IllegalArgumentException("Failed to find alias '" + metricName + "' - shouldn't be reached");
    }

    private String getInterfaceMetricAlias(String metricName) {
        if (AZURE_INTERFACE_METRIC_TO_ALIAS.containsKey(metricName)) {
            return AZURE_INTERFACE_METRIC_TO_ALIAS.get(metricName);
        }
        if (AZURE_IPINTERFACE_METRIC_TO_ALIAS.containsKey(metricName)) {
            return AZURE_IPINTERFACE_METRIC_TO_ALIAS.get(metricName);
        }
        throw new IllegalArgumentException("Failed to find alias '" + metricName + "' - shouldn't be reached");
    }
}
