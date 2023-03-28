package org.opennms.horizon.inventory.cucumber.steps;

import com.google.protobuf.Empty;
import com.google.protobuf.Int64Value;
import io.cucumber.java.BeforeAll;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.opennms.horizon.inventory.cucumber.InventoryBackgroundHelper;
import org.opennms.horizon.inventory.dto.MonitoringLocationDTO;
import org.opennms.horizon.inventory.dto.NodeCreateDTO;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.inventory.dto.NodeLabelSearchQuery;
import org.opennms.horizon.inventory.dto.NodeList;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NodeStepDefinitions {
    private final InventoryBackgroundHelper backgroundHelper;
    private NodeDTO node;
    private MonitoringLocationDTO monitoringLocation;
    private NodeList fetchedNodeList;

    public NodeStepDefinitions(InventoryBackgroundHelper backgroundHelper) {
        this.backgroundHelper = backgroundHelper;
    }

    /*
     * BACKGROUND GIVEN
     * *********************************************************************************
     */
    @Given("[Node] External GRPC Port in system property {string}")
    public void externalGRPCPortInSystemProperty(String propertyName) {
        backgroundHelper.externalGRPCPortInSystemProperty(propertyName);
    }

    @Given("[Node] Kafka Bootstrap URL in system property {string}")
    public void kafkaBootstrapURLInSystemProperty(String systemPropertyName) {
        backgroundHelper.kafkaBootstrapURLInSystemProperty(systemPropertyName);
    }

    @Given("[Node] Grpc TenantId {string}")
    public void grpcTenantId(String tenantId) {
        backgroundHelper.grpcTenantId(tenantId);
    }

    @Given("[Node] Create Grpc Connection for Inventory")
    public void createGrpcConnectionForInventory() {
        backgroundHelper.createGrpcConnectionForInventory();
    }

    /*
     * SCENARIO GIVEN
     * *********************************************************************************
     */

    @Given("a new node with label {string}, ip address {string} and location {string}")
    public void aNewNodeWithLabelIpAddressAndLocation(String label, String ipAddress, String location) {
        deleteAllNodes();

        var nodeServiceBlockingStub = backgroundHelper.getNodeServiceBlockingStub();
        node = nodeServiceBlockingStub.createNode(NodeCreateDTO.newBuilder().setLabel(label)
            .setManagementIp(ipAddress).setLocation(location).build());

        var monitoringLocationStub = backgroundHelper.getMonitoringLocationStub();
        monitoringLocation = monitoringLocationStub.getLocationById(Int64Value.of(node.getMonitoringLocationId()));
    }

    /*
     * SCENARIO THEN
     * *********************************************************************************
     */

    @Then("verify that a new node is created with label {string}, ip address {string} and location {string}")
    public void verifyThatANewNodeIsCreatedWithLabelIpAddressAndLocation(String label, String ipAddress, String location) {
        assertEquals(label, node.getNodeLabel());
        assertEquals(ipAddress, node.getIpInterfaces(0).getIpAddress());
        assertEquals(location, monitoringLocation.getLocation());
    }

    @Then("fetch a list of nodes by node label with search term {string}")
    public void fetchAListOfNodesByNodeLabelWithSearchTerm(String labelSearchTerm) {
        var nodeServiceBlockingStub = backgroundHelper.getNodeServiceBlockingStub();
        fetchedNodeList = nodeServiceBlockingStub.listNodesByNodeLabel(NodeLabelSearchQuery.newBuilder()
            .setSearchTerm(labelSearchTerm).build());
    }

    @Then("verify the list of nodes has size {int} and labels contain {string}")
    public void verifyTheListOfNodesHasSizeAndLabelsContain(int nodeListSize, String labelSearchTerm) {
        assertEquals(nodeListSize, fetchedNodeList.getNodesCount());

        List<NodeDTO> nodesList = fetchedNodeList.getNodesList();
        nodesList.stream().map(NodeDTO::getNodeLabel)
            .forEach(label -> assertTrue(label.contains(labelSearchTerm)));
    }

    @Then("verify the list of nodes is empty")
    public void verifyTheListOfNodesIsEmpty() {
        assertEquals(0, fetchedNodeList.getNodesCount());
    }

    /*
     * INTERNAL
     * *********************************************************************************
     */
    private void deleteAllNodes() {
        var nodeServiceBlockingStub = backgroundHelper.getNodeServiceBlockingStub();
        for (NodeDTO nodeDTO : nodeServiceBlockingStub.listNodes(Empty.newBuilder().build()).getNodesList()) {
            nodeServiceBlockingStub.deleteNode(Int64Value.newBuilder().setValue(nodeDTO.getId()).build());
        }
    }
}
