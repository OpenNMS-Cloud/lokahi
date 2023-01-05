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

package parser;

import static listeners.utils.BufferUtils.slice;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.opennms.horizon.grpc.telemetry.contract.TelemetryMessage;
import org.opennms.horizon.shared.ipc.sink.api.AsyncDispatcher;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.Sets;
import com.google.protobuf.Message;

import io.netty.buffer.ByteBuf;
import listeners.TcpParser;
import listeners.factory.UdpListenerMessage;
import parser.factory.DnsResolver;
import parser.factory.Identity;
import parser.ipfix.proto.Header;
import parser.ipfix.proto.Packet;
import parser.session.TcpSession;
import parser.state.ParserState;
import parser.transport.IpFixMessageBuilder;

public class IpfixTcpParser extends ParserBase implements TcpParser {

    private final IpFixMessageBuilder messageBuilder = new IpFixMessageBuilder();

    private final Set<TcpSession> sessions = Sets.newConcurrentHashSet();

    public IpfixTcpParser(final String name,
                          final AsyncDispatcher<TelemetryMessage> dispatcher,
                          final Identity identity,
                          final DnsResolver dnsResolver,
                          final MetricRegistry metricRegistry) {
        super(Protocol.IPFIX, name, dispatcher, identity, dnsResolver, metricRegistry);
    }

    @Override
    public IpFixMessageBuilder getMessageBuilder() {
        return this.messageBuilder;
    }

    @Override
    public Handler accept(final InetSocketAddress remoteAddress,
                          final InetSocketAddress localAddress) {
        final TcpSession session = new TcpSession(remoteAddress.getAddress(), this::sequenceNumberTracker);

        return new Handler() {
            @Override
            public Optional<CompletableFuture<?>> parse(final ByteBuf buffer) throws Exception {
                buffer.markReaderIndex();

                final Header header;
                if (buffer.isReadable(Header.SIZE)) {
                    header = new Header(slice(buffer, Header.SIZE));
                } else {
                    buffer.resetReaderIndex();
                    return Optional.empty();
                }

                final Packet packet;
                if (buffer.isReadable(header.payloadLength())) {
                    packet = new Packet(session, header, slice(buffer, header.payloadLength()));
                } else {
                    buffer.resetReaderIndex();
                    return Optional.empty();
                }

                detectClockSkew(header.exportTime * 1000L, session.getRemoteAddress());

                return Optional.of(IpfixTcpParser.this.transmit(packet, session, remoteAddress));
            }

            @Override
            public void active() {
                sessions.add(session);
            }

            @Override
            public void inactive() {
                sessions.remove(session);
            }
        };
    }

    public Long getFlowActiveTimeoutFallback() {
        return this.messageBuilder.getFlowActiveTimeoutFallback();
    }

    public void setFlowActiveTimeoutFallback(final Long flowActiveTimeoutFallback) {
        this.messageBuilder.setFlowActiveTimeoutFallback(flowActiveTimeoutFallback);
    }

    public Long getFlowInactiveTimeoutFallback() {
        return this.messageBuilder.getFlowInactiveTimeoutFallback();
    }

    public void setFlowInactiveTimeoutFallback(final Long flowInactiveTimeoutFallback) {
        this.messageBuilder.setFlowInactiveTimeoutFallback(flowInactiveTimeoutFallback);
    }

    public Long getFlowSamplingIntervalFallback() {
        return this.messageBuilder.getFlowSamplingIntervalFallback();
    }

    public void setFlowSamplingIntervalFallback(final Long flowSamplingIntervalFallback) {
        this.messageBuilder.setFlowSamplingIntervalFallback(flowSamplingIntervalFallback);
    }

    @Override
    public Object dumpInternalState() {
        final ParserState.Builder parser = ParserState.builder();

        this.sessions.stream()
                     .flatMap(TcpSession::dumpInternalState)
                     .forEach(parser::withExporter);

        return parser.build();
    }
}
