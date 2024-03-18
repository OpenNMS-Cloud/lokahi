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
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.server.mapper.EventMapper;
import org.opennms.horizon.server.model.events.Event;
import org.opennms.horizon.server.service.grpc.EventsClient;
import org.opennms.horizon.server.utils.ServerHeaderUtil;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
@GraphQLApi
@Service
public class GrpcEventService {
    private final EventsClient client;
    private final EventMapper mapper;
    private final ServerHeaderUtil headerUtil;

    @GraphQLQuery
    public Flux<Event> findAllEvents(@GraphQLEnvironment ResolutionEnvironment env) {
        return Flux.fromIterable(client.listEvents(headerUtil.getAuthHeader(env)).stream()
                .map(mapper::protoToEvent)
                .toList());
    }

    @GraphQLQuery
    public Flux<Event> findEventsByNodeId(
            @GraphQLArgument(name = "id") Long id,
            @GraphQLArgument(name = "pageSize") Integer pageSize,
            @GraphQLArgument(name = "page") int page,
            @GraphQLArgument(name = "sortBy") String sortBy,
            @GraphQLArgument(name = "sortAscending") boolean sortAscending, @GraphQLEnvironment ResolutionEnvironment env) {
        return Flux.fromIterable(client.getEventsByNodeId(id,pageSize,page,sortBy,sortAscending, headerUtil.getAuthHeader(env)).stream()
                .map(mapper::protoToEvent)
                .toList());
    }

    @GraphQLQuery(name = "searchEvents")
    public Flux<Event> searchEvents(
            @GraphQLArgument(name = "nodeId") Long nodeId,
            @GraphQLArgument(name = "searchTerm") String searchTerm,
            @GraphQLEnvironment ResolutionEnvironment env) {
        return Flux.fromIterable(client.searchEvents(nodeId, searchTerm, headerUtil.getAuthHeader(env)).stream()
                .map(mapper::protoToEvent)
                .toList());
    }
}
