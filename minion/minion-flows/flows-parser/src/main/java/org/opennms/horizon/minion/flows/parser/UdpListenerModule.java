/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2016 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2016 The OpenNMS Group, Inc.
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

import org.opennms.horizon.shared.ipc.sink.aggregation.IdentityAggregationPolicy;
import org.opennms.horizon.shared.ipc.sink.api.AggregationPolicy;
import org.opennms.horizon.shared.ipc.sink.api.AsyncPolicy;
import org.opennms.horizon.shared.ipc.sink.api.SinkModule;

import org.opennms.horizon.minion.flows.listeners.factory.UdpListenerMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UdpListenerModule implements SinkModule<UdpListenerMessage, UdpListenerMessage> {

    @Override
    public String getId() {
        return SinkModule.HEARTBEAT_MODULE_ID;
    }

    @Override
    public int getNumConsumerThreads() {
        return 1;
    }

    @Override
    public byte[] marshal(UdpListenerMessage resultsMessage) {
        try {
            return new byte[12];
        } catch (Exception e) {
            log.warn("Error while marshalling message {}.", resultsMessage, e);
            return new byte[0];
        }
    }

    @Override
    public UdpListenerMessage unmarshal(byte[] message) {
        try {
            return null;
        } catch (Exception e) {
            log.warn("Error while unmarshalling message.", e);
            return null;
        }
    }

    @Override
    public byte[] marshalSingleMessage(UdpListenerMessage message) {
        return marshal(message);
    }

    @Override
    public UdpListenerMessage unmarshalSingleMessage(byte[] message) {
        return unmarshal(message);
    }

    @Override
    public AggregationPolicy<UdpListenerMessage, UdpListenerMessage, ?> getAggregationPolicy() {
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

            public boolean isBlockWhenFull() {
                return true;
            }
        };
    }
}
