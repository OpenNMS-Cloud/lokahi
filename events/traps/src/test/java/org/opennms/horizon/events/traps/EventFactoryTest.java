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
package org.opennms.horizon.events.traps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import java.net.InetAddress;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opennms.horizon.events.api.EventConfDao;
import org.opennms.horizon.events.conf.xml.LogDestType;
import org.opennms.horizon.events.grpc.client.InventoryClient;
import org.opennms.horizon.events.xml.Event;
import org.opennms.horizon.grpc.traps.contract.TrapDTO;
import org.opennms.horizon.shared.snmp.SnmpHelper;

@ExtendWith(MockitoExtension.class)
public class EventFactoryTest {

    @InjectMocks
    EventFactory eventFactory;

    @Mock
    EventConfDao eventConfDao;

    @Mock
    SnmpHelper snmpHelper;

    @Mock
    InventoryClient inventoryClient;

    @Test
    public void testEventWithNoConfig() throws Exception {
        Event e = eventFactory.createEventFrom(
                TrapDTO.newBuilder().build(), "systemId", "location", InetAddress.getByName("127.0.0.1"), "tid");

        assertEquals("uei.opennms.org/default/trap", e.getUei());
        assertNull(e.getDescr());
        assertNull(e.getLogmsg());
    }

    @Test
    public void testEventWithConfig() throws Exception {
        org.opennms.horizon.events.conf.xml.Logmsg eventLogmsg = new org.opennms.horizon.events.conf.xml.Logmsg();
        eventLogmsg.setContent("A real event log message");
        eventLogmsg.setNotify(true);
        eventLogmsg.setDest(LogDestType.LOGNDISPLAY);

        org.opennms.horizon.events.conf.xml.Event eventConf = new org.opennms.horizon.events.conf.xml.Event();
        eventConf.setUei("uei.opennms.org/generic/traps/realone");
        eventConf.setDescr("A real event configuration");
        eventConf.setLogmsg(eventLogmsg);

        Mockito.when(eventConfDao.findByEvent(any())).thenReturn(eventConf);
        Event e = eventFactory.createEventFrom(
                TrapDTO.newBuilder().build(), "systemId", "location", InetAddress.getByName("127.0.0.1"), "tid");

        assertEquals("uei.opennms.org/generic/traps/realone", e.getUei());
        assertEquals("A real event configuration", e.getDescr());
        assertEquals("A real event log message", e.getLogmsg().getContent());
        assertEquals(LogDestType.LOGNDISPLAY.name(), e.getLogmsg().getDest());
        assertTrue(e.getLogmsg().getNotify());
    }
}
