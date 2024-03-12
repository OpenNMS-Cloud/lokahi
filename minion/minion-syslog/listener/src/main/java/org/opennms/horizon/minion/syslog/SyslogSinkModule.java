package org.opennms.horizon.minion.syslog;

import com.google.protobuf.InvalidProtocolBufferException;
import org.opennms.horizon.syslog.api.SyslogMessageLogDTO;
import org.opennms.horizon.syslog.api.SyslogMessageDTO;
import org.opennms.horizon.shared.ipc.sink.api.AggregationPolicy;
import org.opennms.horizon.shared.ipc.sink.api.AsyncPolicy;
import org.opennms.horizon.shared.ipc.sink.api.SinkModule;
import org.opennms.horizon.syslog.api.SyslogdConfiguration;



public class SyslogSinkModule implements SinkModule<SyslogMessageDTO, SyslogMessageLogDTO> {


    private final SyslogdConfig config;

    public SyslogSinkModule(SyslogdConfig syslogdConfiguration) {
        this.config = syslogdConfiguration;
    }

    @Override
    public String getId() {
        return "Syslog";
    }

    @Override
    public int getNumConsumerThreads() {
        return config.getNumThreads();
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
            return null;
        }
    }

    @Override
    public byte[] marshalSingleMessage(SyslogMessageDTO message) {
        return message.toByteArray();
    }

    @Override
    public SyslogMessageDTO unmarshalSingleMessage(byte[] message) {
        try {
            return SyslogMessageDTO.parseFrom(message);
        } catch (InvalidProtocolBufferException e) {
            return null;
        }
    }

    @Override
    public AggregationPolicy<SyslogMessageDTO, SyslogMessageLogDTO, SyslogMessageLogDTO> getAggregationPolicy() {
        return new AggregationPolicy<>() {
            @Override
            public int getCompletionSize() {
                return config.getBatchSize();
            }

            @Override
            public int getCompletionIntervalMs() {
                return config.getBatchIntervalMs();
            }

            @Override
            public Object key(SyslogMessageDTO message) {
                return message.getMMessage();
            }

            @Override
            public SyslogMessageLogDTO aggregate(SyslogMessageLogDTO accumulator, SyslogMessageDTO newMessage) {
                if (accumulator == null) {
                    accumulator = SyslogMessageLogDTO.newBuilder()
                        .setSourceAddress(newMessage.getMHostname())
                        .setLocation(newMessage.getMMessage())
                        .addMessages(newMessage)
                        .build();
                } else {
                    SyslogMessageLogDTO.newBuilder(accumulator).addMessages(newMessage);
                }
                return accumulator;
            }

            @Override
            public SyslogMessageLogDTO build(SyslogMessageLogDTO accumulator) {
                return accumulator;
            }
        };
    }

    @Override
    public AsyncPolicy getAsyncPolicy() {
        return new AsyncPolicy() {
            @Override
            public int getQueueSize() {
                return config.getQueueSize();
            }

            @Override
            public int getNumThreads() {
                return config.getNumThreads();
            }
        };
    }
}
