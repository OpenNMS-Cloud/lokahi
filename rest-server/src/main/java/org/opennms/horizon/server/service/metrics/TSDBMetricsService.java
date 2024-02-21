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
package org.opennms.horizon.server.service.metrics;

import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.server.model.TimeRangeUnit;
import org.opennms.horizon.server.model.TimeSeriesQueryResult;
import org.opennms.horizon.server.service.grpc.InventoryClient;
import org.opennms.horizon.server.utils.ServerHeaderUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@GraphQLApi
@Service
public class TSDBMetricsService {

    private static final String QUERY_ENDPOINT = "/query";
    private static final String QUERY_RANGE_ENDPOINT = "/query_range";

    private final ServerHeaderUtil headerUtil;
    private final MetricLabelUtils metricLabelUtils;
    private final QueryService queryService;
    private final InventoryClient inventoryClient;
    private final WebClient tsdbQueryWebClient;
    private final WebClient tsdbrangeQueryWebClient;

    public TSDBMetricsService(
            ServerHeaderUtil headerUtil,
            MetricLabelUtils metricLabelUtils,
            QueryService queryService,
            InventoryClient inventoryClient,
            @Value("${tsdb.url}") String tsdbURL) {

        this.headerUtil = headerUtil;
        this.metricLabelUtils = metricLabelUtils;
        this.queryService = queryService;
        this.inventoryClient = inventoryClient;
        String tsdbQueryURL = tsdbURL + QUERY_ENDPOINT;
        String tsdbRangeQueryURL = tsdbURL + QUERY_RANGE_ENDPOINT;
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
    public Mono<TimeSeriesQueryResult> getMetric(
            @GraphQLEnvironment ResolutionEnvironment env,
            String name,
            Map<String, String> labels,
            Integer timeRange,
            TimeRangeUnit timeRangeUnit) {

        Map<String, String> metricLabels =
                Optional.ofNullable(labels).map(HashMap::new).orElseGet(HashMap::new);

        String tenantId = headerUtil.extractTenant(env);

        // in the case of minion echo, there is no node information
        Optional<NodeDTO> nodeOpt = getNode(env, metricLabels);

        String queryString = queryService.getQueryString(nodeOpt, name, metricLabels, timeRange, timeRangeUnit);

        if (queryService.isRangeQuery(name)) {
            return getRangeMetrics(tenantId, queryString);
        }
        return getMetrics(tenantId, queryString);
    }

    public Mono<TimeSeriesQueryResult> getCustomMetric(
            @GraphQLEnvironment ResolutionEnvironment env,
            String name,
            Map<String, String> labels,
            Integer timeRange,
            TimeRangeUnit timeRangeUnit,
            Map<String, String> optionalParams) {

        Map<String, String> metricLabels =
                Optional.ofNullable(labels).map(HashMap::new).orElseGet(HashMap::new);

        String tenantId = headerUtil.extractTenant(env);
        String queryString =
                queryService.getCustomQueryString(name, metricLabels, timeRange, timeRangeUnit, optionalParams);
        return getMetrics(tenantId, queryString);
    }

    private Optional<NodeDTO> getNode(ResolutionEnvironment env, Map<String, String> metricLabels) {
        return metricLabelUtils
                .getNodeId(metricLabels)
                .map(nodeId -> {
                    String accessToken = headerUtil.getAuthHeader(env);
                    return Optional.of(inventoryClient.getNodeById(nodeId, accessToken));
                })
                .orElse(Optional.empty());
    }

    private Mono<TimeSeriesQueryResult> getMetrics(String tenantId, String queryString) {
        return tsdbQueryWebClient
                .post()
                .header("X-Scope-OrgID", tenantId)
                .bodyValue(queryString)
                .retrieve()
                .bodyToMono(TimeSeriesQueryResult.class);
    }

    private Mono<TimeSeriesQueryResult> getRangeMetrics(String tenantId, String queryString) {
        return tsdbrangeQueryWebClient
                .post()
                .header("X-Scope-OrgID", tenantId)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .bodyValue(queryString)
                .retrieve()
                .bodyToMono(TimeSeriesQueryResult.class);
    }
}
