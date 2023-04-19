/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2023 The OpenNMS Group, Inc.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.opennms.horizon.minion.flows.listeners.utils.BufferUtils.slice;

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

import com.google.common.base.Throwables;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class ParserTest {

    private static final String IP_FIX_RESOURCE = "/flows/ipfix.dat";

    @Test
    public void canReadValidIPFIX() {
        execute(IP_FIX_RESOURCE, buffer -> {
            try {

                final Session session = new TcpSession(InetAddress.getLoopbackAddress(), () -> new SequenceNumberTracker(32));

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
