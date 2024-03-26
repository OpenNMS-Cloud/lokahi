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
package org.opennms.horizon.server.service.flows;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.server.mapper.IpInterfaceMapper;
import org.opennms.horizon.server.mapper.NodeMapper;
import org.opennms.horizon.server.mapper.SnmpInterfaceMapper;
import org.opennms.horizon.server.mapper.flows.FlowingPointMapper;
import org.opennms.horizon.server.mapper.flows.TrafficSummaryMapper;
import org.opennms.horizon.server.model.flows.Exporter;
import org.opennms.horizon.server.model.flows.FlowingPoint;
import org.opennms.horizon.server.model.flows.RequestCriteria;
import org.opennms.horizon.server.model.flows.TrafficSummary;
import org.opennms.horizon.server.service.grpc.InventoryClient;
import org.opennms.horizon.server.utils.ServerHeaderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Slf4j
@GraphQLApi
@Service
@RequiredArgsConstructor
public class GraphQLFlowService {
    private static final Logger LOG = LoggerFactory.getLogger(GraphQLFlowService.class);
    private final ServerHeaderUtil headerUtil;
    private final FlowClient flowClient;
    private final InventoryClient inventoryClient;

    private final NodeMapper nodeMapper;
    private final IpInterfaceMapper ipInterfaceMapper;

    private final SnmpInterfaceMapper snmpInterfaceMapper;
    private final TrafficSummaryMapper trafficSummaryMapper;
    private final FlowingPointMapper flowingPointMapper;

    @GraphQLQuery(name = "findExporters")
    public Flux<Exporter> findExporters(
            RequestCriteria requestCriteria, @GraphQLEnvironment ResolutionEnvironment env) {
        String tenantId = headerUtil.extractTenant(env);
        String authHeader = headerUtil.getAuthHeader(env);
        var interfaceIds = flowClient.findExporters(requestCriteria, tenantId, authHeader);
        return Flux.fromIterable(interfaceIds.stream()
                .map(interfaceId -> getExporter(interfaceId, env))
                .filter(Objects::nonNull)
                .toList());
    }

    @GraphQLQuery(name = "findApplications")
    public Flux<String> findApplications(
            RequestCriteria requestCriteria, @GraphQLEnvironment ResolutionEnvironment env) {
        String tenantId = headerUtil.extractTenant(env);
        String authHeader = headerUtil.getAuthHeader(env);
        return Flux.fromIterable(flowClient.findApplications(requestCriteria, tenantId, authHeader));
    }

    @GraphQLQuery(name = "findApplicationSummaries")
    public Flux<TrafficSummary> findApplicationSummaries(
            RequestCriteria requestCriteria, @GraphQLEnvironment ResolutionEnvironment env) {
        String tenantId = headerUtil.extractTenant(env);
        String authHeader = headerUtil.getAuthHeader(env);
        var summaries = flowClient.getApplicationSummaries(requestCriteria, tenantId, authHeader);
        return Flux.fromIterable(summaries.getSummariesList().stream()
                .map(trafficSummaryMapper::map)
                .toList());
    }

    @GraphQLQuery(name = "findApplicationSeries")
    public Flux<FlowingPoint> findApplicationSeries(
            RequestCriteria requestCriteria, @GraphQLEnvironment ResolutionEnvironment env) {
        String tenantId = headerUtil.extractTenant(env);
        String authHeader = headerUtil.getAuthHeader(env);
        var series = flowClient.getApplicationSeries(requestCriteria, tenantId, authHeader);
        var points = series.getPointList().stream().map(flowingPointMapper::map).toList();
        UnitConverter.convert(points);
        return Flux.fromIterable(points);
    }

    private Exporter getExporter(long interfaceId, ResolutionEnvironment env) {
        String authHeader = Objects.requireNonNull(headerUtil.getAuthHeader(env));
        Exporter exporter = null;
        try {
            var ipInterfaceDTO = inventoryClient.getIpInterfaceById(interfaceId, authHeader);
            if (ipInterfaceDTO != null) {
                var nodeDTO = inventoryClient.getNodeById(ipInterfaceDTO.getNodeId(), authHeader);
                exporter = new Exporter();
                exporter.setIpInterface(ipInterfaceMapper.protoToIpInterface(ipInterfaceDTO));
                exporter.setNode(nodeMapper.protoToNode(nodeDTO));
                if (nodeDTO.getSnmpInterfacesCount() > 0) {
                    var filteredSnmpInterfaces = nodeDTO.getSnmpInterfacesList().stream()
                            .filter(s -> s.getId() == ipInterfaceDTO.getSnmpInterfaceId())
                            .toList();
                    if (filteredSnmpInterfaces.size() == 1) {
                        exporter.setSnmpInterface(
                                snmpInterfaceMapper.protobufToSnmpInterface(filteredSnmpInterfaces.get(0)));
                    }
                }
                exporter.setNode(nodeMapper.protoToNode(nodeDTO));
            }
        } catch (StatusRuntimeException ex) {
            if (Status.Code.NOT_FOUND.equals(ex.getStatus().getCode())) {
                LOG.debug("Fail to getExporter by interfaceId: {} Message: {}", interfaceId, ex.getMessage());
                return null;
            } else {
                throw ex;
            }
        }
        return exporter;
    }
}
