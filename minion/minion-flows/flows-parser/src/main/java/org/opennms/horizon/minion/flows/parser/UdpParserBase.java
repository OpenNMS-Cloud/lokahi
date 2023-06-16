/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2018-2023 The OpenNMS Group, Inc.
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

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.opennms.horizon.flows.document.FlowDocument;
import org.opennms.horizon.minion.flows.listeners.UdpParser;
import org.opennms.horizon.minion.flows.parser.factory.DnsResolver;
import org.opennms.horizon.minion.flows.parser.ie.RecordProvider;
import org.opennms.horizon.minion.flows.parser.session.Session;
import org.opennms.horizon.minion.flows.parser.session.UdpSessionManager;
import org.opennms.horizon.shared.ipc.rpc.IpcIdentity;
import org.opennms.horizon.shared.ipc.sink.api.AsyncDispatcher;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;

import io.netty.buffer.ByteBuf;
import lombok.Getter;

public abstract class UdpParserBase extends ParserBase implements UdpParser {
    public final static long HOUSEKEEPING_INTERVAL = 60000;

    private final Meter packetsReceived;
    private final Counter parserErrors;

    @Getter
    private UdpSessionManager sessionManager;

    private ScheduledFuture<?> housekeepingFuture;
    private final Duration templateTimeout = Duration.ofMinutes(30);

    public UdpParserBase(final Protocol protocol,
                         final String name,
                         final AsyncDispatcher<FlowDocument> dispatcher,
                         final IpcIdentity identity,
                         final DnsResolver dnsResolver,
                         final MetricRegistry metricRegistry) {
        super(protocol, name, dispatcher, identity, dnsResolver, metricRegistry);

        this.packetsReceived = metricRegistry.meter(MetricRegistry.name("parsers",  name, "packetsReceived"));
        this.parserErrors = metricRegistry.counter(MetricRegistry.name("parsers",  name, "parserErrors"));

        String sessionCountGauge = MetricRegistry.name("parsers",  name, "sessionCount");

        // Register only if it's not already there in the registry.
        if (!metricRegistry.getGauges().containsKey(sessionCountGauge)) {
            metricRegistry.register(sessionCountGauge, (Gauge<Integer>) () -> (this.sessionManager != null) ? this.sessionManager.count() : null);
        }
    }

    protected abstract RecordProvider parse(final Session session, final ByteBuf buffer) throws Exception;

    protected abstract UdpSessionManager.SessionKey buildSessionKey(final InetSocketAddress remoteAddress, final InetSocketAddress localAddress);

    public final CompletableFuture<?> parse(final ByteBuf buffer,
                                            final InetSocketAddress remoteAddress,
                                            final InetSocketAddress localAddress) throws Exception {
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
        this.housekeepingFuture = executorService.scheduleAtFixedRate(this.sessionManager::doHousekeeping,
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
