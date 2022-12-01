package org.opennms.horizon.server.service.grpc;

import com.google.protobuf.Empty;
import com.google.protobuf.Int64Value;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.events.proto.EventDTO;
import org.opennms.horizon.events.proto.EventServiceGrpc;
import org.opennms.horizon.inventory.Constants;

import java.util.List;

@RequiredArgsConstructor
public class EventsClient {
    private final ManagedChannel channel;

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
        metadata.put(Constants.AUTHORIZATION_METADATA_KEY, accessToken);
        return eventsStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).listEvents(Empty.newBuilder().build()).getEventsList();
    }

    public List<EventDTO> getEventsByNodeId(long nodeId, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(Constants.AUTHORIZATION_METADATA_KEY, accessToken);
        return eventsStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).getEventsByNodeId(Int64Value.of(nodeId)).getEventsList();
    }
}
