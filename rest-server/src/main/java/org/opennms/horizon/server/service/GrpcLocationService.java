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
import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.inventory.dto.MonitoringLocationDTO;
import org.opennms.horizon.server.mapper.LocationMapper;
import org.opennms.horizon.server.model.inventory.Location;
import org.opennms.horizon.server.service.grpc.InventoryClient;
import org.opennms.horizon.server.utils.ServerHeaderUtil;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@GraphQLApi
@Service
public class GrpcLocationService {
    private final InventoryClient client;
    private final LocationMapper mapper;
    private final ServerHeaderUtil headerUtil;

    @GraphQLQuery
    public Flux<Location> findAllLocations(@GraphQLEnvironment ResolutionEnvironment env) {
        return Flux.fromIterable(client.listLocations(headerUtil.getAuthHeader(env)).stream().map(mapper::protoToLocation).toList());
    }
    @GraphQLQuery
    public Mono<Location> findLocationById(@GraphQLArgument(name = "id") long id, @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(mapper.protoToLocation(client.getLocationById(id, headerUtil.getAuthHeader(env))));
    }

    @GraphQLQuery
    public Flux<Location> searchLocation(@GraphQLArgument(name="searchTerm") String searchTerm, @GraphQLEnvironment ResolutionEnvironment env) {
        return Flux.fromIterable(client.searchLocations(searchTerm, headerUtil.getAuthHeader(env))
            .stream().map(mapper::protoToLocation).toList());
    }

    @GraphQLMutation
    public Mono<Location> createLocation(@GraphQLArgument(name="location") String location, @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(mapper.protoToLocation(client.createLocation(MonitoringLocationDTO.newBuilder().setLocation(location).build(), headerUtil.getAuthHeader(env))));
    }

    @GraphQLMutation
    public Mono<Location> updateLocation(@GraphQLArgument(name = "id") Long id, @GraphQLArgument(name="location") String location, @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(mapper.protoToLocation(client.updateLocation(MonitoringLocationDTO.newBuilder().setId(id).setLocation(location).build(), headerUtil.getAuthHeader(env))));
    }

    @GraphQLMutation
    public Mono<Boolean> deleteLocation(@GraphQLArgument(name="id") long id, @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(client.deleteLocation(id, headerUtil.getAuthHeader(env)));
    }
}
