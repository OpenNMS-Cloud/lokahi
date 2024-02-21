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
import static org.junit.Assert.assertTrue;
import static org.opennms.horizon.minion.flows.listeners.utils.BufferUtils.slice;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;
import org.opennms.horizon.flows.document.FlowDocument;
import org.opennms.horizon.minion.flows.parser.ie.Value;
import org.opennms.horizon.minion.flows.parser.ie.values.UnsignedValue;
import org.opennms.horizon.minion.flows.parser.netflow9.proto.Header;
import org.opennms.horizon.minion.flows.parser.netflow9.proto.Packet;
import org.opennms.horizon.minion.flows.parser.session.SequenceNumberTracker;
import org.opennms.horizon.minion.flows.parser.session.Session;
import org.opennms.horizon.minion.flows.parser.session.TcpSession;
import org.opennms.horizon.minion.flows.parser.transport.Netflow9MessageBuilder;

public class NMS13006_Test {
    private static final Path FOLDER = Paths.get("src/test/resources/flows");

    @Test
    public void firstAndLastSwitchedTest() throws Exception {
        final RecordEnrichment enrichment = (address -> Optional.empty());
        final List<Value<?>> record = new ArrayList<>();
        record.add(new UnsignedValue("@unixSecs", 1000));
        record.add(new UnsignedValue("@sysUpTime", 1000));
        record.add(new UnsignedValue("FIRST_SWITCHED", 2000));
        record.add(new UnsignedValue("LAST_SWITCHED", 3000));
        final Netflow9MessageBuilder builder = new Netflow9MessageBuilder();
        final FlowDocument flowMessage =
                builder.buildMessage(record, enrichment).build();

        Assert.assertEquals(1001000L, flowMessage.getFirstSwitched().getValue());
        Assert.assertEquals(1002000L, flowMessage.getLastSwitched().getValue());
        Assert.assertEquals(1001000L, flowMessage.getDeltaSwitched().getValue());
    }

    @Test
    public void flowStartAndEndMsTest() throws Exception {
        final RecordEnrichment enrichment = (address -> Optional.empty());
        final List<Value<?>> record = new ArrayList<>();
        record.add(new UnsignedValue("@unixSecs", 1000));
        record.add(new UnsignedValue("@sysUpTime", 1000));
        record.add(new UnsignedValue("flowStartMilliseconds", 2001000));
        record.add(new UnsignedValue("flowEndMilliseconds", 2002000));
        final Netflow9MessageBuilder builder = new Netflow9MessageBuilder();
        final FlowDocument flowMessage =
                builder.buildMessage(record, enrichment).build();

        Assert.assertEquals(2001000L, flowMessage.getFirstSwitched().getValue());
        Assert.assertEquals(2002000L, flowMessage.getLastSwitched().getValue());
        Assert.assertEquals(2001000L, flowMessage.getDeltaSwitched().getValue());
    }

    @Test
    public void captureFileTest() throws Exception {
        testFile("nms-13006.dat");
    }

    public void testFile(final String filename) throws Exception {
        final Session session = new TcpSession(InetAddress.getLoopbackAddress(), () -> new SequenceNumberTracker(32));

        try (final FileChannel channel = FileChannel.open(FOLDER.resolve(filename))) {
            final ByteBuffer buffer = ByteBuffer.allocate((int) channel.size());
            channel.read(buffer);
            buffer.flip();

            final ByteBuf buf = Unpooled.wrappedBuffer(buffer);

            do {
                final Header header = new Header(slice(buf, Header.SIZE));
                final Packet packet = new Packet(session, header, buf);

                final RecordEnrichment enrichment = (address -> Optional.empty());

                packet.getRecords().forEach(r -> {
                    final Netflow9MessageBuilder builder = new Netflow9MessageBuilder();
                    final FlowDocument flowMessage =
                            builder.buildMessage(r, enrichment).build();

                    assertTrue(flowMessage.hasFirstSwitched());
                    assertTrue(flowMessage.hasLastSwitched());
                    assertTrue(flowMessage.hasDeltaSwitched());
                });
                assertEquals(packet.header.versionNumber, 0x0009);
            } while (buf.isReadable());
        }
    }
}
