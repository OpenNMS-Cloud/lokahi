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
import org.opennms.dataplatform.flows.document.FlowDocument;
import org.opennms.dataplatform.flows.document.FlowDocumentLog;
import org.opennms.horizon.shared.ipc.rpc.IpcIdentity;
import org.opennms.horizon.shared.ipc.sink.api.AggregationPolicy;
import org.opennms.horizon.shared.ipc.sink.api.AsyncPolicy;
import org.opennms.horizon.shared.ipc.sink.api.SinkModule;

import java.util.Objects;

public class FlowSinkModule implements SinkModule<FlowDocument, FlowDocumentLog> {

    private static final String ID = "Flow";

    private final IpcIdentity identity;

    public FlowSinkModule(IpcIdentity identity) {
        this.identity = Objects.requireNonNull(identity);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public int getNumConsumerThreads() {
        return 1;
    }

    @Override
    public byte[] marshal(FlowDocumentLog message) {
        return message.toByteArray();
    }

    @Override
    public FlowDocumentLog unmarshal(byte[] message) {
        try {
            return FlowDocumentLog.parseFrom(message);
        } catch (InvalidProtocolBufferException e) {
            throw new UnmarshalException(e);
        }
    }

    @Override
    public byte[] marshalSingleMessage(FlowDocument message) {
        return message.toByteArray();
    }

    @Override
    public FlowDocument unmarshalSingleMessage(byte[] message) {
        try {
            return FlowDocument.parseFrom(message);
        } catch (InvalidProtocolBufferException e) {
            throw new UnmarshalException(e);
        }
    }

    @Override
    public AggregationPolicy<FlowDocument, FlowDocumentLog, FlowDocumentLog> getAggregationPolicy() {
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
            public Object key(FlowDocument flowDocument) {
                return flowDocument.getTimestamp();
            }

            @Override
            public FlowDocumentLog aggregate(FlowDocumentLog accumulator, FlowDocument newMessage) {
                if (accumulator == null) {
                    accumulator = FlowDocumentLog.newBuilder()
                        .setLocation(identity.getLocation())
                        .setSystemId(identity.getId())
                        .addMessage(newMessage).build();
                } else {
                    if (newMessage != null) {
                        FlowDocumentLog.newBuilder(accumulator).addMessage(newMessage);
                    }
                }
                return accumulator;
            }

            @Override
            public FlowDocumentLog build(FlowDocumentLog message) {
                return message;
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

        };
    }
}
