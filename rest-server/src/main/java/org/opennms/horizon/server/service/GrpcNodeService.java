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
import io.leangen.graphql.annotations.GraphQLContext;
import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.dataloader.DataLoader;
import org.opennms.horizon.server.config.DataLoaderFactory;
import org.opennms.horizon.server.mapper.IpInterfaceMapper;
import org.opennms.horizon.server.mapper.NodeMapper;
import org.opennms.horizon.server.model.TimeRangeUnit;
import org.opennms.horizon.server.model.inventory.DownloadFormat;
import org.opennms.horizon.server.model.inventory.IpInterface;
import org.opennms.horizon.server.model.inventory.MonitoringLocation;
import org.opennms.horizon.server.model.inventory.Node;
import org.opennms.horizon.server.model.inventory.NodeCreate;
import org.opennms.horizon.server.model.inventory.NodeUpdate;
import org.opennms.horizon.server.model.inventory.TopNNode;
import org.opennms.horizon.server.model.inventory.TopNResponse;
import org.opennms.horizon.server.model.status.NodeStatus;
import org.opennms.horizon.server.service.grpc.InventoryClient;
import org.opennms.horizon.server.utils.ServerHeaderUtil;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@GraphQLApi
@Service
public class GrpcNodeService {
    private static final String ICMP_MONITOR_TYPE = "ICMP";

    private final InventoryClient client;
    private final NodeMapper mapper;
    private final IpInterfaceMapper ipInterfaceMapper;
    private final ServerHeaderUtil headerUtil;
    private final NodeStatusService nodeStatusService;

    @GraphQLQuery
    public Flux<Node> findAllNodes(@GraphQLEnvironment ResolutionEnvironment env) {
        return Flux.fromIterable(client.listNodes(headerUtil.getAuthHeader(env)).stream()
                .map(mapper::protoToNode)
                .toList());
    }

    @GraphQLQuery
    public Flux<Node> findAllNodesByMonitoredState(
            @GraphQLArgument(name = "monitoredState") String monitoredState,
            @GraphQLEnvironment ResolutionEnvironment env) {
        return Flux.fromIterable(
                client.listNodesByMonitoredState(monitoredState, headerUtil.getAuthHeader(env)).stream()
                        .map(mapper::protoToNode)
                        .toList());
    }

    @GraphQLQuery
    public Flux<Node> findAllNodesByNodeLabelSearch(
            @GraphQLArgument(name = "labelSearchTerm") String labelSearchTerm,
            @GraphQLEnvironment ResolutionEnvironment env) {
        return Flux.fromIterable(
                client.listNodesByNodeLabelSearch(labelSearchTerm, headerUtil.getAuthHeader(env)).stream()
                        .map(mapper::protoToNode)
                        .toList());
    }

    @GraphQLQuery
    public Flux<Node> findAllNodesByTags(
            @GraphQLArgument(name = "tags") List<String> tags, @GraphQLEnvironment ResolutionEnvironment env) {
        return Flux.fromIterable(client.listNodesByTags(tags, headerUtil.getAuthHeader(env)).stream()
                .map(mapper::protoToNode)
                .toList());
    }

    @GraphQLQuery
    public Mono<Node> findNodeById(
            @GraphQLArgument(name = "id") Long id, @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(mapper.protoToNode(client.getNodeById(id, headerUtil.getAuthHeader(env))));
    }

    @GraphQLMutation
    public Mono<Node> addNode(NodeCreate node, @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(mapper.protoToNode(
                client.createNewNode(mapper.nodeCreateToProto(node), headerUtil.getAuthHeader(env))));
    }

    @GraphQLMutation
    public Mono<Long> updateNode(NodeUpdate node, @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(client.updateNode(mapper.nodeUpdateToProto(node), headerUtil.getAuthHeader(env)));
    }

    @GraphQLQuery
    public CompletableFuture<MonitoringLocation> location(
            @GraphQLContext Node node, @GraphQLEnvironment ResolutionEnvironment env) {
        DataLoader<DataLoaderFactory.Key, MonitoringLocation> locationLoader =
                env.dataFetchingEnvironment.getDataLoader(DataLoaderFactory.DATA_LOADER_LOCATION);
        DataLoaderFactory.Key key =
                new DataLoaderFactory.Key(node.getMonitoringLocationId(), headerUtil.getAuthHeader(env));
        return locationLoader.load(key);
    }

    @GraphQLQuery
    public Mono<NodeStatus> getNodeStatus(
            @GraphQLArgument(name = "id") Long id, @GraphQLEnvironment ResolutionEnvironment env) {
        return nodeStatusService.getNodeStatus(id, ICMP_MONITOR_TYPE, env);
    }

    @GraphQLQuery(name = "allNodeStatus")
    public Flux<NodeStatus> getAllNodeStatus(@GraphQLEnvironment ResolutionEnvironment env) {
        return Flux.fromIterable(client.listNodes(headerUtil.getAuthHeader(env)))
                .flatMap(nodeDTO -> nodeStatusService.getNodeStatus(nodeDTO, env));
    }

    @GraphQLMutation
    public Mono<Boolean> deleteNode(
            @GraphQLArgument(name = "id") Long id, @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(client.deleteNode(id, headerUtil.getAuthHeader(env)));
    }

    @GraphQLMutation
    public Mono<Boolean> discoveryByNodeIds(List<Long> ids, @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(client.startScanByNodeIds(ids, headerUtil.getAuthHeader(env)));
    }

    @GraphQLQuery(name = "topNNode")
    public Flux<TopNNode> getTopNNode(
            @GraphQLEnvironment ResolutionEnvironment env,
            @GraphQLArgument(name = "timeRange") Integer timeRange,
            @GraphQLArgument(name = "timeRangeUnit") TimeRangeUnit timeRangeUnit,
            @GraphQLArgument(name = "pageSize") Integer pageSize,
            @GraphQLArgument(name = "page") Integer page,
            @GraphQLArgument(name = "sortBy") String sortBy,
            @GraphQLArgument(name = "sortAscending") boolean sortAscending) {
        return Flux.fromIterable(client.listNodes(headerUtil.getAuthHeader(env)))
                .flatMap(nodeDTO -> nodeStatusService.getTopNNode(nodeDTO, timeRange, timeRangeUnit, env))
                .sort(TopNNode.getComparator(sortBy, sortAscending))
                .skip((long) (page - 1) * pageSize)
                .take(pageSize);
    }

    @GraphQLQuery(name = "nodeCount")
    public Mono<Integer> getNodeCount(@GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(Math.toIntExact(client.getNodeCount(headerUtil.getAuthHeader(env))));
    }

    @GraphQLQuery(name = "downloadTopN")
    public Mono<TopNResponse> downloadTopN(
            @GraphQLEnvironment ResolutionEnvironment env,
            @GraphQLArgument(name = "timeRange") Integer timeRange,
            @GraphQLArgument(name = "timeRangeUnit") TimeRangeUnit timeRangeUnit,
            @GraphQLArgument(name = "sortBy") String sortBy,
            @GraphQLArgument(name = "sortAscending") boolean sortAscending,
            @GraphQLArgument(name = "downloadFormat") DownloadFormat downloadFormat) {

        return Flux.fromIterable(client.listNodes(headerUtil.getAuthHeader(env)))
                .flatMap(nodeDTO -> nodeStatusService.getTopNNode(nodeDTO, timeRange, timeRangeUnit, env))
                .sort(TopNNode.getComparator(sortBy, sortAscending))
                .collectList()
                .map(topNList -> {
                    try {
                        return generateDownloadableTopNResponse(topNList, downloadFormat);
                    } catch (IOException e) {
                        throw new IllegalArgumentException("Failed to download TopN List");
                    }
                });
    }

    private static TopNResponse generateDownloadableTopNResponse(
            List<TopNNode> topNNodes, DownloadFormat downloadFormat) throws IOException {
        if (downloadFormat == null) {
            downloadFormat = DownloadFormat.CSV;
        }
        if (downloadFormat.equals(DownloadFormat.CSV)) {
            StringBuilder csvData = new StringBuilder();
            var csvformat = CSVFormat.Builder.create()
                    .setHeader("Node Label", "Location", "Avg Response Time", "Reachability")
                    .build();

            CSVPrinter csvPrinter = new CSVPrinter(csvData, csvformat);
            for (TopNNode topNNode : topNNodes) {
                csvPrinter.printRecord(
                        topNNode.getNodeLabel(),
                        topNNode.getLocation(),
                        topNNode.getAvgResponseTime(),
                        topNNode.getReachability());
            }
            csvPrinter.flush();
            return new TopNResponse(csvData.toString().getBytes(StandardCharsets.UTF_8), downloadFormat);
        }
        throw new IllegalArgumentException("Invalid download format" + downloadFormat.value);
    }

    @GraphQLQuery(name = "listIpInterfacesByNodeSearch")
    public Flux<IpInterface> searchIpInterfacesByNodeAndSearchTerm(
            @GraphQLEnvironment ResolutionEnvironment env,
            @GraphQLArgument(name = "nodeId") Long nodeId,
            @GraphQLArgument(name = "searchTerm") String searchTerm) {

        return Flux.fromIterable(
                client.searchIpInterfacesByNodeAndSearchTerm(nodeId, searchTerm, headerUtil.getAuthHeader(env)).stream()
                        .map(ipInterfaceMapper::protoToIpInterface)
                        .toList());
    }
}
