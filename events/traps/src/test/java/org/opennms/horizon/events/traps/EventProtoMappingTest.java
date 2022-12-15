/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
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

package org.opennms.horizon.events.traps;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opennms.horizon.events.DefaultEventConfDao;
import org.opennms.horizon.events.api.EventBuilder;
import org.opennms.horizon.events.proto.EventSeverity;
import org.opennms.horizon.events.xml.Event;

class EventProtoMappingTest {


    @Test
    void testEventSeverityMapping() {
        Event event = Mockito.mock(Event.class);
        Mockito.when(event.getSeverity()).thenReturn("Cleared");
        var builder = org.opennms.horizon.events.proto.Event.newBuilder();
        TrapsConsumer.mapSeverity(event, builder);
        Assertions.assertEquals(EventSeverity.CLEARED, builder.build().getEventSeverity());
    }

    @Test
    void testAlarmDataMapping() {

        DefaultEventConfDao defaultEventConfDao = new DefaultEventConfDao();
        defaultEventConfDao.init();
        String uei = "uei.opennms.org/translator/traps/SNMP_Link_Down";
        EventBuilder eb = new EventBuilder(uei, "JUnit");
        var matchingEvent = defaultEventConfDao.findByEvent(eb.getEvent());
        var event = eb.getEvent();
        EventFactory.expandEventWithAlarmData(event, matchingEvent);
        var builder = org.opennms.horizon.events.proto.Event.newBuilder();
        TrapsConsumer.mapAlarmData(event, builder);
        var eventProto = builder.build();
        Assertions.assertNotNull(eventProto.getAlarmData());

    }
}
