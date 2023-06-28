/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2018 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2018 The OpenNMS Group, Inc.
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

package org.opennms.horizon.minion.flows.listeners.factory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.opennms.horizon.minion.flows.listeners.Listener;
import org.opennms.horizon.minion.flows.listeners.Parser;
import org.opennms.horizon.minion.flows.listeners.UdpListener;
import org.opennms.horizon.minion.flows.listeners.UdpParser;
import org.opennms.horizon.minion.flows.parser.TelemetryRegistry;
import org.opennms.sink.flows.contract.ListenerConfig;
import org.opennms.sink.flows.contract.Parameter;

import com.codahale.metrics.MetricRegistry;

public class UdpListenerFactory implements ListenerFactory {

    private final TelemetryRegistry telemetryRegistry;
    private final MetricRegistry metricRegistry;

    public UdpListenerFactory(TelemetryRegistry telemetryRegistry, MetricRegistry metricRegistry) {
        this.telemetryRegistry = Objects.requireNonNull(telemetryRegistry);
        this.metricRegistry = Objects.requireNonNull(metricRegistry);
    }

    @Override
    public Class<? extends Listener> getListenerClass() {
        return UdpListener.class;
    }

    @Override
    public Listener create(ListenerConfig listenerConfig) {
        // Ensure each defined parser is of type UdpParser
        final List<Parser> parsers = listenerConfig.getParsersList().stream()
                .map(telemetryRegistry::createParser)
                .toList();

        final List<UdpParser> udpParsers = parsers.stream()
                .filter(p -> p instanceof UdpParser)
                .map(p -> (UdpParser) p).toList();
        if (parsers.size() != udpParsers.size()) {
            throw new IllegalArgumentException("Each parser must be of type UdpParser but was not: " + parsers);
        }

        int port = 0;
        try {
            Optional<Parameter> parameter = listenerConfig.getParametersList().stream().filter(p -> "port".equals(p.getKey())).findFirst();
            if (parameter.isPresent()) {
                port = Integer.parseUnsignedInt(parameter.get().getValue());
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(String.format("Invalid port for listener: %s, error: %s", listenerConfig.getName(), e.getMessage()));
        }

        return new UdpListener(listenerConfig.getName(), port, udpParsers, metricRegistry);
    }
}
