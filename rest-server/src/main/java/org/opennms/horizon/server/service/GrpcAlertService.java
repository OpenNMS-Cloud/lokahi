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

import java.util.List;

import org.opennms.horizon.server.mapper.AlertMapper;
import org.opennms.horizon.server.model.alerts.Alert;
import org.opennms.horizon.server.model.alerts.AlertResponse;
import org.opennms.horizon.server.model.alerts.MonitorPolicy;
import org.opennms.horizon.server.service.grpc.AlertsClient;
import org.opennms.horizon.server.utils.ServerHeaderUtil;
import org.springframework.stereotype.Service;

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@GraphQLApi
@Service
public class GrpcAlertService {

    private final AlertsClient alertsClient;
    private final ServerHeaderUtil headerUtil;
    private final AlertMapper mapper;

    @GraphQLQuery
    public Mono<AlertResponse> findAllAlerts(@GraphQLArgument(name = "pageSize") Integer pageSize,
                                             @GraphQLArgument(name = "page") String page,
                                             @GraphQLArgument(name = "hours") long hours,
                                             @GraphQLArgument(name = "severities") List<String> severities,
                                             @GraphQLArgument(name = "sortBy") String sortBy,
                                             @GraphQLArgument(name = "sortAscending") boolean sortAscending,
                                             @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(mapper.protoToAlertResponse(alertsClient.listAlerts(pageSize, page, severities, hours, sortBy, sortAscending, headerUtil.getAuthHeader(env))));
    }

    @GraphQLQuery(
        name = "countAlerts",
        description = "Returns the total count of alerts filtered by severity and time."
    )
    public Mono<Long> countAlerts(@GraphQLArgument(name = "hours") long hours,
                                  @GraphQLArgument(name = "severityFilters") List<String> severityFilters,
                                  @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(alertsClient.countAlerts(severityFilters, hours, headerUtil.getAuthHeader(env)));
    }

    @GraphQLQuery(
        name = "countAlerts",
        description = "Returns the total count of alerts for the last 24h."
    )
    public Mono<Long> countAlerts(@GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(alertsClient.countAlerts(null, 0L, headerUtil.getAuthHeader(env)));
    }

    @GraphQLMutation
    public Mono<Alert> acknowledgeAlert(@GraphQLArgument(name = "id") long id, @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(mapper.protoToAlert(alertsClient.acknowledgeAlert(id, headerUtil.getAuthHeader(env))));
    }

    @GraphQLMutation
    public Mono<Alert> unacknowledgeAlert(@GraphQLArgument(name = "id") long id, @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(mapper.protoToAlert(alertsClient.unacknowledgeAlert(id, headerUtil.getAuthHeader(env))));
    }

    @GraphQLMutation
    public Mono<Alert> escalateAlert(@GraphQLArgument(name = "id") long id, @GraphQLArgument(name = "newNodeCriteria") String newNodeCriteria, @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(mapper.protoToAlert(alertsClient.escalateAlert(id, headerUtil.getAuthHeader(env))));
    }

    @GraphQLMutation
    public Mono<Alert> clearAlert(@GraphQLArgument(name = "id") long id, @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(mapper.protoToAlert(alertsClient.clearAlert(id, headerUtil.getAuthHeader(env))));
    }

    @GraphQLMutation
    public Mono<Boolean> deleteAlert(@GraphQLArgument(name = "id") long id, @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(alertsClient.deleteAlert(id, headerUtil.getAuthHeader(env)));
    }

    @GraphQLMutation
    public Mono<MonitorPolicy> createMonitorPolicy(MonitorPolicy policy, @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(alertsClient.createMonitorPolicy(policy, headerUtil.getAuthHeader(env)));
    }

    @GraphQLQuery
    public Flux<MonitorPolicy> listMonitoryPolicies(@GraphQLEnvironment ResolutionEnvironment env) {
        return Flux.fromIterable(alertsClient.listMonitorPolicies(headerUtil.getAuthHeader(env)));
    }

    @GraphQLQuery
    public Mono<MonitorPolicy> findMonitorPolicyById(Long id, @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(alertsClient.getMonitorPolicyById(id, headerUtil.getAuthHeader(env)));
    }
}
