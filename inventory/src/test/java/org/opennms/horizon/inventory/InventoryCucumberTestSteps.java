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

package org.opennms.horizon.inventory;

import com.google.protobuf.Empty;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.MetadataUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.horizon.grpc.heartbeat.contract.HeartbeatMessage;
import org.opennms.horizon.inventory.dto.MonitoringLocationServiceGrpc;
import org.opennms.horizon.inventory.dto.MonitoringSystemServiceGrpc;
import org.opennms.horizon.inventory.dto.NodeCreateDTO;
import org.opennms.horizon.inventory.dto.NodeIdQuery;
import org.opennms.horizon.inventory.dto.NodeServiceGrpc;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


public class InventoryCucumberTestSteps {

    private static final Logger LOG = LoggerFactory.getLogger(InventoryCucumberTestSteps.class);
    private Integer externalGrpcPort;
    private String kafkaBootstrapUrl;
    private String tenantId;
    private String location;
    private String systemId;
    private MonitoringSystemServiceGrpc.MonitoringSystemServiceBlockingStub monitoringSystemStub;
    private MonitoringLocationServiceGrpc.MonitoringLocationServiceBlockingStub monitoringLocationStub;
    private NodeServiceGrpc.NodeServiceBlockingStub nodeServiceBlockingStub;
    private final Map<String, String> grpcHeaders = new TreeMap<>();


    @Given("External GRPC Port in system property {string}")
    public void externalGRPCPortInSystemProperty(String propertyName) {
        String value = System.getProperty(propertyName);
        externalGrpcPort = Integer.parseInt(value);
        LOG.info("Using External gRPC port {}", externalGrpcPort);
    }

    @Given("Kafka Bootstrap URL in system property {string}")
    public void kafkaBootstrapURLInSystemProperty(String systemPropertyName) {
        kafkaBootstrapUrl = System.getProperty(systemPropertyName);
        LOG.info("Using Kafka Bootstrap URL {}", kafkaBootstrapUrl);
    }


    @Given("Grpc TenantId {string}")
    public void grpcTenantId(String tenantId) {
        Objects.requireNonNull(tenantId);
        this.tenantId = tenantId;
        grpcHeaders.put(GrpcConstants.TENANT_ID_KEY, tenantId);
        LOG.info("Using Tenant Id {}", tenantId);
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
        NettyChannelBuilder channelBuilder =
            NettyChannelBuilder.forAddress("localhost", externalGrpcPort);

        ManagedChannel managedChannel = channelBuilder.usePlaintext().build();
        managedChannel.getState(true);
        monitoringSystemStub = MonitoringSystemServiceGrpc.newBlockingStub(managedChannel)
            .withInterceptors(prepareGrpcHeaderInterceptor()).withDeadlineAfter(30, TimeUnit.SECONDS);
        monitoringLocationStub = MonitoringLocationServiceGrpc.newBlockingStub(managedChannel)
            .withInterceptors(prepareGrpcHeaderInterceptor()).withDeadlineAfter(30, TimeUnit.SECONDS);
        nodeServiceBlockingStub = NodeServiceGrpc.newBlockingStub(managedChannel)
            .withInterceptors(prepareGrpcHeaderInterceptor()).withDeadlineAfter(30, TimeUnit.SECONDS);

    }


    @Given("send heartbeat message to Kafka topic {string}")
    public void sendHeartbeatMessageToKafkaTopic(String topic) {
        Properties producerConfig = new Properties();
        producerConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapUrl);
        producerConfig.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getCanonicalName());
        producerConfig.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getCanonicalName());
        KafkaProducer<String, byte[]> kafkaProducer = new KafkaProducer<String, byte[]>(producerConfig);
        long millis = System.currentTimeMillis();
        HeartbeatMessage heartbeatMessage = HeartbeatMessage.newBuilder()
            .setIdentity(Identity.newBuilder().setLocation(location).setSystemId(systemId).build())
            .setTimestamp(Timestamp.newBuilder().setSeconds(millis / 1000).setNanos((int) ((millis % 1000) * 1000000)).build())
            .build();
        var producerRecord = new ProducerRecord<String, byte[]>(topic, heartbeatMessage.toByteArray());
        grpcHeaders.forEach((key, value) -> producerRecord.headers().add(key, value.getBytes(StandardCharsets.UTF_8)));
        kafkaProducer.send(producerRecord);
    }


    @Then("verify Monitoring system is created with system id {string}")
    public void verifyMonitoringSystemIsCreatedWithSystemId(String systemId) {
        await().pollInterval(5, TimeUnit.SECONDS).atMost(30, TimeUnit.SECONDS).until(() -> monitoringSystemStub.listMonitoringSystem(Empty.newBuilder().build()).getSystemsList().size(),
            Matchers.equalTo(1));
        var systems = monitoringSystemStub.listMonitoringSystem(Empty.newBuilder().build()).getSystemsList();
        assertEquals(systemId, systems.get(0).getSystemId());
        assertEquals(tenantId, systems.get(0).getTenantId());
    }

    @Then("verify Monitoring location is created with location {string}")
    public void verifyMonitoringLocationIsCreatedWithLocation(String location) {
        await().pollInterval(5, TimeUnit.SECONDS)
            .atMost(30, TimeUnit.SECONDS).until(() ->
                    monitoringLocationStub.getLocationByName(StringValue.newBuilder().setValue(location).build()).getLocation(),
            Matchers.notNullValue());
        var locationDTO = monitoringLocationStub.getLocationByName(StringValue.newBuilder().setValue(location).build());
        assertEquals(location, locationDTO.getLocation());
        assertEquals(tenantId, locationDTO.getTenantId());
    }

    private ClientInterceptor prepareGrpcHeaderInterceptor() {
        return MetadataUtils.newAttachHeadersInterceptor(prepareGrpcHeaders());
    }

    private Metadata prepareGrpcHeaders() {
        Metadata result = new Metadata();
        result.put(GrpcConstants.AUTHORIZATION_BYPASS_KEY, String.valueOf(true));
        result.put(GrpcConstants.TENANT_ID_BYPASS_KEY, tenantId);
        return result;
    }

    @Given("add a new device with label {string} and ip address {string}")
    public void addANewDeviceWithLabelAndIpAddress(String label, String ipAddress) {
        var nodeDto = nodeServiceBlockingStub.createNode(NodeCreateDTO.newBuilder().setLabel(label).setManagementIp(ipAddress).build());
        assertNotNull(nodeDto);
    }


    @Then("verify that a new node is created with label {string} and ip address {string}")
    public void verifyThatANewNodeIsCreatedWithLabelAndIpAddress(String label, String ipAddress) {
        var nodeList = nodeServiceBlockingStub.listNodes(Empty.newBuilder().build());
        Assertions.assertFalse(nodeList.getNodesList().isEmpty());
        var nodeOptional = nodeList.getNodesList().stream().filter(
                nodeDTO ->
                    nodeDTO.getNodeLabel().equals(label) &&
                        nodeDTO.getIpInterfacesList().stream().anyMatch(ipInterfaceDTO -> ipInterfaceDTO.getIpAddress().equals(ipAddress)))
            .findFirst();
        assertTrue(nodeOptional.isPresent());
        var node = nodeOptional.get();
        assertEquals(label, node.getNodeLabel());
    }

    @Given("add a new device with label {string} and ip address {string} and location {string}")
    public void addANewDeviceWithLabelAndIpAddressAndLocation(String label, String ipAddress, String location) {
        var nodeDto = nodeServiceBlockingStub.createNode(NodeCreateDTO.newBuilder().setLabel(label).setLocation(location)
            .setManagementIp(ipAddress).build());
        assertNotNull(nodeDto);
    }

    @Then("verify that a new node is created with location {string} and ip address {string}")
    public void verifyThatANewNodeIsCreatedWithLocationAndIpAddress(String location, String ipAddress) {

        var nodeId = nodeServiceBlockingStub.getNodeIdFromQuery(NodeIdQuery.newBuilder()
            .setIpAddress(ipAddress).setLocation(location).build());
        assertNotNull(nodeId);
        var node = nodeServiceBlockingStub.getNodeById(nodeId);
        assertNotNull(node);
    }

    @Then("verify adding existing device with label {string} and ip address {string} and location {string} will fail")
    public void verifyAddingExistingDeviceWithLabelAndIpAddressAndLocationWillFail(String label, String ipAddress, String location) {
        try {
            var nodeDto = nodeServiceBlockingStub.createNode(NodeCreateDTO.newBuilder().setLabel(label).setLocation(location)
                .setManagementIp(ipAddress).build());
            fail();
        } catch (Exception e) {

        }
    }
}
