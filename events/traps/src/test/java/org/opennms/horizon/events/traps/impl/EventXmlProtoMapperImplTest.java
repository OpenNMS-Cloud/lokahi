package org.opennms.horizon.events.traps.impl;

import nl.altindag.log.LogCaptor;
import nl.altindag.log.model.LogEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.events.xml.Event;
import org.opennms.horizon.events.xml.Parm;
import org.opennms.horizon.events.xml.Snmp;
import org.opennms.horizon.events.xml.Value;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

public class EventXmlProtoMapperImplTest {

    private Event testEvent;

    private EventXmlToProtoMapperImpl target;

    @BeforeEach
    public void setUp() {
        testEvent = new Event();
        testEvent.setUei("x-uei-x");
        testEvent.setCreationTime(new Date());
        testEvent.setNodeid(131313L);
        testEvent.setDistPoller("x-dist-poller-x");
        testEvent.setInterface("1.2.3.4");

        Snmp testSnmp = new Snmp();
        testSnmp.setId("x-snmp-id-x");
        testSnmp.setVersion("x-version-x");
        testSnmp.setGeneric(171717);
        testSnmp.setCommunity("x-community-x");
        testSnmp.setSpecific(232323);
        testSnmp.setTrapOID("x-trap-oid-x");
        testEvent.setSnmp(testSnmp);

        target = new EventXmlToProtoMapperImpl();
    }

    @Test
    void testMapping() {
        //
        // Setup Test Data and Interactions
        //
        Parm testParm = new Parm();
        testParm.setParmName("x-parm-name-x");

        Value testValue = new Value("x-value-x");
        testValue.setEncoding("x-encoding-x");
        testValue.setType("x-type-x");
        testParm.setValue(testValue);

        testEvent.setParmCollection(
            List.of(testParm)
        );

        //
        // Execute
        //
        org.opennms.horizon.events.proto.Event result = target.convert(testEvent, "x-tenant-id-x");

        //
        // Verify the Results
        //
        assertEquals("x-uei-x", result.getUei());
        assertEquals("x-dist-poller-x", result.getLocation());
        assertEquals("1.2.3.4", result.getIpAddress());

        assertNotNull(result.getInfo());
        assertNotNull(result.getInfo().getSnmp());
        assertEquals("x-snmp-id-x", result.getInfo().getSnmp().getId());
        assertEquals("x-version-x", result.getInfo().getSnmp().getVersion());
        assertEquals(171717, result.getInfo().getSnmp().getGeneric());
        assertEquals("x-community-x", result.getInfo().getSnmp().getCommunity());
        assertEquals(232323, result.getInfo().getSnmp().getSpecific());
        assertEquals("x-trap-oid-x", result.getInfo().getSnmp().getTrapOid());

        assertEquals(1, result.getParametersCount());
        assertEquals("x-parm-name-x", result.getParameters(0).getName());
        assertNotNull(result.getParameters(0).getValue());
        assertEquals("x-value-x", result.getParameters(0).getValue());
        assertEquals("x-encoding-x", result.getParameters(0).getEncoding());
        assertEquals("x-type-x", result.getParameters(0).getType());
    }

    @Test
    void testMappingInvalidParm() {
        //
        // Setup Test Data and Interactions
        //
        Parm testInvalidParm = new Parm();

        testEvent.setParmCollection(
            List.of(testInvalidParm)
        );

        //
        // Execute
        //
        org.opennms.horizon.events.proto.Event result = target.convert(testEvent, "x-tenant-id-x");

        //
        // Verify the Results
        //
        assertEquals(0, result.getParametersCount());
    }
}
