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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opennms.horizon.minion.flows.listeners.UdpListener;
import org.opennms.horizon.minion.flows.listeners.factory.ListenerDefinition;
import org.opennms.horizon.minion.flows.listeners.factory.UdpListenerFactory;


public class ConfigManager {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigManager.class);

    private final UdpListenerFactory udpListenerFactory;

    private final ListenerDefinition listenerDefinition;

    private static final boolean ENABLE_UDP_LISTENER = false;

    public ConfigManager(UdpListenerFactory udpListenerFactory, ListenerDefinition listenerDefinition) {
        this.udpListenerFactory = udpListenerFactory;
        this.listenerDefinition = listenerDefinition;
        configure();
    }

    public synchronized void configure() {
        LOG.debug("Init Udp ConfigManager.. ");
        UdpListener udpListener = (UdpListener) udpListenerFactory.createBean(listenerDefinition);
        try {
            if (ENABLE_UDP_LISTENER) {
                udpListener.start();
                LOG.info("UDP Listener started.. ");
            } else {
                LOG.info("UDP Listener currently disabled. ");
            }
        } catch (InterruptedException e) {
            LOG.error("Starting of UDP Listener failed: ", e);
            Thread.currentThread().interrupt();
        }
    }
}
