/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.opennms.horizon.minion.flows.parser;

import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Objects;
import org.opennms.horizon.flows.document.FlowDocument;
import org.opennms.horizon.flows.document.FlowDocumentLog;
import org.opennms.horizon.shared.ipc.rpc.IpcIdentity;
import org.opennms.horizon.shared.ipc.sink.api.AggregationPolicy;
import org.opennms.horizon.shared.ipc.sink.api.AsyncPolicy;
import org.opennms.horizon.shared.ipc.sink.api.SinkModule;

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
    public AggregationPolicy<FlowDocument, FlowDocumentLog, FlowDocumentLog.Builder> getAggregationPolicy() {
        return new AggregationPolicy<>() {
            // TODO: hardcode for now. Will fix in DC-455
            @Override
            public int getCompletionSize() {
                return 1000;
            }

            // TODO: hardcode for now. Will fix in DC-455
            @Override
            public int getCompletionIntervalMs() {
                return 1000;
            }

            @Override
            public Object key(FlowDocument flowDocument) {
                return flowDocument.getTimestamp();
            }

            @Override
            public FlowDocumentLog.Builder aggregate(FlowDocumentLog.Builder accumulator, FlowDocument newMessage) {
                if (accumulator == null) {
                    accumulator = FlowDocumentLog.newBuilder()
                            .setSystemId(identity.getId())
                            .addMessage(newMessage);
                } else {
                    if (newMessage != null) {
                        accumulator.addMessage(newMessage);
                    }
                }
                return accumulator;
            }

            @Override
            public FlowDocumentLog build(FlowDocumentLog.Builder message) {
                return message.build();
            }
        };
    }

    // TODO: hardcode for now. Will fix in DC-455
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
