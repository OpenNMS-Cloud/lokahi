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
public class GrpcIcmpActiveDiscoveryService {
    private final IcmpActiveDiscoveryMapper mapper;
    private final ServerHeaderUtil headerUtil;
    private final InventoryClient client;

    @GraphQLMutation
    public Mono<IcmpActiveDiscovery> createIcmpActiveDiscovery(IcmpActiveDiscoveryCreate request, @GraphQLEnvironment ResolutionEnvironment env) {
        IcmpActiveDiscoveryCreateDTO requestDto = mapper.mapRequest(request);
        return Mono.just(mapper.dtoToIcmpActiveDiscovery(client.createIcmpActiveDiscovery(requestDto, headerUtil.getAuthHeader(env))));
    }

    @GraphQLQuery
    public Flux<IcmpActiveDiscovery> listIcmpActiveDiscovery(@GraphQLEnvironment ResolutionEnvironment env) {
        return Flux.fromIterable(mapper.dtoListToIcmpActiveDiscoveryList(client.listIcmpDiscoveries(headerUtil.getAuthHeader(env))));
    }

    @GraphQLQuery
    public Mono<IcmpActiveDiscovery> getIcmpActiveDiscoveryById(Long id, @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(mapper.dtoToIcmpActiveDiscovery(client.getIcmpDiscoveryById(id, headerUtil.getAuthHeader(env))));
    }

    @GraphQLMutation
    public Mono<IcmpActiveDiscovery> upsertIcmpActiveDiscovery(IcmpActiveDiscoveryCreate request, @GraphQLEnvironment ResolutionEnvironment env) {
        IcmpActiveDiscoveryCreateDTO requestDto = mapper.mapRequest(request);
        return Mono.just(mapper.dtoToIcmpActiveDiscovery(client.upsertIcmpActiveDiscovery(requestDto, headerUtil.getAuthHeader(env))));
    }

    @GraphQLMutation
    public Mono<Boolean> deleteIcmpActiveDiscovery(Long id, @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(client.deleteIcmpActiveDiscovery(id, headerUtil.getAuthHeader(env)));
    }
}
