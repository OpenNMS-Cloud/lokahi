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
import org.opennms.horizon.server.mapper.AddressMapper;
import org.opennms.horizon.server.model.inventory.Address;
import org.opennms.horizon.server.model.inventory.AddressCreate;
import org.opennms.horizon.server.model.inventory.AddressUpdate;
import org.opennms.horizon.server.service.grpc.InventoryClient;
import org.opennms.horizon.server.utils.ServerHeaderUtil;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@GraphQLApi
@Service
public class GrpcAddressService {
    private final InventoryClient client;
    private final AddressMapper mapper;
    private final ServerHeaderUtil headerUtil;

    @GraphQLQuery
    public Flux<Address> findAllAddresses(@GraphQLEnvironment ResolutionEnvironment env) {
        return Flux.fromIterable(client.listAddresses(headerUtil.getAuthHeader(env)).stream().map(mapper::protoToAddress).toList());
    }
    @GraphQLQuery
    public Mono<Address> findAddressById(@GraphQLArgument(name = "id") long id, @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(mapper.protoToAddress(client.getAddressById(id, headerUtil.getAuthHeader(env))));
    }

    @GraphQLMutation
    public Mono<Address> createAddress(@GraphQLArgument(name="address") AddressCreate address, @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(mapper.protoToAddress(client.createAddress(mapper.addressCreateToAddressCreateProto(address), headerUtil.getAuthHeader(env))));
    }

    @GraphQLMutation
    public Mono<Address> updateAddress(@GraphQLArgument(name="address") AddressUpdate address, @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(mapper.protoToAddress(client.updateAddress(mapper.addressUpdateToAddressUpdateProto(address), headerUtil.getAuthHeader(env))));
    }

    @GraphQLMutation
    public Mono<Boolean> deleteAddress(@GraphQLArgument(name="id") long id, @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(client.deleteAddress(id, headerUtil.getAuthHeader(env)));
    }
}
