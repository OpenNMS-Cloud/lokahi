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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.common.VerificationException;
import org.opennms.horizon.inventory.SpringContextTestInitializer;
import org.opennms.horizon.inventory.dto.ListAllTagsParamsDTO;
import org.opennms.horizon.inventory.dto.ListTagsByEntityIdParamsDTO;
import org.opennms.horizon.inventory.dto.TagCreateDTO;
import org.opennms.horizon.inventory.dto.TagCreateListDTO;
import org.opennms.horizon.inventory.dto.TagDTO;
import org.opennms.horizon.inventory.dto.TagListDTO;
import org.opennms.horizon.inventory.dto.TagListParamsDTO;
import org.opennms.horizon.inventory.dto.TagServiceGrpc;
import org.opennms.horizon.inventory.model.AzureCredential;
import org.opennms.horizon.inventory.model.MonitoringLocation;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.model.Tag;
import org.opennms.horizon.inventory.repository.AzureCredentialRepository;
import org.opennms.horizon.inventory.repository.MonitoringLocationRepository;
import org.opennms.horizon.inventory.repository.NodeRepository;
import org.opennms.horizon.inventory.repository.TagRepository;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import io.grpc.Context;
import io.grpc.stub.MetadataUtils;


@SpringBootTest
@ContextConfiguration(initializers = {SpringContextTestInitializer.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class TagGrpcItTest extends GrpcTestBase {

    private static final String TEST_NODE_LABEL_1 = "node-label-1";
    private static final String TEST_NODE_LABEL_2 = "node-label-2";
    private static final String TEST_LOCATION = "test-location";
    private static final String TEST_TAG_NAME_1 = "tag-name-1";
    private static final String TEST_TAG_NAME_2 = "tag-name-2";

    private TagServiceGrpc.TagServiceBlockingStub serviceStub;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private MonitoringLocationRepository locationRepository;

    @Autowired
    private AzureCredentialRepository azureCredentialRepository;

    @BeforeEach
    public void prepare() throws VerificationException {
        prepareServer();
        serviceStub = TagServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    public void cleanUp() throws InterruptedException {
        afterTest();
    }

    @Test
    void testCreateTag() {
        long nodeId = setupDatabase();

        TagCreateDTO createDTO = TagCreateDTO.newBuilder()
            .setName(TEST_TAG_NAME_1)
            .build();

        TagCreateListDTO createListDTO = TagCreateListDTO.newBuilder()
            .addAllTags(Collections.singletonList(createDTO)).setNodeId(nodeId).build();

        serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).addTags(createListDTO);

        List<Tag> allTags = tagRepository.findAll();
        assertEquals(1, allTags.size());

        Tag savedTag = allTags.get(0);
        assertEquals(createDTO.getName(), savedTag.getName());
        assertEquals(tenantId, savedTag.getTenantId());

        List<Node> nodes = savedTag.getNodes();
        assertEquals(1, nodes.size());

        Node node = nodes.get(0);
        assertEquals(nodeId, node.getId());
        assertEquals(tenantId, node.getTenantId());
    }

    @Test
    void testCreateTagAlreadyCreatedOnce() {
        long nodeId = setupDatabase();

        TagCreateDTO createDTO = TagCreateDTO.newBuilder()
            .setName(TEST_TAG_NAME_1)
            .build();

        TagCreateListDTO createListDTO = TagCreateListDTO.newBuilder()
            .addAllTags(Collections.singletonList(createDTO)).setNodeId(nodeId).build();

        for (int index = 0; index < 2; index++) {
            serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).addTags(createListDTO);
        }

        List<Tag> allTags = tagRepository.findAll();
        assertEquals(1, allTags.size());

        Tag savedTag = allTags.get(0);
        assertEquals(createDTO.getName(), savedTag.getName());
        assertEquals(tenantId, savedTag.getTenantId());

        List<Node> nodes = savedTag.getNodes();
        assertEquals(1, nodes.size());

        Node node = nodes.get(0);
        assertEquals(nodeId, node.getId());
        assertEquals(tenantId, node.getTenantId());
    }

    @Test
    void testCreateTwoTagsOnNode() {
        long nodeId = setupDatabase();

        TagCreateDTO createDTO1 = TagCreateDTO.newBuilder()
            .setName(TEST_TAG_NAME_1)
            .build();

        TagCreateListDTO createListDTO1 = TagCreateListDTO.newBuilder()
            .addAllTags(Collections.singletonList(createDTO1)).setNodeId(nodeId).build();

        serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).addTags(createListDTO1);

        TagCreateDTO createDTO2 = TagCreateDTO.newBuilder()
            .setName(TEST_TAG_NAME_2)
            .build();

        TagCreateListDTO createListDTO2 = TagCreateListDTO.newBuilder()
            .addAllTags(Collections.singletonList(createDTO2)).setNodeId(nodeId).build();

        serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).addTags(createListDTO2);

        List<Tag> allTags = tagRepository.findAll();
        assertEquals(2, allTags.size());

        //tag 1
        Tag savedTag = allTags.get(0);
        assertEquals(createDTO1.getName(), savedTag.getName());
        assertEquals(tenantId, savedTag.getTenantId());

        List<Node> nodes = savedTag.getNodes();
        assertEquals(1, nodes.size());

        Node node = nodes.get(0);
        assertEquals(nodeId, node.getId());
        assertEquals(tenantId, node.getTenantId());

        //tag 2
        savedTag = allTags.get(1);
        assertEquals(createDTO2.getName(), savedTag.getName());
        assertEquals(tenantId, savedTag.getTenantId());

        nodes = savedTag.getNodes();
        assertEquals(1, nodes.size());

        node = nodes.get(0);
        assertEquals(nodeId, node.getId());
        assertEquals(tenantId, node.getTenantId());
    }

    @Test
    void testCreateTwoTagsTwoNodes() {
        MonitoringLocation location = new MonitoringLocation();
        location.setLocation(TEST_LOCATION);
        location.setTenantId(tenantId);
        location = locationRepository.saveAndFlush(location);

        Node node1 = new Node();
        node1.setNodeLabel(TEST_NODE_LABEL_1);
        node1.setCreateTime(LocalDateTime.now());
        node1.setTenantId(tenantId);
        node1.setMonitoringLocation(location);
        node1.setMonitoringLocationId(location.getId());
        node1 = nodeRepository.saveAndFlush(node1);

        Node node2 = new Node();
        node2.setNodeLabel(TEST_NODE_LABEL_2);
        node2.setCreateTime(LocalDateTime.now());
        node2.setTenantId(tenantId);
        node2.setMonitoringLocation(location);
        node2.setMonitoringLocationId(location.getId());
        node2 = nodeRepository.saveAndFlush(node2);

        TagCreateDTO createDTO1 = TagCreateDTO.newBuilder()
            .setName(TEST_TAG_NAME_1)
            .build();

        TagCreateDTO createDTO2 = TagCreateDTO.newBuilder()
            .setName(TEST_TAG_NAME_2)
            .build();

        TagCreateListDTO createListDTO1 = TagCreateListDTO.newBuilder()
            .addAllTags(List.of(createDTO1, createDTO2)).setNodeId(node1.getId()).build();

        serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).addTags(createListDTO1);

        TagCreateDTO createDTO3 = TagCreateDTO.newBuilder()
            .setName(TEST_TAG_NAME_2)
            .build();

        TagCreateListDTO createListDTO3 = TagCreateListDTO.newBuilder()
            .addAllTags(List.of(createDTO3)).setNodeId(node2.getId()).build();

        serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).addTags(createListDTO3);

        List<Tag> allTags = tagRepository.findAll();
        assertEquals(2, allTags.size());

        List<Node> allNodes = nodeRepository.findAll();
        assertEquals(2, allNodes.size());

        Node savedNode1 = allNodes.get(0);
        assertEquals(node1.getId(), savedNode1.getId());
        assertEquals(2, savedNode1.getTags().size());
        assertEquals(TEST_TAG_NAME_1, savedNode1.getTags().get(0).getName());
        assertEquals(TEST_TAG_NAME_2, savedNode1.getTags().get(1).getName());

        Node savedNode2 = allNodes.get(1);
        assertEquals(node2.getId(), savedNode2.getId());
        assertEquals(1, savedNode2.getTags().size());
        assertEquals(TEST_TAG_NAME_2, savedNode2.getTags().get(0).getName());
    }

    @Test
    void testGetTagListForNode() {
        long nodeId = setupDatabase();

        TagCreateDTO createDTO1 = TagCreateDTO.newBuilder()
            .setName(TEST_TAG_NAME_1)
            .build();

        TagCreateListDTO createListDTO1 = TagCreateListDTO.newBuilder()
            .addAllTags(Collections.singletonList(createDTO1)).setNodeId(nodeId).build();

        serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).addTags(createListDTO1);

        TagCreateDTO createDTO2 = TagCreateDTO.newBuilder()
            .setName(TEST_TAG_NAME_2)
            .build();

        TagCreateListDTO createListDTO2 = TagCreateListDTO.newBuilder()
            .addAllTags(Collections.singletonList(createDTO2)).setNodeId(nodeId).build();

        serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).addTags(createListDTO2);

        List<Tag> allTags = tagRepository.findAll();
        assertEquals(2, allTags.size());

        ListTagsByEntityIdParamsDTO params = ListTagsByEntityIdParamsDTO.newBuilder().setNodeId(nodeId).setParams(TagListParamsDTO.newBuilder().build()).build();
        TagListDTO tagsByNodeId = serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).getTagsByEntityId(params);
        List<TagDTO> tagsList = tagsByNodeId.getTagsList();
        assertEquals(2, tagsList.size());
    }

    @Test
    void testGetTagListForNodeWithNameLike() {
        long nodeId = setupDatabase();

        TagCreateDTO createDTO1 = TagCreateDTO.newBuilder()
            .setName(TEST_TAG_NAME_1)
            .build();

        TagCreateListDTO createListDTO1 = TagCreateListDTO.newBuilder()
            .addAllTags(Collections.singletonList(createDTO1)).setNodeId(nodeId).build();

        serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).addTags(createListDTO1);

        TagCreateDTO createDTO2 = TagCreateDTO.newBuilder()
            .setName(TEST_TAG_NAME_2)
            .build();

        TagCreateListDTO createListDTO2 = TagCreateListDTO.newBuilder()
            .addAllTags(Collections.singletonList(createDTO2)).setNodeId(nodeId).build();

        serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).addTags(createListDTO2);

        List<Tag> allTags = tagRepository.findAll();
        assertEquals(2, allTags.size());

        ListTagsByEntityIdParamsDTO params = ListTagsByEntityIdParamsDTO.newBuilder().setNodeId(nodeId).setParams(TagListParamsDTO.newBuilder().setSearchTerm("tag-name").build()).build();
        TagListDTO tagsByNodeId = serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).getTagsByEntityId(params);
        List<TagDTO> tagsList = tagsByNodeId.getTagsList();
        assertEquals(2, tagsList.size());
    }

    @Test
    void testGetTagListForAzureCredentialWithNameLikeNoResults() {
        long credentialId = setupAzureCredentialDatabase();

        TagCreateDTO createDTO1 = TagCreateDTO.newBuilder()
            .setName(TEST_TAG_NAME_1)
            .build();

        TagCreateListDTO createListDTO1 = TagCreateListDTO.newBuilder()
            .addAllTags(Collections.singletonList(createDTO1)).setAzureCredentialId(credentialId).build();

        serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).addTags(createListDTO1);

        TagCreateDTO createDTO2 = TagCreateDTO.newBuilder()
            .setName(TEST_TAG_NAME_2)
            .build();

        TagCreateListDTO createListDTO2 = TagCreateListDTO.newBuilder()
            .addAllTags(Collections.singletonList(createDTO2)).setAzureCredentialId(credentialId).build();

        serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).addTags(createListDTO2);

        List<Tag> allTags = tagRepository.findAll();
        assertEquals(2, allTags.size());

        ListTagsByEntityIdParamsDTO params = ListTagsByEntityIdParamsDTO.newBuilder().setAzureCredentialId(credentialId).setParams(TagListParamsDTO.newBuilder().setSearchTerm("tag-name-INVALID").build()).build();
        TagListDTO tagsByNodeId = serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).getTagsByEntityId(params);
        List<TagDTO> tagsList = tagsByNodeId.getTagsList();
        assertEquals(0, tagsList.size());
    }

    @Test
    void testGetTagListForNodeWithNameLikeNoResults() {
        long nodeId = setupDatabase();

        TagCreateDTO createDTO1 = TagCreateDTO.newBuilder()
            .setName(TEST_TAG_NAME_1)
            .build();

        TagCreateListDTO createListDTO1 = TagCreateListDTO.newBuilder()
            .addAllTags(Collections.singletonList(createDTO1)).setNodeId(nodeId).build();

        serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).addTags(createListDTO1);

        TagCreateDTO createDTO2 = TagCreateDTO.newBuilder()
            .setName(TEST_TAG_NAME_2)
            .build();

        TagCreateListDTO createListDTO2 = TagCreateListDTO.newBuilder()
            .addAllTags(Collections.singletonList(createDTO2)).setNodeId(nodeId).build();

        serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).addTags(createListDTO2);

        List<Tag> allTags = tagRepository.findAll();
        assertEquals(2, allTags.size());

        ListTagsByEntityIdParamsDTO params = ListTagsByEntityIdParamsDTO.newBuilder().setNodeId(nodeId).setParams(TagListParamsDTO.newBuilder().setSearchTerm("tag-name-INVALID").build()).build();
        TagListDTO tagsByNodeId = serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).getTagsByEntityId(params);
        List<TagDTO> tagsList = tagsByNodeId.getTagsList();
        assertEquals(0, tagsList.size());
    }

    @Test
    void testGetTagListForAzureCredential() {
        long credentialId = setupAzureCredentialDatabase();

        TagCreateDTO createDTO1 = TagCreateDTO.newBuilder()
            .setName(TEST_TAG_NAME_1)
            .build();

        TagCreateListDTO createListDTO1 = TagCreateListDTO.newBuilder()
            .addAllTags(Collections.singletonList(createDTO1)).setAzureCredentialId(credentialId).build();

        serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).addTags(createListDTO1);

        TagCreateDTO createDTO2 = TagCreateDTO.newBuilder()
            .setName(TEST_TAG_NAME_2)
            .build();

        TagCreateListDTO createListDTO2 = TagCreateListDTO.newBuilder()
            .addAllTags(Collections.singletonList(createDTO2)).setAzureCredentialId(credentialId).build();

        serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).addTags(createListDTO2);

        List<Tag> allTags = tagRepository.findAll();
        assertEquals(2, allTags.size());

        ListTagsByEntityIdParamsDTO params = ListTagsByEntityIdParamsDTO.newBuilder().setAzureCredentialId(credentialId).setParams(TagListParamsDTO.newBuilder().build()).build();
        TagListDTO tagsByNodeId = serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).getTagsByEntityId(params);
        List<TagDTO> tagsList = tagsByNodeId.getTagsList();
        assertEquals(2, tagsList.size());
    }

    @Test
    void testGetTagListForAzureCredentialWithNameLike() {
        long credentialId = setupAzureCredentialDatabase();

        TagCreateDTO createDTO1 = TagCreateDTO.newBuilder()
            .setName(TEST_TAG_NAME_1)
            .build();

        TagCreateListDTO createListDTO1 = TagCreateListDTO.newBuilder()
            .addAllTags(Collections.singletonList(createDTO1)).setAzureCredentialId(credentialId).build();

        serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).addTags(createListDTO1);

        TagCreateDTO createDTO2 = TagCreateDTO.newBuilder()
            .setName(TEST_TAG_NAME_2)
            .build();

        TagCreateListDTO createListDTO2 = TagCreateListDTO.newBuilder()
            .addAllTags(Collections.singletonList(createDTO2)).setAzureCredentialId(credentialId).build();

        serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).addTags(createListDTO2);

        List<Tag> allTags = tagRepository.findAll();
        assertEquals(2, allTags.size());

        ListTagsByEntityIdParamsDTO params = ListTagsByEntityIdParamsDTO.newBuilder().setAzureCredentialId(credentialId).setParams(TagListParamsDTO.newBuilder().setSearchTerm("tag-name").build()).build();
        TagListDTO tagsByNodeId = serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).getTagsByEntityId(params);
        List<TagDTO> tagsList = tagsByNodeId.getTagsList();
        assertEquals(2, tagsList.size());
    }




    @Test
    void testGetTagList() {
        long nodeId = setupDatabase();

        TagCreateDTO createDTO1 = TagCreateDTO.newBuilder()
            .setName(TEST_TAG_NAME_1)
            .build();

        TagCreateListDTO createListDTO1 = TagCreateListDTO.newBuilder()
            .addAllTags(Collections.singletonList(createDTO1)).setNodeId(nodeId).build();

        serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).addTags(createListDTO1);

        TagCreateDTO createDTO2 = TagCreateDTO.newBuilder()
            .setName(TEST_TAG_NAME_2)
            .build();

        TagCreateListDTO createListDTO2 = TagCreateListDTO.newBuilder()
            .addAllTags(Collections.singletonList(createDTO2)).setNodeId(nodeId).build();

        serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).addTags(createListDTO2);

        List<Tag> allTags = tagRepository.findAll();
        assertEquals(2, allTags.size());

        ListAllTagsParamsDTO params = ListAllTagsParamsDTO.newBuilder().setParams(TagListParamsDTO.newBuilder().build()).build();
        TagListDTO tagsByNodeId = serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).getTags(params);
        List<TagDTO> tagsList = tagsByNodeId.getTagsList();
        assertEquals(2, tagsList.size());
    }

    @Test
    void testGetTagListWithNameLike() {
        long nodeId = setupDatabase();

        TagCreateDTO createDTO1 = TagCreateDTO.newBuilder()
            .setName(TEST_TAG_NAME_1)
            .build();

        TagCreateListDTO createListDTO1 = TagCreateListDTO.newBuilder()
            .addAllTags(Collections.singletonList(createDTO1)).setNodeId(nodeId).build();

        serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).addTags(createListDTO1);

        TagCreateDTO createDTO2 = TagCreateDTO.newBuilder()
            .setName(TEST_TAG_NAME_2)
            .build();

        TagCreateListDTO createListDTO2 = TagCreateListDTO.newBuilder()
            .addAllTags(Collections.singletonList(createDTO2)).setNodeId(nodeId).build();

        serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).addTags(createListDTO2);

        List<Tag> allTags = tagRepository.findAll();
        assertEquals(2, allTags.size());

        ListAllTagsParamsDTO params = ListAllTagsParamsDTO.newBuilder().setParams(TagListParamsDTO.newBuilder().setSearchTerm("tag-name").build()).build();
        TagListDTO tagsByNodeId = serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).getTags(params);
        List<TagDTO> tagsList = tagsByNodeId.getTagsList();
        assertEquals(2, tagsList.size());
    }

    @Test
    void testGetTagListWithNameLikeNoResults() {
        long nodeId = setupDatabase();

        TagCreateDTO createDTO1 = TagCreateDTO.newBuilder()
            .setName(TEST_TAG_NAME_1)
            .build();

        TagCreateListDTO createListDTO1 = TagCreateListDTO.newBuilder()
            .addAllTags(Collections.singletonList(createDTO1)).setNodeId(nodeId).build();

        serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).addTags(createListDTO1);

        TagCreateDTO createDTO2 = TagCreateDTO.newBuilder()
            .setName(TEST_TAG_NAME_2)
            .build();

        TagCreateListDTO createListDTO2 = TagCreateListDTO.newBuilder()
            .addAllTags(Collections.singletonList(createDTO2)).setNodeId(nodeId).build();

        serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).addTags(createListDTO2);

        List<Tag> allTags = tagRepository.findAll();
        assertEquals(2, allTags.size());

        ListAllTagsParamsDTO params = ListAllTagsParamsDTO.newBuilder().setParams(TagListParamsDTO.newBuilder().setSearchTerm("tag-name-INVALID").build()).build();
        TagListDTO tagsByNodeId = serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(authHeader))).getTags(params);
        List<TagDTO> tagsList = tagsByNodeId.getTagsList();
        assertEquals(0, tagsList.size());
    }

    private long setupDatabase() {
        MonitoringLocation location = new MonitoringLocation();
        location.setLocation(TEST_LOCATION);
        location.setTenantId(tenantId);
        location = locationRepository.saveAndFlush(location);

        Node node = new Node();
        node.setNodeLabel(TEST_NODE_LABEL_1);
        node.setCreateTime(LocalDateTime.now());
        node.setTenantId(tenantId);
        node.setMonitoringLocation(location);
        node.setMonitoringLocationId(location.getId());
        node = nodeRepository.saveAndFlush(node);
        return node.getId();
    }

    private long setupAzureCredentialDatabase() {
        MonitoringLocation location = new MonitoringLocation();
        location.setLocation(TEST_LOCATION);
        location.setTenantId(tenantId);
        location = locationRepository.saveAndFlush(location);

        AzureCredential azureCredential = new AzureCredential();
        azureCredential.setMonitoringLocation(location);
        azureCredential.setName("test-name");
        azureCredential.setDirectoryId("test-directory-id");
        azureCredential.setSubscriptionId("test-subscription-id");
        azureCredential.setClientSecret("test-client-secret");
        azureCredential.setClientId("test-client-id");
        azureCredential.setCreateTime(LocalDateTime.now());
        azureCredential.setTenantId(tenantId);
        azureCredential = azureCredentialRepository.saveAndFlush(azureCredential);
        return azureCredential.getId();
    }
}
