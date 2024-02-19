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
package org.opennms.horizon.minion.flows.parser;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.opennms.horizon.minion.flows.listeners.Listener;
import org.opennms.sink.flows.contract.FlowsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowsListenerFactory implements org.opennms.horizon.minion.plugin.api.ListenerFactory {

    private static final Logger LOG = LoggerFactory.getLogger(FlowsListenerFactory.class);

    private final TelemetryRegistry telemetryRegistry;

    public FlowsListenerFactory(final TelemetryRegistry telemetryRegistry) {
        this.telemetryRegistry = Objects.requireNonNull(telemetryRegistry);
    }

    @Override
    public FlowsListener create(Any config) {
        LOG.info("FlowsConfig: {}", config.toString());

        if (!config.is(FlowsConfig.class)) {
            throw new IllegalArgumentException("configuration must be FlowsConfig; type-url=" + config.getTypeUrl());
        }

        final FlowsConfig flowsConfig;
        try {
            flowsConfig = config.unpack(FlowsConfig.class);
        } catch (final InvalidProtocolBufferException e) {
            throw new IllegalArgumentException("Error while parsing config with type-url=" + config.getTypeUrl(), e);
        }

        final var listeners = flowsConfig.getListenersList().stream()
                .map(telemetryRegistry::createListener)
                .toList();

        return new FlowsListener(listeners);
    }

    public static class FlowsListener implements org.opennms.horizon.minion.plugin.api.Listener {

        private final List<Listener> listeners;

        private FlowsListener(final List<Listener> listeners) {
            this.listeners = Collections.unmodifiableList(Objects.requireNonNull(listeners));
        }

        @Override
        public void start() throws Exception {
            for (final var listener : this.listeners) {
                listener.start();
            }
        }

        @Override
        public void stop() {
            for (final var listener : this.listeners) {
                listener.stop();
            }
        }

        public List<Listener> getListeners() {
            return this.listeners;
        }
    }
}
