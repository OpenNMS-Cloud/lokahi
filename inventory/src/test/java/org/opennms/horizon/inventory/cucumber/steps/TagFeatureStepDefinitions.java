package org.opennms.horizon.inventory.cucumber.steps;

import com.google.protobuf.Int64Value;
import io.cucumber.java.BeforeAll;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.opennms.horizon.inventory.cucumber.InventoryBackgroundHelper;
import org.opennms.horizon.inventory.dto.NodeCreateDTO;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.inventory.dto.TagCreateDTO;
import org.opennms.horizon.inventory.dto.TagCreateListDTO;
import org.opennms.horizon.inventory.dto.TagListDTO;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class TagFeatureStepDefinitions {

    private static InventoryBackgroundHelper backgroundHelper;

    private NodeDTO node;
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
    @Given("A new node with tags {string}")
    public void aNewNodeWithTags(String tags) {
        var nodeServiceBlockingStub = backgroundHelper.getNodeServiceBlockingStub();
        node = nodeServiceBlockingStub.createNode(NodeCreateDTO.newBuilder().setLabel("node")
            .setLocation("location").setManagementIp("127.0.0.1").build());
        String[] tagArray = tags.split(",");
        var tagServiceBlockingStub = backgroundHelper.getTagServiceBlockingStub();
        List<TagCreateDTO> tagCreateList = new ArrayList<>();
        for (String name : tagArray) {
            tagCreateList.add(TagCreateDTO.newBuilder().setName(name).build());
        }
        tagServiceBlockingStub.addTags(TagCreateListDTO.newBuilder()
            .addAllTags(tagCreateList).setNodeId(node.getId()).build());
    }

    @Given("A new node with no tags")
    public void aNewNodeWithNoTags() {
        var nodeServiceBlockingStub = backgroundHelper.getNodeServiceBlockingStub();
        node = nodeServiceBlockingStub.createNode(NodeCreateDTO.newBuilder().setLabel("node")
            .setLocation("location").setManagementIp("127.0.0.2").build());
    }

    /*
     * SCENARIO WHEN
     * *********************************************************************************
     */
    @When("A GRPC request to fetch tags for node")
    public void aGrpcRequestToFetchTagsForNode() {
        var tagServiceBlockingStub = backgroundHelper.getTagServiceBlockingStub();
        fetchedTagList = tagServiceBlockingStub.getTagsByNodeId(Int64Value.newBuilder()
            .setValue(node.getId()).build());
    }

    /*
     * SCENARIO THEN
     * *********************************************************************************
     */
    @Then("The response should contain only tags {string}")
    public void theResponseShouldContainOnlyTags(String tags) {
        assertNotNull(fetchedTagList);
        assertEquals(2, fetchedTagList.getTagsCount());

        String[] tagArray = tags.split(",");
        assertEquals(tagArray[0], fetchedTagList.getTags(0).getName());
        assertEquals(tagArray[1], fetchedTagList.getTags(1).getName());
    }

    @Then("The response should contain an empty list of tags")
    public void theResponseShouldContainAnEmptyListOfTags() {
        assertNotNull(fetchedTagList);
        assertEquals(0, fetchedTagList.getTagsCount());
    }
}
