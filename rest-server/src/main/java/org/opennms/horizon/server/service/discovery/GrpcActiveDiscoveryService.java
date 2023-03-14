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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.inventory.dto.ActiveDiscoveryDTO;
import org.opennms.horizon.server.mapper.AzureActiveDiscoveryMapper;
import org.opennms.horizon.server.mapper.IcmpActiveDiscoveryMapper;
import org.opennms.horizon.server.model.inventory.discovery.active.ActiveDiscovery;
import org.opennms.horizon.server.service.grpc.InventoryClient;
import org.opennms.horizon.server.utils.ServerHeaderUtil;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@RequiredArgsConstructor
@GraphQLApi
@Service
public class GrpcActiveDiscoveryService {
    private final InventoryClient client;
    private final IcmpActiveDiscoveryMapper icmpMapper;
    private final AzureActiveDiscoveryMapper azureMapper;
    private final ServerHeaderUtil headerUtil;

    @GraphQLQuery
    public Flux<ActiveDiscovery> listActiveDiscovery(@GraphQLEnvironment ResolutionEnvironment env) {
        List<ActiveDiscoveryDTO> discoveriesDto = client.listActiveDiscoveries(headerUtil.getAuthHeader(env));
        return Flux.fromIterable(discoveriesDto.stream().map(this::getActiveDiscovery).toList());
    }

    private ActiveDiscovery getActiveDiscovery(ActiveDiscoveryDTO activeDiscoveryDTO) {
        ObjectMapper objectMapper = new ObjectMapper();
        ActiveDiscovery discovery = new ActiveDiscovery();
        if (activeDiscoveryDTO.hasAzure()) {
            discovery.setDetails(objectMapper.valueToTree(azureMapper.dtoToAzureActiveDiscovery(activeDiscoveryDTO.getAzure())));
            discovery.setDiscoveryType("AZURE");
        } else if (activeDiscoveryDTO.hasIcmp()) {
            discovery.setDetails(objectMapper.valueToTree(icmpMapper.dtoToIcmpActiveDiscovery(activeDiscoveryDTO.getIcmp())));
            discovery.setDiscoveryType("ICMP");
        } else {
            throw new RuntimeException("Invalid Active Discovery type returned");
        }
        return discovery;
    }
}
