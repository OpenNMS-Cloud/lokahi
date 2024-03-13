package org.opennms.miniongateway.grpc.server.syslog;

import com.google.protobuf.InvalidProtocolBufferException;
import org.opennms.horizon.grpc.syslog.contract.SyslogMessageLogDTO;
import org.opennms.horizon.shared.ipc.sink.aggregation.IdentityAggregationPolicy;
import org.opennms.horizon.shared.ipc.sink.api.AggregationPolicy;
import org.opennms.horizon.shared.ipc.sink.api.AsyncPolicy;
import org.opennms.horizon.shared.ipc.sink.api.SinkModule;

public class SyslogSinkModule implements SinkModule<SyslogMessageLogDTO, SyslogMessageLogDTO> {
    @Override
    public String getId() {
        return "Trap";
    }

    @Override
    public int getNumConsumerThreads() {
        return 1;
    }

    @Override
    public byte[] marshal(SyslogMessageLogDTO message) {
        return message.toByteArray();
    }

    @Override
    public SyslogMessageLogDTO unmarshal(byte[] message) {
        try {
            return SyslogMessageLogDTO.parseFrom(message);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] marshalSingleMessage(SyslogMessageLogDTO message) {
        return message.toByteArray();
    }

    @Override
    public SyslogMessageLogDTO unmarshalSingleMessage(byte[] message) {
        try {
            return SyslogMessageLogDTO.parseFrom(message);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public AggregationPolicy<SyslogMessageLogDTO, SyslogMessageLogDTO, ?> getAggregationPolicy() {
        // Aggregation should be performed on Minion not on gateway
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
