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
