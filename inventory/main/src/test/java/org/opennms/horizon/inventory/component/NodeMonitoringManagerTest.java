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

package org.opennms.horizon.inventory.component;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opennms.horizon.events.proto.Event;
import org.opennms.horizon.inventory.dto.NodeCreateDTO;
import org.opennms.horizon.inventory.exception.EntityExistException;
import org.opennms.horizon.inventory.exception.InventoryRuntimeException;
import org.opennms.horizon.inventory.exception.LocationNotFoundException;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.model.Tag;
import org.opennms.horizon.inventory.model.discovery.PassiveDiscovery;
import org.opennms.horizon.inventory.repository.discovery.PassiveDiscoveryRepository;
import org.opennms.horizon.inventory.service.NodeService;
import org.opennms.horizon.inventory.service.discovery.PassiveDiscoveryService;
import org.opennms.horizon.shared.events.EventConstants;
import org.opennms.taskset.contract.ScanType;

import java.util.ArrayList;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class NodeMonitoringManagerTest {

    @Mock
    private NodeService nodeService;
    @Mock
    private PassiveDiscoveryService passiveDiscoveryService;
    @Mock
    private PassiveDiscoveryRepository passiveDiscoveryRepository;
    @InjectMocks
    private NodeMonitoringManager nodeMonitoringManager;
    private final String tenantId = "test-tenant";
    private Event event;
    private Node node;
    Long locationId = 5040302010L;

    private Optional<PassiveDiscovery> passiveDiscovery;

    @BeforeEach
    public void prepare() {
        event = Event.newBuilder()
            .setTenantId(tenantId)
            .setUei(EventConstants.NEW_SUSPECT_INTERFACE_EVENT_UEI)
            .setLocationId(String.valueOf(locationId))
            .setIpAddress("127.0.0.1")
            .build();
        node = new Node();

        Tag tag = new Tag();
        tag.setName("tag1");
        ArrayList<Tag> tags = new ArrayList();
        tags.add(tag);
        PassiveDiscovery discovery = new PassiveDiscovery();
        discovery.setName("test");
        discovery.setLocationId(locationId);
        discovery.setTags(tags);
        passiveDiscovery = Optional.ofNullable(discovery);
    }

    @AfterEach
    public void afterTest() {
        verifyNoMoreInteractions(nodeService);
        verifyNoMoreInteractions(passiveDiscoveryService);
    }

    @Test
    void testReceiveEventAndCreateNewNode() throws EntityExistException, LocationNotFoundException {
        doReturn(node).when(nodeService).createNode(any(NodeCreateDTO.class), eq(ScanType.NODE_SCAN), eq(tenantId));
        doReturn(passiveDiscovery).when(passiveDiscoveryRepository).findByTenantIdAndLocationId(tenantId, locationId);
        ArgumentCaptor<NodeCreateDTO> argumentCaptor = ArgumentCaptor.forClass(NodeCreateDTO.class);
        nodeMonitoringManager.receiveTrapEvent(event.toByteArray());
        verify(nodeService).createNode(argumentCaptor.capture(), eq(ScanType.NODE_SCAN), eq(tenantId));
        verify(passiveDiscoveryService).sendNodeScan(node);
        NodeCreateDTO createDTO = argumentCaptor.getValue();
        assertThat(createDTO.getLocationId()).isEqualTo(event.getLocationId());
        assertThat(createDTO.getManagementIp()).isEqualTo(event.getIpAddress());
        assertThat(createDTO.getLabel()).endsWith(event.getIpAddress());
        assertEquals(createDTO.getTagsCount(),1);
    }

    @Test
    void testReceiveEventWithDifferentUEI() {
        var anotherEvent = Event.newBuilder()
            .setUei("something else").build();
        nodeMonitoringManager.receiveTrapEvent(anotherEvent.toByteArray());
        verifyNoInteractions(passiveDiscoveryService);
        verifyNoInteractions(nodeService);
    }

    @Test
    void testMissingTenantID() {
        Event testEvent = Event.newBuilder().setUei(EventConstants.NEW_SUSPECT_INTERFACE_EVENT_UEI).build();
        assertThatThrownBy(() -> nodeMonitoringManager.receiveTrapEvent(testEvent.toByteArray())).isInstanceOf(InventoryRuntimeException.class);
    }

    @Test
    void testEntityExistException() throws EntityExistException, LocationNotFoundException {
        doThrow(new EntityExistException("bad request")).when(nodeService).createNode(any(NodeCreateDTO.class), eq(ScanType.NODE_SCAN), eq(tenantId));
        ArgumentCaptor<NodeCreateDTO> argumentCaptor = ArgumentCaptor.forClass(NodeCreateDTO.class);
        nodeMonitoringManager.receiveTrapEvent(event.toByteArray());
        verify(nodeService).createNode(argumentCaptor.capture(), eq(ScanType.NODE_SCAN), eq(tenantId));
        NodeCreateDTO createDTO = argumentCaptor.getValue();
        assertThat(createDTO.getLocationId()).isEqualTo(event.getLocationId());
        assertThat(createDTO.getManagementIp()).isEqualTo(event.getIpAddress());
        assertThat(createDTO.getLabel()).endsWith(event.getIpAddress());
        verifyNoInteractions(passiveDiscoveryService);
    }
}
