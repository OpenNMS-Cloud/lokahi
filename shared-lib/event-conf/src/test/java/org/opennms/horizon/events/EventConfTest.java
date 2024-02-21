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
package org.opennms.horizon.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.opennms.horizon.events.api.EventBuilder;
import org.opennms.horizon.events.conf.xml.Event;

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
