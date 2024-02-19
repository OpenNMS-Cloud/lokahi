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
package org.opennms.horizon.server.service.discovery;

import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.inventory.dto.AzureActiveDiscoveryCreateDTO;
import org.opennms.horizon.inventory.dto.AzureActiveDiscoveryDTO;
import org.opennms.horizon.server.mapper.discovery.AzureActiveDiscoveryMapper;
import org.opennms.horizon.server.model.inventory.discovery.active.AzureActiveDiscovery;
import org.opennms.horizon.server.model.inventory.discovery.active.AzureActiveDiscoveryCreate;
import org.opennms.horizon.server.service.grpc.InventoryClient;
import org.opennms.horizon.server.utils.ServerHeaderUtil;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@GraphQLApi
@Service
public class GrpcAzureActiveDiscoveryService {
    private final InventoryClient client;
    private final AzureActiveDiscoveryMapper mapper;
    private final ServerHeaderUtil headerUtil;

    @GraphQLMutation
    public Mono<AzureActiveDiscovery> createAzureActiveDiscovery(
            AzureActiveDiscoveryCreate discovery, @GraphQLEnvironment ResolutionEnvironment env) {
        AzureActiveDiscoveryCreateDTO createDto = mapper.azureDiscoveryCreateToProto(discovery);
        AzureActiveDiscoveryDTO discoveryDto =
                client.createAzureActiveDiscovery(createDto, headerUtil.getAuthHeader(env));
        return Mono.just(mapper.dtoToAzureActiveDiscovery(discoveryDto));
    }
}
