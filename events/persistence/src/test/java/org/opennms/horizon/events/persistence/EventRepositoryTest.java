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

package org.opennms.horizon.events.persistence;


import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.events.persistence.model.Event;
import org.opennms.horizon.events.persistence.model.EventParameter;
import org.opennms.horizon.events.persistence.model.EventParameters;
import org.opennms.horizon.events.persistence.repository.EventRepository;
import org.opennms.horizon.events.proto.EventInfo;
import org.opennms.horizon.events.proto.SnmpInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = EventsTestApp.class)
class EventRepositoryTest {


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
    private EventRepository eventRepository;

    @Test
    void testPersistence() throws InvalidProtocolBufferException, UnknownHostException {
        var event = new Event();
        event.setEventUei("uei");
        event.setTenantId("test");
        event.setProducedTime(LocalDateTime.now());
        event.setIpAddress(InetAddress.getByName("192.168.1.1"));
        var parms = new EventParameters();
        var parm = new EventParameter();
        parm.setName("ifIndex");
        parm.setType("int32");
        parm.setValue("64");
        parms.addEventParameter(parm);
        event.setEventParameters(parms);
        SnmpInfo snmpInfo = SnmpInfo.newBuilder().setId("snmp").setTrapOid("0.0.1.2").setCommunity("public").setGeneric(34).build();
        EventInfo eventInfo = EventInfo.newBuilder().setSnmp(snmpInfo).build();
        event.setEventInfo(eventInfo.toByteArray());
        Event retrieved = eventRepository.save(event);
        assertNotNull(retrieved);
        Assertions.assertEquals("snmp", EventInfo.parseFrom(retrieved.getEventInfo()).getSnmp().getId());
        Assertions.assertEquals("public", EventInfo.parseFrom(retrieved.getEventInfo()).getSnmp().getCommunity());
        assertEquals("192.168.1.1", retrieved.getIpAddress().getHostAddress());
        assertEquals(parm, retrieved.getEventParameters().getParameters().get(0));
    }
}
