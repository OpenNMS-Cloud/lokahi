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

package org.opennms.horizon.minion.flows.adapter.netflow9;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.opennms.horizon.minion.flows.listeners.utils.BufferUtils.slice;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.opennms.horizon.minion.flows.adapter.common.NetflowMessage;
import org.opennms.horizon.minion.flows.adapter.imported.Flow;
import org.opennms.horizon.minion.flows.parser.InvalidPacketException;
import org.opennms.horizon.minion.flows.parser.flowmessage.Direction;
import org.opennms.horizon.minion.flows.parser.flowmessage.FlowMessage;
import org.opennms.horizon.minion.flows.parser.flowmessage.NetflowVersion;
import org.opennms.horizon.minion.flows.parser.netflow9.proto.Header;
import org.opennms.horizon.minion.flows.parser.netflow9.proto.Packet;
import org.opennms.horizon.minion.flows.parser.session.SequenceNumberTracker;
import org.opennms.horizon.minion.flows.parser.session.Session;
import org.opennms.horizon.minion.flows.parser.session.TcpSession;
import org.opennms.horizon.minion.flows.parser.transport.Netflow9MessageBuilder;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class Netflow9ConverterTest {

    @Test
    public void canParseNetflow9Flows() {
        // Generate flows from existing packet payloads
        List<Flow> flows = getFlowsForPayloadsInSession("/flows/netflow9_template.dat", "/flows/netflow9_records.dat");
        assertEquals(5, flows.size());
        // Verify a flow
        Flow flow = flows.get(4);
        assertEquals(flow.getNetflowVersion(), NetflowVersion.V9);
        assertEquals(flow.getSrcAddr(), "10.1.20.85");
        assertEquals(flow.getSrcAddrHostname(), Optional.empty());
        assertEquals(137, flow.getSrcPort().intValue());
        assertEquals(flow.getDstAddr(), "10.1.20.127");
        assertEquals(flow.getDstAddrHostname(), Optional.empty());
        assertEquals(flow.getDstPort().intValue(), 137);
        assertEquals(flow.getProtocol().intValue(), 17); // UDP
        assertEquals(flow.getBytes().intValue(), 156L);
        assertEquals(flow.getInputSnmp().intValue(), 369098754);
        assertEquals(flow.getOutputSnmp().intValue(), 0);
        assertEquals(flow.getFirstSwitched(), Instant.ofEpochMilli(1524773519000L)); // Thu Apr 26 16:11:59 EDT 2018
        assertEquals(flow.getLastSwitched(), Instant.ofEpochMilli(1524773527000L)); // Thu Apr 26 16:12:07 EDT 2018
        assertEquals(flow.getPackets().longValue(), 2L);
        assertEquals(flow.getDirection(), Direction.INGRESS);
        assertEquals(flow.getNextHop(), "0.0.0.0");
        assertEquals(flow.getNextHopHostname(), Optional.empty());
        assertNull(flow.getVlan());
    }

    private List<Flow> getFlowsForPayloadsInSession(String... resources) {
        final List<byte[]> payloads = new ArrayList<>(resources.length);
        for (String resource : resources) {
            URL resourceURL = getClass().getResource(resource);
            try {
                payloads.add(Files.readAllBytes(Paths.get(resourceURL.toURI())));
            } catch (IOException | URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
        return getFlowsForPayloadsInSession(payloads);
    }

    private List<Flow> getFlowsForPayloadsInSession(List<byte[]> payloads) {
        final List<Flow> flows = new ArrayList<>();
        final Session session = new TcpSession(InetAddress.getLoopbackAddress(), () -> new SequenceNumberTracker(32));
        for (byte[] payload : payloads) {
            final ByteBuf buffer = Unpooled.wrappedBuffer(payload);
            final Header header;
            try {
                header = new Header(slice(buffer, Header.SIZE));
                final Packet packet = new Packet(session, header, buffer);
                packet.getRecords().forEach(rec -> {
                    final FlowMessage flowMessage = new Netflow9MessageBuilder().buildMessage(rec, (address) -> Optional.empty()).build();
                    flows.add(new NetflowMessage(flowMessage, Instant.now()));
                });
            } catch (InvalidPacketException e) {
                throw new RuntimeException(e);
            }
        }
        return flows;
    }


}
