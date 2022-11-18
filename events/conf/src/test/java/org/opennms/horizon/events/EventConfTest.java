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

package org.opennms.horizon.events;

import org.junit.jupiter.api.Test;
import org.opennms.horizon.events.api.EventBuilder;
import org.opennms.horizon.events.conf.xml.Event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class EventConfTest {


    @Test
    public void testEventConf() {
        DefaultEventConfDao eventConfDao = new DefaultEventConfDao();
        eventConfDao.init();
        var ueis = eventConfDao.getEventUEIs();
        assertFalse(ueis.isEmpty(), "Should have loaded some ueis");
        String uei = "uei.opennms.org/generic/traps/SNMP_Cold_Start";
        EventBuilder eb = new EventBuilder(uei, "JUnit");
        Event event = eventConfDao.findByEvent(eb.getEvent());
        assertNotNull(event);
        assertEquals(uei, event.getUei());
        assertEquals("Normal", event.getSeverity());
    }
}
