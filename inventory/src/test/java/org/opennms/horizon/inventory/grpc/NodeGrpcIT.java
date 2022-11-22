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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.grpc.detector.contract.Detector;
import org.opennms.horizon.inventory.InventoryApplication;
import org.opennms.horizon.inventory.SpringContextTestInitializer;
import org.opennms.horizon.inventory.dto.NodeCreateDTO;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.inventory.dto.NodeServiceGrpc;
import org.opennms.horizon.inventory.grpc.taskset.TestTaskSetGrpcService;
import org.opennms.horizon.inventory.model.IpInterface;
import org.opennms.horizon.inventory.model.MonitoringLocation;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.repository.IpInterfaceRepository;
import org.opennms.horizon.inventory.repository.MonitoringLocationRepository;
import org.opennms.horizon.inventory.repository.NodeRepository;
import org.opennms.taskset.contract.TaskSet;
import org.opennms.taskset.service.contract.PublishTaskSetRequest;
import org.opennms.taskset.service.contract.TaskSetServiceGrpc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import com.google.rpc.Code;
import com.google.rpc.Status;
import com.vladmihalcea.hibernate.type.basic.Inet;
import java.util.List;

import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = InventoryApplication.class)
@ContextConfiguration(initializers = {SpringContextTestInitializer.class})
class NodeGrpcIT extends GrpcTestBase {
    private static final int EXPECTED_TASK_DEF_COUNT = 2;
    private NodeServiceGrpc.NodeServiceBlockingStub serviceStub;

    @Autowired
    private NodeRepository nodeRepository;
    @Autowired
    private MonitoringLocationRepository monitoringLocationRepository;
    @Autowired
    private IpInterfaceRepository ipInterfaceRepository;

    private static TestTaskSetGrpcService testGrpcService;

    public void initStub() {
        serviceStub = NodeServiceGrpc.newBlockingStub(channel);
    }

    @BeforeAll
    public static void setup() throws IOException {
        testGrpcService = new TestTaskSetGrpcService();
        server = startMockServer(TaskSetServiceGrpc.SERVICE_NAME, testGrpcService);
    }

    @AfterEach
    public void cleanUp(){
        ipInterfaceRepository.deleteAll();
        nodeRepository.deleteAll();
        monitoringLocationRepository.deleteAll();

        testGrpcService.reset();
        channel.shutdown();
    }

    @AfterAll
    public static void tearDown() throws InterruptedException {
        server.shutdownNow();
        server.awaitTermination();
    }

    @Test
    void testCreateNode() throws Exception {
        setupGrpc();
        initStub();

        String label = "label";

        NodeCreateDTO createDTO = NodeCreateDTO.newBuilder()
            .setLocation("location")
            .setLabel(label)
            .setManagementIp("127.0.0.1")
            .build();

        NodeDTO node = serviceStub.createNode(createDTO);

        assertEquals(label, node.getNodeLabel());
        assertEquals(1, testGrpcService.getTimesCalled());

        List<PublishTaskSetRequest> grpcRequests = testGrpcService.getRequests();
        assertEquals(1, grpcRequests.size());

        PublishTaskSetRequest request = grpcRequests.get(0);
        TaskSet taskSet = request.getTaskSet();
        assertNotNull(taskSet);
        assertEquals(EXPECTED_TASK_DEF_COUNT, taskSet.getTaskDefinitionCount());
    }

    @Test
    void testCreateNodeExistingIpAddress() throws Exception {
        String location = "location";
        String ip = "127.0.0.1";
        String label = "label";

        setupGrpc();
        populateTables(location, ip);
        initStub();

        NodeCreateDTO createDTO = NodeCreateDTO.newBuilder()
            .setLocation(location)
            .setLabel(label)
            .setManagementIp(ip)
            .build();

        StatusRuntimeException exception = Assertions.assertThrows(StatusRuntimeException.class, ()->serviceStub.createNode(createDTO));
        Status status = StatusProto.fromThrowable(exception);
        assertThat(status.getCode()).isEqualTo(Code.ALREADY_EXISTS_VALUE);
        assertThat(status.getMessage()).isEqualTo("Ip address already exists for location");
        assertEquals(0, testGrpcService.getTimesCalled());
    }

    @Test
    void testCreateNodeExistingIpAddressDifferentTenantId() throws Exception {
        String location = "location";
        String ip = "127.0.0.1";
        String label = "label";

        setupGrpcWithDifferentTenantID();
        populateTables(location, ip);
        initStub();

        NodeCreateDTO createDTO = NodeCreateDTO.newBuilder()
            .setLocation(location)
            .setLabel(label)
            .setManagementIp(ip)
            .build();

        NodeDTO node = serviceStub.createNode(createDTO);

        assertEquals(label, node.getNodeLabel());
        assertEquals(1, testGrpcService.getTimesCalled());

        List<PublishTaskSetRequest> grpcRequests = testGrpcService.getRequests();
        assertEquals(1, grpcRequests.size());

        PublishTaskSetRequest request = grpcRequests.get(0);
        TaskSet taskSet = request.getTaskSet();
        assertNotNull(taskSet);
        assertEquals(EXPECTED_TASK_DEF_COUNT, taskSet.getTaskDefinitionCount());
    }

    @Test
    void testCreateNodeExistingIpAddressNewDifferentLocation() throws Exception {
        String location = "location";
        String ip = "127.0.0.1";
        String label = "label";

        setupGrpc();
        populateTables(location, ip);
        initStub();

        NodeCreateDTO createDTO = NodeCreateDTO.newBuilder()
            .setLocation("different")
            .setLabel(label)
            .setManagementIp(ip)
            .build();

        NodeDTO node = serviceStub.createNode(createDTO);

        assertEquals(label, node.getNodeLabel());
        assertEquals(1, testGrpcService.getTimesCalled());

        List<PublishTaskSetRequest> grpcRequests = testGrpcService.getRequests();
        assertEquals(1, grpcRequests.size());

        PublishTaskSetRequest request = grpcRequests.get(0);
        TaskSet taskSet = request.getTaskSet();
        assertNotNull(taskSet);
        assertEquals(EXPECTED_TASK_DEF_COUNT, taskSet.getTaskDefinitionCount());
    }

    @Test
    void testCreateNodeExistingIpAddressInDifferentLocation() throws Exception {
        String location = "location";
        String ip = "127.0.0.1";
        String label = "label";
        String secondLocation = "loc2";
        String secondIp = "127.0.0.2";

        setupGrpc();
        populateTables(location, ip);
        populateTables(secondLocation, secondIp);
        initStub();

        NodeCreateDTO createDTO = NodeCreateDTO.newBuilder()
            .setLocation(secondLocation)
            .setLabel(label)
            .setManagementIp(ip)
            .build();

        NodeDTO node = serviceStub.createNode(createDTO);

        assertEquals(label, node.getNodeLabel());
        assertEquals(1, testGrpcService.getTimesCalled());

        List<PublishTaskSetRequest> grpcRequests = testGrpcService.getRequests();
        assertEquals(1, grpcRequests.size());

        PublishTaskSetRequest request = grpcRequests.get(0);
        TaskSet taskSet = request.getTaskSet();
        assertNotNull(taskSet);
        assertEquals(EXPECTED_TASK_DEF_COUNT, taskSet.getTaskDefinitionCount());
    }

    private void populateTables(String location, String ip) {
        MonitoringLocation ml = new MonitoringLocation();
        ml.setLocation(location);
        ml.setTenantId(tenantId);
        MonitoringLocation savedML = monitoringLocationRepository.save(ml);

        Node node = new Node();
        node.setTenantId(tenantId);
        node.setNodeLabel("label");
        node.setMonitoringLocation(savedML);
        node.setCreateTime(LocalDateTime.now());
        Node savedNode = nodeRepository.save(node);

        IpInterface ipInterface = new IpInterface();
        ipInterface.setTenantId(tenantId);
        ipInterface.setIpAddress(new Inet(ip));
        ipInterface.setNode(savedNode);
        IpInterface savedIpInterface = ipInterfaceRepository.save(ipInterface);
    }

    @Test
    void testCreateNodeMissingTenantId() throws Exception {
        setupGrpcWithOutTenantID();
        initStub();

        String label = "label";

        NodeCreateDTO createDTO = NodeCreateDTO.newBuilder()
            .setLocation("location")
            .setLabel(label)
            .setManagementIp("127.0.0.1")
            .build();

        StatusRuntimeException exception = Assertions.assertThrows(StatusRuntimeException.class, ()->serviceStub.createNode(createDTO));
        Status status = StatusProto.fromThrowable(exception);
        assertThat(status.getCode()).isEqualTo(Code.UNAUTHENTICATED_VALUE);
        assertThat(status.getMessage()).isEqualTo("Missing tenant id");
        assertEquals(0, testGrpcService.getTimesCalled());
    }

    @Test
    void testCreateNodeBadIPAddress() throws Exception {
        setupGrpc();
        initStub();

        String label = "label";

        NodeCreateDTO createDTO = NodeCreateDTO.newBuilder()
            .setLocation("location")
            .setLabel(label)
            .setManagementIp("BAD")
            .build();

        StatusRuntimeException exception = Assertions.assertThrows(StatusRuntimeException.class, ()->serviceStub.createNode(createDTO));
        Status status = StatusProto.fromThrowable(exception);
        assertThat(status.getCode()).isEqualTo(Code.INVALID_ARGUMENT_VALUE);
        assertThat(status.getMessage()).isEqualTo("Bad management_ip: BAD");
        assertEquals(0, testGrpcService.getTimesCalled());
    }
}
