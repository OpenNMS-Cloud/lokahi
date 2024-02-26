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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.opennms.horizon.minion.flows.listeners.utils.BufferUtils.slice;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.function.Consumer;
import org.junit.Test;
import org.opennms.horizon.minion.flows.parser.ipfix.proto.Header;
import org.opennms.horizon.minion.flows.parser.ipfix.proto.Packet;
import org.opennms.horizon.minion.flows.parser.session.SequenceNumberTracker;
import org.opennms.horizon.minion.flows.parser.session.Session;
import org.opennms.horizon.minion.flows.parser.session.TcpSession;

public class ParserTest {

    private static final String IP_FIX_RESOURCE = "/flows/ipfix.dat";

    @Test
    public void canReadValidIPFIX() {
        execute(IP_FIX_RESOURCE, buffer -> {
            try {

                final Session session =
                        new TcpSession(InetAddress.getLoopbackAddress(), () -> new SequenceNumberTracker(32));

                final Header h1 = new Header(slice(buffer, Header.SIZE));
                final Packet p1 = new Packet(session, h1, slice(buffer, h1.length - Header.SIZE));

                assertEquals(0x000a, p1.header.versionNumber);
                assertEquals(0L, p1.header.observationDomainId);
                assertEquals(1431516026L, p1.header.exportTime); // "2015-05-13T11:20:26.000Z"

                final Header h2 = new Header(slice(buffer, Header.SIZE));
                final Packet p2 = new Packet(session, h2, slice(buffer, h2.length - Header.SIZE));

                assertEquals(0x000a, p2.header.versionNumber);
                assertEquals(0L, p2.header.observationDomainId);
                assertEquals(1431516026L, p2.header.exportTime); // "2015-05-13T11:20:26.000Z"

                final Header h3 = new Header(slice(buffer, Header.SIZE));
                final Packet p3 = new Packet(session, h3, slice(buffer, h3.length - Header.SIZE));

                assertEquals(0x000a, p3.header.versionNumber);
                assertEquals(0L, p3.header.observationDomainId);
                assertEquals(1431516028L, p3.header.exportTime); // "2015-05-13T11:20:26.000Z"

                assertFalse(buffer.isReadable());

            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void execute(final String resource, final Consumer<ByteBuf> consumer) {
        Objects.requireNonNull(resource);
        Objects.requireNonNull(consumer);

        final URL resourceURL = getClass().getResource(resource);
        Objects.requireNonNull(resourceURL);

        try {
            try (final FileChannel channel = FileChannel.open(Paths.get(resourceURL.toURI()))) {
                final ByteBuffer buffer = ByteBuffer.allocate((int) channel.size());
                channel.read(buffer);
                buffer.flip();
                consumer.accept(Unpooled.wrappedBuffer(buffer));
            }

        } catch (final URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
