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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
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
import org.opennms.horizon.minion.flows.parser.netflow9.proto.Header;
import org.opennms.horizon.minion.flows.parser.netflow9.proto.Packet;
import org.opennms.horizon.minion.flows.parser.session.SequenceNumberTracker;
import org.opennms.horizon.minion.flows.parser.session.Session;
import org.opennms.horizon.minion.flows.parser.session.TcpSession;

public class PayloadTest {

    @Test
    public void outputPayloadTest() {
        execute("/flows/nf9_broken.dat", buffer -> {
            try {
                final Session session =
                        new TcpSession(InetAddress.getLoopbackAddress(), () -> new SequenceNumberTracker(32));
                final Header h1 = new Header(slice(buffer, Header.SIZE));
                new Packet(session, h1, buffer);
            } catch (final Exception e) {
                assertTrue(e instanceof InvalidPacketException);
                assertTrue(e.getMessage().contains("Invalid template ID: 8, Offset: [0x001E], Payload:"));
                assertTrue(e.getMessage()
                        .contains("|00000000| 00 09 00 01 23 bc 9f 78 5f 1e 2e 03 05 cc 4e f2 |....#..x_.....N.|"));
                assertTrue(e.getMessage()
                        .contains("|00000070| 00 12 00 04 00 3d 00 01                         |.....=..        |"));
                return;
            }
            fail();
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
