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

import org.opennms.horizon.server.service.gateway.PlatformGateway;
import org.opennms.horizon.shared.dto.minion.MinionCollectionDTO;
import org.opennms.horizon.shared.dto.minion.MinionDTO;
import org.springframework.stereotype.Service;

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
import reactor.core.publisher.Mono;


@GraphQLApi
@Service
public class MinionService {
    private final PlatformGateway gateway;

    public MinionService(PlatformGateway gateway) {
        this.gateway = gateway;
    }

    @GraphQLQuery
    public Mono<MinionCollectionDTO> listMinions(@GraphQLEnvironment ResolutionEnvironment env) {//TODO: add search TDO object with pagination as cache key
        return gateway.get(PlatformGateway.URL_PATH_MINIONS, gateway.getAuthHeader(env), MinionCollectionDTO.class);
    }

    @GraphQLQuery
    public Mono<MinionDTO> getMinionById(@GraphQLArgument(name = "id") String id, @GraphQLEnvironment ResolutionEnvironment env) {
        return gateway.get(String.format(PlatformGateway.URL_PATH_MINIONS_ID, id), gateway.getAuthHeader(env), MinionDTO.class);
    }
}
