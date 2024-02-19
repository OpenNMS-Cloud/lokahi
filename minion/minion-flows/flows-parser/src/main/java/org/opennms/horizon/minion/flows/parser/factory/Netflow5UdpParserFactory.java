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
package org.opennms.horizon.minion.flows.parser.factory;

import com.codahale.metrics.MetricRegistry;
import java.util.Objects;
import org.opennms.horizon.minion.flows.listeners.Parser;
import org.opennms.horizon.minion.flows.parser.Netflow5UdpParser;
import org.opennms.horizon.minion.flows.parser.TelemetryRegistry;
import org.opennms.horizon.shared.ipc.rpc.IpcIdentity;
import org.opennms.sink.flows.contract.ParserConfig;

public class Netflow5UdpParserFactory implements ParserFactory {

    private final TelemetryRegistry telemetryRegistry;
    private final IpcIdentity identity;
    private final DnsResolver dnsResolver;
    private final MetricRegistry metricRegistry;

    public Netflow5UdpParserFactory(
            final TelemetryRegistry telemetryRegistry,
            final IpcIdentity identity,
            final DnsResolver dnsResolver,
            final MetricRegistry metricRegistry) {
        this.telemetryRegistry = Objects.requireNonNull(telemetryRegistry);
        this.identity = Objects.requireNonNull(identity);
        this.dnsResolver = Objects.requireNonNull(dnsResolver);
        this.metricRegistry = Objects.requireNonNull(metricRegistry);
    }

    @Override
    public Class<? extends Parser> getParserClass() {
        return Netflow5UdpParser.class;
    }

    @Override
    public Parser create(final ParserConfig parserConfig) {
        final var dispatcher = telemetryRegistry.getDispatcher();

        return new Netflow5UdpParser(
                parserConfig.getName(), dispatcher, this.identity, this.dnsResolver, metricRegistry);
    }
}
