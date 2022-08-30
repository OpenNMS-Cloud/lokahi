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
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
import org.opennms.horizon.shared.dto.minion.MinionCollectionDTO;
import org.opennms.horizon.shared.dto.minion.MinionDTO;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;


@GraphQLApi
@Service
public class MinionService {
    private static String MINION_COLLECTION_CACHE= "minion-collections";
    private final PlatformGateway gateway;
    private final CacheManager cacheManager;

    public MinionService(PlatformGateway gateway, CacheManager cacheManager) {
        this.gateway = gateway;
        this.cacheManager = cacheManager;
    }

    @GraphQLQuery
    public MinionCollectionDTO listMinions(@GraphQLEnvironment ResolutionEnvironment env) {//TODO: add search TDO object with pagination as cache key
        MinionCollectionDTO minionCollectionDTO = gateway.get(String.format(PlatformGateway.URL_PATH_MINIONS), gateway.getAuthHeader(env), MinionCollectionDTO.class).getBody();
        updateCache(minionCollectionDTO);
        return minionCollectionDTO;
    }

    @GraphQLQuery
    @Cacheable(value = "minions", key = "#id")
    public MinionDTO getMinionById(@GraphQLArgument(name = "id") String id, @GraphQLEnvironment ResolutionEnvironment env) {
        return gateway.get(String.format(PlatformGateway.URL_PATH_MINIONS_ID, id), gateway.getAuthHeader(env), MinionDTO.class).getBody();
    }
    private void updateCache(MinionCollectionDTO collectionDTO) {
        Cache minionsCache = cacheManager.getCache("minions");
        collectionDTO.getMinions().forEach(m->minionsCache.put(m.getId(), m));
    }
}
