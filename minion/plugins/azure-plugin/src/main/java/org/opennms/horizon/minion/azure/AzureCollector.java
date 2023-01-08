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
import org.opennms.horizon.shared.azure.http.dto.metrics.AzureDatum;
import org.opennms.horizon.shared.azure.http.dto.metrics.AzureMetrics;
import org.opennms.horizon.shared.azure.http.dto.metrics.AzureTimeseries;
import org.opennms.horizon.shared.azure.http.dto.metrics.AzureValue;
import org.opennms.horizon.snmp.api.SnmpResponseMetric;
import org.opennms.taskset.contract.MonitorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class AzureCollector implements ServiceCollector {
    private final Logger log = LoggerFactory.getLogger(AzureCollector.class);

    private static final String INTERVAL_PARAM = "interval";
    private static final String METRIC_NAMES_PARAM = "metricnames";

    private static final String METRIC_INTERVAL = "PT1M";
    private static final String[] METRIC_NAMES = {
        "Network In Total", "Network Out Total", "Percentage CPU", "Disk Read Bytes", "Disk Write Bytes"
    };
    private static final String METRIC_DELIMITER = ",";

    private final AzureHttpClient client;

    public AzureCollector(AzureHttpClient client) {
        this.client = client;
    }

    @Override
    public CompletableFuture<CollectionSet> collect(CollectionRequest svc, Any config) {
        CompletableFuture<CollectionSet> future = new CompletableFuture<>();

        try {
            if (!config.is(AzureCollectorRequest.class)) {
                throw new IllegalArgumentException("configuration must be an AzureCollectorRequest; type-url=" + config.getTypeUrl());
            }

            AzureCollectorRequest request = config.unpack(AzureCollectorRequest.class);

            AzureOAuthToken token = client.login(request.getDirectoryId(),
                request.getClientId(), request.getClientSecret(), request.getTimeout());

            AzureInstanceView instanceView = client.getInstanceView(token, request.getSubscriptionId(),
                request.getResourceGroup(), request.getResource(), request.getTimeout());

            if (instanceView.isUp()) {

                Map<String, Double> collectedData = new HashMap<>();
                collectedData.put("Uptime Ms", instanceView.getUptimeInMs().doubleValue());

                collect(svc, request, collectedData, future, token);

            } else {
                future.complete(ServiceCollectorResponseImpl.builder()
                    .nodeId(svc.getNodeId())
                    .monitorType(MonitorType.SNMP)
                    .status(false)
                    .ipAddress(svc.getIpAddress()).build());
            }
        } catch (Exception e) {
            log.error("Failed to collect for azure resource", e);
            future.complete(ServiceCollectorResponseImpl.builder()
                .nodeId(svc.getNodeId())
                .monitorType(MonitorType.SNMP)
                .status(false)
                .ipAddress(svc.getIpAddress()).build());
        }
        return future;
    }

    private void collect(CollectionRequest svc, AzureCollectorRequest request, Map<String, Double> collectedData,
                         CompletableFuture<CollectionSet> future, AzureOAuthToken token) throws AzureHttpException {

        Map<String, String> params = new HashMap<>();
        params.put(INTERVAL_PARAM, METRIC_INTERVAL);
        params.put(METRIC_NAMES_PARAM, String.join(METRIC_DELIMITER, METRIC_NAMES));

        AzureMetrics metrics = client.getMetrics(token, request.getSubscriptionId(),
            request.getResourceGroup(), request.getResource(), params, request.getTimeout());

        for (AzureValue metric : metrics.getValue()) {
            collectMetric(collectedData, metric);
        }

        System.out.println("collectedData = " + collectedData);

        SnmpResponseMetric response = SnmpResponseMetric.newBuilder()
//                .addAllResults(snmpResults)
            .build();

        future.complete(ServiceCollectorResponseImpl.builder()
            .results(response)
            .nodeId(svc.getNodeId())
            .monitorType(MonitorType.SNMP)
            .status(true)
            .ipAddress(svc.getIpAddress()).build());
    }

    private void collectMetric(Map<String, Double> collectedData, AzureValue metric) {
        String metricName = metric.getName().getValue();

        List<AzureTimeseries> timeseriesList = metric.getTimeseries();
        if (timeseriesList.isEmpty()) {
            collectedData.put(metricName, 0d);
        } else {

            AzureTimeseries timeseries = timeseriesList.get(0);
            List<AzureDatum> data = timeseries.getData();

            //todo: sanity check - double check we actually need to sort
            data.sort((o1, o2) -> {
                Instant t1 = Instant.parse(o1.getTimeStamp());
                Instant t2 = Instant.parse(o2.getTimeStamp());
                return t1.compareTo(t2);
            });

            // for now getting last value as it is most recent
            AzureDatum datum = data.get(data.size() - 1);

            Double value = datum.getValue();
            if (value == null) {
                value = 0d;
            }
            collectedData.put(metricName, value);
        }
    }
}
