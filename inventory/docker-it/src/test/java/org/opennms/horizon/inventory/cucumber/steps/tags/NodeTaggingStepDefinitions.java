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

package org.opennms.horizon.inventory.cucumber.steps.tags;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.opennms.horizon.inventory.cucumber.InventoryBackgroundHelper;
import org.opennms.horizon.inventory.dto.DeleteTagsDTO;
import org.opennms.horizon.inventory.dto.ListAllTagsParamsDTO;
import org.opennms.horizon.inventory.dto.ListTagsByEntityIdParamsDTO;
import org.opennms.horizon.inventory.dto.NodeCreateDTO;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.inventory.dto.NodeList;
import org.opennms.horizon.inventory.dto.TagCreateDTO;
import org.opennms.horizon.inventory.dto.TagCreateListDTO;
import org.opennms.horizon.inventory.dto.TagDTO;
import org.opennms.horizon.inventory.dto.TagEntityIdDTO;
import org.opennms.horizon.inventory.dto.TagListDTO;
import org.opennms.horizon.inventory.dto.TagListParamsDTO;
import org.opennms.horizon.inventory.dto.TagNameQuery;
import org.opennms.horizon.inventory.dto.TagRemoveListDTO;
import org.opennms.horizon.shared.common.tag.proto.TagOperationList;
import org.opennms.horizon.shared.common.tag.proto.TagOperationProto;

import com.google.protobuf.Empty;
import com.google.protobuf.Int64Value;
import com.google.protobuf.InvalidProtocolBufferException;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NodeTaggingStepDefinitions {
    private final InventoryBackgroundHelper backgroundHelper;

    private NodeDTO node1;
    private NodeDTO node2;
    private TagListDTO addedTagList;
    private TagListDTO fetchedTagList;
    private NodeList fetchedNodeList;
    private long tagMessageFilterTime;

    public NodeTaggingStepDefinitions(InventoryBackgroundHelper backgroundHelper) {
        this.backgroundHelper = backgroundHelper;
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
        tagMessageFilterTime = System.currentTimeMillis();
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
        tagMessageFilterTime = System.currentTimeMillis();
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
        tagMessageFilterTime = System.currentTimeMillis();
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
    @Then("The node tag response should contain only tags {string}")
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

    @Then("A GRPC request to get nodes for tags {string}")
    public void aGRPCRequestToGetNodesForTags(String tags) {
        String[] tagArray = tags.split(",");

        var nodeServiceBlockingStub = backgroundHelper.getNodeServiceBlockingStub();
        fetchedNodeList = nodeServiceBlockingStub.listNodesByTags(TagNameQuery.newBuilder()
            .addAllTags(Arrays.stream(tagArray).toList()).build());
    }

    @Then("Both nodes should be fetched for")
    public void bothNodesShouldBeFetchedFor() {
        assertEquals(2, fetchedNodeList.getNodesCount());
        List<NodeDTO> nodesList = fetchedNodeList.getNodesList();

        List<Long> nodeIds = nodesList.stream().map(NodeDTO::getId).toList();
        assertTrue(nodeIds.contains(node1.getId()));
        assertTrue(nodeIds.contains(node2.getId()));

        List<String> nodeLabels = nodesList.stream().map(NodeDTO::getNodeLabel).toList();
        assertTrue(nodeLabels.contains(node1.getNodeLabel()));
        assertTrue(nodeLabels.contains(node2.getNodeLabel()));
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

    @Then("Verify Kafka message with {int} node(s)")
    public void verifyKafkaMessageWithData(int nodeCount, DataTable table) throws InvalidProtocolBufferException, InterruptedException {
        List<Map<String, String>> dataList = table.asMaps();
        long endTime = System.currentTimeMillis() + 60000; //1 minute
        List<Long> nodeIdList = switch (nodeCount) {
            case 1 -> List.of(node1.getId());
            case 2 -> List.of(node1.getId(), node2.getId());
            default -> new ArrayList<>();
        };
        while (System.currentTimeMillis() < endTime) {
            ConsumerRecords<String, byte[]> records = InventoryBackgroundHelper.getKafkaConsumer().poll(Duration.ofMillis(300));
            if(!records.isEmpty()) {
                List<TagOperationList> tagOpList = new ArrayList<>();
                for(ConsumerRecord<String, byte[]> r : records) {
                    if (r.timestamp() > tagMessageFilterTime) {
                        tagOpList.add(TagOperationList.parseFrom(r.value()));
                    }
                }

                List<TagOperationList> list = tagOpList.stream().filter(t -> t.getTagsList().size() == dataList.size()).toList();
                list.forEach(l -> {
                    List<TagOperationProto> opList = l.getTagsList();
                    assertEquals(dataList.size(), opList.size());
                    verifyKafkaMessage(dataList, opList, nodeIdList);
                });

                return;
            }
            Thread.sleep(300);
        }
        fail("Failed receiving Kafka message in 1 minute");
    }

    @Given("Kafka topic {string}")
    public void kafkaTopic(String topic) {
        backgroundHelper.subscribeKafkaTopics(List.of(topic));
    }

    @Then("Delete the node")
    public void deleteTheNode() {
        tagMessageFilterTime = System.currentTimeMillis();
        backgroundHelper.getNodeServiceBlockingStub().deleteNode(Int64Value.of(node1.getId()));
    }

    private void verifyKafkaMessage(List<Map<String, String>> data, List<TagOperationProto> result, List<Long> nodeIds) {
        data.forEach(map -> {
            List<TagOperationProto> topPlist = result.stream().filter(r -> r.getTenantId().equals(map.get("tenant_id"))
                && r.getTagName().equals(map.get("tag_name"))).toList();
            if(topPlist.isEmpty()) {
                fail("Failed receiving TagOperation massages");
            }
            topPlist.forEach(top -> {
                assertEquals(map.get("action"), top.getOperation().name());
                assertTrue(nodeIds.containsAll(top.getNodeIdList()));
                assertEquals(1, top.getNodeIdList().size());
            });
        });
    }
}
