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

package org.opennms.horizon.server.service;

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLContext;
import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
import lombok.RequiredArgsConstructor;
import org.dataloader.DataLoader;
import org.opennms.horizon.server.config.DataLoaderFactory;
import org.opennms.horizon.server.mapper.NodeMapper;
import org.opennms.horizon.server.model.inventory.MonitoringLocation;
import org.opennms.horizon.server.model.inventory.Node;
import org.opennms.horizon.server.model.inventory.NodeCreate;
import org.opennms.horizon.server.model.status.NodeStatus;
import org.opennms.horizon.server.service.grpc.InventoryClient;
import org.opennms.horizon.server.utils.ServerHeaderUtil;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
@GraphQLApi
@Service
public class GrpcNodeService {
    private static final String ICMP_MONITOR_TYPE = "ICMP";

    private final InventoryClient client;
    private final NodeMapper mapper;
    private final ServerHeaderUtil headerUtil;
    private final NodeStatusService nodeStatusService;

    @GraphQLQuery
    public Flux<Node> findAllNodes(@GraphQLEnvironment ResolutionEnvironment env) {
        return Flux.fromIterable(client.listNodes(headerUtil.getAuthHeader(env)).stream().map(mapper::protoToNode).toList());
    }

    @GraphQLQuery
    public Flux<Node> findAllNodesByMonitoredState(@GraphQLArgument(name = "monitoredState") String monitoredState, @GraphQLEnvironment ResolutionEnvironment env) {
        return Flux.fromIterable(client.listNodesByMonitoredState(monitoredState, headerUtil.getAuthHeader(env)).stream().map(mapper::protoToNode).toList());
    }

    @GraphQLQuery
    public Flux<Node> findAllNodesByNodeLabelSearch(@GraphQLArgument(name = "labelSearchTerm") String labelSearchTerm, @GraphQLEnvironment ResolutionEnvironment env) {
        return Flux.fromIterable(client.listNodesByNodeLabelSearch(labelSearchTerm, headerUtil.getAuthHeader(env)).stream().map(mapper::protoToNode).toList());
    }

    @GraphQLQuery
    public Flux<Node> findAllNodesByTags(@GraphQLArgument(name = "tags") List<String> tags, @GraphQLEnvironment ResolutionEnvironment env) {
        return Flux.fromIterable(client.listNodesByTags(tags, headerUtil.getAuthHeader(env)).stream().map(mapper::protoToNode).toList());
    }

    @GraphQLQuery
    public Mono<Node> findNodeById(@GraphQLArgument(name = "id") Long id, @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(mapper.protoToNode(client.getNodeById(id, headerUtil.getAuthHeader(env))));
    }

    @GraphQLMutation
    public Mono<Node> addNode(NodeCreate node, @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(mapper.protoToNode(client.createNewNode(mapper.nodeCreateToProto(node), headerUtil.getAuthHeader(env))));
    }

    @GraphQLQuery
    public CompletableFuture<MonitoringLocation> location(@GraphQLContext Node node, @GraphQLEnvironment ResolutionEnvironment env) {
        DataLoader<DataLoaderFactory.Key, MonitoringLocation> locationLoader = env.dataFetchingEnvironment.getDataLoader(DataLoaderFactory.DATA_LOADER_LOCATION);
        DataLoaderFactory.Key key = new DataLoaderFactory.Key(node.getMonitoringLocationId(), headerUtil.getAuthHeader(env));
        return locationLoader.load(key);
    }

    @GraphQLQuery
    public Mono<NodeStatus> getNodeStatus(@GraphQLArgument(name = "id") Long id, @GraphQLEnvironment ResolutionEnvironment env) {
        return nodeStatusService.getNodeStatus(id, ICMP_MONITOR_TYPE, env);
    }

    @GraphQLMutation
    public Mono<Boolean> deleteNode(@GraphQLArgument(name = "id") Long id, @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(client.deleteNode(id, headerUtil.getAuthHeader(env)));
    }

    @GraphQLMutation
    public Mono<Boolean> discoveryByNodeIds(List<Long> ids, @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(client.startScanByNodeIds(ids, headerUtil.getAuthHeader(env)));
    }
}
