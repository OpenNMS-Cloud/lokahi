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

package org.opennms.horizon.server.service.metrics;

import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.server.model.TimeRangeUnit;
import org.opennms.horizon.server.model.TimeSeriesQueryResult;
import org.opennms.horizon.server.service.grpc.InventoryClient;
import org.opennms.horizon.server.service.metrics.normalization.NormalizationService;
import org.opennms.horizon.server.utils.ServerHeaderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.opennms.horizon.server.service.metrics.normalization.Constants.AZURE_MONITOR_TYPE;
import static org.opennms.horizon.server.service.metrics.normalization.Constants.AZURE_SCAN_TYPE;
import static org.opennms.horizon.server.service.metrics.normalization.Constants.QUERY_FOR_TOTAL_NETWORK_BYTES_IN;
import static org.opennms.horizon.server.service.metrics.normalization.Constants.QUERY_FOR_TOTAL_NETWORK_BYTES_OUT;
import static org.opennms.horizon.server.service.metrics.normalization.Constants.TOTAL_NETWORK_BYTES_IN;
import static org.opennms.horizon.server.service.metrics.normalization.Constants.TOTAL_NETWORK_BYTES_OUT;

@Slf4j
@GraphQLApi
@Service
public class TSDBMetricsService {

    private final ServerHeaderUtil headerUtil;
    private final MetricLabelUtils metricLabelUtils;
    private final QueryService queryService;
    private final NormalizationService normalizationService;
    private final InventoryClient inventoryClient;
    private final WebClient tsdbQueryWebClient;
    private final WebClient tsdbrangeQueryWebClient;
    private static final Logger LOG = LoggerFactory.getLogger(TSDBMetricsService.class);

    public TSDBMetricsService(ServerHeaderUtil headerUtil,
                              MetricLabelUtils metricLabelUtils,
                              QueryService queryService,
                              NormalizationService normalizationService,
                              InventoryClient inventoryClient,
                              @Value("${tsdb.url}") String tsdbURL) {

        this.headerUtil = headerUtil;
        this.metricLabelUtils = metricLabelUtils;
        this.queryService = queryService;
        this.normalizationService = normalizationService;
        this.inventoryClient = inventoryClient;
        String tsdbQueryURL = tsdbURL + "/query";
        String tsdbRangeQueryURL = tsdbURL + "/query_range";
        this.tsdbQueryWebClient = WebClient.builder()
            .baseUrl(tsdbQueryURL)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
            .build();
        this.tsdbrangeQueryWebClient = WebClient.builder()
            .baseUrl(tsdbRangeQueryURL)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
            .build();
    }

    @GraphQLQuery
    public Mono<TimeSeriesQueryResult> getMetric(@GraphQLEnvironment ResolutionEnvironment env,
                                                 String name, Map<String, String> labels,
                                                 Integer timeRange, TimeRangeUnit timeRangeUnit) {

        Map<String, String> metricLabels = Optional.ofNullable(labels)
            .map(HashMap::new).orElseGet(HashMap::new);

        String metricNameRegex = name;
        String tenantId = headerUtil.extractTenant(env);
        if (TOTAL_NETWORK_BYTES_IN.equals(name) || TOTAL_NETWORK_BYTES_OUT.equals(name)) {
            // TODO: Defaults to 24h with 1h steps but may need to align both step and range in the rate query
            long end = System.currentTimeMillis() / 1000L;
            long start = end - getDuration(timeRange, timeRangeUnit).orElse(Duration.ofHours(24)).getSeconds();
            String rangeQuerySuffix = "&start=" + start + "&end=" + end +
                "&step=1h";
            if (TOTAL_NETWORK_BYTES_IN.equals(name)) {
                String rangeQuery = QUERY_FOR_TOTAL_NETWORK_BYTES_IN + rangeQuerySuffix;
                return getRangeMetrics(tenantId, rangeQuery);
            }
            String rangeQuery = QUERY_FOR_TOTAL_NETWORK_BYTES_OUT + rangeQuerySuffix;
            return getRangeMetrics(tenantId, rangeQuery);
        }
        //in the case of minion echo, there is no node information
        Optional<NodeDTO> nodeOpt = getNode(env, metricLabels);
        if (nodeOpt.isPresent()) {
            NodeDTO node = nodeOpt.get();
            setMonitorTypeByScanType(node, metricLabels);

            metricNameRegex = normalizationService
                .getQueryMetricRegex(node, name, metricLabels);
        }

        String queryString = queryService
            .getQueryString(metricNameRegex, metricLabels, timeRange, timeRangeUnit);

        Mono<TimeSeriesQueryResult> resultMono = getMetrics(tenantId, queryString);
        return nodeOpt.map(nodeDTO -> resultMono.map(result ->
                normalizationService.normalizeResults(name, result)))
            .orElse(resultMono);
    }

    public static Optional<Duration> getDuration(Integer timeRange, TimeRangeUnit timeRangeUnit) {
        try {
            if (TimeRangeUnit.DAY.value.equals(timeRangeUnit.value)) {
                return Optional.of(Duration.parse("P" + timeRange + timeRangeUnit.value));
            }
            return Optional.of(Duration.parse("PT" + timeRange + timeRangeUnit.value));
        } catch (Exception e) {
            LOG.warn("Exception while parsing time range with timeRange {} in units {}", timeRange, timeRangeUnit, e);
        }
        return Optional.empty();
    }


    private Optional<NodeDTO> getNode(ResolutionEnvironment env, Map<String, String> metricLabels) {
        return metricLabelUtils.getNodeId(metricLabels).map(nodeId -> {
            String accessToken = headerUtil.getAuthHeader(env);
            return Optional.of(inventoryClient.getNodeById(nodeId, accessToken));
        }).orElse(Optional.empty());
    }

    private void setMonitorTypeByScanType(NodeDTO node, Map<String, String> metricLabels) {
        String scanType = node.getScanType();
        if (AZURE_SCAN_TYPE.equals(scanType)) {
            metricLabels.put(MetricLabelUtils.MONITOR_KEY, AZURE_MONITOR_TYPE);
        }
    }

    private Mono<TimeSeriesQueryResult> getMetrics(String tenantId, String queryString) {
        return tsdbQueryWebClient.post()
            .header("X-Scope-OrgID", tenantId)
            .bodyValue(queryString)
            .retrieve()
            .bodyToMono(TimeSeriesQueryResult.class);
    }

    private Mono<TimeSeriesQueryResult> getRangeMetrics(String tenantId, String queryString) {
        return tsdbrangeQueryWebClient.post()
            .header("X-Scope-OrgID", tenantId)
            .bodyValue(queryString)
            .retrieve()
            .bodyToMono(TimeSeriesQueryResult.class);
    }
}
