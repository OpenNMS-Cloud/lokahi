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
package org.opennms.horizon.events.persistence.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.events.persistence.model.Event;
import org.opennms.horizon.events.persistence.model.EventParameter;
import org.opennms.horizon.events.persistence.model.EventParameters;
import org.opennms.horizon.events.persistence.repository.EventRepository;
import org.opennms.horizon.events.proto.EventInfo;
import org.opennms.horizon.events.proto.SnmpInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

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
            .withDatabaseName("events")
            .withUsername("events")
            .withPassword("password")
            .withExposedPorts(5432);

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.datasource.url",
                () -> String.format(
                        "jdbc:postgresql://localhost:%d/%s",
                        postgres.getFirstMappedPort(), postgres.getDatabaseName()));
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

        List<org.opennms.horizon.events.proto.Event> events = service.findEvents(TEST_TENANT_ID);
        assertEquals(count, events.size());

        for (int index = 0; index < events.size(); index++) {
            org.opennms.horizon.events.proto.Event event = events.get(index);
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

        List<org.opennms.horizon.events.proto.Event> eventsNode1 = service.findEventsByNodeId(TEST_TENANT_ID, 1);
        assertEquals(3, eventsNode1.size());
        for (org.opennms.horizon.events.proto.Event event : eventsNode1) {
            assertEquals(1, event.getNodeId());
            assertEvent(event);
        }

        List<org.opennms.horizon.events.proto.Event> eventsNode2 = service.findEventsByNodeId(TEST_TENANT_ID, 2);
        assertEquals(5, eventsNode2.size());
        for (org.opennms.horizon.events.proto.Event event : eventsNode2) {
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
                .setGeneric(TEST_GENERIC)
                .build();
        EventInfo eventInfo = EventInfo.newBuilder().setSnmp(snmpInfo).build();

        event.setEventInfo(eventInfo.toByteArray());

        repository.save(event);
    }

    private static void assertEvent(org.opennms.horizon.events.proto.Event event) {
        assertEquals(TEST_TENANT_ID, event.getTenantId());
        assertEquals(TEST_UEI, event.getUei());
        assertNotEquals(0, event.getProducedTimeMs());
        assertEquals(TEST_IP_ADDRESS, event.getIpAddress());

        assertNotNull(event.getParametersList());
        event.getParametersList().forEach(parameter -> {
            assertEquals(TEST_NAME, parameter.getName());
            assertEquals(TEST_TYPE, parameter.getType());
            assertEquals(TEST_VALUE, parameter.getValue());
            assertEquals(TEST_ENCODING, parameter.getEncoding());
        });

        EventInfo eventInfo = event.getInfo();
        assertNotNull(eventInfo);

        SnmpInfo snmpInfo = eventInfo.getSnmp();
        assertNotNull(snmpInfo);
        assertEquals(TEST_ID, snmpInfo.getId());
        assertEquals(TEST_TRAP_OID, snmpInfo.getTrapOid());
        assertEquals(TEST_COMMUNITY, snmpInfo.getCommunity());
        assertEquals(TEST_GENERIC, snmpInfo.getGeneric());
    }
}
