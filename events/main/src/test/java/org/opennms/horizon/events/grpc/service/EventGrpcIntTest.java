package org.opennms.horizon.events.grpc.service;

import com.google.protobuf.Empty;
import com.google.protobuf.Int64Value;
import com.google.protobuf.UInt64Value;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.events.persistence.model.Event;
import org.opennms.horizon.events.persistence.model.EventParameter;
import org.opennms.horizon.events.persistence.model.EventParameters;
import org.opennms.horizon.events.persistence.repository.EventRepository;
import org.opennms.horizon.events.proto.EventInfo;
import org.opennms.horizon.events.proto.EventLog;
import org.opennms.horizon.events.proto.EventServiceGrpc;
import org.opennms.horizon.events.proto.SnmpInfo;
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

@Disabled //
@SpringBootTest
class EventGrpcIntTest extends GrpcTestBase {
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
    void testListEvents() throws UnknownHostException {
        setupGrpc();
        initStub();

        int count = 10;

        for (int index = 0; index < count; index++) {
            populateDatabase(index + 1);
        }

        EventLog eventLog = serviceStub.listEvents(Empty.getDefaultInstance());
        List<org.opennms.horizon.events.proto.Event> events = eventLog.getEventsList();
        assertEquals(count, events.size());

        for (int index = 0; index < events.size(); index++) {
            org.opennms.horizon.events.proto.Event event = events.get(index);
            assertEquals(index + 1, event.getNodeId());
            assertEvent(event);
        }
    }

    @Test
    void testListEventsDifferentTenantId() throws UnknownHostException {
        setupGrpcWithDifferentTenantID();
        initStub();

        int count = 10;

        for (int index = 0; index < count; index++) {
            populateDatabase(index + 1);
        }

        EventLog eventLog = serviceStub.listEvents(Empty.getDefaultInstance());
        List<org.opennms.horizon.events.proto.Event> events = eventLog.getEventsList();
        assertEquals(0, events.size());
    }

    @Test
    void testFindAllEventsByNodeId() throws UnknownHostException {
        setupGrpc();
        initStub();

        for (int index = 0; index < 3; index++) {
            populateDatabase(1);
        }
        for (int index = 0; index < 5; index++) {
            populateDatabase(2);
        }


        EventLog eventLog1 = serviceStub.getEventsByNodeId(UInt64Value.of(1));
        List<org.opennms.horizon.events.proto.Event> eventsNode1 = eventLog1.getEventsList();

        assertNotNull(eventsNode1);
        assertEquals(3, eventsNode1.size());
        for (org.opennms.horizon.events.proto.Event event : eventsNode1) {
            assertEquals(1, event.getNodeId());
            assertEvent(event);
        }

        EventLog eventLog2 = serviceStub.getEventsByNodeId(UInt64Value.of(2));
        List<org.opennms.horizon.events.proto.Event> eventsNode2 = eventLog2.getEventsList();

        assertNotNull(eventsNode2);
        assertEquals(5, eventsNode2.size());
        for (org.opennms.horizon.events.proto.Event event : eventsNode2) {
            assertEquals(2, event.getNodeId());
            assertEvent(event);
        }
    }

    @Test
    void testFindAllEventsByNodeIdDifferentTenantId() throws UnknownHostException {
        setupGrpcWithDifferentTenantID();
        initStub();

        for (int index = 0; index < 3; index++) {
            populateDatabase(1);
        }

        EventLog eventLog = serviceStub.getEventsByNodeId(UInt64Value.of(1));
        List<org.opennms.horizon.events.proto.Event> eventsNode1 = eventLog.getEventsList();

        assertEquals(0, eventsNode1.size());
    }

    private void populateDatabase(long nodeId) throws UnknownHostException {

        Event event = new Event();
        event.setTenantId(tenantId);
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
