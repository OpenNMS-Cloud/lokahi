/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022-2023 The OpenNMS Group, Inc.
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

package org.opennms.horizon.minion.flows.parser;

import com.google.protobuf.InvalidProtocolBufferException;

import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.horizon.grpc.telemetry.contract.TelemetryMessage;
import org.opennms.horizon.grpc.telemetry.contract.TelemetryMessageLog;
import org.opennms.horizon.shared.ipc.rpc.IpcIdentity;
import org.opennms.horizon.shared.ipc.sink.api.AggregationPolicy;
import org.opennms.horizon.shared.ipc.sink.api.AsyncPolicy;
import org.opennms.horizon.shared.ipc.sink.api.SinkModule;


import java.util.Objects;

public class FlowSinkModule implements SinkModule<TelemetryMessage, TelemetryMessageLog> {

    private final IpcIdentity identity;

    private final String id;

    public FlowSinkModule(IpcIdentity identity, String id) {
        this.identity = Objects.requireNonNull(identity);
        this.id = Objects.requireNonNull(id);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public int getNumConsumerThreads() {
        return 1;
    }

    @Override
    public byte[] marshal(TelemetryMessageLog message) {
        return message.toByteArray();
    }

    @Override
    public TelemetryMessageLog unmarshal(byte[] message) {
        try {
            return TelemetryMessageLog.parseFrom(message);
        } catch (InvalidProtocolBufferException e) {
            throw new UnmarshalException(e);
        }
    }

    @Override
    public byte[] marshalSingleMessage(TelemetryMessage message) {
        return message.toByteArray();
    }

    @Override
    public TelemetryMessage unmarshalSingleMessage(byte[] message) {
        try {
            return TelemetryMessage.parseFrom(message);
        } catch (InvalidProtocolBufferException e) {
            throw new UnmarshalException(e);
        }
    }

    @Override
    public AggregationPolicy<TelemetryMessage, TelemetryMessageLog, TelemetryMessageLog> getAggregationPolicy() {
        return new AggregationPolicy<>() {
            //TODO: hardcode for now. Will fix in DC-455
            @Override
            public int getCompletionSize() {
                return 1;
            }
            //TODO: hardcode for now. Will fix in DC-455
            @Override
            public int getCompletionIntervalMs() {
                return 1000;
            }

            @Override
            public Object key(TelemetryMessage telemetryMessage) {
                return telemetryMessage.getTimestamp();
            }

            @Override
            public TelemetryMessageLog aggregate(TelemetryMessageLog accumulator, TelemetryMessage newMessage) {
                if (accumulator == null) {
                    accumulator = TelemetryMessageLog.newBuilder()
                        .setSystemId(Identity.newBuilder()
                            .setSystemId(identity.getId()).setLocation(identity.getLocation()).build().toString())
                        .addMessage(newMessage).build();
                } else {
                    TelemetryMessageLog.newBuilder(accumulator).addMessage(newMessage);
                }
                return accumulator;
            }

            @Override
            public TelemetryMessageLog build(TelemetryMessageLog telemetryMessageLog) {
                return telemetryMessageLog;
            }
        };
    }

    //TODO: hardcode for now. Will fix in DC-455
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

            @Override
            public boolean isBlockWhenFull() {
                return true;
            }
        };
    }
}
