package org.opennms.horizon.events.persistence.service;

import com.vladmihalcea.hibernate.type.basic.Inet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.events.persistence.model.Event;
import org.opennms.horizon.events.persistence.model.EventParameter;
import org.opennms.horizon.events.persistence.model.EventParameters;
import org.opennms.horizon.events.persistence.repository.EventRepository;
import org.opennms.horizon.events.proto.EventDTO;
import org.opennms.horizon.events.proto.EventInfo;
import org.opennms.horizon.events.proto.SnmpInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

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
    private static final Inet TEST_IP_ADDRESS = new Inet("192.168.1.1");
    private static final String TEST_NAME = "ifIndex";
    private static final String TEST_TYPE = "int32";
    private static final String TEST_VALUE = "64";
    private static final String TEST_ENCODING = "encoding";

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
    void testFindAllEvents() {
        int count = 10;

        for (int index = 0; index < count; index++) {
            populateDatabase(index + 1);
        }

        List<EventDTO> events = service.findEvents();
        assertEquals(count, events.size());

        for (int index = 0; index < events.size(); index++) {
            EventDTO event = events.get(index);
            assertEquals(index + 1, event.getNodeId());
            assertEquals(TEST_TENANT_ID, event.getTenantId());
            assertEquals(TEST_UEI, event.getUei());
            assertNotEquals(0, event.getProducedTime());
            assertEquals(TEST_IP_ADDRESS.getAddress(), event.getIpAddress());

            assertNotNull(event.getEventParamsList());
            event.getEventParamsList().forEach(parameter -> {
                assertEquals(TEST_NAME, parameter.getName());
                assertEquals(TEST_TYPE, parameter.getType());
                assertEquals(TEST_VALUE, parameter.getValue());
                assertEquals(TEST_ENCODING, parameter.getEncoding());
            });
        }
    }

    @Test
    void testFindAllEventsByNodeId() {
        for (int index = 0; index < 3; index++) {
            populateDatabase(1);
        }
        for (int index = 0; index < 5; index++) {
            populateDatabase(2);
        }

        List<EventDTO> eventsNode1 = service.findEventsByNodeId(1);
        assertEquals(3, eventsNode1.size());
        for (EventDTO event : eventsNode1) {
            assertEquals(1, event.getNodeId());
            assertEquals(TEST_TENANT_ID, event.getTenantId());
            assertEquals(TEST_UEI, event.getUei());
            assertNotEquals(0, event.getProducedTime());
            assertEquals(TEST_IP_ADDRESS.getAddress(), event.getIpAddress());
        }

        List<EventDTO> eventsNode2 = service.findEventsByNodeId(2);
        assertEquals(5, eventsNode2.size());
        for (EventDTO event : eventsNode2) {
            assertEquals(2, event.getNodeId());
            assertEquals(TEST_TENANT_ID, event.getTenantId());
            assertEquals(TEST_UEI, event.getUei());
            assertNotEquals(0, event.getProducedTime());
            assertEquals(TEST_IP_ADDRESS.getAddress(), event.getIpAddress());
        }
    }

    private void populateDatabase(long nodeId) {

        Event event = new Event();
        event.setTenantId(TEST_TENANT_ID);
        event.setEventUei(TEST_UEI);
        event.setProducedTime(LocalDateTime.now());
        event.setNodeId(nodeId);
        event.setIpAddress(TEST_IP_ADDRESS);

        EventParameters parms = new EventParameters();
        EventParameter param = new EventParameter();
        param.setName(TEST_NAME);
        param.setType(TEST_TYPE);
        param.setValue(TEST_VALUE);
        param.setEncoding(TEST_ENCODING);
        parms.setParameters(Collections.singletonList(param));

        event.setEventParameters(parms);

        SnmpInfo snmpInfo = SnmpInfo.newBuilder()
            .setId("snmp")
            .setTrapOid("0.0.1.2")
            .setCommunity("public")
            .setGeneric(34).build();
        EventInfo eventInfo = EventInfo.newBuilder()
            .setSnmp(snmpInfo).build();

        event.setEventInfo(eventInfo.toByteArray());

        repository.save(event);
    }
}
