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

package org.opennms.horizon.inventory.cucumber.steps;

import com.google.protobuf.Empty;
import com.google.protobuf.Int64Value;
import io.cucumber.java.BeforeAll;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.jetbrains.annotations.NotNull;
import org.opennms.horizon.inventory.cucumber.InventoryBackgroundHelper;
import org.opennms.horizon.inventory.dto.DeleteTagsDTO;
import org.opennms.horizon.inventory.dto.ListAllTagsParamsDTO;
import org.opennms.horizon.inventory.dto.ListTagsByEntityIdParamsDTO;
import org.opennms.horizon.inventory.dto.NodeCreateDTO;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.inventory.dto.TagCreateDTO;
import org.opennms.horizon.inventory.dto.TagCreateListDTO;
import org.opennms.horizon.inventory.dto.TagDTO;
import org.opennms.horizon.inventory.dto.TagEntityIdDTO;
import org.opennms.horizon.inventory.dto.TagListDTO;
import org.opennms.horizon.inventory.dto.TagListParamsDTO;
import org.opennms.horizon.inventory.dto.TagRemoveListDTO;
import org.opennms.horizon.inventory.dto.TagServiceGrpc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class NodeTaggingStepDefinitions {
    private static InventoryBackgroundHelper backgroundHelper;

    private NodeDTO node1;
    private NodeDTO node2;
    private TagListDTO addedTagList;
    private TagListDTO fetchedTagList;

    @BeforeAll
    public static void beforeAll() {
        backgroundHelper = new InventoryBackgroundHelper();
    }

    /*
     * BACKGROUND GIVEN
     * *********************************************************************************
     */
    @Given("[Tags] External GRPC Port in system property {string}")
    public void externalGRPCPortInSystemProperty(String propertyName) {
        backgroundHelper.externalGRPCPortInSystemProperty(propertyName);
    }

    @Given("[Tags] Kafka Bootstrap URL in system property {string}")
    public void kafkaBootstrapURLInSystemProperty(String systemPropertyName) {
        backgroundHelper.kafkaBootstrapURLInSystemProperty(systemPropertyName);
    }

    @Given("[Tags] Grpc TenantId {string}")
    public void grpcTenantId(String tenantId) {
        backgroundHelper.grpcTenantId(tenantId);
    }

    @Given("[Tags] Create Grpc Connection for Inventory")
    public void createGrpcConnectionForInventory() {
        backgroundHelper.createGrpcConnectionForInventory();
    }

    /*
     * SCENARIO GIVEN
     * *********************************************************************************
     */
    @Given("A new node")
    public void aNewNode() {
        deleteAllTags();
        deleteAllNodes();

        var nodeServiceBlockingStub = backgroundHelper.getNodeServiceBlockingStub();
        node1 = nodeServiceBlockingStub.createNode(NodeCreateDTO.newBuilder().setLabel("node")
            .setLocation("location").setManagementIp("127.0.0.1").build());
    }

    @Given("2 new nodes")
    public void twoNewNodes() {
        deleteAllTags();
        deleteAllNodes();

        var nodeServiceBlockingStub = backgroundHelper.getNodeServiceBlockingStub();
        node1 = nodeServiceBlockingStub.createNode(NodeCreateDTO.newBuilder().setLabel("node1")
            .setLocation("location").setManagementIp("127.0.0.1").build());
        node2 = nodeServiceBlockingStub.createNode(NodeCreateDTO.newBuilder().setLabel("node2")
            .setLocation("location").setManagementIp("127.0.0.2").build());
    }

    @Given("A new node with tags {string}")
    public void aNewNodeWithTags(String tags) {
        deleteAllTags();
        deleteAllNodes();

        var nodeServiceBlockingStub = backgroundHelper.getNodeServiceBlockingStub();
        node1 = nodeServiceBlockingStub.createNode(NodeCreateDTO.newBuilder().setLabel("node")
            .setLocation("location").setManagementIp("127.0.0.1").build());
        String[] tagArray = tags.split(",");
        var tagServiceBlockingStub = backgroundHelper.getTagServiceBlockingStub();
        List<TagCreateDTO> tagCreateList = getTagCreateList(tagArray);
        addedTagList = tagServiceBlockingStub.addTags(TagCreateListDTO.newBuilder()
            .addAllTags(tagCreateList)
            .addEntityIds(TagEntityIdDTO.newBuilder()
                .setNodeId(node1.getId())).build());
    }


    @Given("A new node with no tags")
    public void aNewNodeWithNoTags() {
        deleteAllTags();
        deleteAllNodes();

        var nodeServiceBlockingStub = backgroundHelper.getNodeServiceBlockingStub();
        node1 = nodeServiceBlockingStub.createNode(NodeCreateDTO.newBuilder().setLabel("node")
            .setLocation("location").setManagementIp("127.0.0.1").build());
    }

    @Given("Another node with tags {string}")
    public void anotherNodeWithTags(String tags) {
        var nodeServiceBlockingStub = backgroundHelper.getNodeServiceBlockingStub();
        NodeDTO node = nodeServiceBlockingStub.createNode(NodeCreateDTO.newBuilder().setLabel("Another Node")
            .setLocation("location").setManagementIp("127.0.0.2").build());
        String[] tagArray = tags.split(",");
        var tagServiceBlockingStub = backgroundHelper.getTagServiceBlockingStub();
        List<TagCreateDTO> tagCreateList = getTagCreateList(tagArray);
        tagServiceBlockingStub.addTags(TagCreateListDTO.newBuilder()
            .addAllTags(tagCreateList)
            .addEntityIds(TagEntityIdDTO.newBuilder()
                .setNodeId(node.getId())).build());
    }

    /*
     * SCENARIO WHEN
     * *********************************************************************************
     */
    @When("A GRPC request to create tags {string} for node")
    public void aGRPCRequestToCreateTagsForNode(String tags) {
        String[] tagArray = tags.split(",");
        var tagServiceBlockingStub = backgroundHelper.getTagServiceBlockingStub();
        List<TagCreateDTO> tagCreateList = getTagCreateList(tagArray);
        fetchedTagList = tagServiceBlockingStub.addTags(TagCreateListDTO.newBuilder()
            .addAllTags(tagCreateList)
            .addEntityIds(TagEntityIdDTO.newBuilder()
                .setNodeId(node1.getId())).build());
    }

    @When("A GRPC request to create tags {string} for both nodes")
    public void aGRPCRequestToCreateTagsForBothNodes(String tags) {
        String[] tagArray = tags.split(",");
        var tagServiceBlockingStub = backgroundHelper.getTagServiceBlockingStub();
        List<TagCreateDTO> tagCreateList = getTagCreateList(tagArray);

        List<TagEntityIdDTO> tagEntityList = new ArrayList<>();
        tagEntityList.add(TagEntityIdDTO.newBuilder().setNodeId(node1.getId()).build());
        tagEntityList.add(TagEntityIdDTO.newBuilder().setNodeId(node2.getId()).build());

        fetchedTagList = tagServiceBlockingStub.addTags(TagCreateListDTO.newBuilder()
            .addAllTags(tagCreateList)
            .addAllEntityIds(tagEntityList).build());
    }

    @When("A GRPC request to fetch tags for node")
    public void aGrpcRequestToFetchTagsForNode() {
        var tagServiceBlockingStub = backgroundHelper.getTagServiceBlockingStub();
        ListTagsByEntityIdParamsDTO params = ListTagsByEntityIdParamsDTO.newBuilder()
            .setEntityId(TagEntityIdDTO.newBuilder()
                .setNodeId(node1.getId()))
            .setParams(TagListParamsDTO.newBuilder().build()).build();
        fetchedTagList = tagServiceBlockingStub.getTagsByEntityId(params);
    }

    @When("A GRPC request to remove tag {string} for node")
    public void aGRPCRequestToRemoveTagForNode(String tag) {
        var tagServiceBlockingStub = backgroundHelper.getTagServiceBlockingStub();
        for (TagDTO tagDTO : addedTagList.getTagsList()) {
            if (tagDTO.getName().equals(tag)) {
                tagServiceBlockingStub.removeTags(TagRemoveListDTO.newBuilder()
                    .addAllTagIds(Collections.singletonList(Int64Value.newBuilder()
                        .setValue(tagDTO.getId()).build()))
                    .addEntityIds(TagEntityIdDTO.newBuilder()
                        .setNodeId(node1.getId())).build());
                break;
            }
        }
        ListTagsByEntityIdParamsDTO params = ListTagsByEntityIdParamsDTO.newBuilder()
            .setEntityId(TagEntityIdDTO.newBuilder()
                .setNodeId(node1.getId()))
            .setParams(TagListParamsDTO.newBuilder().build()).build();
        fetchedTagList = tagServiceBlockingStub.getTagsByEntityId(params);
    }

    @When("A GRPC request to fetch all tags")
    public void aGRPCRequestToFetchAllTags() {
        var tagServiceBlockingStub = backgroundHelper.getTagServiceBlockingStub();
        ListAllTagsParamsDTO params = ListAllTagsParamsDTO.newBuilder()
            .setParams(TagListParamsDTO.newBuilder().build()).build();
        fetchedTagList = tagServiceBlockingStub.getTags(params);
    }

    @When("A GRPC request to fetch all tags for node with name like {string}")
    public void aGRPCRequestToFetchAllTagsForNodeWithNameLike(String searchTerm) {
        var tagServiceBlockingStub = backgroundHelper.getTagServiceBlockingStub();
        ListTagsByEntityIdParamsDTO params = ListTagsByEntityIdParamsDTO.newBuilder()
            .setEntityId(TagEntityIdDTO.newBuilder()
                .setNodeId(node1.getId()))
            .setParams(TagListParamsDTO.newBuilder().setSearchTerm(searchTerm).build()).build();
        fetchedTagList = tagServiceBlockingStub.getTagsByEntityId(params);
    }

    @When("A GRPC request to fetch all tags with name like {string}")
    public void aGRPCRequestToFetchAllTagsWithNameLike(String searchTerm) {
        var tagServiceBlockingStub = backgroundHelper.getTagServiceBlockingStub();
        ListAllTagsParamsDTO params = ListAllTagsParamsDTO.newBuilder()
            .setParams(TagListParamsDTO.newBuilder().setSearchTerm(searchTerm).build()).build();
        fetchedTagList = tagServiceBlockingStub.getTags(params);
    }

    /*
     * SCENARIO THEN
     * *********************************************************************************
     */
    @Then("The response should contain only tags {string}")
    public void theResponseShouldContainOnlyTags(String tags) {
        String[] tagArray = tags.split(",");

        assertNotNull(fetchedTagList);
        assertEquals(tagArray.length, fetchedTagList.getTagsCount());

        List<String> tagArraySorted = Arrays.stream(tagArray).sorted().toList();
        List<TagDTO> fetchedTagListSorted = fetchedTagList.getTagsList().stream()
            .sorted(Comparator.comparing(TagDTO::getName)).toList();

        for (int index = 0; index < tagArraySorted.size(); index++) {
            assertEquals(tagArraySorted.get(index), fetchedTagListSorted.get(index).getName());
        }
    }

    @Then("The response should contain an empty list of tags")
    public void theResponseShouldContainAnEmptyListOfTags() {
        assertNotNull(fetchedTagList);
        assertEquals(0, fetchedTagList.getTagsCount());
    }

    @And("Both nodes have the same tags of {string}")
    public void bothNodesHaveTheSameTagsOf(String tags) {
        String[] tagArray = tags.split(",");

        var tagServiceBlockingStub = backgroundHelper.getTagServiceBlockingStub();
        TagListDTO node1TagList = tagServiceBlockingStub.getTagsByEntityId(ListTagsByEntityIdParamsDTO.newBuilder()
            .setEntityId(TagEntityIdDTO.newBuilder().setNodeId(node1.getId())).build());
        TagListDTO node2TagList = tagServiceBlockingStub.getTagsByEntityId(ListTagsByEntityIdParamsDTO.newBuilder()
            .setEntityId(TagEntityIdDTO.newBuilder().setNodeId(node2.getId())).build());

        assertEquals(tagArray.length, node1TagList.getTagsCount());
        assertEquals(node1TagList.getTagsCount(), node2TagList.getTagsCount());

        List<String> tagArraySorted = Arrays.stream(tagArray).sorted().toList();
        List<TagDTO> node1TagListSorted = node1TagList.getTagsList().stream()
            .sorted(Comparator.comparing(TagDTO::getName)).toList();
        List<TagDTO> node2TagListSorted = node2TagList.getTagsList().stream()
            .sorted(Comparator.comparing(TagDTO::getName)).toList();

        assertEquals(node1TagListSorted, node2TagListSorted);

        for (int index = 0; index < tagArraySorted.size(); index++) {
            assertEquals(tagArraySorted.get(index), node1TagListSorted.get(index).getName());
        }
    }

    /*
     * INTERNAL
     * *********************************************************************************
     */
    private void deleteAllTags() {
        var tagServiceBlockingStub = backgroundHelper.getTagServiceBlockingStub();
        List<Int64Value> tagIds = tagServiceBlockingStub.getTags(ListAllTagsParamsDTO.newBuilder().build())
            .getTagsList().stream().map(tagDTO -> Int64Value.of(tagDTO.getId())).toList();
        tagServiceBlockingStub.deleteTags(DeleteTagsDTO.newBuilder().addAllTagIds(tagIds).build());
    }

    private void deleteAllNodes() {
        var nodeServiceBlockingStub = backgroundHelper.getNodeServiceBlockingStub();
        for (NodeDTO nodeDTO : nodeServiceBlockingStub.listNodes(Empty.newBuilder().build()).getNodesList()) {
            nodeServiceBlockingStub.deleteNode(Int64Value.newBuilder().setValue(nodeDTO.getId()).build());
        }
    }

    private static List<TagCreateDTO> getTagCreateList(String[] tagArray) {
        List<TagCreateDTO> tagCreateList = new ArrayList<>();
        for (String name : tagArray) {
            tagCreateList.add(TagCreateDTO.newBuilder().setName(name).build());
        }
        return tagCreateList;
    }
}
