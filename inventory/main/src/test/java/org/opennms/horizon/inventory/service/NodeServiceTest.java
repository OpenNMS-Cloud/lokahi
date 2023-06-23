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


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.opennms.horizon.inventory.dto.MonitoredState;
import org.opennms.horizon.inventory.component.TagPublisher;
import org.opennms.horizon.inventory.dto.NodeCreateDTO;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.inventory.dto.TagCreateDTO;
import org.opennms.horizon.inventory.dto.TagCreateListDTO;
import org.opennms.horizon.inventory.exception.EntityExistException;
import org.opennms.horizon.inventory.exception.LocationNotFoundException;
import org.opennms.horizon.inventory.mapper.IpInterfaceMapper;
import org.opennms.horizon.inventory.mapper.NodeMapper;
import org.opennms.horizon.inventory.mapper.SnmpInterfaceMapper;
import org.opennms.horizon.inventory.model.IpInterface;
import org.opennms.horizon.inventory.model.MonitoringLocation;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.model.Tag;
import org.opennms.horizon.inventory.repository.IpInterfaceRepository;
import org.opennms.horizon.inventory.repository.MonitoringLocationRepository;
import org.opennms.horizon.inventory.repository.NodeRepository;
import org.opennms.horizon.inventory.service.taskset.CollectorTaskSetService;
import org.opennms.horizon.inventory.service.taskset.MonitorTaskSetService;
import org.opennms.horizon.inventory.service.taskset.ScannerTaskSetService;
import org.opennms.horizon.inventory.service.taskset.publisher.TaskSetPublisher;
import org.opennms.taskset.contract.ScanType;

public class NodeServiceTest {

    private final static String TENANT_ID = "test-tenant";

    private NodeService nodeService;
    private NodeRepository mockNodeRepository;
    private MonitoringLocationRepository mockMonitoringLocationRepository;
    private IpInterfaceRepository mockIpInterfaceRepository;
    private ConfigUpdateService mockConfigUpdateService;
    private TagService tagService;
    private TagPublisher mockTagPublisher;

    @BeforeEach
    void prepareTest() {
        NodeMapper nodeMapper = Mappers.getMapper(NodeMapper.class);
        mockNodeRepository = mock(NodeRepository.class);
        mockMonitoringLocationRepository = mock(MonitoringLocationRepository.class);
        mockIpInterfaceRepository = mock(IpInterfaceRepository.class);
        mockConfigUpdateService = mock(ConfigUpdateService.class);
        tagService = mock(TagService.class);
        mockTagPublisher = mock(TagPublisher.class);


        nodeService = new NodeService(mockNodeRepository,
            mockMonitoringLocationRepository,
            mockIpInterfaceRepository,
            mockConfigUpdateService,
            mock(CollectorTaskSetService.class),
            mock(MonitorTaskSetService.class),
            mock(ScannerTaskSetService.class),
            mock(TaskSetPublisher.class),
            tagService,
            nodeMapper,
            mock(SnmpInterfaceMapper.class),
            mock(IpInterfaceMapper.class),
            mockTagPublisher);

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
    public void deleteNodeNotExist() {
        assertThatThrownBy(() -> nodeService.deleteNode(1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Node with ID : 1doesn't exist");
        verify(mockNodeRepository).findById(any());
    }

    @Test
    public void deleteNode() {
        Node node = mock(Node.class);
        MonitoringLocation monitoringLocation = mock(MonitoringLocation.class);
        when(node.getMonitoringLocation()).thenReturn(monitoringLocation);
        List<Tag> tags = getTags();
        when(node.getTags()).thenReturn(tags);

        Optional<Node> optNode = Optional.of(node);
        when(mockNodeRepository.findById(1L)).thenReturn(optNode);

        nodeService.deleteNode(1);

        verify(mockNodeRepository).findById(any());
        verify(mockNodeRepository).deleteById(any());
    }

    private List<Tag> getTags() {
        List<Tag> tags = new ArrayList<>();
        Tag t2 = mock(Tag.class);
        when(t2.getNodes()).thenReturn(Arrays.asList(mock(Node.class)));
        when(t2.getName()).thenReturn("FRED");
        when(t2.getTenantId()).thenReturn("TENANT");
        tags.add(t2);
        return tags;
    }

    @Test
    public void createNode() throws EntityExistException, LocationNotFoundException {
        String tenant = "ANY";
        MonitoringLocation ml = new MonitoringLocation();
        ml.setId(5678L);
        ml.setTenantId(tenant);
        ml.setLocation("location 5678L");

        when(mockMonitoringLocationRepository.findByIdAndTenantId(5678L, tenant)).thenReturn(Optional.of(ml));

        NodeCreateDTO nodeCreateDTO = NodeCreateDTO.newBuilder()
            .setLabel("Label")
            .setLocationId("5678")
            .setManagementIp("127.0.0.1")
            .addTags(TagCreateDTO.newBuilder().setName("tag-name").build())
            .build();
        doReturn(Optional.empty()).when(mockIpInterfaceRepository).findByIpLocationIdTenantAndScanType(any(InetAddress.class), eq(5678L), eq(tenant),eq(ScanType.NODE_SCAN));

        nodeService.createNode(nodeCreateDTO, ScanType.NODE_SCAN, tenant);
        verify(mockNodeRepository).save(any(Node.class));
        verify(mockIpInterfaceRepository).save(any(IpInterface.class));
        verify(mockMonitoringLocationRepository).findByIdAndTenantId(5678L, tenant);
        verify(tagService).addTags(eq(tenant), any(TagCreateListDTO.class));
        verify(mockIpInterfaceRepository).findByIpLocationIdTenantAndScanType(any(InetAddress.class), eq(5678L), eq(tenant), eq(ScanType.NODE_SCAN));
        // Check the default state
        assertEquals(MonitoredState.DETECTED, nodeCreateDTO.getMonitoredState());
    }

    @Test
    public void createNodeExistingLocation() throws EntityExistException, LocationNotFoundException {
        String tenantId = "ANY";

        NodeCreateDTO nodeCreateDTO = NodeCreateDTO.newBuilder()
            .setLabel("Label")
            .setLocationId("1234")
            .setManagementIp("127.0.0.1")
            .build();

        doReturn(Optional.of(new MonitoringLocation())).when(mockMonitoringLocationRepository).findByIdAndTenantId(1234, tenantId);

        doReturn(Optional.empty()).when(mockIpInterfaceRepository).findByIpLocationIdTenantAndScanType(any(InetAddress.class), eq(1234L), eq(tenantId), eq(ScanType.NODE_SCAN));

        nodeService.createNode(nodeCreateDTO, ScanType.NODE_SCAN, tenantId);
        verify(mockNodeRepository).save(any(Node.class));
        verify(mockIpInterfaceRepository).save(any(IpInterface.class));
        verify(mockMonitoringLocationRepository).findByIdAndTenantId(1234L, tenantId);
        verify(mockConfigUpdateService, timeout(5000).times(0)).sendConfigUpdate(eq(tenantId), any());
        verify(mockIpInterfaceRepository).findByIpLocationIdTenantAndScanType(any(InetAddress.class), eq(1234L), eq(tenantId), eq(ScanType.NODE_SCAN));
    }

    @Test
    public void createNodeNoIp() throws EntityExistException, LocationNotFoundException {
        String tenant = "TENANT";
        String location = "101010";
        MonitoringLocation ml = new MonitoringLocation();
        ml.setId(101010L);
        ml.setTenantId(tenant);
        ml.setLocation(location);
        when(mockMonitoringLocationRepository.findByIdAndTenantId(Long.parseLong(location), tenant)).thenReturn(Optional.of(ml));

        NodeCreateDTO nodeCreateDTO = NodeCreateDTO.newBuilder()
            .setLabel("Label")
            .setLocationId(location)
            .build();

        nodeService.createNode(nodeCreateDTO, ScanType.NODE_SCAN, tenant);
        verify(mockNodeRepository).save(any(Node.class));
        verify(mockMonitoringLocationRepository).findByIdAndTenantId(Long.parseLong(location), tenant);
        verifyNoInteractions(mockIpInterfaceRepository);
    }

    @Test
    public void createNodeWithLocationTestLocationExist() throws EntityExistException, LocationNotFoundException {
        NodeCreateDTO nodeCreate = NodeCreateDTO.newBuilder()
            .setLabel("test-node")
            .setLocationId("321")
            .setManagementIp("127.0.0.1").build();
        MonitoringLocation location = new MonitoringLocation();
        doReturn(Optional.of(location)).when(mockMonitoringLocationRepository).findByIdAndTenantId(321L, TENANT_ID);
        doReturn(Optional.empty()).when(mockIpInterfaceRepository).findByIpLocationIdTenantAndScanType(any(InetAddress.class), eq(321L), eq(TENANT_ID), eq(ScanType.NODE_SCAN));
        nodeService.createNode(nodeCreate, ScanType.NODE_SCAN, TENANT_ID);
        verify(mockMonitoringLocationRepository).findByIdAndTenantId(321, TENANT_ID);
        verify(mockNodeRepository).save(any(Node.class));
        verify(mockIpInterfaceRepository).save(any(IpInterface.class));
        verify(mockIpInterfaceRepository).findByIpLocationIdTenantAndScanType(any(InetAddress.class), eq(321L), eq(
            TENANT_ID), eq(ScanType.NODE_SCAN));
    }

    @Test
    public void createNodeWithLocationNotExist() throws EntityExistException, LocationNotFoundException {
        MonitoringLocation ml = new MonitoringLocation();
        ml.setTenantId(TENANT_ID);
        ml.setLocation("US-West-1");
        NodeCreateDTO nodeCreate = NodeCreateDTO.newBuilder()
            .setLabel("test-node")
            .setLocationId("1020")
            .setManagementIp("127.0.0.1").build();
        doReturn(Optional.empty()).when(mockMonitoringLocationRepository).findByLocationAndTenantId("US-West-1", TENANT_ID);
        doReturn(new MonitoringLocation()).when(mockMonitoringLocationRepository).save(any(MonitoringLocation.class));
        doReturn(Optional.empty()).when(mockIpInterfaceRepository).findByIpLocationIdTenantAndScanType(any(InetAddress.class), eq(1020L), eq(TENANT_ID), eq(ScanType.NODE_SCAN));
        assertThatThrownBy(() -> nodeService.createNode(nodeCreate, ScanType.NODE_SCAN, TENANT_ID))
            .isInstanceOf(LocationNotFoundException.class);
        verify(mockMonitoringLocationRepository).findByIdAndTenantId(1020L, TENANT_ID);
//        verify(mockNodeRepository).save(any(Node.class));
//        verify(mockIpInterfaceRepository).save(any(IpInterface.class));
        verify(mockIpInterfaceRepository).findByIpLocationIdTenantAndScanType(any(InetAddress.class), eq(1020L), eq(TENANT_ID), eq(ScanType.NODE_SCAN));
    }

    @Test
    public void testListNodesByIds() {
        MonitoringLocation location1 = new MonitoringLocation();
        location1.setId(123L);
        location1.setLocation("location-1");

        MonitoringLocation location2 = new MonitoringLocation();
        location2.setId(303L);
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

        doReturn(List.of(node1, node2, node3)).when(mockNodeRepository).findByIdInAndTenantId(List.of(1L, 2L, 3L), TENANT_ID);

        Map<Long, List<NodeDTO>> result = nodeService.listNodeByIds(List.of(1L, 2L, 3L), TENANT_ID);
        assertThat(result).asInstanceOf(InstanceOfAssertFactories.MAP).hasSize(2)
            .containsKeys(location1.getId(), location2.getId())
            .extractingByKey(location1.getId())
            .asList().hasSize(2).extracting("nodeLabel_").containsExactly(node1.getNodeLabel(), node2.getNodeLabel());
        assertThat(result.get(location2.getId())).asList().hasSize(1).extracting("nodeLabel_").containsExactly(node3.getNodeLabel());
        verify(mockNodeRepository).findByIdInAndTenantId(List.of(1L, 2L, 3L), TENANT_ID);
    }

    @Test
    public void testListNodesByIdsEmpty() {
        doReturn(Collections.emptyList()).when(mockNodeRepository).findByIdInAndTenantId(List.of(1L, 2L, 3L), TENANT_ID);
        Map<Long, List<NodeDTO>> result = nodeService.listNodeByIds(List.of(1L, 2L, 3L), TENANT_ID);
        assertThat(result).isEmpty();
        verify(mockNodeRepository).findByIdInAndTenantId(List.of(1L, 2L, 3L), TENANT_ID);
    }

    @Test
    public void testCreateNodeIPExists() {
        Node node = new Node();
        IpInterface ipInterface = new IpInterface();
        ipInterface.setNode(node);
        NodeCreateDTO nodeCreate = NodeCreateDTO.newBuilder()
            .setLabel("test-node")
            .setManagementIp("127.0.0.1")
            .setLocationId("2020")
            .build();
        doReturn(Optional.of(ipInterface)).when(mockIpInterfaceRepository).findByIpLocationIdTenantAndScanType(any(InetAddress.class), eq(2020L), eq(TENANT_ID), eq(ScanType.NODE_SCAN));
        assertThatThrownBy(() -> nodeService.createNode(nodeCreate, ScanType.NODE_SCAN, TENANT_ID))
            .isInstanceOf(EntityExistException.class)
            .hasMessageContaining("already exists in the system ");
        verify(mockIpInterfaceRepository).findByIpLocationIdTenantAndScanType(any(InetAddress.class), eq(2020L), eq(TENANT_ID), eq(ScanType.NODE_SCAN));
        verifyNoInteractions(mockNodeRepository);
        verifyNoInteractions(mockMonitoringLocationRepository);
        verifyNoInteractions(tagService);
        verifyNoInteractions(mockConfigUpdateService);
    }
}
