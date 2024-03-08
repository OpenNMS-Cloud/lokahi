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

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import io.netty.buffer.ByteBuf;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import org.opennms.horizon.flows.document.FlowDocument;
import org.opennms.horizon.minion.flows.listeners.UdpParser;
import org.opennms.horizon.minion.flows.parser.factory.DnsResolver;
import org.opennms.horizon.minion.flows.parser.ie.RecordProvider;
import org.opennms.horizon.minion.flows.parser.session.Session;
import org.opennms.horizon.minion.flows.parser.session.UdpSessionManager;
import org.opennms.horizon.shared.ipc.rpc.IpcIdentity;
import org.opennms.horizon.shared.ipc.sink.api.AsyncDispatcher;

public abstract class UdpParserBase extends ParserBase implements UdpParser {
    public static final long HOUSEKEEPING_INTERVAL = 60000;

    private final Meter packetsReceived;
    private final Counter parserErrors;

    @Getter
    private UdpSessionManager sessionManager;

    private ScheduledFuture<?> housekeepingFuture;
    private final Duration templateTimeout = Duration.ofMinutes(30);

    public UdpParserBase(
            final Protocol protocol,
            final String name,
            final AsyncDispatcher<FlowDocument> dispatcher,
            final IpcIdentity identity,
            final DnsResolver dnsResolver,
            final MetricRegistry metricRegistry) {
        super(protocol, name, dispatcher, identity, dnsResolver, metricRegistry);

        this.packetsReceived = metricRegistry.meter(MetricRegistry.name("parsers", name, "packetsReceived"));
        this.parserErrors = metricRegistry.counter(MetricRegistry.name("parsers", name, "parserErrors"));

        String sessionCountGauge = MetricRegistry.name("parsers", name, "sessionCount");

        // Register only if it's not already there in the registry.
        if (!metricRegistry.getGauges().containsKey(sessionCountGauge)) {
            metricRegistry.register(sessionCountGauge, (Gauge<Integer>)
                    () -> (this.sessionManager != null) ? this.sessionManager.count() : null);
        }
    }

    protected abstract RecordProvider parse(final Session session, final ByteBuf buffer) throws Exception;

    protected abstract UdpSessionManager.SessionKey buildSessionKey(
            final InetSocketAddress remoteAddress, final InetSocketAddress localAddress);

    public final CompletableFuture<?> parse(
            final ByteBuf buffer, final InetSocketAddress remoteAddress, final InetSocketAddress localAddress)
            throws Exception {
        this.packetsReceived.mark();

        final UdpSessionManager.SessionKey sessionKey = this.buildSessionKey(remoteAddress, localAddress);
        final Session session = this.sessionManager.getSession(sessionKey);

        try {
            return this.transmit(this.parse(session, buffer), session, remoteAddress);
        } catch (Exception e) {
            this.sessionManager.drop(sessionKey);
            this.parserErrors.inc();
            throw e;
        }
    }

    @Override
    public void start(final ScheduledExecutorService executorService) {
        super.start(executorService);
        this.sessionManager = new UdpSessionManager(this.templateTimeout, this::sequenceNumberTracker);
        this.housekeepingFuture = executorService.scheduleAtFixedRate(
                this.sessionManager::doHousekeeping,
                HOUSEKEEPING_INTERVAL,
                HOUSEKEEPING_INTERVAL,
                TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        if (housekeepingFuture != null) {
            this.housekeepingFuture.cancel(false);
        }
        super.stop();
    }

    @Override
    public Object dumpInternalState() {
        return this.sessionManager.dumpInternalState();
    }
}
