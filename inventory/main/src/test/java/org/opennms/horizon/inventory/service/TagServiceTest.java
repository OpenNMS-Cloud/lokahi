/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2023 The OpenNMS Group; Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group; Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group; Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation; either version 3 of the License;
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful;
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not; see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.horizon.inventory.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.alerts.proto.MonitorPolicyProto;
import org.opennms.horizon.inventory.component.AlertClient;
import org.opennms.horizon.inventory.component.TagPublisher;
import org.opennms.horizon.inventory.dto.TagCreateDTO;
import org.opennms.horizon.inventory.dto.TagCreateListDTO;
import org.opennms.horizon.inventory.dto.TagEntityIdDTO;
import org.opennms.horizon.inventory.exception.InventoryRuntimeException;
import org.opennms.horizon.inventory.mapper.TagMapper;
import org.opennms.horizon.inventory.mapper.TagMapperImpl;
import org.opennms.horizon.inventory.model.Tag;
import org.opennms.horizon.inventory.repository.NodeRepository;
import org.opennms.horizon.inventory.repository.TagRepository;
import org.opennms.horizon.inventory.repository.discovery.PassiveDiscoveryRepository;
import org.opennms.horizon.inventory.repository.discovery.active.ActiveDiscoveryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {TagMapperImpl.class})
public class TagServiceTest {
    public static final String TEST_TENANT_ID = "x-tenant-id-x";

    private TagRepository mockTagRepository;
    private NodeRepository mockNodeRepository;
    private ActiveDiscoveryRepository mockActiveDiscoveryRepository;
    private PassiveDiscoveryRepository mockPassiveDiscoveryRepository;
    @Autowired
    private TagMapper tagMapper;
    private TagPublisher mockTagPublisher;
    private NodeService mockNodeService;
    private AlertClient mockAlertClient;

    private TagService tagService;

    @BeforeEach
    public void setup() {
        mockTagRepository = mock(TagRepository.class);
        mockNodeRepository = mock(NodeRepository.class);
        mockActiveDiscoveryRepository = mock(ActiveDiscoveryRepository.class);
        mockPassiveDiscoveryRepository = mock(PassiveDiscoveryRepository.class);
        mockTagPublisher = mock(TagPublisher.class);
        mockNodeService = mock(NodeService.class);
        mockAlertClient = mock(AlertClient.class);
        tagService = new TagService(mockTagRepository, mockNodeRepository, mockActiveDiscoveryRepository,
            mockPassiveDiscoveryRepository, tagMapper, mockTagPublisher, mockNodeService, mockAlertClient);
    }

    @Test
    void testAddTags() {
        long testNodeId = 1L;
        long testPolicyId = 2L;
        when(mockAlertClient.getPolicyById(testPolicyId, TEST_TENANT_ID)).thenReturn(MonitorPolicyProto.newBuilder().build());
        when(mockTagRepository.save(any(Tag.class))).thenAnswer((arg) -> arg.getArgument(0));
        var addTags = TagCreateListDTO.newBuilder()
            .addEntityIds(TagEntityIdDTO.newBuilder().setNodeId(testNodeId).setMonitoringPolicyId(testPolicyId).build())
            .addTags(TagCreateDTO.newBuilder().setName("tag1"))
            .addTags(TagCreateDTO.newBuilder().setName("tag2"))
            .build();
        var savedTags = tagService.addTags(TEST_TENANT_ID, addTags);
        Assertions.assertEquals(2, savedTags.size());
    }

    @Test
    void testAddTagsMissingPolicy() {
        long testNodeId = 1L;
        long testPolicyId = 2L;

        var addTags = TagCreateListDTO.newBuilder()
            .addEntityIds(TagEntityIdDTO.newBuilder().setNodeId(testNodeId).setMonitoringPolicyId(testPolicyId).build())
            .addTags(TagCreateDTO.newBuilder().setName("tag1"))
            .build();
        var exception = Assertions.assertThrows(InventoryRuntimeException.class, () -> tagService.addTags(TEST_TENANT_ID, addTags));
        Assertions.assertEquals("MonitoringPolicyId not found for id: " + testPolicyId, exception.getMessage());
    }

    @Test
    void testAddTagsMissingNodeId() {
        long testNodeId = 1L;
        long nodeId = 2L;

        var addTags = TagCreateListDTO.newBuilder()
            .addEntityIds(TagEntityIdDTO.newBuilder().setNodeId(testNodeId).setNodeId(nodeId).build())
            .addTags(TagCreateDTO.newBuilder().setName("tag1"))
            .build();
        var exception = Assertions.assertThrows(InventoryRuntimeException.class, () -> tagService.addTags(TEST_TENANT_ID, addTags));
        Assertions.assertEquals("Node not found for id: " + nodeId, exception.getMessage());
    }

    @Test
    void testAddTagsMissingActiveDiscoveryId() {
        long testNodeId = 1L;
        long activeDiscoveryId = 2L;

        var addTags = TagCreateListDTO.newBuilder()
            .addEntityIds(TagEntityIdDTO.newBuilder().setNodeId(testNodeId).setActiveDiscoveryId(activeDiscoveryId).build())
            .addTags(TagCreateDTO.newBuilder().setName("tag1"))
            .build();
        var exception = Assertions.assertThrows(InventoryRuntimeException.class, () -> tagService.addTags(TEST_TENANT_ID, addTags));
        Assertions.assertEquals("Active Discovery not found for id: " + activeDiscoveryId, exception.getMessage());
    }

    @Test
    void testAddTagsMissingPassiveDiscoveryId() {
        long testNodeId = 1L;
        long passiveDiscoveryId = 2L;

        var addTags = TagCreateListDTO.newBuilder()
            .addEntityIds(TagEntityIdDTO.newBuilder().setNodeId(testNodeId).setPassiveDiscoveryId(passiveDiscoveryId).build())
            .addTags(TagCreateDTO.newBuilder().setName("tag1"))
            .build();
        var exception = Assertions.assertThrows(InventoryRuntimeException.class, () -> tagService.addTags(TEST_TENANT_ID, addTags));
        Assertions.assertEquals("Passive Discovery not found for id: " + passiveDiscoveryId, exception.getMessage());
    }
}
