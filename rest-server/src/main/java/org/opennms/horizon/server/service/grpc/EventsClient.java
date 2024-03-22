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
package org.opennms.horizon.server.service.grpc;

import com.google.protobuf.Empty;
import com.google.protobuf.UInt64Value;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.events.proto.Event;
import org.opennms.horizon.events.proto.EventServiceGrpc;
import org.opennms.horizon.events.proto.EventsSearchBy;
import org.opennms.horizon.shared.constants.GrpcConstants;

@RequiredArgsConstructor
public class EventsClient {
    private final ManagedChannel channel;
    private final long deadline;

    private EventServiceGrpc.EventServiceBlockingStub eventsStub;

    protected void initialStubs() {
        eventsStub = EventServiceGrpc.newBlockingStub(channel);
    }

    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
        }
    }

    public List<Event> listEvents(String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return eventsStub
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
                .withDeadlineAfter(deadline, TimeUnit.MILLISECONDS)
                .listEvents(Empty.newBuilder().build())
                .getEventsList();
    }

    public List<Event> getEventsByNodeId(long nodeId, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return eventsStub
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
                .withDeadlineAfter(deadline, TimeUnit.MILLISECONDS)
                .getEventsByNodeId(UInt64Value.of(nodeId))
                .getEventsList();
    }

    public List<Event> searchEvents(
            Long nodeId,
            String searchTerm,
            int pageSize,
            int page,
            String sortBy,
            boolean sortAscending,
            String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);

        EventsSearchBy searchEventsRequest = EventsSearchBy.newBuilder()
                .setNodeId(nodeId)
                .setSearchTerm(searchTerm)
                .setPageSize(pageSize)
                .setPage(page)
                .setSortBy(sortBy)
                .setSortAscending(sortAscending)
                .build();
        return eventsStub
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
                .withDeadlineAfter(deadline, TimeUnit.MILLISECONDS)
                .searchEvents(searchEventsRequest)
                .getEventsList();
    }
}
