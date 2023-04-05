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

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.inventory.mapper.node.NodeMapper;
import org.opennms.horizon.inventory.model.MonitoringLocation;
import org.opennms.horizon.inventory.model.node.Node;
import org.opennms.horizon.inventory.repository.IpInterfaceRepository;
import org.opennms.horizon.inventory.repository.node.NodeRepository;
import org.opennms.horizon.inventory.service.node.NodeService;
import org.opennms.horizon.inventory.service.taskset.CollectorTaskSetService;
import org.opennms.horizon.inventory.service.taskset.MonitorTaskSetService;
import org.opennms.horizon.inventory.service.taskset.ScannerTaskSetService;
import org.opennms.horizon.inventory.service.taskset.publisher.TaskSetPublisher;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class NodeServiceTest {
    private NodeService nodeService;
    private NodeRepository mockNodeRepository;
    private IpInterfaceRepository mockIpInterfaceRepository;
    private final String tenantID = "test-tenant";

    @BeforeEach
    void prepareTest() {
        mockNodeRepository = mock(NodeRepository.class);
        mockIpInterfaceRepository = mock(IpInterfaceRepository.class);

        nodeService = new NodeService(mockNodeRepository,
            mock(CollectorTaskSetService.class),
            mock(MonitorTaskSetService.class),
            mock(ScannerTaskSetService.class),
            mock(TaskSetPublisher.class),
            mock(NodeMapper.class));
    }

    @AfterEach
    public void afterTest() {
        verifyNoMoreInteractions(mockNodeRepository);
        verifyNoMoreInteractions(mockIpInterfaceRepository);
    }

    @Test
    void testListNodesByIds() {
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
            .asList().hasSize(2);
        assertThat(result.get(location2.getLocation())).asList().hasSize(1);
        verify(mockNodeRepository).findByIdInAndTenantId(List.of(1L, 2L, 3L), tenantID);
    }

    @Test
    void testListNodesByIdsEmpty() {
        doReturn(Collections.emptyList()).when(mockNodeRepository).findByIdInAndTenantId(List.of(1L, 2L, 3L), tenantID);
        Map<String, List<NodeDTO>> result = nodeService.listNodeByIds(List.of(1L, 2L, 3L), tenantID);
        assertThat(result.isEmpty()).isTrue();
        verify(mockNodeRepository).findByIdInAndTenantId(List.of(1L, 2L, 3L), tenantID);
    }
}
