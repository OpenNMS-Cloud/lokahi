/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
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
    public Mono<MonitoredServiceStatus> getMonitorStatus(
            @GraphQLArgument(name = "request") MonitoredServiceStatusRequest request,
            @GraphQLEnvironment ResolutionEnvironment env) {
        var monitorStatusProto = client.getMonitorStatus(request, headerUtil.getAuthHeader(env));
        var monitoredServiceStatus = mapper.protoToModel(monitorStatusProto);
        return Mono.just(monitoredServiceStatus);
    }
}
