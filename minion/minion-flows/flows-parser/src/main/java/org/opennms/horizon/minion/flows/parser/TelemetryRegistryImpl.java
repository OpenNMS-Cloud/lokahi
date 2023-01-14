package org.opennms.horizon.minion.flows.parser; /*******************************************************************************
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

import org.opennms.horizon.grpc.telemetry.contract.TelemetryMessage;
import org.opennms.horizon.minion.flows.parser.FlowSinkModule;
import org.opennms.horizon.minion.flows.parser.factory.Netflow9UdpParserFactory;
import org.opennms.horizon.shared.ipc.sink.api.AsyncDispatcher;
import org.opennms.horizon.shared.ipc.sink.api.MessageDispatcherFactory;

import com.codahale.metrics.MetricRegistry;

import org.opennms.horizon.minion.flows.listeners.Parser;
import org.opennms.horizon.minion.flows.listeners.factory.TelemetryRegistry;
import org.opennms.horizon.minion.flows.listeners.factory.UdpListenerMessage;
import org.opennms.sink.flows.contract.ParserConfig;

public class TelemetryRegistryImpl implements TelemetryRegistry {

    private final Netflow9UdpParserFactory netflow9UdpParserFactory;

    private final MessageDispatcherFactory messageDispatcherFactory;

    private final FlowSinkModule flowSinkModule;

    public TelemetryRegistryImpl(Netflow9UdpParserFactory netflow9UdpParserFactory,
                                 MessageDispatcherFactory messageDispatcherFactory, FlowSinkModule flowSinkModule) {
        this.netflow9UdpParserFactory = netflow9UdpParserFactory;
        this.messageDispatcherFactory = messageDispatcherFactory;
        this.flowSinkModule = flowSinkModule;
    }

    @Override
    public Parser getParser(ParserConfig parserConfig) {
        return netflow9UdpParserFactory.createBean(parserConfig);
    }


    @Override
    public MetricRegistry getMetricRegistry() {
        return new MetricRegistry();
    }

    @Override
    public AsyncDispatcher<TelemetryMessage> getDispatcher(String queueName) {
        return messageDispatcherFactory.createAsyncDispatcher(flowSinkModule);

    }
}
