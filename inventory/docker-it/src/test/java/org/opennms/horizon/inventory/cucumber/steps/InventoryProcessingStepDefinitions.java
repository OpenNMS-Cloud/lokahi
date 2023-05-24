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

import com.google.protobuf.Any;
import com.google.protobuf.BoolValue;
import com.google.protobuf.Empty;
import com.google.protobuf.Int64Value;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.horizon.grpc.heartbeat.contract.HeartbeatMessage;
import org.opennms.horizon.grpc.heartbeat.contract.TenantLocationSpecificHeartbeatMessage;
import org.opennms.horizon.inventory.cucumber.InventoryBackgroundHelper;
import org.opennms.horizon.inventory.dto.NodeCreateDTO;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.inventory.dto.NodeIdQuery;
import org.opennms.horizon.inventory.dto.NodeList;
import org.opennms.horizon.inventory.testtool.miniongateway.wiremock.client.MinionGatewayWiremockTestSteps;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.opennms.inventory.types.ServiceType;
import org.opennms.node.scan.contract.NodeScanResult;
import org.opennms.node.scan.contract.ServiceResult;
import org.opennms.taskset.contract.ScannerResponse;
import org.opennms.taskset.contract.TaskResult;
import org.opennms.taskset.contract.TenantLocationSpecificTaskSetResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class InventoryProcessingStepDefinitions {
    private static final Logger LOG = LoggerFactory.getLogger(InventoryProcessingStepDefinitions.class);

    private InventoryBackgroundHelper backgroundHelper;
    private MinionGatewayWiremockTestSteps minionGatewayWiremockTestSteps;

    private String label;
    private String newDeviceIpAddress;
    private String location = GrpcConstants.DEFAULT_LOCATION;
    private String systemId;
    private boolean deviceDetectedInd;
    private String reason;

    private NodeDTO node;
    private NodeList nodeList;
    private Int64Value nodeIdCreated;

//========================================
// Constructor
//----------------------------------------

    public InventoryProcessingStepDefinitions(
        MinionGatewayWiremockTestSteps minionGatewayWiremockTestSteps,
        InventoryBackgroundHelper inventoryBackgroundHelper) {

        this.minionGatewayWiremockTestSteps = minionGatewayWiremockTestSteps;
        this.backgroundHelper = inventoryBackgroundHelper;
    }


//========================================
// Step Definitions
//----------------------------------------

    @Given("External GRPC Port in system property {string}")
    public void externalGRPCPortInSystemProperty(String propertyName) {
        backgroundHelper.externalGRPCPortInSystemProperty(propertyName);
    }

    @Given("Kafka Bootstrap URL in system property {string}")
    public void kafkaBootstrapURLInSystemProperty(String systemPropertyName) {
        backgroundHelper.kafkaBootstrapURLInSystemProperty(systemPropertyName);
    }

    @Given("Grpc TenantId {string}")
    public void grpcTenantId(String tenantId) {
        backgroundHelper.grpcTenantId(tenantId);
        minionGatewayWiremockTestSteps.setTaskTenantId(tenantId);
    }

    @Given("Grpc location {string}")
    public void grpcLocation(String location) {
        backgroundHelper.grpcLocation(location);
    }

    @Given("Minion at location {string} with system Id {string}")
    public void minionAtLocationWithSystemId(String location, String systemId) {
        Objects.requireNonNull(location);
        Objects.requireNonNull(systemId);
        this.location = location;
        this.systemId = systemId;
        LOG.info("Using Location {} and systemId {}", location, systemId);
    }

    @Given("Create Grpc Connection for Inventory")
    public void createGrpcConnectionForInventory() {
        backgroundHelper.createGrpcConnectionForInventory();
    }

    @Given("send heartbeat message to Kafka topic {string}")
    public void sendHeartbeatMessageToKafkaTopic(String topic) {
        Properties producerConfig = new Properties();
        producerConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, backgroundHelper.getKafkaBootstrapUrl());
        producerConfig.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getCanonicalName());
        producerConfig.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getCanonicalName());
        try (KafkaProducer<String, byte[]> kafkaProducer = new KafkaProducer<>(producerConfig)) {
            long millis = System.currentTimeMillis();
            TenantLocationSpecificHeartbeatMessage heartbeatMessage =
                TenantLocationSpecificHeartbeatMessage.newBuilder()
                    .setTenantId(backgroundHelper.getTenantId())
                    .setLocation(backgroundHelper.getLocation())
                    .setIdentity(Identity.newBuilder().setSystemId(systemId).build())
                    .setTimestamp(Timestamp.newBuilder().setSeconds(millis / 1000).setNanos((int) ((millis % 1000) * 1000000)).build())
                    .build();
            var producerRecord = new ProducerRecord<String, byte[]>(topic, heartbeatMessage.toByteArray());

            kafkaProducer.send(producerRecord);
        }
    }


    @Then("verify Monitoring system is created with system id {string}")
    public void verifyMonitoringSystemIsCreatedWithSystemId(String systemId) {
        var monitoringSystemStub = backgroundHelper.getMonitoringSystemStub();
        await().pollInterval(5, TimeUnit.SECONDS).atMost(30, TimeUnit.SECONDS).until(() -> monitoringSystemStub.listMonitoringSystem(Empty.newBuilder().build()).getSystemsList().size(),
            Matchers.equalTo(1));
        var systems = monitoringSystemStub.listMonitoringSystem(Empty.newBuilder().build()).getSystemsList();
        assertEquals(systemId, systems.get(0).getSystemId());
        assertEquals(backgroundHelper.getTenantId(), systems.get(0).getTenantId());
    }

    @Then("verify Monitoring location is created with location {string}")
    public void verifyMonitoringLocationIsCreatedWithLocation(String location) {
        var monitoringLocationStub = backgroundHelper.getMonitoringLocationStub();
        await().pollInterval(5, TimeUnit.SECONDS)
            .atMost(30, TimeUnit.SECONDS).until(() ->
                    monitoringLocationStub.getLocationByName(StringValue.newBuilder().setValue(location).build()).getLocation(),
                Matchers.notNullValue());
        var locationDTO = monitoringLocationStub.getLocationByName(StringValue.newBuilder().setValue(location).build());
        assertEquals(location, locationDTO.getLocation());
        assertEquals(backgroundHelper.getTenantId(), locationDTO.getTenantId());
    }

    @Then("verify the device has an interface with the given IP address")
    public void verifyTheDeviceHasAnInterfaceWithTheGivenIPAddress() {
        NodeDTO nodeDTO =
            backgroundHelper.getNodeServiceBlockingStub()
                .withInterceptors()
                .getNodeById(Int64Value.of(node.getId()))
                ;

        assertNotNull(nodeDTO);
        assertTrue(
            nodeDTO.getIpInterfacesList().stream().anyMatch((ele) -> ele.getIpAddress().equals(newDeviceIpAddress))
        );
    }

    @Given("Label {string}")
    public void label(String label) {
        this.label = label;
    }

    @Given("New/Existing Device IP Address {string}")
    public void newDeviceIPAddress(String ipAddress) {
        this.newDeviceIpAddress = ipAddress;
    }

    @Given("New/Existing Device Location {string}")
    public void newDeviceLocation(String location) {
        this.location = location;
        minionGatewayWiremockTestSteps.setTaskLocation(location);
    }

    @Then("add a new device")
    public void addANewDevice() {
        var nodeServiceBlockingStub = backgroundHelper.getNodeServiceBlockingStub();
        node = nodeServiceBlockingStub.createNode(NodeCreateDTO.newBuilder().setLabel(label).setManagementIp(newDeviceIpAddress).build());
        minionGatewayWiremockTestSteps.setNodeId(node.getId());
        assertNotNull(node);
    }

    @Then("remove the device")
    public void removeTheDevice() {
        var nodeServiceBlockingStub = backgroundHelper.getNodeServiceBlockingStub();

        var nodeId = nodeServiceBlockingStub.getNodeIdFromQuery(NodeIdQuery.newBuilder()
            .setIpAddress(newDeviceIpAddress).setLocation(location).build());

        assertNotNull(nodeId);

        BoolValue boolValue = nodeServiceBlockingStub.deleteNode(nodeId);

        assertTrue(boolValue.getValue());
    }

    @Then("verify the new node return fields match")
    public void verifyTheNewNodeReturnFieldsMatch() {
        assertEquals(label, node.getNodeLabel());
        assertEquals(1, node.getIpInterfacesCount());
        assertEquals(newDeviceIpAddress, node.getIpInterfacesList().get(0).getIpAddress());

        assertTrue(node.getObjectId().isEmpty());
        assertTrue(node.getSystemName().isEmpty());
        assertTrue(node.getSystemDescr().isEmpty());
        assertTrue(node.getSystemLocation().isEmpty());
        assertTrue(node.getSystemContact().isEmpty());
    }

    @Then("retrieve the list of nodes from Inventory")
    public void retrieveTheListOfNodesFromInventory() {
        var nodeServiceBlockingStub = backgroundHelper.getNodeServiceBlockingStub();
        nodeList = nodeServiceBlockingStub.listNodes(Empty.newBuilder().build());
    }

    @Then("verify that the new node is in the list returned from inventory")
    public void verifyThatTheNewNodeIsInTheListReturnedFromInventory() {
        Assertions.assertFalse(nodeList.getNodesList().isEmpty());

        var nodeOptional = nodeList.getNodesList().stream().filter(
                nodeDTO ->
                    (
                        ( nodeDTO.getNodeLabel().equals(label) ) &&
                        ( nodeDTO.getIpInterfacesList().stream().anyMatch(ipInterfaceDTO -> ipInterfaceDTO.getIpAddress().equals(newDeviceIpAddress))) )
                    )
            .findFirst();

        assertTrue(nodeOptional.isPresent());

        var node = nodeOptional.get();
        assertEquals(label, node.getNodeLabel());
    }

    @Given("add a new device with label {string} and ip address {string} and location {string}")
    public void addANewDeviceWithLabelAndIpAddressAndLocation(String label, String ipAddress, String location) {
        var nodeServiceBlockingStub = backgroundHelper.getNodeServiceBlockingStub();
        var nodeDto = nodeServiceBlockingStub.createNode(NodeCreateDTO.newBuilder().setLabel(label).setLocation(location)
            .setManagementIp(ipAddress).build());
        assertNotNull(nodeDto);
    }

    @Then("verify that a new node is created with location {string} and ip address {string}")
    public void verifyThatANewNodeIsCreatedWithLocationAndIpAddress(String location, String ipAddress) {
        var nodeServiceBlockingStub = backgroundHelper.getNodeServiceBlockingStub();
        var nodeId = nodeServiceBlockingStub.getNodeIdFromQuery(NodeIdQuery.newBuilder()
            .setIpAddress(ipAddress).setLocation(location).build());
        assertNotNull(nodeId);
        var node = nodeServiceBlockingStub.getNodeById(nodeId);
        assertNotNull(node);
    }

    @Then("verify adding existing device with label {string} and ip address {string} and location {string} will fail")
    public void verifyAddingExistingDeviceWithLabelAndIpAddressAndLocationWillFail(String label, String ipAddress, String location) {
        var nodeServiceBlockingStub = backgroundHelper.getNodeServiceBlockingStub();
        try {
            var nodeDto = nodeServiceBlockingStub.createNode(NodeCreateDTO.newBuilder().setLabel(label).setLocation(location)
                .setManagementIp(ipAddress).build());
            fail();
        } catch (Exception e) {
            // left intentionally empty
        }
    }
    @Given("Device detected indicator = {string}")
    public void deviceDetectedIndicator(String deviceDetectedInd) {
        this.deviceDetectedInd = Boolean.parseBoolean(deviceDetectedInd);
    }

    @Given("Device detected reason = {string}")
    public void deviceDetectedReason(String reason) {
        this.reason = reason;
    }

    @Then("lookup node with location {string} and ip address {string}")
    public void lookupNodeWithLocationAndIpAddress(String location, String ipAddress) {
        var nodeServiceBlockingStub = backgroundHelper.getNodeServiceBlockingStub();
        var nodeId = nodeServiceBlockingStub.getNodeIdFromQuery(NodeIdQuery.newBuilder()
            .setIpAddress(ipAddress).setLocation(location).build());
        nodeIdCreated = nodeId;
        assertNotNull(nodeId);

        node = nodeServiceBlockingStub.getNodeById(nodeId);
        minionGatewayWiremockTestSteps.setNodeId(node.getId());

        assertNotNull(node);
    }


    @Then("send Device Detection to Kafka topic {string} for an ip address {string} at location {string}")
    public void sendDeviceDetectionToKafkaTopicForAnIpAddressAtLocation(String kafkaTopic, String ipAddress, String location) {
        Properties producerConfig = new Properties();
        producerConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, backgroundHelper.getKafkaBootstrapUrl());
        producerConfig.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getCanonicalName());
        producerConfig.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getCanonicalName());

        try (KafkaProducer<String, byte[]> kafkaProducer = new KafkaProducer<>(producerConfig)) {

            NodeScanResult nodeScanResult = NodeScanResult.newBuilder()
                .setNodeId(nodeIdCreated.getValue())
                .addDetectorResult(ServiceResult.newBuilder()
                    .setService(ServiceType.ICMP)
                    .setIpAddress(ipAddress)
                    .setStatus(true).build())
                .addDetectorResult(ServiceResult.newBuilder()
                    .setService(ServiceType.SNMP)
                    .setIpAddress(ipAddress)
                    .setStatus(true).build())
                .build();

            TaskResult taskResult =
                TaskResult.newBuilder()
                    .setIdentity(
                        org.opennms.taskset.contract.Identity.newBuilder()
                            .setSystemId(systemId)
                            .build()
                    )
                    .setScannerResponse(ScannerResponse.newBuilder()
                        .setResult(Any.pack(nodeScanResult)).build())
                    .build();

            TenantLocationSpecificTaskSetResults taskSetResults =
                TenantLocationSpecificTaskSetResults.newBuilder()
                    .setTenantId(backgroundHelper.getTenantId())
                    .setLocation(backgroundHelper.getLocation())
                    .addResults(taskResult)
                    .build();

            var producerRecord = new ProducerRecord<String, byte[]>(kafkaTopic, taskSetResults.toByteArray());

            // producerRecord.headers().add(GrpcConstants.TENANT_ID_KEY, backgroundHelper.getTenantId().getBytes(StandardCharsets.UTF_8));
            // producerRecord.headers().add(GrpcConstants.LOCATION_KEY, backgroundHelper.getLocation().getBytes(StandardCharsets.UTF_8));

            kafkaProducer.send(producerRecord);
        }
    }


}
