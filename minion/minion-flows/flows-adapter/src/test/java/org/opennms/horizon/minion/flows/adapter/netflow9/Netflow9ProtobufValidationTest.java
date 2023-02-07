/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2020 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2020 The OpenNMS Group, Inc.
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
import org.opennms.horizon.minion.flows.adapter.Utils;
import org.opennms.horizon.minion.flows.adapter.common.NetflowMessage;
import org.opennms.horizon.minion.flows.adapter.imported.Flow;
import org.opennms.horizon.minion.flows.parser.InvalidPacketException;
import org.opennms.horizon.minion.flows.parser.flowmessage.Direction;
import org.opennms.horizon.minion.flows.parser.flowmessage.FlowMessage;
import org.opennms.horizon.minion.flows.parser.netflow9.proto.Header;
import org.opennms.horizon.minion.flows.parser.netflow9.proto.Packet;
import org.opennms.horizon.minion.flows.parser.session.SequenceNumberTracker;
import org.opennms.horizon.minion.flows.parser.session.Session;
import org.opennms.horizon.minion.flows.parser.session.TcpSession;
import org.opennms.horizon.minion.flows.parser.transport.Netflow9MessageBuilder;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;


/**
 * This test validates netflow protobuf values against json output.
 */
public class Netflow9ProtobufValidationTest {

    @Test
    public void canValidateNetflow9FlowsWithJsonOutput() {
        // Generate flows from existing packet payloads
        List<Flow> flows = getFlowsForPayloadsInSession("/flows/netflow9.dat",
                "/flows/netflow9_template.dat",
                "/flows/netflow9_records.dat");

        assertEquals(12, flows.size());
        List<Flow> jsonFlows = Utils.getJsonFlowFromResources(Instant.now(),
                                                              "/flows/netflow9.json",
                                                              "/flows/netflow9_1.json");
        assertEquals(12, jsonFlows.size());
        for (int i = 0; i < 12; i++) {
            assertEquals(flows.get(i).getFlowSeqNum(), jsonFlows.get(i).getFlowSeqNum());
            assertEquals(flows.get(i).getFlowRecords(), jsonFlows.get(i).getFlowRecords());
            assertEquals(flows.get(i).getTimestamp(), jsonFlows.get(i).getTimestamp());
            assertEquals(flows.get(i).getBytes(), jsonFlows.get(i).getBytes());
            Direction direction = jsonFlows.get(i).getDirection() != null ? jsonFlows.get(i).getDirection() : Direction.INGRESS;
            assertEquals(flows.get(i).getDirection(), direction);
            assertEquals(flows.get(i).getFirstSwitched(), jsonFlows.get(i).getFirstSwitched());
            assertEquals(flows.get(i).getLastSwitched(), jsonFlows.get(i).getLastSwitched());
            assertEquals(flows.get(i).getDeltaSwitched(), jsonFlows.get(i).getDeltaSwitched());
            assertEquals(flows.get(i).getDstAddr(), jsonFlows.get(i).getDstAddr());
            assertEquals(flows.get(i).getDstAs(), jsonFlows.get(i).getDstAs());
            assertEquals(flows.get(i).getDstPort(), jsonFlows.get(i).getDstPort());
            assertEquals(flows.get(i).getDstMaskLen(), jsonFlows.get(i).getDstMaskLen());
            assertEquals(flows.get(i).getDstAddrHostname(), jsonFlows.get(i).getDstAddrHostname());
            assertEquals(flows.get(i).getSrcAddr(), jsonFlows.get(i).getSrcAddr());
            assertEquals(flows.get(i).getSrcAs(), jsonFlows.get(i).getSrcAs());
            assertEquals(flows.get(i).getSrcPort(), jsonFlows.get(i).getSrcPort());
            assertEquals(flows.get(i).getSrcAddrHostname(), jsonFlows.get(i).getSrcAddrHostname());
            assertEquals(flows.get(i).getSrcMaskLen(), jsonFlows.get(i).getSrcMaskLen());
            assertEquals(flows.get(i).getNextHop(), jsonFlows.get(i).getNextHop());
            assertEquals(flows.get(i).getInputSnmp(), jsonFlows.get(i).getInputSnmp());
            assertEquals(flows.get(i).getOutputSnmp(), jsonFlows.get(i).getOutputSnmp());
            assertEquals(flows.get(i).getNetflowVersion(), jsonFlows.get(i).getNetflowVersion());
            assertEquals(flows.get(i).getTcpFlags(), jsonFlows.get(i).getTcpFlags());
            assertEquals(flows.get(i).getProtocol(), jsonFlows.get(i).getProtocol());
            assertEquals(flows.get(i).getTos(), jsonFlows.get(i).getTos());
            assertEquals(flows.get(i).getEngineId(), jsonFlows.get(i).getEngineId());
            assertEquals(flows.get(i).getEngineType(), jsonFlows.get(i).getEngineType());
            assertEquals(flows.get(i).getPackets(), jsonFlows.get(i).getPackets());
            assertEquals(flows.get(i).getSamplingAlgorithm(), jsonFlows.get(i).getSamplingAlgorithm());
            assertEquals(flows.get(i).getSamplingInterval(), jsonFlows.get(i).getSamplingInterval());
            assertEquals(flows.get(i).getIpProtocolVersion(), jsonFlows.get(i).getIpProtocolVersion());
            assertEquals(flows.get(i).getVlan(), jsonFlows.get(i).getVlan());
            assertEquals(flows.get(i).getNodeIdentifier(), jsonFlows.get(i).getNodeIdentifier());

        }
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
