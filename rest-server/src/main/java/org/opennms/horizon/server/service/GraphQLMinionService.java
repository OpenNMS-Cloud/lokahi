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
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.dataloader.DataLoader;
import org.opennms.horizon.server.config.DataLoaderFactory;
import org.opennms.horizon.server.mapper.MinionMapper;
import org.opennms.horizon.server.model.inventory.Minion;
import org.opennms.horizon.server.model.inventory.MonitoringLocation;
import org.opennms.horizon.server.service.grpc.InventoryClient;
import org.opennms.horizon.server.utils.ServerHeaderUtil;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@GraphQLApi
@Service
public class GraphQLMinionService {
    private final InventoryClient client;
    private final MinionMapper mapper;
    private final ServerHeaderUtil headerUtil;

    @GraphQLQuery
    public Flux<Minion> findAllMinions(@GraphQLEnvironment ResolutionEnvironment env) {
        return Flux.fromIterable(client.listMonitoringSystems(headerUtil.getAuthHeader(env)).stream()
                .map(mapper::protoToMinion)
                .toList());
    }

    @GraphQLQuery
    public Flux<Minion> findMinionsByLocationId(
            @GraphQLArgument(name = "locationId") long locationId, @GraphQLEnvironment ResolutionEnvironment env) {
        return Flux.fromIterable(
                client.getMonitoringSystemsByLocationId(locationId, headerUtil.getAuthHeader(env)).stream()
                        .map(mapper::protoToMinion)
                        .toList());
    }

    @GraphQLQuery
    public Mono<Minion> findMinionById(
            @GraphQLArgument(name = "id") long id, @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(mapper.protoToMinion(client.getSystemBySystemId(id, headerUtil.getAuthHeader(env))));
    }

    @GraphQLQuery
    public CompletableFuture<MonitoringLocation> location(
            @GraphQLContext Minion minion, @GraphQLEnvironment ResolutionEnvironment env) {
        DataLoader<DataLoaderFactory.Key, MonitoringLocation> locationDataLoader =
                env.dataFetchingEnvironment.getDataLoader(DataLoaderFactory.DATA_LOADER_LOCATION);
        DataLoaderFactory.Key key = new DataLoaderFactory.Key(minion.getLocationId(), headerUtil.getAuthHeader(env));
        return locationDataLoader.load(key);
    }

    @GraphQLMutation
    public Mono<Boolean> deleteMinion(
            @GraphQLArgument(name = "id") long id, @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(client.deleteMonitoringSystem(id, headerUtil.getAuthHeader(env)));
    }
}
