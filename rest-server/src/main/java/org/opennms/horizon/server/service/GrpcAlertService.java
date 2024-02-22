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
package org.opennms.horizon.server.service;

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.alerts.proto.EventType;
import org.opennms.horizon.server.mapper.alert.AlertMapper;
import org.opennms.horizon.server.model.alerts.AlertCount;
import org.opennms.horizon.server.model.alerts.AlertEventDefinition;
import org.opennms.horizon.server.model.alerts.AlertResponse;
import org.opennms.horizon.server.model.alerts.CountAlertResponse;
import org.opennms.horizon.server.model.alerts.DeleteAlertResponse;
import org.opennms.horizon.server.model.alerts.ListAlertResponse;
import org.opennms.horizon.server.model.alerts.MonitorPolicy;
import org.opennms.horizon.server.model.alerts.TimeRange;
import org.opennms.horizon.server.service.grpc.AlertsClient;
import org.opennms.horizon.server.utils.ServerHeaderUtil;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@GraphQLApi
@Service
public class GrpcAlertService {

    private final AlertsClient alertsClient;
    private final ServerHeaderUtil headerUtil;
    private final AlertMapper mapper;

    @SuppressWarnings("squid:S107")
    @GraphQLQuery
    public Mono<ListAlertResponse> findAllAlerts(
            @GraphQLArgument(name = "pageSize") Integer pageSize,
            @GraphQLArgument(name = "page") int page,
            @GraphQLArgument(name = "timeRange") TimeRange timeRange,
            @GraphQLArgument(name = "severities") List<String> severities,
            @GraphQLArgument(name = "sortBy") String sortBy,
            @GraphQLArgument(name = "sortAscending") boolean sortAscending,
            @GraphQLArgument(name = "nodeLabel") String nodeLabel,
            @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(alertsClient.listAlerts(
                        pageSize,
                        page,
                        severities,
                        timeRange,
                        sortBy,
                        sortAscending,
                        nodeLabel,
                        headerUtil.getAuthHeader(env)))
                .map(mapper::protoToAlertResponse);
    }

    @GraphQLQuery(
            name = "countAlerts",
            description = "Returns the total count of alerts filtered by severity and time.")
    public Mono<CountAlertResponse> countAlerts(
            @GraphQLArgument(name = "timeRange") TimeRange timeRange,
            @GraphQLArgument(name = "severityFilters") List<String> severityFilters,
            @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(mapper.protoToCountAlertResponse(
                alertsClient.countAlerts(severityFilters, timeRange, headerUtil.getAuthHeader(env))));
    }

    @GraphQLMutation
    public Mono<AlertResponse> acknowledgeAlert(
            @GraphQLArgument(name = "ids") List<Long> ids, @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(
                mapper.protoToAlertResponse(alertsClient.acknowledgeAlert(ids, headerUtil.getAuthHeader(env))));
    }

    @GraphQLMutation
    public Mono<AlertResponse> unacknowledgeAlert(
            @GraphQLArgument(name = "ids") List<Long> ids, @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(
                mapper.protoToAlertResponse(alertsClient.unacknowledgeAlert(ids, headerUtil.getAuthHeader(env))));
    }

    @GraphQLMutation
    public Mono<AlertResponse> escalateAlert(
            @GraphQLArgument(name = "ids") List<Long> ids, @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(mapper.protoToAlertResponse(alertsClient.escalateAlert(ids, headerUtil.getAuthHeader(env))));
    }

    @GraphQLMutation
    public Mono<AlertResponse> clearAlert(
            @GraphQLArgument(name = "ids") List<Long> ids, @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(mapper.protoToAlertResponse(alertsClient.clearAlert(ids, headerUtil.getAuthHeader(env))));
    }

    @GraphQLMutation
    public Mono<DeleteAlertResponse> deleteAlert(
            @GraphQLArgument(name = "ids") List<Long> ids, @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(
                mapper.protoToDeleteAlertResponse(alertsClient.deleteAlert(ids, headerUtil.getAuthHeader(env))));
    }

    @GraphQLMutation
    public Mono<MonitorPolicy> createMonitorPolicy(
            MonitorPolicy policy, @GraphQLEnvironment ResolutionEnvironment env) {
        var monitorPolicy = alertsClient.createMonitorPolicy(policy, headerUtil.getAuthHeader(env));
        return Mono.just(monitorPolicy);
    }

    @GraphQLQuery
    public Flux<MonitorPolicy> listMonitoryPolicies(@GraphQLEnvironment ResolutionEnvironment env) {
        return Flux.fromIterable(alertsClient.listMonitorPolicies(headerUtil.getAuthHeader(env)));
    }

    @GraphQLQuery
    public Mono<MonitorPolicy> findMonitorPolicyById(Long id, @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(alertsClient.getMonitorPolicyById(id, headerUtil.getAuthHeader(env)));
    }

    @GraphQLQuery
    public Mono<MonitorPolicy> getDefaultPolicy(@GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(alertsClient.getDefaultPolicy(headerUtil.getAuthHeader(env)));
    }

    @GraphQLQuery
    public Flux<AlertEventDefinition> listAlertEventDefinitions(
            EventType eventType, @GraphQLEnvironment ResolutionEnvironment env) {
        return Flux.fromIterable(alertsClient.listAlertEventDefinitions(eventType, headerUtil.getAuthHeader(env)));
    }

    @GraphQLMutation
    public Mono<Boolean> deletePolicyById(Long id, @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(alertsClient.deletePolicyById(id, headerUtil.getAuthHeader(env)));
    }

    @GraphQLMutation
    public Mono<Boolean> deleteRuleById(Long id, @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(alertsClient.deleteRuleById(id, headerUtil.getAuthHeader(env)));
    }

    @GraphQLQuery
    public Mono<Long> countAlertByPolicyId(Long id, @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(alertsClient.countAlertByPolicyId(id, headerUtil.getAuthHeader(env)));
    }

    @GraphQLQuery
    public Mono<Long> countAlertByRuleId(Long id, @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(alertsClient.countAlertByRuleId(id, headerUtil.getAuthHeader(env)));
    }

    @GraphQLQuery(name = "alertCounts")
    public Mono<AlertCount> getAlertCounts(@GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(alertsClient.countAlerts(headerUtil.getAuthHeader(env)));
    }

    @GraphQLQuery(name = "getAlertsByNode")
    public Mono<ListAlertResponse> getRecentAlertsByNode(
            @GraphQLArgument(name = "pageSize") Integer pageSize,
            @GraphQLArgument(name = "page") int page,
            @GraphQLArgument(name = "sortBy") String sortBy,
            @GraphQLArgument(name = "sortAscending") boolean sortAscending,
            @GraphQLArgument(name = "nodeId") long nodeId,
            @GraphQLEnvironment ResolutionEnvironment env) {

        return Mono.just(alertsClient.getAlertsByNode(
                        pageSize, page, sortBy, sortAscending, nodeId, headerUtil.getAuthHeader(env)))
                .map(mapper::protoToAlertResponse);
    }
}
