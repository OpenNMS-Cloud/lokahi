package org.opennms.horizon.server.service.grpc;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.opennms.horizon.events.proto.EventDTO;
import org.opennms.horizon.events.proto.EventServiceGrpc;
import org.opennms.horizon.shared.constants.GrpcConstants;

import com.google.protobuf.Empty;
import com.google.protobuf.Int64Value;

import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import lombok.RequiredArgsConstructor;

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

    public List<EventDTO> listEvents(String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return eventsStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).withDeadlineAfter(deadline, TimeUnit.MILLISECONDS).listEvents(Empty.newBuilder().build()).getEventsList();
    }

    public List<EventDTO> getEventsByNodeId(long nodeId, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return eventsStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).withDeadlineAfter(deadline, TimeUnit.MILLISECONDS).getEventsByNodeId(Int64Value.of(nodeId)).getEventsList();
    }
}
