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

package org.opennms.horizon.inventory.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.opennms.horizon.inventory.dto.NodeCreateDTO;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.inventory.dto.TagCreateDTO;
import org.opennms.horizon.inventory.dto.TagCreateListDTO;
import org.opennms.horizon.inventory.exception.EntityExistException;
import org.opennms.horizon.inventory.mapper.NodeMapper;
import org.opennms.horizon.inventory.model.IpInterface;
import org.opennms.horizon.inventory.model.MonitoringLocation;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.repository.IpInterfaceRepository;
import org.opennms.horizon.inventory.repository.MonitoringLocationRepository;
import org.opennms.horizon.inventory.repository.NodeRepository;
import org.opennms.horizon.inventory.service.taskset.CollectorTaskSetService;
import org.opennms.horizon.inventory.service.taskset.MonitorTaskSetService;
import org.opennms.horizon.inventory.service.taskset.ScannerTaskSetService;
import org.opennms.horizon.inventory.service.taskset.publisher.TaskSetPublisher;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.opennms.taskset.contract.ScanType;

public class NodeServiceTest {
    private NodeService nodeService;
    private NodeRepository mockNodeRepository;
    private MonitoringLocationRepository mockMonitoringLocationRepository;
    private IpInterfaceRepository mockIpInterfaceRepository;
    private ConfigUpdateService mockConfigUpdateService;
    private TagService tagService;
    private final String tenantID = "test-tenant";

    @BeforeEach
    void prepareTest() {
        NodeMapper nodeMapper = Mappers.getMapper(NodeMapper.class);
        mockNodeRepository = mock(NodeRepository.class);
        mockMonitoringLocationRepository = mock(MonitoringLocationRepository.class);
        mockIpInterfaceRepository = mock(IpInterfaceRepository.class);
        mockConfigUpdateService = mock(ConfigUpdateService.class);
        tagService = mock(TagService.class);


        nodeService = new NodeService(mockNodeRepository,
            mockMonitoringLocationRepository,
            mockIpInterfaceRepository,
            mockConfigUpdateService,
            mock(CollectorTaskSetService.class),
            mock(MonitorTaskSetService.class),
            mock(ScannerTaskSetService.class),
            mock(TaskSetPublisher.class),
            tagService,
            nodeMapper);

        Node node = new Node();
        doReturn(node).when(mockNodeRepository).save(any(node.getClass()));
    }

    @AfterEach
    public void afterTest(){
        verifyNoMoreInteractions(mockNodeRepository);
        verifyNoMoreInteractions(mockMonitoringLocationRepository);
        verifyNoMoreInteractions(mockIpInterfaceRepository);
    }

    @Test
    public void createNode() throws EntityExistException {
        String tenant = "ANY";
        String location = "loc";
        MonitoringLocation ml = new MonitoringLocation();
        ml.setTenantId(tenant);
        ml.setLocation(location);

        when(mockMonitoringLocationRepository.save(any())).thenReturn(ml);

        NodeCreateDTO nodeCreateDTO = NodeCreateDTO.newBuilder()
            .setLabel("Label")
            .setLocation("loc")
            .setManagementIp("127.0.0.1")
            .addTags(TagCreateDTO.newBuilder().setName("tag-name").build())
            .build();
        doReturn(Optional.empty()).when(mockIpInterfaceRepository).findByIpAddressAndLocationAndTenantId(any(InetAddress.class), eq(nodeCreateDTO.getLocation()), eq(tenant));

        nodeService.createNode(nodeCreateDTO, ScanType.NODE_SCAN, tenant);
        verify(mockNodeRepository).save(any(Node.class));
        verify(mockIpInterfaceRepository).save(any(IpInterface.class));
        verify(mockMonitoringLocationRepository).save(any(MonitoringLocation.class));
        verify(mockMonitoringLocationRepository).findByLocationAndTenantId(location, tenant);
        verify(tagService).addTags(eq(tenant), any(TagCreateListDTO.class));
        verify(mockConfigUpdateService, timeout(5000)).sendConfigUpdate(tenant, location);
        verify(mockIpInterfaceRepository).findByIpAddressAndLocationAndTenantId(any(InetAddress.class), eq(nodeCreateDTO.getLocation()), eq(tenant));
    }

    @Test
    public void createNodeExistingLocation() throws EntityExistException {
        String location = "loc";
        String tenantId = "ANY";

        NodeCreateDTO nodeCreateDTO = NodeCreateDTO.newBuilder()
            .setLabel("Label")
            .setLocation(location)
            .setManagementIp("127.0.0.1")
            .build();

        doReturn(Optional.of(new MonitoringLocation())).when(mockMonitoringLocationRepository).findByLocationAndTenantId(location, tenantId);

        doReturn(Optional.empty()).when(mockIpInterfaceRepository).findByIpAddressAndLocationAndTenantId(any(InetAddress.class), eq(nodeCreateDTO.getLocation()), eq(tenantId));

        nodeService.createNode(nodeCreateDTO, ScanType.NODE_SCAN, tenantId);
        verify(mockNodeRepository).save(any(Node.class));
        verify(mockIpInterfaceRepository).save(any(IpInterface.class));
        verify(mockMonitoringLocationRepository).findByLocationAndTenantId(location, tenantId);
        verify(mockConfigUpdateService, timeout(5000).times(0)).sendConfigUpdate(eq(tenantId), any());
        verify(mockIpInterfaceRepository).findByIpAddressAndLocationAndTenantId(any(InetAddress.class), eq(nodeCreateDTO.getLocation()), eq(tenantId));
    }

    @Test
    public void createNodeNoIp() throws EntityExistException {
        String tenant = "TENANT";
        String location = "LOCATION";
        MonitoringLocation ml = new MonitoringLocation();
        ml.setTenantId(tenant);
        ml.setLocation(location);

        when(mockMonitoringLocationRepository.save(any(MonitoringLocation.class))).thenReturn(ml);

        NodeCreateDTO nodeCreateDTO = NodeCreateDTO.newBuilder()
            .setLabel("Label")
            .setLocation(location)
            .build();

        nodeService.createNode(nodeCreateDTO, ScanType.NODE_SCAN, tenant);
        verify(mockNodeRepository).save(any(Node.class));
        verify(mockMonitoringLocationRepository).findByLocationAndTenantId(location, tenant);
        verify(mockMonitoringLocationRepository).save(any(MonitoringLocation.class));
        verifyNoInteractions(mockIpInterfaceRepository);
    }

    @Test
    public void createNodeWithLocationDefaultLocationExist() throws EntityExistException {
        NodeCreateDTO nodeCreate = NodeCreateDTO.newBuilder()
            .setLabel("test-node")
            .setManagementIp("127.0.0.1").build();
        MonitoringLocation location = new MonitoringLocation();
        doReturn(Optional.of(location)).when(mockMonitoringLocationRepository).findByLocationAndTenantId(GrpcConstants.DEFAULT_LOCATION, tenantID);
        doReturn(Optional.empty()).when(mockIpInterfaceRepository).findByIpAddressAndLocationAndTenantId(any(InetAddress.class), eq(nodeCreate.getLocation()), eq(tenantID));
        nodeService.createNode(nodeCreate, ScanType.NODE_SCAN, tenantID);
        verify(mockMonitoringLocationRepository).findByLocationAndTenantId(GrpcConstants.DEFAULT_LOCATION, tenantID);
        verify(mockNodeRepository).save(any(Node.class));
        verify(mockIpInterfaceRepository).save(any(IpInterface.class));
        verify(mockIpInterfaceRepository).findByIpAddressAndLocationAndTenantId(any(InetAddress.class), eq(nodeCreate.getLocation()), eq(tenantID));
    }

    @Test
    public void createNodeWithLocationDefaultLocationNotExist() throws EntityExistException {
        NodeCreateDTO nodeCreate = NodeCreateDTO.newBuilder()
            .setLabel("test-node")
            .setManagementIp("127.0.0.1").build();
        doReturn(Optional.empty()).when(mockMonitoringLocationRepository).findByLocationAndTenantId(GrpcConstants.DEFAULT_LOCATION, tenantID);
        doReturn(new MonitoringLocation()).when(mockMonitoringLocationRepository).save(any(MonitoringLocation.class));
        doReturn(Optional.empty()).when(mockIpInterfaceRepository).findByIpAddressAndLocationAndTenantId(any(InetAddress.class), eq(nodeCreate.getLocation()), eq(tenantID));
        ArgumentCaptor<MonitoringLocation> captor = ArgumentCaptor.forClass(MonitoringLocation.class);
        nodeService.createNode(nodeCreate, ScanType.NODE_SCAN, tenantID);
        verify(mockMonitoringLocationRepository).findByLocationAndTenantId(GrpcConstants.DEFAULT_LOCATION, tenantID);
        verify(mockMonitoringLocationRepository).save(captor.capture());
        assertThat(captor.getValue().getLocation()).isEqualTo(GrpcConstants.DEFAULT_LOCATION);
        verify(mockNodeRepository).save(any(Node.class));
        verify(mockIpInterfaceRepository).save(any(IpInterface.class));
        verify(mockIpInterfaceRepository).findByIpAddressAndLocationAndTenantId(any(InetAddress.class), eq(nodeCreate.getLocation()), eq(tenantID));
    }

    @Test
    public void testListNodesByIds() {
        MonitoringLocation location1 = new MonitoringLocation();
        location1.setLocation("location-1");

        MonitoringLocation location2 = new MonitoringLocation();
        location2.setLocation("location-2");

        Node node1 = new Node();
        node1.setId(1L);
        node1.setNodeLabel("node-1");
        node1.setMonitoringLocation(location1);
        node1.setCreateTime(LocalDateTime.now());
        Node node2 = new Node();
        node2.setId(2L);
        node2.setNodeLabel("node-2");
        node2.setMonitoringLocation(location1);
        node2.setCreateTime(LocalDateTime.now());
        Node node3 = new Node();
        node3.setId(3L);
        node3.setNodeLabel("node-3");
        node3.setMonitoringLocation(location2);
        node3.setCreateTime(LocalDateTime.now());

        doReturn(List.of(node1, node2, node3)).when(mockNodeRepository).findByIdInAndTenantId(List.of(1L, 2L, 3L), tenantID);

        Map<String, List<NodeDTO>> result = nodeService.listNodeByIds(List.of(1L, 2L, 3L), tenantID);
        assertThat(result).asInstanceOf(InstanceOfAssertFactories.MAP).hasSize(2)
            .containsKeys(location1.getLocation(), location2.getLocation())
            .extractingByKey(location1.getLocation())
            .asList().hasSize(2).extracting("nodeLabel_").containsExactly(node1.getNodeLabel(), node2.getNodeLabel());
        assertThat(result.get(location2.getLocation())).asList().hasSize(1).extracting("nodeLabel_").containsExactly(node3.getNodeLabel());
        verify(mockNodeRepository).findByIdInAndTenantId(List.of(1L, 2L, 3L), tenantID);
    }

    @Test
    public void testListNodesByIdsEmpty() {
        doReturn(Collections.emptyList()).when(mockNodeRepository).findByIdInAndTenantId(List.of(1L, 2L, 3L), tenantID);
        Map<String, List<NodeDTO>> result = nodeService.listNodeByIds(List.of(1L, 2L, 3L), tenantID);
        assertThat(result.isEmpty()).isTrue();
        verify(mockNodeRepository).findByIdInAndTenantId(List.of(1L, 2L, 3L), tenantID);
    }

    @Test
    public void testCreateNodeIPExists() {
        Node node = new Node();
        IpInterface ipInterface = new IpInterface();
        ipInterface.setNode(node);
        NodeCreateDTO nodeCreate = NodeCreateDTO.newBuilder()
            .setLabel("test-node")
            .setManagementIp("127.0.0.1").build();
        doReturn(Optional.of(ipInterface)).when(mockIpInterfaceRepository).findByIpAddressAndLocationAndTenantId(any(InetAddress.class), eq(nodeCreate.getLocation()), eq(tenantID));
        assertThatThrownBy(() -> nodeService.createNode(nodeCreate, ScanType.NODE_SCAN, tenantID))
            .isInstanceOf(EntityExistException.class)
            .hasMessageContaining("already exists in the system ");
        verify(mockIpInterfaceRepository).findByIpAddressAndLocationAndTenantId(any(InetAddress.class), eq(nodeCreate.getLocation()), eq(tenantID));
        verifyNoInteractions(mockNodeRepository);
        verifyNoInteractions(mockMonitoringLocationRepository);
        verifyNoInteractions(tagService);
        verifyNoInteractions(mockConfigUpdateService);
    }
}
