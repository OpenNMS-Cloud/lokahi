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
package org.opennms.horizon.events.grpc.service;

import com.google.protobuf.Empty;
import com.google.protobuf.UInt64Value;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.Context;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.events.grpc.client.InventoryClient;
import org.opennms.horizon.events.grpc.config.TenantLookup;
import org.opennms.horizon.events.persistence.service.EventService;
import org.opennms.horizon.events.proto.Event;
import org.opennms.horizon.events.proto.EventLog;
import org.opennms.horizon.events.proto.EventServiceGrpc;
import org.opennms.horizon.events.proto.EventsSearchBy;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventGrpcService extends EventServiceGrpc.EventServiceImplBase {
    private final EventService eventService;
    private final InventoryClient inventoryClient;
    private final TenantLookup tenantLookup;

    @Override
    public void listEvents(Empty request, StreamObserver<EventLog> responseObserver) {
        String tenantId = tenantLookup.lookupTenantId(Context.current()).orElseThrow();

        List<Event> events = eventService.findEvents(tenantId);
        EventLog eventList =
                EventLog.newBuilder().setTenantId(tenantId).addAllEvents(events).build();

        responseObserver.onNext(eventList);
        responseObserver.onCompleted();
    }

    @Override
    public void getEventsByNodeId(UInt64Value nodeId, StreamObserver<EventLog> responseObserver) {
        String tenantId = tenantLookup.lookupTenantId(Context.current()).orElseThrow();

        try {
            inventoryClient.getNodeById(tenantId, nodeId.getValue());
        } catch (StatusRuntimeException e) {
            if (e.getStatus() != null) {
                responseObserver.onError(StatusProto.toStatusRuntimeException(createStatus(
                        e.getStatus().getCode().value(), e.getStatus().getDescription())));
            } else {
                responseObserver.onError(
                        StatusProto.toStatusRuntimeException(createStatus(Code.INTERNAL_VALUE, e.getMessage())));
            }
            return;
        }

        List<Event> events = eventService.findEventsByNodeId(tenantId, nodeId.getValue());
        EventLog eventList =
                EventLog.newBuilder().setTenantId(tenantId).addAllEvents(events).build();

        responseObserver.onNext(eventList);
        responseObserver.onCompleted();
    }

    private Status createStatus(int code, String msg) {
        return Status.newBuilder().setCode(code).setMessage(msg).build();
    }

    @Override
    public void searchEvents(EventsSearchBy request, StreamObserver<EventLog> responseObserver) {
        String tenantId = tenantLookup.lookupTenantId(Context.current()).orElseThrow();
        List<Event> events = eventService.searchEvents(tenantId, request);
        EventLog eventList =
                EventLog.newBuilder().setTenantId(tenantId).addAllEvents(events).build();
        responseObserver.onNext(eventList);
        responseObserver.onCompleted();
    }
}
