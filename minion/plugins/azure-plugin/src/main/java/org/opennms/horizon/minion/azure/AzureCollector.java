/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
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
import org.opennms.horizon.minion.plugin.api.CollectionRequest;
import org.opennms.horizon.minion.plugin.api.CollectionSet;
import org.opennms.horizon.minion.plugin.api.ServiceCollector;
import org.opennms.horizon.minion.plugin.api.ServiceCollectorResponseImpl;
import org.opennms.horizon.shared.azure.http.AzureHttpClient;
import org.opennms.horizon.shared.azure.http.AzureHttpException;
import org.opennms.horizon.shared.azure.http.dto.instanceview.AzureInstanceView;
import org.opennms.horizon.shared.azure.http.dto.login.AzureOAuthToken;
import org.opennms.horizon.shared.azure.http.dto.metrics.AzureMetrics;
import org.opennms.horizon.snmp.api.SnmpResponseMetric;
import org.opennms.horizon.snmp.api.SnmpResultMetric;
import org.opennms.horizon.snmp.api.SnmpValueMetric;
import org.opennms.horizon.snmp.api.SnmpValueType;
import org.opennms.taskset.contract.MonitorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class AzureCollector implements ServiceCollector {
    private final Logger log = LoggerFactory.getLogger(AzureCollector.class);

    private static final String INTERVAL_PARAM = "interval";
    private static final String METRIC_NAMES_PARAM = "metricnames";


    //   Supported intervals: PT1M,PT5M,PT15M,PT30M,PT1H,PT6H,PT12H,P1D
    private static final String METRIC_INTERVAL = "PT1M";

    private static final Map<String, String> AZURE_METRIC_TO_ALIAS = new HashMap<>();

    static {
        // Azure Metric Key - Metrics Processor Key
        AZURE_METRIC_TO_ALIAS.put("Network In Total", "ifInOctets");
        AZURE_METRIC_TO_ALIAS.put("Network Out Total", "ifOutOctets");
    }

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

                Map<String, Double> collectedData = new HashMap<>();

                collect(request, token, collectedData);

                SnmpResponseMetric results = SnmpResponseMetric.newBuilder()
                    .addAllResults(mapCollectedDataToResults(request, collectedData))
                    .build();

                future.complete(ServiceCollectorResponseImpl.builder()
                    .results(results)
                    .nodeId(collectionRequest.getNodeId())
                    .monitorType(MonitorType.SNMP)
                    .status(true)
                    .ipAddress(request.getHost())
                    .timeStamp(System.currentTimeMillis())
                    .build());

            } else {
                future.complete(ServiceCollectorResponseImpl.builder()
                    .nodeId(collectionRequest.getNodeId())
                    .monitorType(MonitorType.SNMP)
                    .status(false)
                    .ipAddress(request.getHost())
                    .build());
            }
        } catch (Exception e) {
            log.error("Failed to collect for azure resource", e);
            future.complete(ServiceCollectorResponseImpl.builder()
                .nodeId(collectionRequest.getNodeId())
                .monitorType(MonitorType.SNMP)
                .status(false)
                .build());
        }
        return future;
    }

    private void collect(AzureCollectorRequest request,
                         AzureOAuthToken token,
                         Map<String, Double> collectedData) throws AzureHttpException {

        String[] metricNames = AZURE_METRIC_TO_ALIAS.keySet().toArray(new String[0]);

        Map<String, String> params = new HashMap<>();
        params.put(INTERVAL_PARAM, METRIC_INTERVAL);
        params.put(METRIC_NAMES_PARAM, String.join(METRIC_DELIMITER, metricNames));

        AzureMetrics metrics = client.getMetrics(token, request.getSubscriptionId(),
            request.getResourceGroup(), request.getResource(), params, request.getTimeoutMs(), request.getRetries());

        metrics.collect(collectedData);
    }

    private List<SnmpResultMetric> mapCollectedDataToResults(AzureCollectorRequest request,
                                                             Map<String, Double> collectedData) {
        return collectedData.entrySet().stream()
            .map(collectedMetric -> mapMetric(request, collectedMetric))
            .collect(Collectors.toList());
    }

    private SnmpResultMetric mapMetric(AzureCollectorRequest request,
                                       Map.Entry<String, Double> collectedMetric) {

        return SnmpResultMetric.newBuilder()
            .setBase(request.getResourceGroup())
            .setInstance(request.getResource())
            .setAlias(getAlias(collectedMetric.getKey()))
            .setValue(
                SnmpValueMetric.newBuilder()
                    .setType(SnmpValueType.INT32)
                    .setSint64(collectedMetric.getValue().longValue())
                    .build())
            .build();
    }

    private String getAlias(String metricName) {
        if (AZURE_METRIC_TO_ALIAS.containsKey(metricName)) {
            return AZURE_METRIC_TO_ALIAS.get(metricName);
        }
        throw new IllegalArgumentException("Failed to find alias - shouldn't be reached");
    }
}
