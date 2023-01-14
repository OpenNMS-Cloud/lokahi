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

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import org.opennms.horizon.minion.flows.listeners.FlowsListener;
import org.opennms.horizon.minion.flows.listeners.TcpListener;
import org.opennms.horizon.minion.flows.listeners.UdpListener;
import org.opennms.horizon.minion.flows.listeners.factory.TcpListenerFactory;
import org.opennms.horizon.minion.flows.listeners.factory.UdpListenerFactory;
import org.opennms.horizon.minion.plugin.api.Listener;
import org.opennms.horizon.minion.plugin.api.ListenerFactory;
import org.opennms.horizon.minion.plugin.api.ServiceMonitorResponse;
import org.opennms.horizon.shared.ipc.rpc.IpcIdentity;
import org.opennms.horizon.shared.ipc.sink.api.MessageDispatcherFactory;
import org.opennms.sink.flows.contract.FlowsConfig;
import org.opennms.sink.flows.contract.ListenerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ConfigManager implements ListenerFactory {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigManager.class);

    private final MessageDispatcherFactory messageDispatcherFactory;

    private final IpcIdentity identity;

    private final UdpListenerFactory udpListenerFactory;

    private final TcpListenerFactory tcpListenerFactory;

    private final ListenerHolder listenerHolder;

    private FlowsConfig flowsConfig;

    public ConfigManager(MessageDispatcherFactory messageDispatcherFactory,
                         IpcIdentity identity,
                         ListenerHolder listenerHolder,
                         UdpListenerFactory udpListenerFactory,
                         TcpListenerFactory tcpListenerFactory) {
        this.messageDispatcherFactory = messageDispatcherFactory;
        this.identity = identity;
        this.listenerHolder = listenerHolder;
        this.udpListenerFactory = udpListenerFactory;
        this.tcpListenerFactory = tcpListenerFactory;

    }

    public synchronized void configure(FlowsConfig flowsConfig) {
        listenerHolder.replaceAll(flowsConfig.getListenersList().stream().map(this::createListener)
            .filter(v -> v != null).collect(Collectors.toList()));
    }

    private FlowsListener createListener(ListenerConfig listenerConfig) {
        if (!listenerConfig.getEnabled()) {
            LOG.info("Listener: {} currently disabled. ", listenerConfig.getName());
            return null;
        }
        FlowsListener listener;
        if (listenerConfig.getClassName().equals(TcpListener.class.getCanonicalName())) {
            listener = tcpListenerFactory.createBean(listenerConfig);
        } else if (listenerConfig.getClassName().equals(UdpListener.class.getCanonicalName())) {
            listener = udpListenerFactory.createBean(listenerConfig);
        } else {
            LOG.error("Unknown listener class: {}", listenerConfig.getClassName());
            return null;
        }
        try {
            listener.start();
            LOG.debug("Listener: {} started. ", listenerConfig.getName());
        } catch (Exception e) {
            LOG.error("Fail to start listener: {}", listenerConfig.getName());
        }
        return listener;
    }

    @Override
    public Listener create(Consumer<ServiceMonitorResponse> resultProcessor, Any config) {
        LOG.info("FlowsConfig: {}", config.toString());
        if (!config.is(FlowsConfig.class)) {
            throw new IllegalArgumentException("configuration must be FlowsConfig; type-url=" + config.getTypeUrl());
        }
        try {
            this.flowsConfig = config.unpack(FlowsConfig.class);
            configure(flowsConfig);

        } catch (InvalidProtocolBufferException e) {
            throw new IllegalArgumentException("Error while parsing config with type-url=" + config.getTypeUrl());
        }
        return null;
    }
}
