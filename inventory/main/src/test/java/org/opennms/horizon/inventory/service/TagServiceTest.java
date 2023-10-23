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

import com.google.protobuf.Int64Value;
import com.google.rpc.Code;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.alerts.proto.MonitorPolicyProto;
import org.opennms.horizon.inventory.component.AlertClient;
import org.opennms.horizon.inventory.component.TagPublisher;
import org.opennms.horizon.inventory.dto.DeleteTagsDTO;
import org.opennms.horizon.inventory.dto.ListTagsByEntityIdParamsDTO;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
    void testAddTagsAlertClientException() {
        long testNodeId = 1L;
        long testPolicyId = 2L;
        when(mockAlertClient.getPolicyById(testPolicyId, TEST_TENANT_ID)).thenThrow(new StatusRuntimeException(Status.NOT_FOUND));

        var addTags = TagCreateListDTO.newBuilder()
            .addEntityIds(TagEntityIdDTO.newBuilder().setNodeId(testNodeId).setMonitoringPolicyId(testPolicyId).build())
            .addTags(TagCreateDTO.newBuilder().setName("tag1"))
            .build();

        var exception = Assertions.assertThrows(InventoryRuntimeException.class, () -> tagService.addTags(TEST_TENANT_ID, addTags));

        Assertions.assertEquals("MonitoringPolicyId not found for id: " + testPolicyId, exception.getMessage());
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

        verify(mockNodeRepository).findByIdAndTenantId(nodeId, TEST_TENANT_ID);
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

        verify(mockActiveDiscoveryRepository).findByTenantIdAndId(TEST_TENANT_ID, activeDiscoveryId);
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

        verify(mockPassiveDiscoveryRepository).findByTenantIdAndId(TEST_TENANT_ID, passiveDiscoveryId);
        Assertions.assertEquals("Passive Discovery not found for id: " + passiveDiscoveryId, exception.getMessage());
    }

    @Test
    void testGetTagsByEntityId() {
        long testNodeId = 1L;
        long testPolicyId = 2L;
        Tag tag1 = new Tag();
        tag1.setName("tag1");
        tag1.setMonitorPolicyIds(List.of(testPolicyId));
        tag1.setTenantId(TEST_TENANT_ID);
        Tag tag2 = new Tag();
        tag2.setName("tag2");
        tag2.setMonitorPolicyIds(List.of(testPolicyId));
        tag2.setTenantId(TEST_TENANT_ID);
        when(mockAlertClient.getPolicyById(testPolicyId, TEST_TENANT_ID)).thenReturn(MonitorPolicyProto.newBuilder().build());
        when(mockTagRepository.findByTenantId(TEST_TENANT_ID)).thenReturn(List.of(tag1, tag2));

        var listTags = ListTagsByEntityIdParamsDTO.newBuilder()
            .setEntityId(TagEntityIdDTO.newBuilder().setNodeId(testNodeId).setMonitoringPolicyId(testPolicyId).build())
            .build();

        var tags = tagService.getTagsByEntityId(TEST_TENANT_ID, listTags);

        Assertions.assertEquals(2, tags.size());
    }

    @Test
    void testGetTagsByEntityIdMissingPolicy() {
        long testNodeId = 1L;
        long testPolicyId = 2L;

        var listTags = ListTagsByEntityIdParamsDTO.newBuilder()
            .setEntityId(TagEntityIdDTO.newBuilder().setNodeId(testNodeId).setMonitoringPolicyId(testPolicyId).build())
            .build();

        var exception = Assertions.assertThrows(InventoryRuntimeException.class, () -> tagService.getTagsByEntityId(TEST_TENANT_ID, listTags));

        verify(mockAlertClient).getPolicyById(testPolicyId, TEST_TENANT_ID);
        Assertions.assertEquals("MonitoringPolicyId not found for id: " + testPolicyId, exception.getMessage());
    }

    @Test
    void testDeleteTags() {
        long testPolicyId = 2L;
        Tag tag1 = new Tag();
        tag1.setId(1L);
        tag1.setName("tag1");
        tag1.setMonitorPolicyIds(new ArrayList<>(List.of(testPolicyId)));
        tag1.setTenantId(TEST_TENANT_ID);
        Tag tag2 = new Tag();
        tag2.setId(2L);
        tag2.setName("tag2");
        tag2.setMonitorPolicyIds(new ArrayList<>(List.of(testPolicyId)));
        tag2.setTenantId(TEST_TENANT_ID);
        when(mockAlertClient.getPolicyById(testPolicyId, TEST_TENANT_ID)).thenReturn(MonitorPolicyProto.newBuilder().build());
        when(mockTagRepository.findByTenantIdAndId(eq(TEST_TENANT_ID), any())).thenAnswer((args) -> {
            long id = args.getArgument(1);
            if (id == tag1.getId()) {
                return Optional.of(tag1);
            } else if (id == tag2.getId()) {
                return Optional.of(tag2);
            } else {
                return Optional.empty();
            }
        });

        var deleteTags = DeleteTagsDTO.newBuilder()
            .addTagIds(Int64Value.of(1)).addTagIds(Int64Value.of(2))
            .build();

        tagService.deleteTags(TEST_TENANT_ID, deleteTags);

        verify(mockTagRepository, times(2)).delete(any(Tag.class));
    }

    @Test
    void testDeleteTagsException() {
        long testPolicyId = 3L;
        Tag tag1 = new Tag();
        tag1.setId(1L);
        tag1.setName("tag1");
        tag1.setMonitorPolicyIds(new ArrayList<>(List.of(testPolicyId)));
        tag1.setTenantId(TEST_TENANT_ID);
        Tag tag2 = new Tag();
        tag2.setId(2L);
        tag2.setName("tag2");
        tag2.setMonitorPolicyIds(new ArrayList<>(List.of(testPolicyId)));
        tag2.setTenantId(TEST_TENANT_ID);

        var deleteTags = DeleteTagsDTO.newBuilder()
            .addTagIds(Int64Value.of(1)).addTagIds(Int64Value.of(2))
            .build();

        var exception = Assertions.assertThrows(InventoryRuntimeException.class, () ->
            tagService.deleteTags(TEST_TENANT_ID, deleteTags));

        verify(mockTagRepository).findByTenantIdAndId(TEST_TENANT_ID, tag1.getId());
        Assertions.assertEquals("Invalid Tag id: " + tag1.getId(), exception.getMessage());
    }
}
