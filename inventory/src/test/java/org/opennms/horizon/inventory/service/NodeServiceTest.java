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

import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opennms.horizon.inventory.dto.NodeCreateDTO;
import org.opennms.horizon.inventory.mapper.NodeMapper;
import org.opennms.horizon.inventory.model.IpInterface;
import org.opennms.horizon.inventory.model.MonitoringLocation;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.repository.IpInterfaceRepository;
import org.opennms.horizon.inventory.repository.MonitoringLocationRepository;
import org.opennms.horizon.inventory.repository.NodeRepository;
import org.opennms.horizon.shared.constants.GrpcConstants;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NodeServiceTest {

    @InjectMocks
    NodeService nodeService;

    @Mock
    NodeRepository nodeRepository;

    @Mock
    MonitoringLocationRepository monitoringLocationRepository;

    @Mock
    IpInterfaceRepository ipInterfaceRepository;

    @Mock
    ConfigUpdateService configUpdateService;

    @Mock
    NodeMapper mapper;

    private final String tenantID = "test-tenant";

    @AfterEach
    public void afterTest(){
        verifyNoMoreInteractions(nodeRepository);
        verifyNoMoreInteractions(monitoringLocationRepository);
        verifyNoMoreInteractions(ipInterfaceRepository);
    }

    @Test
    public void createNode() {
        String tenant = "ANY";
        String location = "loc";
        MonitoringLocation ml = new MonitoringLocation();
        ml.setTenantId(tenant);
        ml.setLocation(location);

        when(monitoringLocationRepository.save(any())).thenReturn(ml);

        NodeCreateDTO nodeCreateDTO = NodeCreateDTO.newBuilder()
            .setLabel("Label")
            .setLocation("loc")
            .setManagementIp("127.0.0.1")
            .build();

        nodeService.createNode(nodeCreateDTO, tenant);

        verify(ipInterfaceRepository).save(any(IpInterface.class));
        verify(monitoringLocationRepository).save(any(MonitoringLocation.class));
        verify(configUpdateService, timeout(5000)).sendConfigUpdate(tenant, location);
    }

    @Test
    public void createNodeExistingLocation() {
        String location = "loc";
        String tenantId = "ANY";

        NodeCreateDTO nodeCreateDTO = NodeCreateDTO.newBuilder()
            .setLabel("Label")
            .setLocation(location)
            .setManagementIp("127.0.0.1")
            .build();

        doReturn(Optional.of(new MonitoringLocation())).when(monitoringLocationRepository).findByLocationAndTenantId(location, tenantId);

        nodeService.createNode(nodeCreateDTO, tenantId);

        verify(ipInterfaceRepository).save(any(IpInterface.class));
        verify(monitoringLocationRepository, times(0)).save(any(MonitoringLocation.class));
        verify(configUpdateService, timeout(5000).times(0)).sendConfigUpdate(eq(tenantId), any());
    }

    @Test
    public void createNodeNoIp() {
        String tenant = "TENANT";
        String location = "LOCATION";
        MonitoringLocation ml = new MonitoringLocation();
        ml.setTenantId(tenant);
        ml.setLocation(location);

        when(monitoringLocationRepository.save(any())).thenReturn(ml);

        NodeCreateDTO nodeCreateDTO = NodeCreateDTO.newBuilder()
            .setLabel("Label")
            .setLocation("loc")
            .build();

        nodeService.createNode(nodeCreateDTO, "ANY");

        verify(ipInterfaceRepository, times(0)).save(any(IpInterface.class));
    }

    @Test
    public void createNodeWithLocationDefaultLocationExist() {
        NodeCreateDTO nodeCreate = NodeCreateDTO.newBuilder()
            .setLabel("test-node")
            .setManagementIp("127.0.0.1").build();
        MonitoringLocation location = new MonitoringLocation();
        doReturn(Optional.of(location)).when(monitoringLocationRepository).findByLocationAndTenantId(GrpcConstants.DEFAULT_LOCATION, tenantID);
        nodeService.createNode(nodeCreate, tenantID);
        verify(monitoringLocationRepository).findByLocationAndTenantId(GrpcConstants.DEFAULT_LOCATION, tenantID);
        verify(nodeRepository).save(any(Node.class));
        verify(ipInterfaceRepository).save(any(IpInterface.class));
    }

    @Test
    public void createNodeWithLocationDefaultLocationNotExist() {
        NodeCreateDTO nodeCreate = NodeCreateDTO.newBuilder()
            .setLabel("test-node")
            .setManagementIp("127.0.0.1").build();
        doReturn(Optional.empty()).when(monitoringLocationRepository).findByLocationAndTenantId(GrpcConstants.DEFAULT_LOCATION, tenantID);
        doReturn(new MonitoringLocation()).when(monitoringLocationRepository).save(any(MonitoringLocation.class));
        ArgumentCaptor<MonitoringLocation> captor = ArgumentCaptor.forClass(MonitoringLocation.class);
        nodeService.createNode(nodeCreate, tenantID);
        verify(monitoringLocationRepository).findByLocationAndTenantId(GrpcConstants.DEFAULT_LOCATION, tenantID);
        verify(monitoringLocationRepository).save(captor.capture());
        assertThat(captor.getValue().getLocation()).isEqualTo(GrpcConstants.DEFAULT_LOCATION);
        verify(nodeRepository).save(any(Node.class));
        verify(ipInterfaceRepository).save(any(IpInterface.class));
    }
}
