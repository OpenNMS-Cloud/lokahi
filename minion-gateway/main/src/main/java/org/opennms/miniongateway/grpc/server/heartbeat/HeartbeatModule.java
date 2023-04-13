package org.opennms.miniongateway.grpc.server.heartbeat;

import com.google.protobuf.InvalidProtocolBufferException;
import org.opennms.horizon.grpc.heartbeat.contract.HeartbeatMessage;
import org.opennms.horizon.shared.ipc.sink.aggregation.IdentityAggregationPolicy;
import org.opennms.horizon.shared.ipc.sink.api.AggregationPolicy;
import org.opennms.horizon.shared.ipc.sink.api.AsyncPolicy;
import org.opennms.horizon.shared.ipc.sink.api.SinkModule;

public class HeartbeatModule implements SinkModule<HeartbeatMessage, HeartbeatMessage> {

    @Override
    public String getId() {
        return HEARTBEAT_MODULE_ID;
    }

    @Override
    public int getNumConsumerThreads() {
        return 1;
    }

    @Override
    public byte[] marshal(HeartbeatMessage message) {
        return message.toByteArray();
    }

    @Override
    public HeartbeatMessage unmarshal(byte[] content) {
        try {
            return HeartbeatMessage.parseFrom(content);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] marshalSingleMessage(HeartbeatMessage message) {
        return marshal(message);
    }

    @Override
    public HeartbeatMessage unmarshalSingleMessage(byte[] message) {
        return unmarshal(message);
    }

    @Override
    public AggregationPolicy<HeartbeatMessage, HeartbeatMessage, ?> getAggregationPolicy() {
        return new IdentityAggregationPolicy<>();
    }

    @Override
    public AsyncPolicy getAsyncPolicy() {
        return new AsyncPolicy() {
            @Override
            public int getQueueSize() {
                return 10;
            }

            @Override
            public int getNumThreads() {
                return 1;
            }

        };
    }
}
