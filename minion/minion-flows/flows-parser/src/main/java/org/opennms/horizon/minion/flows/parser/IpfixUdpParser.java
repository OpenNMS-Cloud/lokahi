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
import static org.opennms.horizon.minion.flows.listeners.utils.BufferUtils.uint16;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import io.netty.buffer.ByteBuf;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import org.opennms.horizon.flows.document.FlowDocument;
import org.opennms.horizon.minion.flows.listeners.Dispatchable;
import org.opennms.horizon.minion.flows.listeners.UdpParser;
import org.opennms.horizon.minion.flows.parser.factory.DnsResolver;
import org.opennms.horizon.minion.flows.parser.ie.RecordProvider;
import org.opennms.horizon.minion.flows.parser.ipfix.proto.Header;
import org.opennms.horizon.minion.flows.parser.ipfix.proto.Packet;
import org.opennms.horizon.minion.flows.parser.session.Session;
import org.opennms.horizon.minion.flows.parser.session.UdpSessionManager;
import org.opennms.horizon.minion.flows.parser.transport.IpFixMessageBuilder;
import org.opennms.horizon.shared.ipc.rpc.IpcIdentity;
import org.opennms.horizon.shared.ipc.sink.api.AsyncDispatcher;
import org.opennms.horizon.shared.utils.InetAddressUtils;

public class IpfixUdpParser extends UdpParserBase implements UdpParser, Dispatchable {

    private final IpFixMessageBuilder messageBuilder = new IpFixMessageBuilder();

    public IpfixUdpParser(
            final String name,
            final AsyncDispatcher<FlowDocument> dispatcher,
            final IpcIdentity identity,
            final DnsResolver dnsResolver,
            final MetricRegistry metricRegistry) {
        super(Protocol.IPFIX, name, dispatcher, identity, dnsResolver, metricRegistry);
    }

    public IpFixMessageBuilder getMessageBuilder() {
        return this.messageBuilder;
    }

    @Override
    protected RecordProvider parse(final Session session, final ByteBuf buffer) throws Exception {
        final Header header = new Header(slice(buffer, Header.SIZE));
        final Packet packet = new Packet(session, header, slice(buffer, header.payloadLength()));

        detectClockSkew(header.exportTime * 1000L, session.getRemoteAddress());

        return packet;
    }

    @Override
    public boolean handles(final ByteBuf buffer) {
        return uint16(buffer) == Header.VERSION;
    }

    @Override
    protected UdpSessionManager.SessionKey buildSessionKey(
            final InetSocketAddress remoteAddress, final InetSocketAddress localAddress) {
        return new SessionKey(remoteAddress, localAddress);
    }

    public static class SessionKey implements UdpSessionManager.SessionKey {
        private final InetSocketAddress remoteAddress;
        private final InetSocketAddress localAddress;

        public SessionKey(final InetSocketAddress remoteAddress, final InetSocketAddress localAddress) {
            this.remoteAddress = remoteAddress;
            this.localAddress = localAddress;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final SessionKey that = (SessionKey) o;
            return Objects.equal(this.localAddress, that.localAddress)
                    && Objects.equal(this.remoteAddress, that.remoteAddress);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(this.localAddress, this.remoteAddress);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("remoteAddress", remoteAddress)
                    .add("localAddress", localAddress)
                    .toString();
        }

        @Override
        public String getDescription() {
            return String.format(
                    "%s:%s", InetAddressUtils.str(this.remoteAddress.getAddress()), this.remoteAddress.getPort());
        }

        @Override
        public InetAddress getRemoteAddress() {
            return this.remoteAddress.getAddress();
        }
    }
}
