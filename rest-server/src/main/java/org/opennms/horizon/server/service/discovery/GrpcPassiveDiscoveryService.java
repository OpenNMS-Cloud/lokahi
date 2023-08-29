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

package org.opennms.horizon.server.service.discovery;

import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
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

import java.util.List;

@RequiredArgsConstructor
@GraphQLApi
@Service
public class GrpcPassiveDiscoveryService {
    private final InventoryClient client;
    private final PassiveDiscoveryMapper mapper;
    private final ServerHeaderUtil headerUtil;

    @GraphQLMutation
    public Mono<PassiveDiscovery> upsertPassiveDiscovery(PassiveDiscoveryUpsert discovery,
                                                         @GraphQLEnvironment ResolutionEnvironment env) {
        String authHeader = headerUtil.getAuthHeader(env);
        PassiveDiscoveryUpsertDTO upsertDto = mapper.discoveryUpsertToProtoCustom(discovery);
        PassiveDiscoveryDTO dto = client.upsertPassiveDiscovery(upsertDto, authHeader);
        return Mono.just(mapper.protoToDiscovery(dto));
    }

    @GraphQLMutation
    public Mono<PassiveDiscoveryToggle> togglePassiveDiscovery(PassiveDiscoveryToggle toggle,
                                                               @GraphQLEnvironment ResolutionEnvironment env) {
        String authHeader = headerUtil.getAuthHeader(env);
        PassiveDiscoveryToggleDTO toggleDto = mapper.discoveryToggleToProto(toggle);
        PassiveDiscoveryDTO dto = client.createPassiveDiscoveryToggle(toggleDto, authHeader);
        return Mono.just(mapper.protoToDiscoveryToggle(dto));
    }

    @GraphQLQuery
    public Mono<List<PassiveDiscovery>> getPassiveDiscoveries(@GraphQLEnvironment ResolutionEnvironment env) {
        List<PassiveDiscoveryDTO> list = client.listPassiveDiscoveries(headerUtil.getAuthHeader(env)).getDiscoveriesList();
        return Mono.just(list.stream().map(mapper::protoToDiscovery).toList());
    }

    @GraphQLMutation
    public Mono<Boolean> deletePassiveDiscovery(Long id, @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(client.deletePassiveDiscovery(id, headerUtil.getAuthHeader(env)));
    }
}
