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
import io.netty.buffer.ByteBuf;
import java.net.InetSocketAddress;
import org.opennms.horizon.flows.document.FlowDocument;
import org.opennms.horizon.minion.flows.listeners.Dispatchable;
import org.opennms.horizon.minion.flows.listeners.UdpParser;
import org.opennms.horizon.minion.flows.listeners.utils.BufferUtils;
import org.opennms.horizon.minion.flows.parser.factory.DnsResolver;
import org.opennms.horizon.minion.flows.parser.ie.RecordProvider;
import org.opennms.horizon.minion.flows.parser.proto.Header;
import org.opennms.horizon.minion.flows.parser.proto.Packet;
import org.opennms.horizon.minion.flows.parser.session.Session;
import org.opennms.horizon.minion.flows.parser.session.UdpSessionManager;
import org.opennms.horizon.minion.flows.parser.transport.Netflow5MessageBuilder;
import org.opennms.horizon.shared.ipc.rpc.IpcIdentity;
import org.opennms.horizon.shared.ipc.sink.api.AsyncDispatcher;

public class Netflow5UdpParser extends UdpParserBase implements UdpParser, Dispatchable {

    private final Netflow5MessageBuilder messageBuilder = new Netflow5MessageBuilder();

    public Netflow5UdpParser(
            final String name,
            final AsyncDispatcher<FlowDocument> dispatcher,
            final IpcIdentity identity,
            final DnsResolver dnsResolver,
            final MetricRegistry metricRegistry) {
        super(Protocol.NETFLOW5, name, dispatcher, identity, dnsResolver, metricRegistry);
    }

    public Netflow5MessageBuilder getMessageBuilder() {
        return this.messageBuilder;
    }

    @Override
    public boolean handles(final ByteBuf buffer) {
        return BufferUtils.uint16(buffer) == 0x0005;
    }

    @Override
    protected RecordProvider parse(final Session session, final ByteBuf buffer) throws Exception {
        final Header header = new Header(slice(buffer, Header.SIZE));
        final Packet packet = new Packet(header, buffer);

        detectClockSkew(header.unixSecs * 1000L + header.unixNSecs / 1000L, session.getRemoteAddress());

        return packet;
    }

    @Override
    protected UdpSessionManager.SessionKey buildSessionKey(
            final InetSocketAddress remoteAddress, final InetSocketAddress localAddress) {
        return new Netflow9UdpParser.SessionKey(remoteAddress.getAddress(), localAddress);
    }
}
