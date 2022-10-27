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

package org.opennms.horizon.traps.consumer;

import com.google.protobuf.InvalidProtocolBufferException;
import org.opennms.horizon.db.model.OnmsDistPoller;
import org.opennms.horizon.grpc.traps.contract.TrapDTO;
import org.opennms.horizon.grpc.traps.contract.TrapLogDTO;
import org.opennms.horizon.shared.ipc.sink.api.AggregationPolicy;
import org.opennms.horizon.shared.ipc.sink.api.AsyncPolicy;
import org.opennms.horizon.shared.ipc.sink.api.SinkModule;
import org.opennms.horizon.shared.snmp.traps.TrapdConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class TrapSinkModule implements SinkModule<org.opennms.horizon.grpc.traps.contract.TrapDTO, org.opennms.horizon.grpc.traps.contract.TrapLogDTO> {

    private static final Logger LOG = LoggerFactory.getLogger(TrapSinkModule.class);

    private final TrapdConfig config;

    private OnmsDistPoller distPoller;

    public TrapSinkModule(TrapdConfig trapdConfig, OnmsDistPoller distPoller) {
        this.config = Objects.requireNonNull(trapdConfig);
        this.distPoller = Objects.requireNonNull(distPoller);
    }

    @Override
    public String getId() {
        return "Trap";
    }

    @Override
    public int getNumConsumerThreads() {
        return config.getNumThreads();
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
                return config.getBatchSize();
            }

            @Override
            public int getCompletionIntervalMs() {
                return config.getBatchIntervalMs();
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
                return config.getQueueSize();
            }

            @Override
            public int getNumThreads() {
                return config.getNumThreads();
            }

            @Override
            public boolean isBlockWhenFull() {
                return true;
            }
        };
    }


}
