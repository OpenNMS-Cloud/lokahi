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

package org.opennms.horizon.inventory.grpc;


import com.google.protobuf.BoolValue;
import com.google.protobuf.Empty;
import com.google.protobuf.Int64Value;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.Context;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.MetadataUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.common.VerificationException;
import org.opennms.horizon.inventory.SpringContextTestInitializer;
import org.opennms.horizon.inventory.dto.NodeCreateDTO;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.inventory.dto.NodeIdList;
import org.opennms.horizon.inventory.dto.NodeList;
import org.opennms.horizon.inventory.dto.NodeServiceGrpc;
import org.opennms.horizon.inventory.dto.TagCreateDTO;
import org.opennms.horizon.inventory.dto.TagCreateListDTO;
import org.opennms.horizon.inventory.dto.TagDTO;
import org.opennms.horizon.inventory.grpc.taskset.TestTaskSetGrpcService;
import org.opennms.horizon.inventory.mapper.NodeMapper;
import org.opennms.horizon.inventory.model.IpInterface;
import org.opennms.horizon.inventory.model.MonitoredService;
import org.opennms.horizon.inventory.model.MonitoredServiceType;
import org.opennms.horizon.inventory.model.MonitoringLocation;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.model.Tag;
import org.opennms.horizon.inventory.repository.IpInterfaceRepository;
import org.opennms.horizon.inventory.repository.MonitoredServiceTypeRepository;
import org.opennms.horizon.inventory.repository.MonitoringLocationRepository;
import org.opennms.horizon.inventory.repository.NodeRepository;
import org.opennms.horizon.inventory.repository.TagRepository;
import org.opennms.horizon.inventory.service.NodeService;
import org.opennms.horizon.inventory.service.TagService;
import org.opennms.horizon.inventory.service.taskset.DetectorTaskSetService;
import org.opennms.horizon.inventory.service.taskset.TaskSetHandler;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.opennms.taskset.contract.MonitorType;
import org.opennms.taskset.contract.ScanType;
import org.opennms.taskset.contract.TaskSet;
import org.opennms.taskset.contract.TaskType;
import org.opennms.taskset.service.contract.PublishTaskSetRequest;
import org.opennms.taskset.service.contract.TaskSetServiceGrpc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.jayway.awaitility.Awaitility.await;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;



@SpringBootTest
@ContextConfiguration(initializers = {SpringContextTestInitializer.class})
class NodeGrpcItTest extends GrpcTestBase {
    private static final int EXPECTED_TASK_DEF_COUNT_FOR_NEW_LOCATION = 4;
    private static final int EXPECTED_TASK_DEF_COUNT_WITHOUT_NEW_LOCATION = 2;
    private static final String NODE_LABEL = "label";
    private static final String TEST_LOCATION = "test-location";

    private NodeServiceGrpc.NodeServiceBlockingStub serviceStub;

    @Autowired
    private NodeRepository nodeRepository;
    @Autowired
    private MonitoringLocationRepository monitoringLocationRepository;
    @Autowired
    private IpInterfaceRepository ipInterfaceRepository;
    @Autowired
    private MonitoredServiceTypeRepository monitoredServiceTypeRepository;
    @Autowired
    private NodeService nodeService;
    @Autowired
    private NodeMapper mapper;
    @Autowired
    private TagService tagService;
    @Autowired
    private TaskSetHandler taskSetHandler;
    @Autowired
    private TagRepository tagRepository;
    @Autowired
    private DetectorTaskSetService detectorTaskSetService;

    private static TestTaskSetGrpcService testGrpcService;

    @BeforeAll
    static void setup() throws IOException {
        testGrpcService = new TestTaskSetGrpcService();
        server = startMockServer(TaskSetServiceGrpc.SERVICE_NAME, testGrpcService);
    }

    @AfterAll
    public static void tearDown() throws InterruptedException {
        server.shutdownNow();
        server.awaitTermination();
    }

    @BeforeEach
    public void prepare() throws VerificationException {
        prepareServer();
        serviceStub = NodeServiceGrpc.newBlockingStub(channel);
    }


    @AfterEach
    public void cleanUp() throws InterruptedException {
        cleanupRepository();
        afterTest();
    }

    private void cleanupRepository() {
        Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, tenantId).run(()->
        {
            tagRepository.deleteAll();
            ipInterfaceRepository.deleteAll();
            nodeRepository.deleteAll();
            monitoringLocationRepository.deleteAll();
            testGrpcService.reset();
        });
        Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, invalidTenantId).run(()->
        {
            tagRepository.deleteAll();
            ipInterfaceRepository.deleteAll();
            nodeRepository.deleteAll();
            monitoringLocationRepository.deleteAll();
            testGrpcService.reset();
        });
    }


    @Test
    void testCreateNode() throws VerificationException {

        NodeCreateDTO createDTO = NodeCreateDTO.newBuilder()
            .setLocation("location")
            .setLabel(NODE_LABEL)
            .setManagementIp("127.0.0.1")
            .build();

        NodeDTO node = serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).createNode(createDTO);

        assertThat(node.getNodeLabel()).isEqualTo(node.getNodeLabel());
        assertThat(ScanType.NODE_SCAN.name()).isEqualTo(node.getScanType());
        assertThat(node.getObjectId()).isEmpty();
        assertThat(node.getSystemName()).isEmpty();
        assertThat(node.getSystemDescr()).isEmpty();
        assertThat(node.getSystemLocation()).isEmpty();
        assertThat(node.getSystemContact()).isEmpty();
        await().atMost(10, TimeUnit.SECONDS).until(() -> testGrpcService.getRequests().size(), Matchers.is(4));

        org.assertj.core.api.Assertions.assertThat(testGrpcService.getRequests())
            .extracting(PublishTaskSetRequest::getTaskSet)
            .isNotNull()
            .extracting(TaskSet::getTaskDefinitionCount)
            .hasSize(EXPECTED_TASK_DEF_COUNT_FOR_NEW_LOCATION);
        verify(spyInterceptor).verifyAccessToken(authHeader);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    @Test
    void testCreateNodeWithBadIp() throws VerificationException {
        NodeCreateDTO createDTO = NodeCreateDTO.newBuilder()
            .setLocation("location")
            .setLabel("test-node")
            .setManagementIp("invalid ip")
            .build();

        assertThatThrownBy(() -> serviceStub
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader)))
            .createNode(createDTO))
            .isInstanceOf(StatusRuntimeException.class)
            .extracting(StatusProto::fromThrowable)
            .extracting(Status::getCode).isEqualTo(Code.INVALID_ARGUMENT_VALUE);

        assertThat(testGrpcService.getRequests()).asList().isEmpty();

        verify(spyInterceptor).verifyAccessToken(authHeader);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    @Test
    void testCreateNodeExistingIpAddress() throws Exception {
        String location = "location";
        String ip = "127.0.0.1";
        populateTables(location, ip);

        NodeCreateDTO createDTO = NodeCreateDTO.newBuilder()
            .setLocation(location)
            .setLabel(NODE_LABEL)
            .setManagementIp(ip)
            .build();
        assertThatThrownBy(() -> serviceStub
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).createNode(createDTO))
            .isInstanceOf(StatusRuntimeException.class)
            .extracting(StatusProto::fromThrowable)
            .extracting(Status::getCode, Status::getMessage)
            .containsExactly(Code.ALREADY_EXISTS_VALUE, "Ip address already exists for location");

        assertThat(testGrpcService.getRequests()).asList().isEmpty();

        verify(spyInterceptor).verifyAccessToken(authHeader);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    @Test
    void testCreateNodeExistingIpAddressDifferentTenantId() throws Exception {
        String location = "location";
        String ip = "127.0.0.1";

        populateTables(location, ip);

        NodeCreateDTO createDTO = NodeCreateDTO.newBuilder()
            .setLocation(location)
            .setLabel(NODE_LABEL)
            .setManagementIp(ip)
            .build();

        NodeDTO node = serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(differentTenantHeader))).createNode(createDTO);

        assertThat(node.getNodeLabel()).isEqualTo(node.getNodeLabel());
        assertThat(ScanType.NODE_SCAN.name()).isEqualTo(node.getScanType());
        await().atMost(10, TimeUnit.SECONDS).until(() -> testGrpcService.getRequests().size(), Matchers.is(4));

        org.assertj.core.api.Assertions.assertThat(testGrpcService.getRequests())
            .extracting(PublishTaskSetRequest::getTaskSet)
            .isNotNull()
            .extracting(TaskSet::getTaskDefinitionCount)
            .hasSize(EXPECTED_TASK_DEF_COUNT_FOR_NEW_LOCATION);
        verify(spyInterceptor).verifyAccessToken(differentTenantHeader);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    @Test
    void testCreateNodeExistingIpAddressNewDifferentLocation() throws Exception {
        String location = "location";
        String ip = "127.0.0.1";

        populateTables(location, ip);

        NodeCreateDTO createDTO = NodeCreateDTO.newBuilder()
            .setLocation("different")
            .setLabel(NODE_LABEL)
            .setManagementIp(ip)
            .build();

        NodeDTO node = serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).createNode(createDTO);

        assertThat(node.getNodeLabel()).isEqualTo(node.getNodeLabel());
        assertThat(ScanType.NODE_SCAN.name()).isEqualTo(node.getScanType());
        await().atMost(10, TimeUnit.SECONDS).until(() -> testGrpcService.getRequests().size(), Matchers.is(4));

        org.assertj.core.api.Assertions.assertThat(testGrpcService.getRequests())
            .extracting(PublishTaskSetRequest::getTaskSet)
            .isNotNull()
            .extracting(TaskSet::getTaskDefinitionCount)
            .hasSize(EXPECTED_TASK_DEF_COUNT_FOR_NEW_LOCATION);
        verify(spyInterceptor).verifyAccessToken(authHeader);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    @Test
    void testCreateNodeExistingIpAddressInDifferentLocation() throws Exception {
        String location = "location";
        String ip = "127.0.0.1";
        String secondLocation = "loc2";
        String secondIp = "127.0.0.2";

        populateTables(location, ip);
        populateTables(secondLocation, secondIp);

        NodeCreateDTO createDTO = NodeCreateDTO.newBuilder()
            .setLocation(secondLocation)
            .setLabel(NODE_LABEL)
            .setManagementIp(ip)
            .build();

        NodeDTO node = serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).createNode(createDTO);

        assertThat(node.getNodeLabel()).isEqualTo(node.getNodeLabel());
        assertThat(ScanType.NODE_SCAN.name()).isEqualTo(node.getScanType());
        await().atMost(10, TimeUnit.SECONDS).until(() -> testGrpcService.getRequests().size(), Matchers.is(2));

        verify(spyInterceptor).verifyAccessToken(authHeader);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));

        List<PublishTaskSetRequest> grpcRequests = testGrpcService.getRequests();
        assertThat(grpcRequests).asList().hasSize(2);

        PublishTaskSetRequest request = grpcRequests.get(0);
        TaskSet taskSet = request.getTaskSet();
        Assertions.assertNotNull(taskSet);
        assertThat(taskSet.getTaskDefinitionCount()).isEqualTo(EXPECTED_TASK_DEF_COUNT_WITHOUT_NEW_LOCATION);
    }

    @Test
    public void testNodeDeletionWithTaskSets() throws UnknownHostException {
        Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, tenantId).run(()->
        {
            String location = "location";
            String ip = "127.0.0.1";
            populateTables(location, ip, MonitorType.SNMP);
            var list = nodeRepository.findByNodeLabel(NODE_LABEL);
            var node = list.get(0);
            Assertions.assertNotNull(node);
            var optional = nodeRepository.findById(node.getId());
            Assertions.assertTrue(optional.isPresent());
            var tasks = nodeService.getTasksForNode(node);
            // Should have two detector tasks ( ICMP/SNMP)
            // one monitor task (SNMP) and one SNMP Collector task
            Assertions.assertEquals(tasks.size(), 5);
            testGrpcService.reset();
            detectorTaskSetService.sendDetectorTasks(node);
            taskSetHandler.sendMonitorTask(location, MonitorType.SNMP, node.getIpInterfaces().get(0), node.getId());
            taskSetHandler.sendCollectorTask(location, MonitorType.SNMP, node.getIpInterfaces().get(0), node.getId());
            var taskDefinitions = testGrpcService.getTaskDefinitions(location);
            var snmpTaskDefinitions = taskDefinitions.stream().filter(taskDefinition ->
                taskDefinition.getPluginName().contains(MonitorType.SNMP.name()) && taskDefinition.getNodeId() == node.getId()).toList();
            Assertions.assertEquals(snmpTaskDefinitions.size(), 3);
            testGrpcService.reset();
            nodeService.deleteNode(node.getId());
            optional = nodeRepository.findById(node.getId());
            Assertions.assertFalse(optional.isPresent());
            await().atMost(10, TimeUnit.SECONDS).until(() -> testGrpcService.getRequests(), Matchers.hasSize(1));
            taskDefinitions = testGrpcService.getTaskDefinitions(location);
            snmpTaskDefinitions = taskDefinitions.stream().filter(taskDefinition ->
                taskDefinition.getPluginName().contains(MonitorType.SNMP.name()) && taskDefinition.getNodeId() == node.getId()).toList();
            Assertions.assertEquals(snmpTaskDefinitions.size(), 0);
        });
    }

    private synchronized void populateTables(String location, String ip) throws UnknownHostException {
        final InetAddress inetAddress = InetAddress.getByName(ip);
        Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, tenantId).run(()->
        {
            Optional<MonitoringLocation> dbL = monitoringLocationRepository.findByLocation(location);
            MonitoringLocation dBLocation;
            if (dbL.isEmpty()) {
                MonitoringLocation ml = new MonitoringLocation();
                ml.setLocation(location);
                ml.setTenantId(tenantId);
                dBLocation = monitoringLocationRepository.save(ml);
            } else {
                dBLocation = dbL.get();
            }

            Node node = new Node();
            node.setTenantId(tenantId);
            node.setNodeLabel(NODE_LABEL);
            node.setScanType(ScanType.NODE_SCAN);
            node.setMonitoringLocation(dBLocation);
            node.setCreateTime(LocalDateTime.now());

            IpInterface ipInterface = new IpInterface();
            ipInterface.setTenantId(tenantId);
            ipInterface.setIpAddress(inetAddress);
            ipInterface.setNode(node);
            var ipInterfaces = new ArrayList<IpInterface>();
            ipInterfaces.add(ipInterface);
            node.setIpInterfaces(ipInterfaces);
            nodeRepository.save(node);
        });
    }

    private synchronized void populateTables(String location, String ip, MonitorType monitorType) {
        Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, tenantId).run(()->
        {
            try {
                populateTables(location, ip);
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
            var nodes = nodeRepository.findByNodeLabel("label");
            var node = nodes.get(0);
            var ipInterface = node.getIpInterfaces().get(0);
            var monitorServiceType = new MonitoredServiceType();
            monitorServiceType.setServiceName(monitorType.name());
            monitorServiceType.setTenantId(tenantId);
            var type = monitoredServiceTypeRepository.save(monitorServiceType);
            var monitorService = new MonitoredService();
            monitorService.setMonitoredServiceType(type);
            monitorService.setTenantId(tenantId);
            monitorService.setIpInterface(ipInterface);
            var monitoredServices = new ArrayList<MonitoredService>();
            monitoredServices.add(monitorService);
            ipInterface.setMonitoredServices(monitoredServices);
            nodeRepository.save(node);
        });
    }

    @Test
    void testCreateNodeMissingTenantId() throws Exception {

        String location = "location-for-missing-tenantId";
        NodeCreateDTO createDTO = NodeCreateDTO.newBuilder()
            .setLocation(location)
            .setLabel(NODE_LABEL)
            .setManagementIp("127.0.0.1")
            .build();
        assertThatThrownBy(() -> serviceStub
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(headerWithoutTenant))).createNode(createDTO))
            .isInstanceOf(StatusRuntimeException.class)
            .extracting(StatusProto::fromThrowable)
            .extracting(Status::getCode, Status::getMessage)
            .containsExactly(Code.UNAUTHENTICATED_VALUE, "Missing tenant id");

        //createNode will create tasks asynchronously, so wait enough time.
        Thread.sleep(5000);
        assertThat(testGrpcService.getTaskDefinitions(location).size()).isZero();

        verify(spyInterceptor).verifyAccessToken(headerWithoutTenant);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    @Test
    void testCreateNodeBadIPAddress() throws Exception {

        NodeCreateDTO createDTO = NodeCreateDTO.newBuilder()
            .setLocation("location")
            .setLabel(NODE_LABEL)
            .setManagementIp("BAD")
            .build();

        assertThatThrownBy(() -> serviceStub
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).createNode(createDTO))
            .isInstanceOf(StatusRuntimeException.class)
            .extracting(StatusProto::fromThrowable)
            .extracting(Status::getCode, Status::getMessage)
            .containsExactly(Code.INVALID_ARGUMENT_VALUE, "Bad management_ip: BAD");
        assertThat(testGrpcService.getRequests()).asList().isEmpty();

        verify(spyInterceptor).verifyAccessToken(authHeader);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }


    @Test
    void testListNodesShouldContainIpInterfaces() throws VerificationException, UnknownHostException {
        String location = "minion";
        String ip = "192.168.1.123";
        populateTables(location, ip);
        var nodeList = serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader)))
            .listNodes(Empty.newBuilder().build());
        assertThat(nodeList.getNodesCount()).isEqualTo(1);
        assertThat(nodeList.getNodes(0).getIpInterfacesList()).asList().hasSize(1);
        var nodeId = nodeList.getNodes(0).getId();
        var node = serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader)))
                .getNodeById(Int64Value.newBuilder().setValue(nodeId).build());
        assertThat(node.getIpInterfacesList()).asList().hasSize(1);
        verify(spyInterceptor, times(2)).verifyAccessToken(authHeader);
        verify(spyInterceptor, times(2)).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));

    }

    @Test
    void testDeleteNode() throws VerificationException {

        NodeCreateDTO createDTO = NodeCreateDTO.newBuilder()
            .setLocation("location")
            .setLabel(NODE_LABEL)
            .setManagementIp("127.0.0.1")
            .build();
        NodeDTO node = serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).createNode(createDTO);
        assertThat(node).isNotNull();
        assertThat(serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).deleteNode(Int64Value.of(node.getId())));
        verify(spyInterceptor, times(2)).verifyAccessToken(authHeader);
        verify(spyInterceptor, times(2)).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    @Test
    void testDeleteNodeWithTags() {
        Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, tenantId).run(()->
        {
            try {
                deleteNodeWithTags();
            } catch (VerificationException e) {
                throw new RuntimeException(e);
            }
        });
    }

    void deleteNodeWithTags() throws VerificationException {
        MonitoringLocation location = new MonitoringLocation();
        location.setLocation("location");
        location.setTenantId(tenantId);
        location = monitoringLocationRepository.saveAndFlush(location);

        Node node1 = new Node();
        node1.setNodeLabel("test-node-label-1");
        node1.setCreateTime(LocalDateTime.now());
        node1.setTenantId(tenantId);
        node1.setMonitoringLocation(location);
        node1.setMonitoringLocationId(location.getId());
        node1 = nodeRepository.saveAndFlush(node1);

        Node node2 = new Node();
        node2.setNodeLabel("test-node-label-2");
        node2.setCreateTime(LocalDateTime.now());
        node2.setTenantId(tenantId);
        node2.setMonitoringLocation(location);
        node2.setMonitoringLocationId(location.getId());
        node2 = nodeRepository.saveAndFlush(node2);

        TagCreateDTO createDTO1 = TagCreateDTO.newBuilder()
            .setName("test-tag-name-1")
            .build();

        TagCreateDTO createDTO2 = TagCreateDTO.newBuilder()
            .setName("test-tag-name-2")
            .build();

        TagCreateListDTO createListDTO1 = TagCreateListDTO.newBuilder()
            .addAllTags(List.of(createDTO1, createDTO2)).setNodeId(node1.getId()).build();

        tagService.addTags(tenantId, createListDTO1);

        TagCreateDTO createDTO3 = TagCreateDTO.newBuilder()
            .setName("test-tag-name-2")
            .build();

        TagCreateListDTO createListDTO3 = TagCreateListDTO.newBuilder()
            .addAllTags(List.of(createDTO3)).setNodeId(node2.getId()).build();

        List<TagDTO> tagsList3 = tagService.addTags(tenantId, createListDTO3);
        TagDTO tag3 = tagsList3.get(0);

        serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).deleteNode(Int64Value.of(node1.getId()));
        nodeRepository.flush();
        tagRepository.flush();

        List<Node> allNodes = nodeRepository.findAll();
        assertThat(allNodes).asList().hasSize(1);
        Node savedNode = allNodes.get(0);
        assertThat(savedNode.getId()).isEqualTo(node2.getId());

        List<Tag> allTags = tagRepository.findAll();
        assertThat(allTags).asList().hasSize(2);

        verify(spyInterceptor).verifyAccessToken(authHeader);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    @Test
    void testDeleteNodeNotFound() throws VerificationException {
        assertThatThrownBy(() -> serviceStub
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).deleteNode(Int64Value.of(100L)))
            .isInstanceOf(StatusRuntimeException.class)
            .extracting(StatusProto::fromThrowable)
            .extracting(Status::getCode).isEqualTo(Code.NOT_FOUND_VALUE);
        verify(spyInterceptor).verifyAccessToken(authHeader);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    @Test
    void testGetNodeWithNodeInfo() throws VerificationException, UnknownHostException {
        Node node = prepareNodes(1, true).get(0);
        NodeDTO nodeDTO = serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader)))
            .getNodeById(Int64Value.of(node.getId()));
        assertNodeDTO(nodeDTO, node);
        verify(spyInterceptor).verifyAccessToken(authHeader);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    @Test
    void testListNodeWithNodInfo() throws VerificationException, UnknownHostException {
        Node node = prepareNodes(1, true).get(0);
        NodeList nodeList = serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader)))
            .listNodes(Empty.newBuilder().build());
        assertThat(nodeList.getNodesList().size()).isEqualTo(1);
        assertNodeDTO(nodeList.getNodes(0), node);
        verify(spyInterceptor).verifyAccessToken(authHeader);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

   @Test
    void testStartScanByIds() throws VerificationException, UnknownHostException {
        List<Long> ids = prepareNodes(2, false).stream().map(Node::getId).collect(Collectors.toList());
        BoolValue result = serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader)))
                .startNodeScanByIds(NodeIdList.newBuilder().addAllIds(ids).build());
        assertThat(result.getValue()).isTrue();
        await().atMost(10, TimeUnit.SECONDS).until(() ->
            testGrpcService.getTaskDefinitions(TEST_LOCATION).stream().filter(taskDef ->
                taskDef.getType().equals(TaskType.SCANNER) && taskDef.getPluginName()
                    .contains("NodeScanner")).collect(Collectors.toSet()).size(), Matchers.is(2));
        verify(spyInterceptor).verifyAccessToken(authHeader);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    @Test
    void testStartScanByIdNotfound() throws VerificationException {
        assertThatThrownBy(() -> serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader)))
            .startNodeScanByIds(NodeIdList.newBuilder().addAllIds(List.of(1L)).build()))
            .isInstanceOf(StatusRuntimeException.class)
            .extracting(StatusProto::fromThrowable)
            .extracting(Status::getCode).isEqualTo(Code.NOT_FOUND_VALUE);
        verify(spyInterceptor).verifyAccessToken(authHeader);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    private void assertNodeDTO(NodeDTO actual, Node node) {
        node.setMonitoringLocationId(node.getMonitoringLocation().getId());
        NodeDTO expected = mapper.modelToDTO(node);
        assertThat(actual).isEqualTo(expected);
    }

    private List<Node> prepareNodes(int number, boolean withNodeInfo) throws UnknownHostException {
        MonitoringLocation location = new MonitoringLocation();
        location.setLocation(TEST_LOCATION);
        location.setTenantId(tenantId);
        Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, tenantId).run(()->
        {
            monitoringLocationRepository.save(location);
        });
        List<Node> list = new ArrayList<>();
        for(int i = 0; i < number; i++) {
            Node node = new Node();
            node.setNodeLabel("test-node" + (i + 1));
            node.setScanType(ScanType.NODE_SCAN);
            node.setMonitoringLocation(location);
            node.setTenantId(tenantId);
            node.setCreateTime(LocalDateTime.now());
            if(withNodeInfo) {
                node.setObjectId("12345." + i);
                node.setSystemLocation("systemLocation" +i);
                node.setSystemName("systemName"+1);
                node.setSystemDescr("desc"+1);
                node.setSystemContact("contact"+1);
            }
            Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, tenantId).run(()->
            {
                nodeRepository.save(node);
            });
            list.add(node);
        }
        createIpInterFaces(list);
        return list;
    }

    private void createIpInterFaces(List<Node> nodeList) throws UnknownHostException {
        String ip = "127.0.0.";
        for(int i = 0; i< nodeList.size(); i++) {
            Node node = nodeList.get(i);
            IpInterface ipInterface = new IpInterface();
            ipInterface.setNodeId(node.getId());
            ipInterface.setNode(node);
            ipInterface.setSnmpPrimary(true);
            ipInterface.setIpAddress(InetAddress.getByName(ip + i));
            ipInterface.setTenantId(tenantId);
            Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, tenantId).run(()->
            {
                ipInterfaceRepository.save(ipInterface);
            });
            node.setIpInterfaces(List.of(ipInterface));
        }
    }

}
