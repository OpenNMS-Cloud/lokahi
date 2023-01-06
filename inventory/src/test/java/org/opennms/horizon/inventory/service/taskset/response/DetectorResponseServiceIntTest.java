package org.opennms.horizon.inventory.service.taskset.response;

import com.vladmihalcea.hibernate.type.basic.Inet;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.inventory.SpringContextTestInitializer;
import org.opennms.horizon.inventory.grpc.GrpcTestBase;
import org.opennms.horizon.inventory.grpc.taskset.TestTaskSetGrpcService;
import org.opennms.horizon.inventory.model.IpInterface;
import org.opennms.horizon.inventory.model.MonitoredService;
import org.opennms.horizon.inventory.model.MonitoredServiceType;
import org.opennms.horizon.inventory.model.MonitoringLocation;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.repository.IpInterfaceRepository;
import org.opennms.horizon.inventory.repository.MonitoredServiceRepository;
import org.opennms.horizon.inventory.repository.MonitoredServiceTypeRepository;
import org.opennms.horizon.inventory.repository.MonitoringLocationRepository;
import org.opennms.horizon.inventory.repository.NodeRepository;
import org.opennms.taskset.contract.DetectorResponse;
import org.opennms.taskset.contract.MonitorType;
import org.opennms.taskset.service.contract.PublishTaskSetRequest;
import org.opennms.taskset.service.contract.TaskSetServiceGrpc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import javax.transaction.Transactional;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;


@SpringBootTest
@ContextConfiguration(initializers = {SpringContextTestInitializer.class})
class DetectorResponseServiceIntTest extends GrpcTestBase {
    private static final String TEST_IP_ADDRESS = "127.0.0.1";
    private static final String TEST_NODE_LABEL = "node-label";
    private static final String TEST_LOCATION = "Default";
    private static final String TEST_TENANT_ID = "test-tenant-id";

    @Autowired
    private DetectorResponseService service;

    @Autowired
    private MonitoringLocationRepository monitoringLocationRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private IpInterfaceRepository ipInterfaceRepository;

    @Autowired
    private MonitoredServiceTypeRepository monitoredServiceTypeRepository;

    @Autowired
    private MonitoredServiceRepository monitoredServiceRepository;

    private static TestTaskSetGrpcService testGrpcService;

    @BeforeAll
    public static void setup() throws IOException {
        testGrpcService = new TestTaskSetGrpcService();
        server = startMockServer(TaskSetServiceGrpc.SERVICE_NAME, testGrpcService);
    }

    @AfterEach
    public void cleanUp() {
        monitoredServiceRepository.deleteAll();
        monitoredServiceTypeRepository.deleteAll();
        ipInterfaceRepository.deleteAll();
        nodeRepository.deleteAll();
        monitoringLocationRepository.deleteAll();

        testGrpcService.reset();
    }

    @AfterAll
    public static void tearDown() throws InterruptedException {
        server.shutdownNow();
        server.awaitTermination();
    }

    @Test
    @Transactional
    void testAccept() {
        populateDatabase();

        DetectorResponse response = DetectorResponse.newBuilder()
            .setDetected(true).setIpAddress(TEST_IP_ADDRESS)
            .setMonitorType(MonitorType.SNMP).build();

        service.accept(TEST_TENANT_ID, TEST_LOCATION, response);

        List<MonitoredServiceType> monitoredServiceTypes = monitoredServiceTypeRepository.findAll();
        assertEquals(1, monitoredServiceTypes.size());

        MonitoredServiceType monitoredServiceType = monitoredServiceTypes.get(0);
        assertEquals(response.getMonitorType().name(), monitoredServiceType.getServiceName());
        assertEquals(TEST_TENANT_ID, monitoredServiceType.getTenantId());

        List<MonitoredService> monitoredServices = monitoredServiceRepository.findAll();
        assertEquals(1, monitoredServices.size());

        MonitoredService monitoredService = monitoredServices.get(0);
        IpInterface ipInterface = monitoredService.getIpInterface();

        assertEquals(TEST_IP_ADDRESS, ipInterface.getIpAddress().getAddress());
        assertEquals(TEST_TENANT_ID, monitoredService.getTenantId());

        MonitoredServiceType relatedType = monitoredService.getMonitoredServiceType();
        assertEquals(monitoredServiceType, relatedType);

        assertEquals(2, testGrpcService.getTimesCalled().intValue());
    }

    @Test
    @Transactional
    void testAcceptMultipleSameIpAddress() {
        populateDatabase();

        DetectorResponse response = DetectorResponse.newBuilder()
            .setDetected(true).setIpAddress(TEST_IP_ADDRESS)
            .setMonitorType(MonitorType.SNMP).build();

        int numberOfCalls = 2;

        for (int index = 0; index < numberOfCalls; index++) {
            service.accept(TEST_TENANT_ID, TEST_LOCATION, response);
        }

        List<MonitoredServiceType> monitoredServiceTypes = monitoredServiceTypeRepository.findAll();
        assertEquals(1, monitoredServiceTypes.size());

        MonitoredServiceType monitoredServiceType = monitoredServiceTypes.get(0);
        assertEquals(response.getMonitorType().name(), monitoredServiceType.getServiceName());
        assertEquals(TEST_TENANT_ID, monitoredServiceType.getTenantId());

        List<MonitoredService> monitoredServices = monitoredServiceRepository.findAll();
        assertEquals(1, monitoredServices.size());

        MonitoredService monitoredService = monitoredServices.get(0);
        IpInterface ipInterface = monitoredService.getIpInterface();

        assertEquals(TEST_IP_ADDRESS, ipInterface.getIpAddress().getAddress());
        assertEquals(TEST_TENANT_ID, monitoredService.getTenantId());

        MonitoredServiceType relatedType = monitoredService.getMonitoredServiceType();
        assertEquals(monitoredServiceType, relatedType);

        assertEquals(numberOfCalls*2, testGrpcService.getTimesCalled().intValue());
    }

    @Test
    @Transactional
    void testAcceptNotDetected() {
        populateDatabase();

        DetectorResponse response = DetectorResponse.newBuilder()
            .setDetected(false).setIpAddress(TEST_IP_ADDRESS)
            .setMonitorType(MonitorType.SNMP).build();

        service.accept(TEST_TENANT_ID, TEST_LOCATION, response);

        List<MonitoredServiceType> monitoredServiceTypes = monitoredServiceTypeRepository.findAll();
        assertEquals(0, monitoredServiceTypes.size());

        List<MonitoredService> monitoredServices = monitoredServiceRepository.findAll();
        assertEquals(0, monitoredServices.size());

        assertEquals(0, testGrpcService.getTimesCalled().intValue());
    }

    @Test
    @Transactional
    void testAcceptMultipleSameIpAddressDifferentMonitorType() {
        populateDatabase();

        DetectorResponse.Builder builder = DetectorResponse.newBuilder()
            .setDetected(true).setIpAddress(TEST_IP_ADDRESS);

        int numberOfCalls = 2;

        MonitorType[] monitorTypes = {MonitorType.ICMP, MonitorType.SNMP};

        for (int index = 0; index < numberOfCalls; index++) {

            DetectorResponse response = builder
                .setMonitorType(monitorTypes[index]).build();

            service.accept(TEST_TENANT_ID, TEST_LOCATION, response);
        }

        List<MonitoredServiceType> monitoredServiceTypes = monitoredServiceTypeRepository.findAll();
        assertEquals(monitorTypes.length, monitoredServiceTypes.size());

        List<String> monitoredServiceNames =
            monitoredServiceTypes.stream()
                .map(MonitoredServiceType::getServiceName)
                .collect(Collectors.toList());

        assertEquals(monitorTypes.length, monitoredServiceNames.size());

        for (int index = 0; index < monitorTypes.length; index++) {
            assertEquals(monitorTypes[index].getValueDescriptor().getName(), monitoredServiceNames.get(index));
        }

        List<MonitoredService> monitoredServices = monitoredServiceRepository.findAll();
        assertEquals(monitorTypes.length, monitoredServices.size());

        for (MonitoredService monitoredService : monitoredServices) {
            IpInterface ipInterface = monitoredService.getIpInterface();

            assertEquals(TEST_IP_ADDRESS, ipInterface.getIpAddress().getAddress());
            assertEquals(TEST_TENANT_ID, monitoredService.getTenantId());
        }
        
        // fragile test : extra 1 call for SNMP collector
        assertEquals(numberOfCalls + 1, testGrpcService.getTimesCalled().intValue());

        List<PublishTaskSetRequest> grpcRequests = testGrpcService.getRequests();
        // fragile test : extra 1 call for SNMP collector
        assertEquals(monitorTypes.length + 1, grpcRequests.size());
    }

    private void populateDatabase() {

        MonitoringLocation monitoringLocation = new MonitoringLocation();
        monitoringLocation.setLocation(TEST_LOCATION);
        monitoringLocation.setTenantId(TEST_TENANT_ID);
        monitoringLocation = monitoringLocationRepository.save(monitoringLocation);

        Node node = new Node();
        node.setNodeLabel(TEST_NODE_LABEL);
        node.setCreateTime(LocalDateTime.now());
        node.setTenantId(TEST_TENANT_ID);
        node.setMonitoringLocation(monitoringLocation);
        node = nodeRepository.save(node);

        IpInterface ipInterface = new IpInterface();
        ipInterface.setIpAddress(new Inet(TEST_IP_ADDRESS));
        ipInterface.setTenantId(TEST_TENANT_ID);
        ipInterface.setNode(node);

        ipInterfaceRepository.save(ipInterface);
    }
}
