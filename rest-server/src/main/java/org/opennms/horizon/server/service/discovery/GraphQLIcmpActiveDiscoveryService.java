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
package org.opennms.horizon.server.service.discovery;

import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.inventory.discovery.IcmpActiveDiscoveryCreateDTO;
import org.opennms.horizon.server.mapper.discovery.IcmpActiveDiscoveryMapper;
import org.opennms.horizon.server.model.inventory.discovery.active.IcmpActiveDiscovery;
import org.opennms.horizon.server.model.inventory.discovery.active.IcmpActiveDiscoveryCreate;
import org.opennms.horizon.server.service.grpc.InventoryClient;
import org.opennms.horizon.server.utils.ServerHeaderUtil;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@GraphQLApi
@Service
public class GraphQLIcmpActiveDiscoveryService {
    private final IcmpActiveDiscoveryMapper mapper;
    private final ServerHeaderUtil headerUtil;
    private final InventoryClient client;

    @GraphQLMutation
    public Mono<IcmpActiveDiscovery> createIcmpActiveDiscovery(
            IcmpActiveDiscoveryCreate request, @GraphQLEnvironment ResolutionEnvironment env) {
        IcmpActiveDiscoveryCreateDTO requestDto = mapper.mapRequest(request);
        return Mono.just(mapper.dtoToIcmpActiveDiscovery(
                client.createIcmpActiveDiscovery(requestDto, headerUtil.getAuthHeader(env))));
    }

    @GraphQLQuery
    public Flux<IcmpActiveDiscovery> listIcmpActiveDiscovery(@GraphQLEnvironment ResolutionEnvironment env) {
        return Flux.fromIterable(
                mapper.dtoListToIcmpActiveDiscoveryList(client.listIcmpDiscoveries(headerUtil.getAuthHeader(env))));
    }

    @GraphQLQuery
    public Mono<IcmpActiveDiscovery> getIcmpActiveDiscoveryById(
            Long id, @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(
                mapper.dtoToIcmpActiveDiscovery(client.getIcmpDiscoveryById(id, headerUtil.getAuthHeader(env))));
    }

    @GraphQLMutation
    public Mono<IcmpActiveDiscovery> upsertIcmpActiveDiscovery(
            IcmpActiveDiscoveryCreate request, @GraphQLEnvironment ResolutionEnvironment env) {
        IcmpActiveDiscoveryCreateDTO requestDto = mapper.mapRequest(request);
        return Mono.just(mapper.dtoToIcmpActiveDiscovery(
                client.upsertIcmpActiveDiscovery(requestDto, headerUtil.getAuthHeader(env))));
    }

    @GraphQLMutation
    public Mono<Boolean> deleteIcmpActiveDiscovery(Long id, @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(client.deleteIcmpActiveDiscovery(id, headerUtil.getAuthHeader(env)));
    }
}
