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

import static org.opennms.horizon.minion.flows.listeners.utils.BufferUtils.slice;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.Sets;
import io.netty.buffer.ByteBuf;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.opennms.horizon.flows.document.FlowDocument;
import org.opennms.horizon.minion.flows.listeners.TcpParser;
import org.opennms.horizon.minion.flows.parser.factory.DnsResolver;
import org.opennms.horizon.minion.flows.parser.ipfix.proto.Header;
import org.opennms.horizon.minion.flows.parser.ipfix.proto.Packet;
import org.opennms.horizon.minion.flows.parser.session.TcpSession;
import org.opennms.horizon.minion.flows.parser.state.ParserState;
import org.opennms.horizon.minion.flows.parser.transport.IpFixMessageBuilder;
import org.opennms.horizon.shared.ipc.rpc.IpcIdentity;
import org.opennms.horizon.shared.ipc.sink.api.AsyncDispatcher;

public class IpfixTcpParser extends ParserBase implements TcpParser {

    private final IpFixMessageBuilder messageBuilder = new IpFixMessageBuilder();

    private final Set<TcpSession> sessions = Sets.newConcurrentHashSet();

    public IpfixTcpParser(
            final String name,
            final AsyncDispatcher<FlowDocument> dispatcher,
            final IpcIdentity identity,
            final DnsResolver dnsResolver,
            final MetricRegistry metricRegistry) {
        super(Protocol.IPFIX, name, dispatcher, identity, dnsResolver, metricRegistry);
    }

    @Override
    public IpFixMessageBuilder getMessageBuilder() {
        return this.messageBuilder;
    }

    @Override
    public Handler accept(final InetSocketAddress remoteAddress, final InetSocketAddress localAddress) {
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

    @Override
    public Object dumpInternalState() {
        final ParserState.Builder parser = ParserState.builder();

        this.sessions.stream().flatMap(TcpSession::dumpInternalState).forEach(parser::withExporter);

        return parser.build();
    }
}
