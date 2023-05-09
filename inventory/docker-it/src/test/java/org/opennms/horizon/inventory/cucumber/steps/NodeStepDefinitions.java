package org.opennms.horizon.inventory.cucumber.steps;

import com.google.protobuf.Empty;
import com.google.protobuf.Int64Value;
import com.google.protobuf.InvalidProtocolBufferException;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.Assert;
import org.opennms.horizon.inventory.cucumber.InventoryBackgroundHelper;
import org.opennms.horizon.inventory.cucumber.RetryUtils;
import org.opennms.horizon.inventory.cucumber.kafkahelper.KafkaTestHelper;
import org.opennms.horizon.inventory.dto.MonitoringLocationDTO;
import org.opennms.horizon.inventory.dto.NodeCreateDTO;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.inventory.dto.NodeLabelSearchQuery;
import org.opennms.horizon.inventory.dto.NodeList;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class NodeStepDefinitions {
    private final InventoryBackgroundHelper backgroundHelper;
    private RetryUtils retryUtils;
    private KafkaTestHelper kafkaTestHelper;
    private NodeDTO node;
    private MonitoringLocationDTO monitoringLocation;
    private NodeList fetchedNodeList;
    private String nodeTopic;

    public NodeStepDefinitions(RetryUtils retryUtils, KafkaTestHelper kafkaTestHelper, InventoryBackgroundHelper backgroundHelper) {
        this.retryUtils = retryUtils;
        this.kafkaTestHelper = kafkaTestHelper;
        this.backgroundHelper = backgroundHelper;
        nodeTopic = "node";
    }

    private void initKafka() {
        kafkaTestHelper.setKafkaBootstrapUrl(backgroundHelper.getKafkaBootstrapUrl());
        kafkaTestHelper.startConsumerAndProducer(nodeTopic, nodeTopic);
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
        initKafka();
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

    @Then("verify node topic has {int} messages with tenant {string}")
    public void verifyNodeTopicContainsTenant(int expectedMessages, String tenant) throws InterruptedException {
        boolean success = retryUtils.retry(
            () -> this.checkNumberOfMessageForOneTenant(tenant, expectedMessages, nodeTopic),
            result -> result,
            100,
            10000,
            false);

        Assert.assertTrue("Verify node topic has the right number of message(s)", success);
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

    private boolean checkNumberOfMessageForOneTenant(String tenant, int expectedMessages, String topic) {
        int foundMessages = 0;
        List<ConsumerRecord<String, byte[]>> records = kafkaTestHelper.getConsumedMessages(topic);
        for (ConsumerRecord<String, byte[]> record: records) {
            if (record.value() == null) {
                continue;
            }
            NodeDTO nodeDTO;
            try {
                nodeDTO = NodeDTO.parseFrom(record.value());
            } catch (InvalidProtocolBufferException e) {
                throw new RuntimeException(e);
            }

            if (tenant.equals(nodeDTO.getTenantId())) {
                foundMessages++;
            }
        }
        log.info("Found {} messages for tenant {}", foundMessages, tenant);
        return foundMessages == expectedMessages;
    }
}
