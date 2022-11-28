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

import java.util.stream.Collectors;

import org.opennms.horizon.server.mapper.MinionMapper;
import org.opennms.horizon.server.model.inventory.Minion;
import org.opennms.horizon.server.service.grpc.InventoryClient;
import org.opennms.horizon.server.utils.ServerHeaderUtil;
import org.springframework.stereotype.Service;

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@GraphQLApi
@Service
public class GrpcMinionService {
    private final InventoryClient client;
    private final MinionMapper mapper;
    private final ServerHeaderUtil headerUtil;

    @GraphQLQuery
    public Flux<Minion> findAllMinions(@GraphQLEnvironment ResolutionEnvironment env) {
        return Flux.fromIterable(client.listMonitoringSystems(headerUtil.getAuthHeader(env)).stream().map(mapper::protoToMinion).collect(Collectors.toList()));
    }

    @GraphQLQuery
    public Mono<Minion> findMinionById(@GraphQLArgument(name = "id") String id, @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(mapper.protoToMinion(client.getSystemBySystemId(id, headerUtil.getAuthHeader(env))));
    }
}
