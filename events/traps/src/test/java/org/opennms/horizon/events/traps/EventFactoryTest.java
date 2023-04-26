package org.opennms.horizon.events.traps;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opennms.horizon.events.api.EventConfDao;
import org.opennms.horizon.events.conf.xml.LogDestType;
import org.opennms.horizon.events.grpc.client.InventoryClient;
import org.opennms.horizon.grpc.traps.contract.TrapDTO;
import org.opennms.horizon.shared.snmp.SnmpHelper;
import  org.opennms.horizon.events.xml.Event;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

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
            TrapDTO.newBuilder().build(),
            "systemId",
            "location",
            InetAddress.getByName("127.0.0.1"),
            "tid"
        );

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
            TrapDTO.newBuilder().build(),
            "systemId",
            "location",
            InetAddress.getByName("127.0.0.1"),
            "tid"
        );

        assertEquals("uei.opennms.org/generic/traps/realone", e.getUei());
        assertEquals("A real event configuration", e.getDescr());
        assertEquals("A real event log message", e.getLogmsg().getContent());
        assertEquals(LogDestType.LOGNDISPLAY.name(), e.getLogmsg().getDest());
        assertTrue(e.getLogmsg().getNotify());
    }


}
