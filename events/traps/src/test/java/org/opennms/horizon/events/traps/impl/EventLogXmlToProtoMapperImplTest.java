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
package org.opennms.horizon.events.traps.impl;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opennms.horizon.events.proto.EventLog;
import org.opennms.horizon.events.traps.EventXmlToProtoMapper;
import org.opennms.horizon.events.xml.Event;
import org.opennms.horizon.events.xml.Events;
import org.opennms.horizon.events.xml.Log;

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
                org.opennms.horizon.events.proto.Event.newBuilder().build();

        Mockito.when(mockEventXmlToProtoMapper.convert(testEvent, "x-tenant-id-x"))
                .thenReturn(testProtoEvent);

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
