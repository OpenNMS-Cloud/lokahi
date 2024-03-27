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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.opennms.horizon.server.mapper.EventMapper;
import org.opennms.horizon.server.model.events.Event;
import org.opennms.horizon.server.model.inventory.DownloadFormat;
import org.opennms.horizon.server.model.inventory.SearchEventsResponse;
import org.opennms.horizon.server.service.grpc.EventsClient;
import org.opennms.horizon.server.utils.DateTimeUtil;
import org.opennms.horizon.server.utils.ServerHeaderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@GraphQLApi
@Service
public class GraphQLEventService {
    private final EventsClient client;
    private final EventMapper mapper;
    private final ServerHeaderUtil headerUtil;
    private static final Logger LOG = LoggerFactory.getLogger(GraphQLEventService.class);

    @GraphQLQuery
    public Flux<Event> findAllEvents(@GraphQLEnvironment ResolutionEnvironment env) {
        return Flux.fromIterable(client.listEvents(headerUtil.getAuthHeader(env)).stream()
                .map(mapper::protoToEvent)
                .toList());
    }

    @GraphQLQuery
    public Flux<Event> findEventsByNodeId(
            @GraphQLArgument(name = "id") Long id, @GraphQLEnvironment ResolutionEnvironment env) {
        return Flux.fromIterable(client.getEventsByNodeId(id, headerUtil.getAuthHeader(env)).stream()
                .map(mapper::protoToEvent)
                .toList());
    }

    @GraphQLQuery(name = "searchEvents")
    public Flux<Event> searchEvents(
            @GraphQLArgument(name = "nodeId") Long nodeId,
            @GraphQLArgument(name = "searchTerm") String searchTerm,
            @GraphQLArgument(name = "pageSize") Integer pageSize,
            @GraphQLArgument(name = "page") int page,
            @GraphQLArgument(name = "sortBy") String sortBy,
            @GraphQLArgument(name = "sortAscending") boolean sortAscending,
            @GraphQLEnvironment ResolutionEnvironment env) {
        return Flux.fromIterable(
                client
                        .searchEvents(
                                nodeId,
                                searchTerm,
                                pageSize,
                                page,
                                sortBy,
                                sortAscending,
                                headerUtil.getAuthHeader(env))
                        .stream()
                        .map(mapper::protoToEvent)
                        .toList());
    }

    @GraphQLQuery(name = "downloadEventsByNodeId")
    public Mono<SearchEventsResponse> downloadEventsByNodeId(
            @GraphQLEnvironment ResolutionEnvironment env,
            @GraphQLArgument(name = "nodeId") Long nodeId,
            @GraphQLArgument(name = "searchTerm") String searchTerm,
            @GraphQLArgument(name = "pageSize") Integer pageSize,
            @GraphQLArgument(name = "page") int page,
            @GraphQLArgument(name = "sortBy") String sortBy,
            @GraphQLArgument(name = "sortAscending") boolean sortAscending,
            @GraphQLArgument(name = "downloadFormat") DownloadFormat downloadFormat) {

        List<Event> events = client
                .searchEvents(nodeId, searchTerm, pageSize, page, sortBy, sortAscending, headerUtil.getAuthHeader(env))
                .stream()
                .map(mapper::protoToEvent)
                .toList();

        try {
            return Mono.just(generateDownloadableEventsResponse(events, downloadFormat));
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to download search events.");
        }
    }

    private static SearchEventsResponse generateDownloadableEventsResponse(
            List<Event> events, DownloadFormat downloadFormat) throws IOException {
        if (downloadFormat == null) {
            downloadFormat = DownloadFormat.CSV;
        }
        if (downloadFormat.equals(DownloadFormat.CSV)) {
            StringBuilder csvData = new StringBuilder();
            var csvformat = CSVFormat.Builder.create()
                    .setHeader("Time", "UEI", "Description")
                    .build();

            try (CSVPrinter csvPrinter = new CSVPrinter(csvData, csvformat)) {
                for (Event event : events) {
                    csvPrinter.printRecord(
                            DateTimeUtil.convertAndFormatLongDate(
                                    event.getProducedTime(), DateTimeUtil.D_MM_YYYY_HH_MM_SS_SSS),
                            event.getUei(),
                            event.getDescription());
                }
                csvPrinter.flush();
            } catch (Exception e) {
                LOG.error("Exception while printing records", e);
            }
            return new SearchEventsResponse(csvData.toString().getBytes(StandardCharsets.UTF_8), downloadFormat);
        }
        throw new IllegalArgumentException("Invalid download format" + downloadFormat.value);
    }
}
