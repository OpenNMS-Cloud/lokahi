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

import com.codahale.metrics.MetricRegistry;
import lombok.Getter;
import org.opennms.horizon.grpc.telemetry.contract.TelemetryMessage;
import org.opennms.horizon.minion.flows.listeners.FlowsListener;
import org.opennms.horizon.minion.flows.listeners.Parser;
import org.opennms.horizon.minion.flows.listeners.TcpListener;
import org.opennms.horizon.minion.flows.listeners.UdpListener;
import org.opennms.horizon.minion.flows.listeners.factory.ListenerFactory;
import org.opennms.horizon.minion.flows.listeners.factory.TelemetryRegistry;
import org.opennms.horizon.minion.flows.parser.factory.ParserFactory;
import org.opennms.horizon.shared.ipc.rpc.IpcIdentity;
import org.opennms.horizon.shared.ipc.sink.api.AsyncDispatcher;
import org.opennms.horizon.shared.ipc.sink.api.MessageDispatcherFactory;
import org.opennms.sink.flows.contract.ListenerConfig;
import org.opennms.sink.flows.contract.ParserConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class TelemetryRegistryImpl implements TelemetryRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(TelemetryRegistryImpl.class);

    private final MessageDispatcherFactory messageDispatcherFactory;

    private final IpcIdentity identity;

    private final List<ListenerFactory> listenerFactories = new ArrayList<>();
    private final List<ParserFactory> parserFactoryList = new ArrayList<>();

    // maintain a list of dispatchers to prevent already exist exception
    private final Map<String, AsyncDispatcher<TelemetryMessage>> dispatcherMap = new HashMap<>();

    @Getter
    private final ListenerHolder listenerHolder;

    public TelemetryRegistryImpl(MessageDispatcherFactory messageDispatcherFactory,
                                 IpcIdentity identity,
                                 ListenerHolder listenerHolder) {
        this.messageDispatcherFactory = Objects.requireNonNull(messageDispatcherFactory);
        this.identity = Objects.requireNonNull(identity);
        this.listenerHolder = Objects.requireNonNull(listenerHolder);
    }


    @Override
    public void addListenerFactory(ListenerFactory factory) {
        Objects.requireNonNull(factory);
        listenerFactories.add(factory);
    }

    @Override
    public void addParserFactory(ParserFactory factory) {
        Objects.requireNonNull(factory);
        parserFactoryList.add(factory);
    }

    @Override
    public FlowsListener getListener(ListenerConfig listenerConfig) {
        var listener = listenerHolder.get(listenerConfig.getName());
        if (listener != null) {
            return listener;
        }
        if (!listenerConfig.getEnabled()) {
            LOG.info("Listener: {} currently disabled. ", listenerConfig.getName());
            return null;
        }
        for (var factory : listenerFactories) {
            if (factory.getClass().getName().contains(listenerConfig.getClassName())) {
                listener = factory.createBean(listenerConfig);
                listenerHolder.put(listener);
                return listener;
            }
        }
        LOG.error("Unknown listener class: {}", listenerConfig.getClassName());
        return null;
    }

    @Override
    public Parser getParser(ParserConfig parserConfig) {
        for (var factory : parserFactoryList) {
            if (factory.getClass().getName().contains(parserConfig.getClassName())) {
                return factory.createBean(parserConfig);
            }
        }
        throw new IllegalArgumentException("Invalid parser class.");
    }

    @Override
    public MetricRegistry getMetricRegistry() {
        return new MetricRegistry();
    }

    @Override
    public AsyncDispatcher<TelemetryMessage> getDispatcher(String queueName) {
        var dispatcher = dispatcherMap.get(queueName);
        if (dispatcher != null) {
            return dispatcher;
        }
        var sink = new FlowSinkModule(identity, queueName);
        dispatcher = messageDispatcherFactory.createAsyncDispatcher(sink);
        dispatcherMap.put(queueName, dispatcher);
        return dispatcher;
    }
}
