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

import com.codahale.metrics.MetricRegistry;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.opennms.horizon.flows.document.FlowDocument;
import org.opennms.horizon.minion.flows.listeners.Listener;
import org.opennms.horizon.minion.flows.listeners.Parser;
import org.opennms.horizon.minion.flows.listeners.factory.ListenerFactory;
import org.opennms.horizon.minion.flows.listeners.factory.TcpListenerFactory;
import org.opennms.horizon.minion.flows.listeners.factory.UdpListenerFactory;
import org.opennms.horizon.minion.flows.parser.factory.DnsResolver;
import org.opennms.horizon.minion.flows.parser.factory.IpfixTcpParserFactory;
import org.opennms.horizon.minion.flows.parser.factory.IpfixUdpParserFactory;
import org.opennms.horizon.minion.flows.parser.factory.Netflow5UdpParserFactory;
import org.opennms.horizon.minion.flows.parser.factory.Netflow9UdpParserFactory;
import org.opennms.horizon.minion.flows.parser.factory.ParserFactory;
import org.opennms.horizon.shared.ipc.rpc.IpcIdentity;
import org.opennms.horizon.shared.ipc.sink.api.AsyncDispatcher;
import org.opennms.horizon.shared.ipc.sink.api.MessageDispatcherFactory;
import org.opennms.sink.flows.contract.ListenerConfig;
import org.opennms.sink.flows.contract.ParserConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: this should be replace by using AlertingPluginRegistry to make plugins work.
// TODO: AlertingPluginRegistry needs de-registration to make dynamic loading work.
// TODO: And then this will replace TelemetryRegistry
public class TelemetryRegistryImpl implements TelemetryRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(TelemetryRegistryImpl.class);

    private final List<ListenerFactory> listenerFactories = new ArrayList<>();
    private final List<ParserFactory> parserFactories = new ArrayList<>();

    private final AsyncDispatcher<FlowDocument> dispatcher;

    public TelemetryRegistryImpl(
            MessageDispatcherFactory messageDispatcherFactory,
            IpcIdentity identity,
            DnsResolver dnsResolver,
            MetricRegistry metricRegistry)
            throws IOException {
        Objects.requireNonNull(messageDispatcherFactory);
        Objects.requireNonNull(identity);
        Objects.requireNonNull(dnsResolver);
        Objects.requireNonNull(metricRegistry);

        var sink = new FlowSinkModule(identity);
        this.dispatcher = messageDispatcherFactory.createAsyncDispatcher(sink);

        this.addListenerFactory(new UdpListenerFactory(this, metricRegistry));
        this.addListenerFactory(new TcpListenerFactory(this, metricRegistry));

        this.addParserFactory(new Netflow5UdpParserFactory(this, identity, dnsResolver, metricRegistry));
        this.addParserFactory(new Netflow9UdpParserFactory(this, identity, dnsResolver, metricRegistry));
        this.addParserFactory(new IpfixUdpParserFactory(this, identity, dnsResolver, metricRegistry));
        this.addParserFactory(new IpfixTcpParserFactory(this, identity, dnsResolver, metricRegistry));
    }

    protected void addListenerFactory(ListenerFactory factory) {
        Objects.requireNonNull(factory);
        this.listenerFactories.add(factory);
    }

    protected void addParserFactory(ParserFactory factory) {
        Objects.requireNonNull(factory);
        this.parserFactories.add(factory);
    }

    @Override
    public Listener createListener(ListenerConfig listenerConfig) {
        for (var factory : this.listenerFactories) {
            if (listenerConfig.getClassName().equals(factory.getListenerClass().getCanonicalName())) {
                return factory.create(listenerConfig);
            }
        }

        throw new IllegalArgumentException("Invalid listener class: " + listenerConfig.getClassName());
    }

    @Override
    public Parser createParser(ParserConfig parserConfig) {
        for (var factory : this.parserFactories) {
            if (parserConfig.getClassName().equals(factory.getParserClass().getCanonicalName())) {
                return factory.create(parserConfig);
            }
        }

        throw new IllegalArgumentException("Invalid parser class: " + parserConfig.getClassName());
    }

    @Override
    public AsyncDispatcher<FlowDocument> getDispatcher() {
        return this.dispatcher;
    }
}
