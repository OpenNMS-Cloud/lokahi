/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
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
                        .setIdentity(Identity.newBuilder().setSystemId(identity.getId()).setLocation(identity.getLocation()).build())
                        .addTrapDTO(newMessage).build();
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
