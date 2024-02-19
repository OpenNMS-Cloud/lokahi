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
package org.opennms.horizon.minion.flows.listeners.factory;

import com.codahale.metrics.MetricRegistry;
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
                .map(p -> (UdpParser) p)
                .toList();
        if (parsers.size() != udpParsers.size()) {
            throw new IllegalArgumentException("Each parser must be of type UdpParser but was not: " + parsers);
        }

        int port = 0;
        try {
            Optional<Parameter> parameter = listenerConfig.getParametersList().stream()
                    .filter(p -> "port".equals(p.getKey()))
                    .findFirst();
            if (parameter.isPresent()) {
                port = Integer.parseUnsignedInt(parameter.get().getValue());
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(String.format(
                    "Invalid port for listener: %s, error: %s", listenerConfig.getName(), e.getMessage()));
        }

        return new UdpListener(listenerConfig.getName(), port, udpParsers, metricRegistry);
    }
}
