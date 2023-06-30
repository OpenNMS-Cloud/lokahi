/*
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
 *
 */

package org.opennms.horizon.events.traps;

import com.google.protobuf.InvalidProtocolBufferException;
import nl.altindag.log.LogCaptor;
import nl.altindag.log.model.LogEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.horizon.events.proto.Event;
import org.opennms.horizon.events.proto.EventLog;
import org.opennms.horizon.events.xml.Events;
import org.opennms.horizon.events.xml.Log;
import org.opennms.horizon.grpc.traps.contract.TenantLocationSpecificTrapLogDTO;
import org.opennms.horizon.grpc.traps.contract.TrapDTO;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TrapsConsumerTest {

    private EventForwarder mockEventForwarder;
    private Function<String, InetAddress> mockInetAddressLookupFunction;
    private TrapLogProtoToEventLogXmlMapper mockTrapLogProtoToXmlMapper;
    private EventLogXmlToProtoMapper mockEventLogXmlToProtoMapper;

    private TenantLocationSpecificTrapLogDTO baseTestTrapLogDTO;
    private InetAddress testInetAddress;

    private TrapsConsumer target;

    @BeforeEach
    public void setUp() {
        mockEventForwarder = Mockito.mock(EventForwarder.class);
        mockInetAddressLookupFunction = Mockito.mock(Function.class);
        mockTrapLogProtoToXmlMapper = Mockito.mock(TrapLogProtoToEventLogXmlMapper.class);
        mockEventLogXmlToProtoMapper = Mockito.mock(EventLogXmlToProtoMapper.class);

        baseTestTrapLogDTO =
            TenantLocationSpecificTrapLogDTO.newBuilder()
                .setTenantId("x-tenant-id-x")
                .setLocationId("x-location-x")
                .setTrapAddress("x-trap-address-x")
                .build();

        testInetAddress = Mockito.mock(InetAddress.class);

        Mockito.when(mockInetAddressLookupFunction.apply("x-trap-address-x")).thenReturn(testInetAddress);

        target = new TrapsConsumer();
        target.setEventForwarder(mockEventForwarder);
        target.setTrapLogProtoToXmlMapper(mockTrapLogProtoToXmlMapper);
        target.setEventLogXmlToProtoMapper(mockEventLogXmlToProtoMapper);
    }

    @Test
    void testConsumeNoNewSuspectEvent() {
        //
        // Setup Test Data and Interactions
        //
        TrapDTO suspectTrapDTO =
            TrapDTO.newBuilder()
                .setAgentAddress("x-agent-address-x")
                .build();

        TenantLocationSpecificTrapLogDTO testTrapLogDTO =
            baseTestTrapLogDTO.toBuilder()
                .addTrapDTO(suspectTrapDTO)
                .build();

        Log testXmlEventLog = new Log();
        Events events = new Events();
        testXmlEventLog.setEvents(events);

        Event testEvent =
            Event.newBuilder()
                .setNodeId(1)
                .build();

        EventLog testProtoEventLog =
            EventLog.newBuilder()
                .addEvents(testEvent)
                .build();

        Mockito.when(mockTrapLogProtoToXmlMapper.convert(testTrapLogDTO)).thenReturn(testXmlEventLog);
        Mockito.when(mockEventLogXmlToProtoMapper.convert(testXmlEventLog, "x-tenant-id-x")).thenReturn(testProtoEventLog);

        byte[] testKafkaPayload = testTrapLogDTO.toByteArray();

        //
        // Execute
        //
        target.consume(testKafkaPayload);

        //
        // Verify the Results
        //
        Mockito.verify(mockEventForwarder).sendTrapEvents(Mockito.any(EventLog.class));
        Mockito.verify(mockEventForwarder, Mockito.times(0)).sendInternalEvent(Mockito.any(Event.class));
    }

    @Test
    void testConsumeNewSuspectEvent() {
        //
        // Setup Test Data and Interactions
        //
        TrapDTO suspectTrapDTO =
            TrapDTO.newBuilder()
                .setAgentAddress("x-agent-address-x")
                .build();

        TenantLocationSpecificTrapLogDTO testTrapLogDTO =
            baseTestTrapLogDTO.toBuilder()
                .setTenantId("x-tenant-id-x")
                .setLocationId("x-location-x")
                .setIdentity(
                    Identity.newBuilder()
                        .setSystemId("x-system-id-x")
                        .build()
                )
                .addTrapDTO(suspectTrapDTO)
                .build();

        org.opennms.horizon.events.xml.Event testXmlEvent = new org.opennms.horizon.events.xml.Event();
        testXmlEvent.setNodeid(-1L);
        testXmlEvent.setUei("x-uei-x");
        testXmlEvent.setCreationTime(new Date());
        testXmlEvent.setDistPoller("x-dist-poller-x");
        testXmlEvent.setInterface("1.2.3.4");

        Log testXmlEventLog = new Log();
        Events events = new Events();
        testXmlEventLog.setEvents(events);
        events.addEvent(testXmlEvent);

        Event testEvent =
            Event.newBuilder()
                .setNodeId(-1)
                .build();

        EventLog testProtoEventLog =
            EventLog.newBuilder()
                .addEvents(testEvent)
                .build();

        Mockito.when(mockTrapLogProtoToXmlMapper.convert(testTrapLogDTO)).thenReturn(testXmlEventLog);
        Mockito.when(mockEventLogXmlToProtoMapper.convert(testXmlEventLog, "x-tenant-id-x")).thenReturn(testProtoEventLog);

        byte[] testKafkaPayload = testTrapLogDTO.toByteArray();


        //
        // Execute
        //
        target.consume(testKafkaPayload);

        //
        // Verify the Results
        //
        Mockito.verify(mockEventForwarder).sendTrapEvents(Mockito.any(EventLog.class));
        Mockito.verify(mockEventForwarder).sendInternalEvent(Mockito.any(Event.class));
    }

    @Test
    void testExceptionOnParseProto() {
        //
        // Setup Test Data and Interactions
        //
        byte[] testKafkaPayload = "BAD-PROTO".getBytes(StandardCharsets.UTF_8);


        try (var logCaptor = LogCaptor.forClass(TrapsConsumer.class)) {
            //
            // Execute
            //
           target.consume(testKafkaPayload);

            //
            // Verify the Results
            //
            Predicate<LogEvent> matcher =
                (logEvent) -> (
                        Objects.equals("Error while parsing traps", logEvent.getMessage()) &&
                        (logEvent.getArguments().size() == 0) &&
                        ( logEvent.getThrowable().orElse(null) instanceof InvalidProtocolBufferException)
                    );

            assertTrue(logCaptor.getLogEvents().stream().anyMatch(matcher));
        }
    }

}
