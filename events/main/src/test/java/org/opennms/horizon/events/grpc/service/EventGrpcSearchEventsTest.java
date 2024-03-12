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
package org.opennms.horizon.events.grpc.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
import org.opennms.horizon.events.proto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest
public class EventGrpcSearchEventsTest extends GrpcTestBase {

    private static final String TEST_UEI = "uei";
    private static final String TEST_IP_ADDRESS = "192.168.1.1";
    private static final String TEST_NAME = "searchEvents";

    private static final String TEST_LOCATION_NAME = "default";
    private static final String TEST_LOG_MESSAGE = "timeout";
    private static final String TEST_DESCRIPTION = "description";
    private static final String TEST_TYPE = "int32";
    private static final String TEST_VALUE = "64";
    private static final String TEST_ENCODING = "encoding";
    private static final String TEST_ID = "snmp";
    private static final String TEST_TRAP_OID = "0.0.1.2";
    private static final String TEST_COMMUNITY = "public";
    private static final int TEST_GENERIC = 34;

    private EventServiceGrpc.EventServiceBlockingStub serviceStub;

    @Autowired
    private EventRepository repository;

    private void initStub() {
        serviceStub = EventServiceGrpc.newBlockingStub(channel);
    }

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

    @AfterEach
    public void cleanUp() {
        repository.deleteAll();
        channel.shutdown();
    }

    @Test
    void testSearchEventsByNodeIdAndDifferentSearchTerm() throws UnknownHostException {
        setupGrpc();
        initStub();

        // persist data in event table.
        for (int index = 0; index < 3; index++) {
            populateEventDatabase(1);
        }
        // Search for nodeId 1 and logMessage "timeout"
        EventsSearchBy searchEventByNodeIdAndLogMessage = EventsSearchBy.newBuilder()
                .setNodeId(1)
                .setSearchTerm(TEST_LOG_MESSAGE)
                .build();

        EventLog eventLog1 = serviceStub.searchEvents(searchEventByNodeIdAndLogMessage);
        List<org.opennms.horizon.events.proto.Event> searchEvents1 = eventLog1.getEventsList();

        assertNotNull(searchEvents1);
        assertEquals(3, searchEvents1.size());
        for (org.opennms.horizon.events.proto.Event event : searchEvents1) {
            assertEquals(1, event.getNodeId());
            assertEvent(event);
        }

        // Search for nodeId 1 and description "description"
        EventsSearchBy searchEventByNodeIdAndDescription = EventsSearchBy.newBuilder()
                .setNodeId(1)
                .setSearchTerm(TEST_DESCRIPTION)
                .build();

        EventLog eventLog2 = serviceStub.searchEvents(searchEventByNodeIdAndDescription);
        List<org.opennms.horizon.events.proto.Event> searchEvents2 = eventLog2.getEventsList();

        assertNotNull(searchEvents2);
        assertEquals(3, searchEvents2.size());
        for (org.opennms.horizon.events.proto.Event event : searchEvents2) {
            assertEquals(1, event.getNodeId());
            assertEvent(event);
        }

        // Search for nodeId 1 and locationName "default"
        EventsSearchBy searchEventByNodeIdAndLocationName = EventsSearchBy.newBuilder()
                .setNodeId(1)
                .setSearchTerm(TEST_LOCATION_NAME)
                .build();

        EventLog eventLog3 = serviceStub.searchEvents(searchEventByNodeIdAndLocationName);
        List<org.opennms.horizon.events.proto.Event> searchEvents3 = eventLog3.getEventsList();

        assertNotNull(searchEvents3);
        assertEquals(3, searchEvents3.size());
        for (org.opennms.horizon.events.proto.Event event : searchEvents3) {
            assertEquals(1, event.getNodeId());
            assertEvent(event);
        }

        // Search for nodeId 1 and IpAddress "192.168.1.1"
        EventsSearchBy searchEventByNodeIdAndIpAddress = EventsSearchBy.newBuilder()
                .setNodeId(1)
                .setSearchTerm(TEST_IP_ADDRESS)
                .build();

        EventLog eventLog4 = serviceStub.searchEvents(searchEventByNodeIdAndIpAddress);
        List<org.opennms.horizon.events.proto.Event> searchEvents4 = eventLog4.getEventsList();

        assertNotNull(searchEvents4);
        assertEquals(3, searchEvents4.size());
        for (org.opennms.horizon.events.proto.Event event : searchEvents4) {
            assertEquals(1, event.getNodeId());
            assertEvent(event);
        }

        // Search for nodeId 1 and IpAddress "127.0.0.1", which does not exist in the database
        EventsSearchBy searchEventByNodeIdAndIpAddressNotExist = EventsSearchBy.newBuilder()
                .setNodeId(1)
                .setSearchTerm("127.0.0.1")
                .build();

        EventLog eventLog5 = serviceStub.searchEvents(searchEventByNodeIdAndIpAddressNotExist);
        List<org.opennms.horizon.events.proto.Event> searchEvents5 = eventLog5.getEventsList();

        assertNotNull(searchEvents5);
        assertEquals(0, searchEvents5.size());
    }

    private void populateEventDatabase(long nodeId) throws UnknownHostException {

        Event event = new Event();
        event.setTenantId(tenantId);
        event.setEventUei(TEST_UEI);
        event.setProducedTime(LocalDateTime.now());
        event.setNodeId(nodeId);
        event.setIpAddress(InetAddress.getByName(TEST_IP_ADDRESS));
        event.setDescription(TEST_DESCRIPTION);
        event.setLogMessage(TEST_LOG_MESSAGE);
        event.setLocationName(TEST_LOCATION_NAME);

        EventParameters params = new EventParameters();
        EventParameter param = new EventParameter();
        param.setName(TEST_NAME);
        param.setType(TEST_TYPE);
        param.setValue(TEST_VALUE);
        param.setEncoding(TEST_ENCODING);
        params.setParameters(Collections.singletonList(param));

        event.setEventParameters(params);

        SnmpInfo snmpInfo = SnmpInfo.newBuilder()
                .setId(TEST_ID)
                .setTrapOid(TEST_TRAP_OID)
                .setCommunity(TEST_COMMUNITY)
                .setGeneric(TEST_GENERIC)
                .build();
        EventInfo eventInfo = EventInfo.newBuilder().setSnmp(snmpInfo).build();

        event.setEventInfo(eventInfo.toByteArray());

        repository.saveAndFlush(event);
    }

    private void assertEvent(org.opennms.horizon.events.proto.Event event) {
        assertEquals(tenantId, event.getTenantId());
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
