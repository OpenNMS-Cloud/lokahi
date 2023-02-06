package org.opennms.horizon.inventory.cucumber.steps;

import io.cucumber.java.BeforeAll;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.opennms.horizon.inventory.cucumber.InventoryBackgroundHelper;
import org.opennms.horizon.inventory.dto.NodeCreateDTO;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.inventory.dto.NodeServiceGrpc;


public class TagFeatureStepDefinitions {

    private static InventoryBackgroundHelper backgroundHelper;

    @BeforeAll
    public static void beforeAll() {
        backgroundHelper = new InventoryBackgroundHelper();
    }

    /*
     * GIVEN
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

    @Given("a node new node with tags {string}")
    public void aNewNodeWithTags(String tags) {
        System.out.println("ListTagsCucumberTestSteps.nodeWithIDWithTags");
        var nodeServiceBlockingStub = backgroundHelper.getNodeServiceBlockingStub();
        NodeDTO node = nodeServiceBlockingStub.createNode(NodeCreateDTO.newBuilder().setLabel("node")
            .setLocation("location").setManagementIp("127.0.0.1").build());
        String[] tagArray = tags.split(",");
    }

    @Given("[Tags] Create Grpc Connection for Inventory")
    public void createGrpcConnectionForInventory() {
        backgroundHelper.createGrpcConnectionForInventory();
    }


    /*
     * WHEN
     * *********************************************************************************
     */


    @When("a GRPC Request with a parameter {string}")
    public void aGRPCRequestWithAParameter(String nodeId) {
        System.out.println("ListTagsCucumberTestSteps.aGRPCRequestWithAParameter");
        System.out.println("nodeId = " + nodeId);
    }


    /*
     * THEN
     * *********************************************************************************
     */

    @Then("the response should contain only tags {string}")
    public void theResponseShouldContainOnlyTags(String tags) {
        System.out.println("ListTagsCucumberTestSteps.theResponseShouldContainOnlyTags");
        System.out.println("tags = " + tags);
    }
}
