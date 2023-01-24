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

package org.opennms.horizon.events.persistence.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.events.persistence.model.Event;
import org.opennms.horizon.events.persistence.model.EventParameter;
import org.opennms.horizon.events.persistence.model.EventParameters;
import org.opennms.horizon.events.persistence.repository.EventRepository;
import org.opennms.horizon.events.proto.EventDTO;
import org.opennms.horizon.events.proto.EventInfo;
import org.opennms.horizon.events.proto.EventInfoDTO;
import org.opennms.horizon.events.proto.SnmpInfo;
import org.opennms.horizon.events.proto.SnmpInfoDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class EventServiceIntTest {
    private static final String TEST_TENANT_ID = "tenant-id";
    private static final String TEST_UEI = "uei";
    private static final String TEST_IP_ADDRESS = "192.168.1.1";
    private static final String TEST_NAME = "ifIndex";
    private static final String TEST_TYPE = "int32";
    private static final String TEST_VALUE = "64";
    private static final String TEST_ENCODING = "encoding";
    private static final String TEST_ID = "snmp";
    private static final String TEST_TRAP_OID = "0.0.1.2";
    private static final String TEST_COMMUNITY = "public";
    private static final int TEST_GENERIC = 34;

    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14.5-alpine")
        .withDatabaseName("events").withUsername("events")
        .withPassword("password").withExposedPorts(5432);


    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
            () -> String.format("jdbc:postgresql://localhost:%d/%s", postgres.getFirstMappedPort(), postgres.getDatabaseName()));
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeEach
    void setup() {
        assertTrue(postgres.isCreated());
        assertTrue(postgres.isRunning());
    }

    @Autowired
    private EventRepository repository;

    @Autowired
    private EventService service;

    @AfterEach
    public void teardown() {
        repository.deleteAll();
    }

    @Test
    void testFindAllEvents() throws UnknownHostException {
        int count = 10;

        for (int index = 0; index < count; index++) {
            populateDatabase(index + 1);
        }

        List<EventDTO> events = service.findEvents(TEST_TENANT_ID);
        assertEquals(count, events.size());

        for (int index = 0; index < events.size(); index++) {
            EventDTO event = events.get(index);
            assertEquals(index + 1, event.getNodeId());
            assertEvent(event);
        }
    }


    @Test
    void testFindAllEventsByNodeId() throws UnknownHostException {
        for (int index = 0; index < 3; index++) {
            populateDatabase(1);
        }
        for (int index = 0; index < 5; index++) {
            populateDatabase(2);
        }

        List<EventDTO> eventsNode1 = service.findEventsByNodeId(TEST_TENANT_ID, 1);
        assertEquals(3, eventsNode1.size());
        for (EventDTO event : eventsNode1) {
            assertEquals(1, event.getNodeId());
            assertEvent(event);
        }

        List<EventDTO> eventsNode2 = service.findEventsByNodeId(TEST_TENANT_ID, 2);
        assertEquals(5, eventsNode2.size());
        for (EventDTO event : eventsNode2) {
            assertEquals(2, event.getNodeId());
            assertEvent(event);
        }
    }

    private void populateDatabase(long nodeId) throws UnknownHostException {

        Event event = new Event();
        event.setTenantId(TEST_TENANT_ID);
        event.setEventUei(TEST_UEI);
        event.setProducedTime(LocalDateTime.now());
        event.setNodeId(nodeId);
        event.setIpAddress(InetAddress.getByName(TEST_IP_ADDRESS));

        EventParameters parms = new EventParameters();
        EventParameter param = new EventParameter();
        param.setName(TEST_NAME);
        param.setType(TEST_TYPE);
        param.setValue(TEST_VALUE);
        param.setEncoding(TEST_ENCODING);
        parms.setParameters(Collections.singletonList(param));

        event.setEventParameters(parms);

        SnmpInfo snmpInfo = SnmpInfo.newBuilder()
            .setId(TEST_ID)
            .setTrapOid(TEST_TRAP_OID)
            .setCommunity(TEST_COMMUNITY)
            .setGeneric(TEST_GENERIC).build();
        EventInfo eventInfo = EventInfo.newBuilder()
            .setSnmp(snmpInfo).build();

        event.setEventInfo(eventInfo.toByteArray());

        repository.save(event);
    }

    private static void assertEvent(EventDTO event) {
        assertEquals(TEST_TENANT_ID, event.getTenantId());
        assertEquals(TEST_UEI, event.getUei());
        assertNotEquals(0, event.getProducedTime());
        assertEquals(TEST_IP_ADDRESS, event.getIpAddress());

        assertNotNull(event.getEventParamsList());
        event.getEventParamsList().forEach(parameter -> {
            assertEquals(TEST_NAME, parameter.getName());
            assertEquals(TEST_TYPE, parameter.getType());
            assertEquals(TEST_VALUE, parameter.getValue());
            assertEquals(TEST_ENCODING, parameter.getEncoding());
        });

        EventInfoDTO eventInfo = event.getEventInfo();
        assertNotNull(eventInfo);

        SnmpInfoDTO snmpInfo = eventInfo.getSnmp();
        assertNotNull(snmpInfo);
        assertEquals(TEST_ID, snmpInfo.getId());
        assertEquals(TEST_TRAP_OID, snmpInfo.getTrapOid());
        assertEquals(TEST_COMMUNITY, snmpInfo.getCommunity());
        assertEquals(TEST_GENERIC, snmpInfo.getGeneric());
    }
}
