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
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.server.mapper.MonitoringLocationMapper;
import org.opennms.horizon.server.model.inventory.MonitoringLocation;
import org.opennms.horizon.server.model.inventory.MonitoringLocationCreate;
import org.opennms.horizon.server.model.inventory.MonitoringLocationUpdate;
import org.opennms.horizon.server.service.grpc.InventoryClient;
import org.opennms.horizon.server.service.grpc.MinionCertificateManagerClient;
import org.opennms.horizon.server.utils.ServerHeaderUtil;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@GraphQLApi
@Service
public class GraphQLLocationService {
    private final InventoryClient inventoryClient;
    private final MinionCertificateManagerClient certificateManagerClient;
    private final MonitoringLocationMapper mapper;
    private final ServerHeaderUtil headerUtil;

    @GraphQLQuery
    public Flux<MonitoringLocation> findAllLocations(@GraphQLEnvironment ResolutionEnvironment env) {
        return Flux.fromIterable(inventoryClient.listLocations(headerUtil.getAuthHeader(env)).stream()
                .map(mapper::protoToLocation)
                .toList());
    }

    @GraphQLQuery
    public Mono<MonitoringLocation> findLocationById(
            @GraphQLArgument(name = "id") long id, @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(mapper.protoToLocation(inventoryClient.getLocationById(id, headerUtil.getAuthHeader(env))));
    }

    @GraphQLQuery
    public Mono<MonitoringLocation> getLocationByName(
            @GraphQLArgument(name = "locationName") String locationName,
            @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(
                mapper.protoToLocation(inventoryClient.getLocationByName(locationName, headerUtil.getAuthHeader(env))));
    }

    @GraphQLQuery
    public Flux<MonitoringLocation> searchLocation(
            @GraphQLArgument(name = "searchTerm") String searchTerm, @GraphQLEnvironment ResolutionEnvironment env) {
        return Flux.fromIterable(inventoryClient.searchLocations(searchTerm, headerUtil.getAuthHeader(env)).stream()
                .map(mapper::protoToLocation)
                .toList());
    }

    @GraphQLMutation
    public Mono<MonitoringLocation> createLocation(
            @GraphQLArgument(name = "location") MonitoringLocationCreate location,
            @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(mapper.protoToLocation(inventoryClient.createLocation(
                mapper.locationCreateToLocationCreateProto(location), headerUtil.getAuthHeader(env))));
    }

    @GraphQLMutation
    public Mono<MonitoringLocation> updateLocation(
            @GraphQLArgument(name = "location") MonitoringLocationUpdate monitoringLocation,
            @GraphQLEnvironment ResolutionEnvironment env) {
        return Mono.just(mapper.protoToLocation(inventoryClient.updateLocation(
                mapper.locationUpdateToLocationProto(monitoringLocation), headerUtil.getAuthHeader(env))));
    }

    @GraphQLMutation
    public Mono<Boolean> deleteLocation(
            @GraphQLArgument(name = "id") long id, @GraphQLEnvironment ResolutionEnvironment env) {
        var accessToken = headerUtil.getAuthHeader(env);
        var tenantId = headerUtil.extractTenant(env);

        var status = inventoryClient.deleteLocation(id, accessToken);

        // we may want to revoke even delete location is fail. E.g. location is
        // already deleted before or it is partially deleted. It will not be
        // able to use anyway.
        certificateManagerClient.revokeCertificate(tenantId, id, accessToken);
        return Mono.just(status);
    }
}
