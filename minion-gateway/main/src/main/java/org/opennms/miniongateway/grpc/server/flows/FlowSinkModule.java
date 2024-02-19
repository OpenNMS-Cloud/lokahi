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
package org.opennms.miniongateway.grpc.server.flows;

import com.google.protobuf.InvalidProtocolBufferException;
import org.opennms.horizon.flows.document.FlowDocumentLog;
import org.opennms.horizon.shared.ipc.sink.aggregation.IdentityAggregationPolicy;
import org.opennms.horizon.shared.ipc.sink.api.AggregationPolicy;
import org.opennms.horizon.shared.ipc.sink.api.AsyncPolicy;
import org.opennms.horizon.shared.ipc.sink.api.SinkModule;
import org.opennms.horizon.shared.ipc.sink.api.UnmarshalException;

// TODO: Why do we even unpack?
public class FlowSinkModule implements SinkModule<FlowDocumentLog, FlowDocumentLog> {
    @Override
    public String getId() {
        return "Flow";
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
    public byte[] marshalSingleMessage(FlowDocumentLog message) {
        return marshal(message);
    }

    @Override
    public FlowDocumentLog unmarshalSingleMessage(byte[] message) {
        return unmarshal(message);
    }

    @Override
    public AggregationPolicy<FlowDocumentLog, FlowDocumentLog, ?> getAggregationPolicy() {
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
