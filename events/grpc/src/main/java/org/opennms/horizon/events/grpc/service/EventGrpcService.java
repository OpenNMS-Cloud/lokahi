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

package org.opennms.horizon.events.grpc.service;

import com.google.protobuf.Empty;
import com.google.protobuf.Int64Value;
import io.grpc.Context;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.events.grpc.config.TenantLookup;
import org.opennms.horizon.events.persistence.service.EventService;
import org.opennms.horizon.events.proto.EventDTO;
import org.opennms.horizon.events.proto.EventList;
import org.opennms.horizon.events.proto.EventServiceGrpc;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class EventGrpcService extends EventServiceGrpc.EventServiceImplBase {
    private final EventService eventService;
    private final TenantLookup tenantLookup;

    @Override
    public void listEvents(Empty request, StreamObserver<EventList> responseObserver) {
        Optional<String> tenantId = tenantLookup.lookupTenantId(Context.current());

        List<EventDTO> events = eventService.findEvents(tenantId.orElseThrow());
        EventList eventList = EventList.newBuilder()
            .addAllEvents(events).build();

        responseObserver.onNext(eventList);
        responseObserver.onCompleted();
    }

    @Override
    public void getEventsByNodeId(Int64Value nodeId, StreamObserver<EventList> responseObserver) {
        Optional<String> tenantId = tenantLookup.lookupTenantId(Context.current());

        List<EventDTO> events = eventService
            .findEventsByNodeId(tenantId.orElseThrow(), nodeId.getValue());
        EventList eventList = EventList.newBuilder()
            .addAllEvents(events).build();

        responseObserver.onNext(eventList);
        responseObserver.onCompleted();
    }
}
