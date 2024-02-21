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
package org.opennms.horizon.minion.traps.listener;

import com.google.protobuf.InvalidProtocolBufferException;
import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.horizon.grpc.traps.contract.TrapDTO;
import org.opennms.horizon.grpc.traps.contract.TrapLogDTO;
import org.opennms.horizon.shared.ipc.rpc.IpcIdentity;
import org.opennms.horizon.shared.ipc.sink.api.AggregationPolicy;
import org.opennms.horizon.shared.ipc.sink.api.AsyncPolicy;
import org.opennms.horizon.shared.ipc.sink.api.SinkModule;
import org.opennms.sink.traps.contract.TrapConfig;

public class TrapSinkModule implements SinkModule<TrapDTO, TrapLogDTO> {

    private final IpcIdentity identity;

    private final TrapConfig config;

    public TrapSinkModule(TrapConfig trapdConfig, IpcIdentity identity) {
        this.config = trapdConfig;
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
    public byte[] marshal(TrapLogDTO message) {
        return message.toByteArray();
    }

    @Override
    public TrapLogDTO unmarshal(byte[] message) {
        try {
            return TrapLogDTO.parseFrom(message);
        } catch (InvalidProtocolBufferException e) {
            return null;
        }
    }

    @Override
    public byte[] marshalSingleMessage(TrapDTO message) {
        return message.toByteArray();
    }

    @Override
    public TrapDTO unmarshalSingleMessage(byte[] message) {
        try {
            return TrapDTO.parseFrom(message);
        } catch (InvalidProtocolBufferException e) {
            return null;
        }
    }

    @Override
    public AggregationPolicy<TrapDTO, TrapLogDTO, TrapLogDTO> getAggregationPolicy() {
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
            public Object key(TrapDTO message) {
                return message.getTrapAddress();
            }

            @Override
            public TrapLogDTO aggregate(TrapLogDTO accumulator, TrapDTO newMessage) {
                if (accumulator == null) {
                    accumulator = TrapLogDTO.newBuilder()
                            .setTrapAddress(newMessage.getTrapAddress())
                            .setIdentity(Identity.newBuilder().setSystemId(identity.getId()))
                            .addTrapDTO(newMessage)
                            .build();
                } else {
                    TrapLogDTO.newBuilder(accumulator).addTrapDTO(newMessage);
                }
                return accumulator;
            }

            @Override
            public TrapLogDTO build(TrapLogDTO accumulator) {
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
