/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
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

package org.opennms.horizon.inventory.service.taskset.publiser;

import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opennms.horizon.inventory.dto.IpInterfaceDTO;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.inventory.service.SnmpConfigService;
import org.opennms.horizon.inventory.service.taskset.ScannerTaskSetService;
import org.opennms.horizon.inventory.service.taskset.publisher.TaskSetPublisher;
import org.opennms.node.scan.contract.NodeScanRequest;
import org.opennms.taskset.contract.TaskDefinition;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
public class ScannerTaskSetServiceTest {
    @Mock
    private TaskSetPublisher mockPublisher;
    @Mock
    private SnmpConfigService configService;

    @InjectMocks
    private ScannerTaskSetService service;
    @Captor
    ArgumentCaptor<List<TaskDefinition>> taskListCaptor;
    private final String tenantId = "testTenant";
    private final Long locationId = 1020304050L;
    private NodeDTO.Builder nodeBuilder;
    private IpInterfaceDTO ipInterface1;
    private IpInterfaceDTO ipInterface2;

    @BeforeEach
    void prepareTest() {
        ipInterface1 = IpInterfaceDTO.newBuilder().setIpAddress("127.0.0.1").setSnmpPrimary(true).build();
        ipInterface2 = IpInterfaceDTO.newBuilder().setIpAddress("127.0.0.1").build();
        nodeBuilder = NodeDTO.newBuilder().setId(1L);
    }

    @AfterEach
    void afterTest() {
        verifyNoMoreInteractions(mockPublisher);
    }

    @Test
    void testSendNodeScanWithTwoIpInterfaces() throws InvalidProtocolBufferException {
        NodeDTO node = nodeBuilder.addAllIpInterfaces(List.of(ipInterface1, ipInterface2)).build();
        service.sendNodeScannerTask(List.of(node), locationId, tenantId);
        verify(mockPublisher).publishNewTasks(eq(tenantId), eq(locationId), taskListCaptor.capture());
        List<TaskDefinition> tasks = taskListCaptor.getValue();
        assertThat(tasks).asList().hasSize(1)
            .extracting("nodeId_").containsExactly(node.getId());
        NodeScanRequest request = tasks.get(0).getConfiguration().unpack(NodeScanRequest.class);
        assertThat(request).extracting(NodeScanRequest::getNodeId, NodeScanRequest::getPrimaryIp)
            .containsExactly(node.getId(), ipInterface1.getIpAddress());
    }

    @Test
    void testSendNodeScanWithIpInterfaceNonPrimary() throws InvalidProtocolBufferException {
        NodeDTO node = nodeBuilder.addAllIpInterfaces(List.of(ipInterface2)).build();
        service.sendNodeScannerTask(List.of(node), locationId, tenantId);
        verify(mockPublisher).publishNewTasks(eq(tenantId), eq(locationId), taskListCaptor.capture());
        List<TaskDefinition> tasks = taskListCaptor.getValue();
        assertThat(tasks).asList().hasSize(1)
            .extracting("nodeId_").containsExactly(node.getId());
        NodeScanRequest request = tasks.get(0).getConfiguration().unpack(NodeScanRequest.class);
        assertThat(request).extracting(NodeScanRequest::getNodeId, NodeScanRequest::getPrimaryIp)
            .containsExactly(node.getId(), ipInterface2.getIpAddress());
    }

    @Test
    void testSendNodeScanWithoutIpInterfaces() {
        NodeDTO node = nodeBuilder.build();
        service.sendNodeScannerTask(List.of(node), locationId, tenantId);
        verifyNoInteractions(mockPublisher);
    }
}
