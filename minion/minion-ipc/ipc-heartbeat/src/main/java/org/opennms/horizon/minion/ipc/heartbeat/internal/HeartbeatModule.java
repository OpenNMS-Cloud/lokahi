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
package org.opennms.horizon.minion.ipc.heartbeat.internal;

import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.grpc.heartbeat.contract.HeartbeatMessage;
import org.opennms.horizon.shared.ipc.sink.aggregation.IdentityAggregationPolicy;
import org.opennms.horizon.shared.ipc.sink.api.AggregationPolicy;
import org.opennms.horizon.shared.ipc.sink.api.AsyncPolicy;
import org.opennms.horizon.shared.ipc.sink.api.SinkModule;

@Slf4j
public class HeartbeatModule implements SinkModule<HeartbeatMessage, HeartbeatMessage> {

    @Override
    public String getId() {
        return SinkModule.HEARTBEAT_MODULE_ID;
    }

    @Override
    public int getNumConsumerThreads() {
        return 1;
    }

    @Override
    public byte[] marshal(HeartbeatMessage resultsMessage) {
        try {
            return resultsMessage.toByteArray();
        } catch (Exception e) {
            log.warn("Error while marshalling message {}.", resultsMessage, e);
            return new byte[0];
        }
    }

    @Override
    public HeartbeatMessage unmarshal(byte[] message) {
        try {
            return HeartbeatMessage.parseFrom(message);
        } catch (Exception e) {
            log.warn("Error while unmarshalling message.", e);
            return null;
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
        return new IdentityAggregationPolicy();
    }

    @Override
    public AsyncPolicy getAsyncPolicy() {
        return new AsyncPolicy() {
            public int getQueueSize() {
                return 10;
            }

            public int getNumThreads() {
                return 10;
            }
        };
    }
}
