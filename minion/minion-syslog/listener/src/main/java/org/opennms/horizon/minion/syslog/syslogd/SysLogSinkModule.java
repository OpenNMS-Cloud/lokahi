package org.opennms.horizon.minion.syslog.syslogd;

import com.google.protobuf.InvalidProtocolBufferException;
import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.horizon.grpc.syslog.contract.SyslogDTO;
import org.opennms.horizon.grpc.syslog.contract.SyslogLogDTO;
import org.opennms.horizon.shared.ipc.rpc.IpcIdentity;
import org.opennms.horizon.shared.ipc.sink.api.AggregationPolicy;
import org.opennms.horizon.shared.ipc.sink.api.AsyncPolicy;
import org.opennms.horizon.shared.ipc.sink.api.SinkModule;
import org.opennms.sink.traps.contract.SyslogConfig;

public class SysLogSinkModule implements SinkModule<SyslogDTO, SyslogLogDTO> {

    private final IpcIdentity identity;

    private final SyslogConfig config;

    public SysLogSinkModule(SyslogConfig syslogConfig, IpcIdentity identity) {
        this.config = syslogConfig;
        this.identity = identity;
    }

    @Override
    public String getId() {
        return "Trap";
    }

    @Override
    public int getNumConsumerThreads() {
        return config.getListenerConfig().getNumThreads();
    }

    @Override
    public byte[] marshal(SyslogLogDTO message) {
        return message.toByteArray();
    }

    @Override
    public SyslogLogDTO unmarshal(byte[] message) {
        try {
            return SyslogLogDTO.parseFrom(message);
        } catch (InvalidProtocolBufferException e) {
            return null;
        }
    }

    @Override
    public byte[] marshalSingleMessage(SyslogDTO message) {
        return message.toByteArray();
    }

    @Override
    public SyslogDTO unmarshalSingleMessage(byte[] message) {
        try {
            return SyslogDTO.parseFrom(message);
        } catch (InvalidProtocolBufferException e) {
            return null;
        }
    }

    @Override
    public AggregationPolicy<SyslogDTO, SyslogLogDTO, SyslogLogDTO> getAggregationPolicy() {
        return new AggregationPolicy<>() {
            @Override
            public int getCompletionSize() {
                return config.getListenerConfig().getBatchSize();
            }

            @Override
            public int getCompletionIntervalMs() {
                return config.getListenerConfig().getBatchIntervalMs();
            }

            @Override
            public Object key(SyslogDTO message) {
                return message.getSyslogAddress();
            }

            @Override
            public SyslogLogDTO aggregate(SyslogLogDTO accumulator, SyslogDTO newMessage) {
                if (accumulator == null) {
                    accumulator = SyslogLogDTO.newBuilder()
                        .setSyslogAddress(newMessage.getSyslogAddress())
                        .setIdentity(Identity.newBuilder().setSystemId(identity.getId()))
                        .addSyslogDTO(newMessage)
                        .build();
                } else {
                    SyslogLogDTO.newBuilder(accumulator).addSyslogDTO(newMessage);
                }
                return accumulator;
            }

            @Override
            public SyslogLogDTO build(SyslogLogDTO accumulator) {
                return accumulator;
            }
        };
    }

    @Override
    public AsyncPolicy getAsyncPolicy() {
        return new AsyncPolicy() {
            @Override
            public int getQueueSize() {
                return config.getListenerConfig().getQueueSize();
            }

            @Override
            public int getNumThreads() {
                return config.getListenerConfig().getNumThreads();
            }
        };
    }
}
