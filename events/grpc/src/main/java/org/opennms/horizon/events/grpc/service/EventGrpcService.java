package org.opennms.horizon.events.grpc.service;

import com.google.protobuf.Empty;
import com.google.protobuf.Int64Value;
import io.grpc.stub.StreamObserver;
import org.opennms.horizon.events.persistence.service.EventService;
import org.opennms.horizon.events.proto.EventDTO;
import org.opennms.horizon.events.proto.EventList;
import org.opennms.horizon.events.proto.EventServiceGrpc;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EventGrpcService extends EventServiceGrpc.EventServiceImplBase {
    private final EventService eventService;

    public EventGrpcService(EventService eventService) {
        this.eventService = eventService;
    }

    @Override
    public void listEvents(Empty request, StreamObserver<EventList> responseObserver) {
        List<EventDTO> events = eventService.findEvents();
        EventList eventList = EventList.newBuilder()
            .addAllEvents(events).build();

        responseObserver.onNext(eventList);
        responseObserver.onCompleted();
    }

    @Override
    public void getEventsByNodeId(Int64Value nodeId, StreamObserver<EventList> responseObserver) {
        List<EventDTO> events = eventService.findEventsByNodeId(nodeId.getValue());
        EventList eventList = EventList.newBuilder()
            .addAllEvents(events).build();

        responseObserver.onNext(eventList);
        responseObserver.onCompleted();
    }
}
