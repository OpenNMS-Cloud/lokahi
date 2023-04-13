package org.opennms.horizon.events.traps.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opennms.horizon.events.proto.EventLog;
import org.opennms.horizon.events.traps.EventXmlToProtoMapper;
import org.opennms.horizon.events.xml.Event;
import org.opennms.horizon.events.xml.Events;
import org.opennms.horizon.events.xml.Log;

import static org.junit.jupiter.api.Assertions.*;

public class EventLogXmlToProtoMapperImplTest {

    private EventLogXmlToProtoMapperImpl target;

    private EventXmlToProtoMapper mockEventXmlToProtoMapper;

    @BeforeEach
    public void setUp() {
        mockEventXmlToProtoMapper = Mockito.mock(EventXmlToProtoMapper.class);

        target = new EventLogXmlToProtoMapperImpl();

        target.setEventXmlToProtoMapper(mockEventXmlToProtoMapper);
    }

    @Test
    void convertToProtoEvents() {
        //
        // Setup Test Data and Interactions
        //
        Log testEventLog = new Log();
        Events testEvents = new Events();
        testEventLog.setEvents(testEvents);

        Event testEvent = new Event();
        testEvents.addEvent(testEvent);

        org.opennms.horizon.events.proto.Event testProtoEvent =
            org.opennms.horizon.events.proto.Event.newBuilder()
                    .build();

        Mockito.when(mockEventXmlToProtoMapper.convert(testEvent, "x-tenant-id-x")).thenReturn(testProtoEvent);

        //
        // Execute
        //
        EventLog result = target.convert(testEventLog, "x-tenant-id-x");

        //
        // Verify the Results
        //
        assertEquals(1, result.getEventsCount());
        assertSame(testProtoEvent, result.getEvents(0));
    }
}
