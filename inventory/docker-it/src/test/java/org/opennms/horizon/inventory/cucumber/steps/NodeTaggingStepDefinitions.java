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
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.opennms.horizon.inventory.cucumber.InventoryBackgroundHelper;
import org.opennms.horizon.inventory.dto.DeleteTagsDTO;
import org.opennms.horizon.inventory.dto.ListAllTagsParamsDTO;
import org.opennms.horizon.inventory.dto.ListTagsByEntityIdParamsDTO;
import org.opennms.horizon.inventory.dto.NodeCreateDTO;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.inventory.dto.TagCreateDTO;
import org.opennms.horizon.inventory.dto.TagCreateListDTO;
import org.opennms.horizon.inventory.dto.TagDTO;
import org.opennms.horizon.inventory.dto.TagListDTO;
import org.opennms.horizon.inventory.dto.TagListParamsDTO;
import org.opennms.horizon.inventory.dto.TagRemoveListDTO;
import org.opennms.horizon.inventory.dto.TagServiceGrpc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class NodeTaggingStepDefinitions {
    private static InventoryBackgroundHelper backgroundHelper;

    private NodeDTO node;
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
        node = nodeServiceBlockingStub.createNode(NodeCreateDTO.newBuilder().setLabel("node")
            .setLocation("location").setManagementIp("127.0.0.1").build());
    }


    @Given("A new node with tags {string}")
    public void aNewNodeWithTags(String tags) {
        deleteAllTags();
        deleteAllNodes();

        var nodeServiceBlockingStub = backgroundHelper.getNodeServiceBlockingStub();
        node = nodeServiceBlockingStub.createNode(NodeCreateDTO.newBuilder().setLabel("node")
            .setLocation("location").setManagementIp("127.0.0.1").build());
        String[] tagArray = tags.split(",");
        var tagServiceBlockingStub = backgroundHelper.getTagServiceBlockingStub();
        List<TagCreateDTO> tagCreateList = new ArrayList<>();
        for (String name : tagArray) {
            tagCreateList.add(TagCreateDTO.newBuilder().setName(name).build());
        }
        addedTagList = tagServiceBlockingStub.addTags(TagCreateListDTO.newBuilder()
            .addAllTags(tagCreateList).setNodeId(node.getId()).build());
    }


    @Given("A new node with no tags")
    public void aNewNodeWithNoTags() {
        deleteAllTags();
        deleteAllNodes();

        var nodeServiceBlockingStub = backgroundHelper.getNodeServiceBlockingStub();
        node = nodeServiceBlockingStub.createNode(NodeCreateDTO.newBuilder().setLabel("node")
            .setLocation("location").setManagementIp("127.0.0.1").build());
    }

    @Given("Another node with tags {string}")
    public void anotherNodeWithTags(String tags) {
        var nodeServiceBlockingStub = backgroundHelper.getNodeServiceBlockingStub();
        NodeDTO node = nodeServiceBlockingStub.createNode(NodeCreateDTO.newBuilder().setLabel("Another Node")
            .setLocation("location").setManagementIp("127.0.0.2").build());
        String[] tagArray = tags.split(",");
        var tagServiceBlockingStub = backgroundHelper.getTagServiceBlockingStub();
        List<TagCreateDTO> tagCreateList = new ArrayList<>();
        for (String name : tagArray) {
            tagCreateList.add(TagCreateDTO.newBuilder().setName(name).build());
        }
        tagServiceBlockingStub.addTags(TagCreateListDTO.newBuilder()
            .addAllTags(tagCreateList).setNodeId(node.getId()).build());
    }

    /*
     * SCENARIO WHEN
     * *********************************************************************************
     */
    @When("A GRPC request to create tags {string} for node")
    public void aGRPCRequestToCreateTagsForNode(String tags) {
        String[] tagArray = tags.split(",");
        var tagServiceBlockingStub = backgroundHelper.getTagServiceBlockingStub();
        List<TagCreateDTO> tagCreateList = new ArrayList<>();
        for (String name : tagArray) {
            tagCreateList.add(TagCreateDTO.newBuilder().setName(name).build());
        }
        fetchedTagList = tagServiceBlockingStub.addTags(TagCreateListDTO.newBuilder()
            .addAllTags(tagCreateList).setNodeId(node.getId()).build());
    }

    @When("A GRPC request to fetch tags for node")
    public void aGrpcRequestToFetchTagsForNode() {
        var tagServiceBlockingStub = backgroundHelper.getTagServiceBlockingStub();
        ListTagsByEntityIdParamsDTO params = ListTagsByEntityIdParamsDTO.newBuilder()
            .setNodeId(node.getId()).setParams(TagListParamsDTO.newBuilder().build()).build();
        fetchedTagList = tagServiceBlockingStub.getTagsByEntityId(params);
    }

    @When("A GRPC request to remove tag {string} for node")
    public void aGRPCRequestToRemoveTagForNode(String tag) {
        var tagServiceBlockingStub = backgroundHelper.getTagServiceBlockingStub();
        for (TagDTO tagDTO : addedTagList.getTagsList()) {
            if (tagDTO.getName().equals(tag)) {
                tagServiceBlockingStub.removeTags(TagRemoveListDTO.newBuilder()
                    .addAllTagIds(Collections.singletonList(Int64Value.newBuilder()
                        .setValue(tagDTO.getId()).build())).setNodeId(node.getId()).build());
                break;
            }
        }
        ListTagsByEntityIdParamsDTO params = ListTagsByEntityIdParamsDTO.newBuilder()
            .setNodeId(node.getId()).setParams(TagListParamsDTO.newBuilder().build()).build();
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
            .setNodeId(node.getId())
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

        for (int index = 0; index < tagArray.length; index++) {
            assertEquals(tagArray[index], fetchedTagList.getTags(index).getName());
        }
    }

    @Then("The response should contain an empty list of tags")
    public void theResponseShouldContainAnEmptyListOfTags() {
        assertNotNull(fetchedTagList);
        assertEquals(0, fetchedTagList.getTagsCount());
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
}
