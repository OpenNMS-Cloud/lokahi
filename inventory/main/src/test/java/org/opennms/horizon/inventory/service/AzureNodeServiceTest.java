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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.inventory.dto.AzureNodeCreateDTO;
import org.opennms.horizon.inventory.dto.TagCreateDTO;
import org.opennms.horizon.inventory.dto.TagCreateListDTO;
import org.opennms.horizon.inventory.model.MonitoringLocation;
import org.opennms.horizon.inventory.model.node.AzureNode;
import org.opennms.horizon.inventory.model.node.Node;
import org.opennms.horizon.inventory.repository.node.AzureNodeRepository;
import org.opennms.horizon.inventory.service.node.AzureNodeService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class AzureNodeServiceTest {
    private AzureNodeService azureNodeService;
    private AzureNodeRepository mockAzureNodeRepository;
    private IpInterfaceService mockIpInterfaceService;
    private MonitoringLocationService mockMonitoringLocationService;
    private TagService tagService;

    @BeforeEach
    void prepareTest() {
        mockAzureNodeRepository = mock(AzureNodeRepository.class);
        mockIpInterfaceService = mock(IpInterfaceService.class);
        mockMonitoringLocationService = mock(MonitoringLocationService.class);
        tagService = mock(TagService.class);


        azureNodeService = new AzureNodeService(mockAzureNodeRepository,
            mockIpInterfaceService,
            mockMonitoringLocationService,
            tagService);

        AzureNode node = new AzureNode();
        doReturn(node).when(mockAzureNodeRepository).save(any(node.getClass()));
    }

    @AfterEach
    public void afterTest() {
        verifyNoMoreInteractions(mockAzureNodeRepository);
        verifyNoMoreInteractions(mockMonitoringLocationService);
        verifyNoMoreInteractions(mockIpInterfaceService);
    }

    @Test
    void createNode() {
        String tenant = "ANY";
        String location = "loc";
        MonitoringLocation ml = new MonitoringLocation();
        ml.setTenantId(tenant);
        ml.setLocation(location);

        when(mockMonitoringLocationService.saveMonitoringLocation(eq(tenant), eq(location))).thenReturn(ml);

        AzureNodeCreateDTO nodeCreateDTO = AzureNodeCreateDTO.newBuilder()
            .setLabel("Label")
            .setLocation("loc")
            .setManagementIp("127.0.0.1")
            .addTags(TagCreateDTO.newBuilder().setName("tag-name").build())
            .build();

        azureNodeService.createNode(tenant, nodeCreateDTO);
        verify(mockAzureNodeRepository).save(any(AzureNode.class));
        verify(mockIpInterfaceService).saveIpInterface(eq(tenant), any(Node.class), eq(nodeCreateDTO.getManagementIp()));
        verify(mockMonitoringLocationService).saveMonitoringLocation(eq(tenant), eq(location));
        verify(tagService).addTags(eq(tenant), any(TagCreateListDTO.class));
    }

    @Test
    void createNodeNoIp() {
        String tenant = "TENANT";
        String location = "LOCATION";
        MonitoringLocation ml = new MonitoringLocation();
        ml.setTenantId(tenant);
        ml.setLocation(location);

        when(mockMonitoringLocationService.saveMonitoringLocation(eq(tenant), eq(location))).thenReturn(ml);

        AzureNodeCreateDTO nodeCreateDTO = AzureNodeCreateDTO.newBuilder()
            .setLabel("Label")
            .setLocation(location)
            .build();

        azureNodeService.createNode(tenant, nodeCreateDTO);
        verify(mockAzureNodeRepository).save(any(AzureNode.class));
        verify(mockMonitoringLocationService).saveMonitoringLocation(eq(tenant), eq(location));
        verifyNoInteractions(mockIpInterfaceService);
    }
}
