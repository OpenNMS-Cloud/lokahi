package org.opennms.horizon.events.grpc.service;

import com.google.protobuf.Empty;
import com.google.protobuf.Int64Value;
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
import org.opennms.horizon.events.proto.EventInfoDTO;
import org.opennms.horizon.events.proto.EventList;
import org.opennms.horizon.events.proto.EventServiceGrpc;
import org.opennms.horizon.events.proto.SnmpInfo;
import org.opennms.horizon.events.proto.SnmpInfoDTO;
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
class EventGrpcIntTest extends GrpcTestBase {
    private static final String TEST_UEI = "uei";
    private static final Inet TEST_IP_ADDRESS = new Inet("192.168.1.1");
    private static final String TEST_NAME = "ifIndex";
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

    @AfterEach
    public void cleanUp() {
        repository.deleteAll();
        channel.shutdown();
    }

    @Test
    void testListEvents() {
        setupGrpc();
        initStub();

        int count = 10;

        for (int index = 0; index < count; index++) {
            populateDatabase(index + 1);
        }

        EventList eventList = serviceStub.listEvents(Empty.getDefaultInstance());

        List<EventDTO> events = eventList.getEventsList();
        assertEquals(count, events.size());

        for (int index = 0; index < events.size(); index++) {
            EventDTO event = events.get(index);
            assertEquals(index + 1, event.getNodeId());
            assertEvent(event);
        }
    }

    @Test
    void testListEventsDifferentTenantId() {
        setupGrpcWithDifferentTenantID();
        initStub();

        int count = 10;

        for (int index = 0; index < count; index++) {
            populateDatabase(index + 1);
        }

        EventList eventList = serviceStub.listEvents(Empty.getDefaultInstance());

        List<EventDTO> events = eventList.getEventsList();
        assertEquals(0, events.size());
    }

    @Test
    void testFindAllEventsByNodeId() {
        setupGrpc();
        initStub();

        for (int index = 0; index < 3; index++) {
            populateDatabase(1);
        }
        for (int index = 0; index < 5; index++) {
            populateDatabase(2);
        }

        EventList eventList1 = serviceStub.getEventsByNodeId(Int64Value
            .newBuilder().setValue(1).build());

        List<EventDTO> eventsNode1 = eventList1.getEventsList();
        assertNotNull(eventsNode1);
        assertEquals(3, eventsNode1.size());
        for (EventDTO event : eventsNode1) {
            assertEquals(1, event.getNodeId());
            assertEvent(event);
        }

        EventList eventList2 = serviceStub.getEventsByNodeId(Int64Value
            .newBuilder().setValue(2).build());

        List<EventDTO> eventsNode2 = eventList2.getEventsList();
        assertNotNull(eventsNode2);
        assertEquals(5, eventsNode2.size());
        for (EventDTO event : eventsNode2) {
            assertEquals(2, event.getNodeId());
            assertEvent(event);
        }
    }

    @Test
    void testFindAllEventsByNodeIdDifferentTenantId() {
        setupGrpcWithDifferentTenantID();
        initStub();

        for (int index = 0; index < 3; index++) {
            populateDatabase(1);
        }

        EventList eventList1 = serviceStub.getEventsByNodeId(Int64Value
            .newBuilder().setValue(1).build());

        List<EventDTO> eventsNode1 = eventList1.getEventsList();
        assertEquals(0, eventsNode1.size());
    }

    private void populateDatabase(long nodeId) {

        Event event = new Event();
        event.setTenantId(tenantId);
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
            .setId(TEST_ID)
            .setTrapOid(TEST_TRAP_OID)
            .setCommunity(TEST_COMMUNITY)
            .setGeneric(TEST_GENERIC).build();
        EventInfo eventInfo = EventInfo.newBuilder()
            .setSnmp(snmpInfo).build();

        event.setEventInfo(eventInfo.toByteArray());

        repository.saveAndFlush(event);
    }

    private void assertEvent(EventDTO event) {
        assertEquals(tenantId, event.getTenantId());
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
