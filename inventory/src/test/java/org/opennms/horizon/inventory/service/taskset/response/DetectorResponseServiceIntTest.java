package org.opennms.horizon.inventory.service.taskset.response;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.inventory.SpringContextTestInitializer;
import org.opennms.horizon.inventory.grpc.GrpcTestBase;
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
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.opennms.taskset.contract.DetectorResponse;
import org.opennms.taskset.contract.MonitorType;
import org.opennms.taskset.contract.TaskType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import io.grpc.Context;


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

    @BeforeEach
    void beforeTest() {
        prepareTestGrpc();
    }

    @AfterEach
    public void cleanUp() {
        Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, TEST_TENANT_ID).run(()->
        {
            monitoredServiceRepository.deleteAll();
            monitoredServiceTypeRepository.deleteAll();
            ipInterfaceRepository.deleteAll();
            nodeRepository.deleteAll();
            monitoringLocationRepository.deleteAll();
        });
    }

    @Test
    void testAccept() {
        Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, TEST_TENANT_ID).run(()->
        {
            try {
                populateDatabase();
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }

            DetectorResponse response = DetectorResponse.newBuilder()
                .setDetected(true).setIpAddress(TEST_IP_ADDRESS)
                .setMonitorType(MonitorType.SNMP).build();

            service.accept(TEST_TENANT_ID, TEST_LOCATION, response);

            checkTestAcceptResults(response, 2);
        });
    }

    private void checkTestAcceptResults(DetectorResponse response, int expected) {
        List<MonitoredServiceType> monitoredServiceTypes = monitoredServiceTypeRepository.findAll();
        assertEquals(1, monitoredServiceTypes.size());

        MonitoredServiceType monitoredServiceType = monitoredServiceTypes.get(0);
        assertEquals(response.getMonitorType().name(), monitoredServiceType.getServiceName());
        assertEquals(TEST_TENANT_ID, monitoredServiceType.getTenantId());

        List<MonitoredService> monitoredServices = monitoredServiceRepository.findAll();
        assertEquals(1, monitoredServices.size());

        MonitoredService monitoredService = monitoredServices.get(0);
        IpInterface ipInterface = monitoredService.getIpInterface();

        assertEquals(TEST_IP_ADDRESS, InetAddressUtils.toIpAddrString(ipInterface.getIpAddress()));
        assertEquals(TEST_TENANT_ID, monitoredService.getTenantId());

        MonitoredServiceType relatedType = monitoredService.getMonitoredServiceType();
        assertEquals(monitoredServiceType.getServiceName(), relatedType.getServiceName());

        assertEquals(expected, testGrpcService.getRequests().size());
    }

    @Test
    void testAcceptMultipleSameIpAddress() {
        Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, TEST_TENANT_ID).run(()->
        {
            try {
                populateDatabase();
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }

            DetectorResponse response = DetectorResponse.newBuilder()
                .setDetected(true).setIpAddress(TEST_IP_ADDRESS)
                .setMonitorType(MonitorType.SNMP).build();

            int numberOfCalls = 2;

            for (int index = 0; index < numberOfCalls; index++) {
                service.accept(TEST_TENANT_ID, TEST_LOCATION, response);
            }

            checkTestAcceptResults(response, numberOfCalls * 2);
        });
    }

    @Test
    void testAcceptNotDetected() {
        Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, TEST_TENANT_ID).run(()->
        {
            try {
                populateDatabase();
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }

            DetectorResponse response = DetectorResponse.newBuilder()
                .setDetected(false).setIpAddress(TEST_IP_ADDRESS)
                .setMonitorType(MonitorType.SNMP).build();

            service.accept(TEST_TENANT_ID, TEST_LOCATION, response);

            List<MonitoredServiceType> monitoredServiceTypes = monitoredServiceTypeRepository.findAll();
            assertEquals(0, monitoredServiceTypes.size());

            List<MonitoredService> monitoredServices = monitoredServiceRepository.findAll();
            assertEquals(0, monitoredServices.size());

            assertEquals(0, testGrpcService.getRequests().size());
        });
    }

    @Test
    void testAcceptMultipleSameIpAddressDifferentMonitorType() {
        Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, TEST_TENANT_ID).run(()->
        {
            try {
                populateDatabase();
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }

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
                    .toList();

            assertEquals(monitorTypes.length, monitoredServiceNames.size());

            for (int index = 0; index < monitorTypes.length; index++) {
                assertEquals(monitorTypes[index].getValueDescriptor().getName(), monitoredServiceNames.get(index));
            }

            List<MonitoredService> monitoredServices = monitoredServiceRepository.findAll();
            assertEquals(monitorTypes.length, monitoredServices.size());

            for (MonitoredService monitoredService : monitoredServices) {
                IpInterface ipInterface = monitoredService.getIpInterface();

                assertEquals(TEST_IP_ADDRESS, InetAddressUtils.toIpAddrString(ipInterface.getIpAddress()));
                assertEquals(TEST_TENANT_ID, monitoredService.getTenantId());
            }

            var taskDefinitions = testGrpcService.getTaskDefinitions(TEST_LOCATION);
            var monitorTasks = taskDefinitions.stream().filter(taskDefinition ->
                    taskDefinition.getType().equals(TaskType.MONITOR))
                .filter(taskDefinition -> taskDefinition.getPluginName().contains(MonitorType.SNMP.name()) ||
                    taskDefinition.getPluginName().contains(MonitorType.ICMP.name())).toList();

            assertEquals(2, monitorTasks.size());

            var collectorTasks = taskDefinitions.stream().filter(taskDefinition ->
                    taskDefinition.getType().equals(TaskType.COLLECTOR))
                .filter(taskDefinition -> taskDefinition.getPluginName().contains(MonitorType.SNMP.name())).toList();

            assertEquals(1, collectorTasks.size());
        });
    }

    private void populateDatabase() throws UnknownHostException {
        final InetAddress inetAddress = InetAddress.getByName(TEST_IP_ADDRESS);
        Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, TEST_TENANT_ID).run(()->
        {
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
            ipInterface.setIpAddress(inetAddress);
            ipInterface.setTenantId(TEST_TENANT_ID);
            ipInterface.setNode(node);

            ipInterfaceRepository.save(ipInterface);
        });
    }
}
