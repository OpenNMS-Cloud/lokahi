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

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.execution.ResolutionEnvironment;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.server.mapper.MonitoredServiceStatusMapper;
import org.opennms.horizon.server.model.inventory.MonitoredServiceStatus;
import org.opennms.horizon.server.model.inventory.MonitoredServiceStatusRequest;
import org.opennms.horizon.server.service.grpc.InventoryClient;
import org.opennms.horizon.server.utils.ServerHeaderUtil;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class MonitorStatusGrpcService {

    private final ServerHeaderUtil headerUtil;
    private final MonitoredServiceStatusMapper mapper;
    private final InventoryClient client;

    @GraphQLQuery
    public Mono<MonitoredServiceStatus> getMonitorStatus(@GraphQLArgument(name = "request") MonitoredServiceStatusRequest request,
                                                         @GraphQLEnvironment ResolutionEnvironment env) {
        var monitorStatusProto = client.getMonitorStatus(request, headerUtil.getAuthHeader(env));
        var monitoredServiceStatus = mapper.protoToModel(monitorStatusProto);
        return Mono.just(monitoredServiceStatus);
    }
}
