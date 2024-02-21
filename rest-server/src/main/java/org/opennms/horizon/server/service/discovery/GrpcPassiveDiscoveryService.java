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
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.inventory.dto.PassiveDiscoveryDTO;
import org.opennms.horizon.inventory.dto.PassiveDiscoveryToggleDTO;
import org.opennms.horizon.inventory.dto.PassiveDiscoveryUpsertDTO;
import org.opennms.horizon.server.mapper.discovery.PassiveDiscoveryMapper;
import org.opennms.horizon.server.model.inventory.discovery.passive.PassiveDiscovery;
import org.opennms.horizon.server.model.inventory.discovery.passive.PassiveDiscoveryToggle;
import org.opennms.horizon.server.model.inventory.discovery.passive.PassiveDiscoveryUpsert;
import org.opennms.horizon.server.service.grpc.InventoryClient;
import org.opennms.horizon.server.utils.ServerHeaderUtil;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@GraphQLApi
@Service
public class GrpcPassiveDiscoveryService {
    private final InventoryClient client;
    private final PassiveDiscoveryMapper mapper;
    private final ServerHeaderUtil headerUtil;

    @GraphQLMutation
    public Mono<PassiveDiscovery> upsertPassiveDiscovery(
            PassiveDiscoveryUpsert discovery, @GraphQLEnvironment ResolutionEnvironment env) {
        String authHeader = headerUtil.getAuthHeader(env);
        PassiveDiscoveryUpsertDTO upsertDto = mapper.discoveryUpsertToProtoCustom(discovery);
        PassiveDiscoveryDTO dto = client.upsertPassiveDiscovery(upsertDto, authHeader);
        return Mono.just(mapper.protoToDiscovery(dto));
    }

    @GraphQLMutation
    public Mono<PassiveDiscoveryToggle> togglePassiveDiscovery(
            PassiveDiscoveryToggle toggle, @GraphQLEnvironment ResolutionEnvironment env) {
        String authHeader = headerUtil.getAuthHeader(env);
        PassiveDiscoveryToggleDTO toggleDto = mapper.discoveryToggleToProto(toggle);
        PassiveDiscoveryDTO dto = client.createPassiveDiscoveryToggle(toggleDto, authHeader);
        return Mono.just(mapper.protoToDiscoveryToggle(dto));
    }

    @GraphQLQuery
    public Mono<List<PassiveDiscovery>> getPassiveDiscoveries(@GraphQLEnvironment ResolutionEnvironment env) {
        List<PassiveDiscoveryDTO> list =
                client.listPassiveDiscoveries(headerUtil.getAuthHeader(env)).getDiscoveriesList();
        return Mono.just(list.stream().map(mapper::protoToDiscovery).toList());
    }

    @GraphQLMutation
    public Mono<Boolean> deletePassiveDiscovery(Long id, @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(client.deletePassiveDiscovery(id, headerUtil.getAuthHeader(env)));
    }
}
