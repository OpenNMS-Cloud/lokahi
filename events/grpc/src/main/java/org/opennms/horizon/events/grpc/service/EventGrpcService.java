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

package org.opennms.horizon.events.grpc.service;

import com.google.protobuf.Empty;
import com.google.protobuf.UInt64Value;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.Context;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.events.grpc.client.InventoryClient;
import org.opennms.horizon.events.grpc.config.TenantLookup;
import org.opennms.horizon.events.persistence.service.EventService;
import org.opennms.horizon.events.proto.Event;
import org.opennms.horizon.events.proto.EventLog;
import org.opennms.horizon.events.proto.EventServiceGrpc;
import org.springframework.stereotype.Component;

import java.util.List;

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
        EventLog eventList = EventLog.newBuilder()
            .setTenantId(tenantId)
            .addAllEvents(events).build();

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
                responseObserver.onError(StatusProto.toStatusRuntimeException(
                    createStatus(e.getStatus().getCode().value(), e.getStatus().getDescription())));
            } else {
                responseObserver.onError(StatusProto.toStatusRuntimeException(
                    createStatus(Code.INTERNAL_VALUE, e.getMessage())));
            }
            return;
        }

        List<Event> events = eventService.findEventsByNodeId(tenantId, nodeId.getValue());
        EventLog eventList = EventLog.newBuilder()
            .setTenantId(tenantId)
            .addAllEvents(events).build();

        responseObserver.onNext(eventList);
        responseObserver.onCompleted();
    }

    private Status createStatus(int code, String msg) {
        return Status.newBuilder()
            .setCode(code)
            .setMessage(msg)
            .build();
    }
}
