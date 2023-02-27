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

package org.opennms.horizon.server.service;

import org.opennms.horizon.inventory.discovery.DiscoveryConfigByLocationDTO;
import org.opennms.horizon.inventory.discovery.DiscoveryConfigRequest;
import org.opennms.horizon.server.mapper.DiscoveryConfigMapper;
import org.opennms.horizon.server.model.inventory.discovery.CreateDiscoveryConfigRequest;
import org.opennms.horizon.server.model.inventory.discovery.DiscoveryConfigByLocation;
import org.opennms.horizon.server.service.grpc.InventoryClient;
import org.opennms.horizon.server.utils.ServerHeaderUtil;
import org.springframework.stereotype.Service;

import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@GraphQLApi
@Service
public class GrpcDiscoveryService {
    private final DiscoveryConfigMapper mapper;
    private final ServerHeaderUtil headerUtil;
    private final InventoryClient client;

    @GraphQLMutation
    public Mono<DiscoveryConfigByLocation> createDiscoveryConfig(CreateDiscoveryConfigRequest request, @GraphQLEnvironment ResolutionEnvironment env) {
        DiscoveryConfigRequest requestDto = mapper.mapRequest(request);
        DiscoveryConfigByLocationDTO dto = client.createDiscoveryConfig(requestDto, headerUtil.getAuthHeader(env));
        return Mono.just(new DiscoveryConfigByLocation(dto.getLocation(), mapper.configDtoListToConfig(dto.getDiscoveryConfigList())));
    }

    @GraphQLQuery
    public Mono<DiscoveryConfigByLocation> listDiscoveryConfig(@GraphQLEnvironment ResolutionEnvironment env) {
        DiscoveryConfigByLocationDTO dto = client.listDiscoveryConfig(headerUtil.getAuthHeader(env));
        return Mono.just(new DiscoveryConfigByLocation(dto.getLocation(), mapper.configDtoListToConfig(dto.getDiscoveryConfigList())));
    }

    @GraphQLQuery
    public Mono<DiscoveryConfigByLocation> getDiscoveryConfigByName(String name, @GraphQLEnvironment ResolutionEnvironment env) {
        DiscoveryConfigByLocationDTO dto = client.getDiscoveryConfigByName(name, headerUtil.getAuthHeader(env));
        return Mono.just(new DiscoveryConfigByLocation(dto.getLocation(), mapper.configDtoListToConfig(dto.getDiscoveryConfigList())));
    }

}
