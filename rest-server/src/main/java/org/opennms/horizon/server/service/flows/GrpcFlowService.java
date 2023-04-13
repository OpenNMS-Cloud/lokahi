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

package org.opennms.horizon.server.service.flows;

import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.server.mapper.IpInterfaceMapper;
import org.opennms.horizon.server.mapper.NodeMapper;
import org.opennms.horizon.server.mapper.flows.FlowingPointMapper;
import org.opennms.horizon.server.mapper.flows.TrafficSummaryMapper;
import org.opennms.horizon.server.model.flows.Exporter;
import org.opennms.horizon.server.model.flows.FlowingPoint;
import org.opennms.horizon.server.model.flows.RequestCriteria;
import org.opennms.horizon.server.model.flows.TrafficSummary;
import org.opennms.horizon.server.service.grpc.InventoryClient;
import org.opennms.horizon.server.utils.ServerHeaderUtil;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Objects;

@Slf4j
@GraphQLApi
@Service
@RequiredArgsConstructor
public class GrpcFlowService {
    private final ServerHeaderUtil headerUtil;
    private final FlowClient flowClient;
    private final InventoryClient inventoryClient;

    private final NodeMapper nodeMapper;
    private final IpInterfaceMapper ipInterfaceMapper;
    private final TrafficSummaryMapper trafficSummaryMapper;
    private final FlowingPointMapper flowingPointMapper;

    @GraphQLQuery(name = "findExporters")
    public Flux<Exporter> findExporters(RequestCriteria requestCriteria,
                                        @GraphQLEnvironment ResolutionEnvironment env) {
        String tenantId = headerUtil.extractTenant(env);
        String authHeader = headerUtil.getAuthHeader(env);
        var interfaceIds = flowClient.findExporters(requestCriteria, tenantId, authHeader);
        return Flux.fromIterable(interfaceIds.stream()
            .map(interfaceId -> getExporter(interfaceId, env)).filter(Objects::nonNull).toList());
    }

    @GraphQLQuery(name = "findApplications")
    public Flux<String> findApplications(RequestCriteria requestCriteria,
                                         @GraphQLEnvironment ResolutionEnvironment env) {
        String tenantId = headerUtil.extractTenant(env);
        String authHeader = headerUtil.getAuthHeader(env);
        return Flux.fromIterable(flowClient.findApplications(requestCriteria, tenantId, authHeader));
    }

    @GraphQLQuery(name = "findApplicationSummaries")
    public Flux<TrafficSummary> findApplicationSummaries(RequestCriteria requestCriteria,
                                                         @GraphQLEnvironment ResolutionEnvironment env) {
        String tenantId = headerUtil.extractTenant(env);
        String authHeader = headerUtil.getAuthHeader(env);
        var summaries = flowClient.getApplicationSummaries(requestCriteria, tenantId, authHeader);
        return Flux.fromIterable(summaries.getSummariesList().stream().map(trafficSummaryMapper::map).toList());
    }

    @GraphQLQuery(name = "findApplicationSeries")
    public Flux<FlowingPoint> findApplicationSeries(RequestCriteria requestCriteria,
                                                    @GraphQLEnvironment ResolutionEnvironment env) {
        String tenantId = headerUtil.extractTenant(env);
        String authHeader = headerUtil.getAuthHeader(env);
        return Flux.fromIterable(flowClient.getApplicationSeries(requestCriteria, tenantId, authHeader).getPointList().stream()
            .map(flowingPointMapper::map).toList());
    }

    private Exporter getExporter(long interfaceId, ResolutionEnvironment env) {
        if (headerUtil.getAuthHeader(env) != null) {
            var ipInterfaceDTO = inventoryClient.getIpInterfaceById(interfaceId, headerUtil.getAuthHeader(env));
            if (ipInterfaceDTO != null) {
                var nodeDTO = inventoryClient.getNodeById(ipInterfaceDTO.getNodeId(), headerUtil.getAuthHeader(env));
                var exporter = new Exporter();
                exporter.setIpInterface(ipInterfaceMapper.protoToIpInterface(ipInterfaceDTO));
                exporter.setNode(nodeMapper.protoToNode(nodeDTO));
                return exporter;
            }
        }
        return null;
    }
}
