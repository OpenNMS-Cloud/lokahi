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

package org.opennms.horizon.server.service;

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.alerts.proto.EventType;
import org.opennms.horizon.server.mapper.TagMapper;
import org.opennms.horizon.server.mapper.alert.AlertMapper;
import org.opennms.horizon.server.model.alerts.AlertEventDefinition;
import org.opennms.horizon.server.model.alerts.AlertResponse;
import org.opennms.horizon.server.model.alerts.CountAlertResponse;
import org.opennms.horizon.server.model.alerts.DeleteAlertResponse;
import org.opennms.horizon.server.model.alerts.ListAlertResponse;
import org.opennms.horizon.server.model.alerts.MonitorPolicy;
import org.opennms.horizon.server.model.alerts.TimeRange;
import org.opennms.horizon.server.service.grpc.AlertsClient;
import org.opennms.horizon.server.service.grpc.InventoryClient;
import org.opennms.horizon.server.utils.ServerHeaderUtil;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RequiredArgsConstructor
@GraphQLApi
@Service
public class GrpcAlertService {

    private final AlertsClient alertsClient;
    private final ServerHeaderUtil headerUtil;
    private final AlertMapper mapper;
    private final TagMapper tagMapper;
    private final InventoryClient client;

    @SuppressWarnings("squid:S107")
    @GraphQLQuery
    public Mono<ListAlertResponse> findAllAlerts(@GraphQLArgument(name = "pageSize") Integer pageSize,
                                                 @GraphQLArgument(name = "page") int page,
                                                 @GraphQLArgument(name = "timeRange") TimeRange timeRange,
                                                 @GraphQLArgument(name = "severities") List<String> severities,
                                                 @GraphQLArgument(name = "sortBy") String sortBy,
                                                 @GraphQLArgument(name = "sortAscending") boolean sortAscending,
                                                 @GraphQLArgument(name = "nodeLabel") String nodeLabel,
                                                 @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(mapper.protoToAlertResponse(alertsClient.listAlerts(pageSize, page, severities, timeRange, sortBy, sortAscending, nodeLabel, headerUtil.getAuthHeader(env))));
    }

    @GraphQLQuery(
        name = "countAlerts",
        description = "Returns the total count of alerts filtered by severity and time."
    )
    public Mono<CountAlertResponse> countAlerts(@GraphQLArgument(name = "timeRange") TimeRange timeRange,
                                  @GraphQLArgument(name = "severityFilters") List<String> severityFilters,
                                  @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(mapper.protoToCountAlertResponse(alertsClient.countAlerts(severityFilters, timeRange, headerUtil.getAuthHeader(env))));
    }

    @GraphQLMutation
    public Mono<AlertResponse> acknowledgeAlert(@GraphQLArgument(name = "ids") List<Long> ids,
                                                @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(mapper.protoToAlertResponse(alertsClient.acknowledgeAlert(ids, headerUtil.getAuthHeader(env))));
    }

    @GraphQLMutation
    public Mono<AlertResponse> unacknowledgeAlert(@GraphQLArgument(name = "ids") List<Long> ids,
                                                  @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(mapper.protoToAlertResponse(alertsClient.unacknowledgeAlert(ids, headerUtil.getAuthHeader(env))));
    }

    @GraphQLMutation
    public Mono<AlertResponse> escalateAlert(@GraphQLArgument(name = "ids") List<Long> ids,
                                             @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(mapper.protoToAlertResponse(alertsClient.escalateAlert(ids, headerUtil.getAuthHeader(env))));
    }

    @GraphQLMutation
    public Mono<AlertResponse> clearAlert(@GraphQLArgument(name = "ids") List<Long> ids,
                                          @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(mapper.protoToAlertResponse(alertsClient.clearAlert(ids, headerUtil.getAuthHeader(env))));
    }

    @GraphQLMutation
    public Mono<DeleteAlertResponse> deleteAlert(@GraphQLArgument(name = "ids") List<Long> ids,
                                                 @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(mapper.protoToDeleteAlertResponse(alertsClient.deleteAlert(ids, headerUtil.getAuthHeader(env))));
    }

    @GraphQLMutation
    public Mono<MonitorPolicy> createMonitorPolicy(MonitorPolicy policy, @GraphQLEnvironment ResolutionEnvironment env) {
        String authHeader = headerUtil.getAuthHeader(env);
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
    public Flux<AlertEventDefinition> listAlertEventDefinitions(EventType eventType, @GraphQLEnvironment ResolutionEnvironment env) {
        return Flux.fromIterable(alertsClient.listAlertEventDefinitions(eventType, headerUtil.getAuthHeader(env)));
    }

}
