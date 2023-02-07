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

package org.opennms.horizon.minion.flows.adapter.ipfix;

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
import org.opennms.horizon.minion.flows.parser.ipfix.proto.Header;
import org.opennms.horizon.minion.flows.parser.ipfix.proto.Packet;
import org.opennms.horizon.minion.flows.parser.session.SequenceNumberTracker;
import org.opennms.horizon.minion.flows.parser.session.Session;
import org.opennms.horizon.minion.flows.parser.session.TcpSession;
import org.opennms.horizon.minion.flows.parser.transport.IpFixMessageBuilder;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;


/**
 * This test validates netflow protobuf values against json output.
 */
public class IpFixProtobufValidationTest {

    @Test
    public void canValidateIpFixFlowsWithJsonOutput() {
        // Generate flows from existing packet payloads
        List<Flow> flows = getFlowsForPayloadsInSession("/flows/ipfix_test_1.dat",
            "/flows/ipfix_test_2.dat");
        assertEquals(8, flows.size());
        List<Flow> jsonData = Utils.getJsonFlowFromResources(Instant.now(),
            "/flows/ipfix_test_1.json",
            "/flows/ipfix_test_2.json");
        assertEquals(8, jsonData.size());
        for (int i = 0; i < 8; i++) {
            assertEquals(flows.get(i).getFlowSeqNum(), jsonData.get(i).getFlowSeqNum());
            assertEquals(flows.get(i).getFlowRecords(), jsonData.get(i).getFlowRecords());
            assertEquals(flows.get(i).getTimestamp(), jsonData.get(i).getTimestamp());
            assertEquals(flows.get(i).getBytes(), jsonData.get(i).getBytes());
            Direction direction = jsonData.get(i).getDirection() != null ? jsonData.get(i).getDirection() : Direction.INGRESS;
            assertEquals(flows.get(i).getDirection(), direction);
            assertEquals(flows.get(i).getFirstSwitched(), jsonData.get(i).getFirstSwitched());
            assertEquals(flows.get(i).getLastSwitched(), jsonData.get(i).getLastSwitched());
            assertEquals(flows.get(i).getDeltaSwitched(), jsonData.get(i).getDeltaSwitched());
            assertEquals(flows.get(i).getDstAddr(), jsonData.get(i).getDstAddr());
            assertEquals(flows.get(i).getDstAs(), jsonData.get(i).getDstAs());
            assertEquals(flows.get(i).getDstPort(), jsonData.get(i).getDstPort());
            assertEquals(flows.get(i).getDstMaskLen(), jsonData.get(i).getDstMaskLen());
            assertEquals(flows.get(i).getDstAddrHostname(), jsonData.get(i).getDstAddrHostname());
            assertEquals(flows.get(i).getSrcAddr(), jsonData.get(i).getSrcAddr());
            assertEquals(flows.get(i).getSrcAs(), jsonData.get(i).getSrcAs());
            assertEquals(flows.get(i).getSrcPort(), jsonData.get(i).getSrcPort());
            assertEquals(flows.get(i).getSrcAddrHostname(), jsonData.get(i).getSrcAddrHostname());
            assertEquals(flows.get(i).getSrcMaskLen(), jsonData.get(i).getSrcMaskLen());
            assertEquals(flows.get(i).getNextHop(), jsonData.get(i).getNextHop());
            assertEquals(flows.get(i).getInputSnmp(), jsonData.get(i).getInputSnmp());
            assertEquals(flows.get(i).getOutputSnmp(), jsonData.get(i).getOutputSnmp());
            assertEquals(flows.get(i).getNetflowVersion(), jsonData.get(i).getNetflowVersion());
            assertEquals(flows.get(i).getTcpFlags(), jsonData.get(i).getTcpFlags());
            assertEquals(flows.get(i).getProtocol(), jsonData.get(i).getProtocol());
            assertEquals(flows.get(i).getTos(), jsonData.get(i).getTos());
            assertEquals(flows.get(i).getEngineId(), jsonData.get(i).getEngineId());
            assertEquals(flows.get(i).getEngineType(), jsonData.get(i).getEngineType());
            assertEquals(flows.get(i).getPackets(), jsonData.get(i).getPackets());
            assertEquals(flows.get(i).getSamplingAlgorithm(), jsonData.get(i).getSamplingAlgorithm());
            assertEquals(flows.get(i).getSamplingInterval(), jsonData.get(i).getSamplingInterval());
            assertEquals(flows.get(i).getIpProtocolVersion(), jsonData.get(i).getIpProtocolVersion());
            assertEquals(flows.get(i).getVlan(), jsonData.get(i).getVlan());
            assertEquals(flows.get(i).getNodeIdentifier(), jsonData.get(i).getNodeIdentifier());

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
                final Packet packet = new Packet(session, header, slice(buffer, header.payloadLength()));
                packet.getRecords().forEach(rec -> {
                    final FlowMessage flowMessage = new IpFixMessageBuilder().buildMessage(rec, (address) -> Optional.empty()).build();
                    flows.add(new NetflowMessage(flowMessage, Instant.now()));
                });
            } catch (InvalidPacketException e) {
                throw new RuntimeException(e);
            }
        }

        return flows;
    }
}
