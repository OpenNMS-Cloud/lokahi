/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.opennms.horizon.inventory.cucumber.steps;

import static com.jayway.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.protobuf.Any;
import com.google.protobuf.BoolValue;
import com.google.protobuf.Empty;
import com.google.protobuf.Int64Value;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.horizon.grpc.heartbeat.contract.TenantLocationSpecificHeartbeatMessage;
import org.opennms.horizon.inventory.cucumber.InventoryBackgroundHelper;
import org.opennms.horizon.inventory.cucumber.kafkahelper.KafkaConsumerRunner;
import org.opennms.horizon.inventory.discovery.IcmpActiveDiscoveryCreateDTO;
import org.opennms.horizon.inventory.discovery.SNMPConfigDTO;
import org.opennms.horizon.inventory.dto.ListTagsByEntityIdParamsDTO;
import org.opennms.horizon.inventory.dto.MonitoringSystemQuery;
import org.opennms.horizon.inventory.dto.NodeCreateDTO;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.inventory.dto.NodeIdQuery;
import org.opennms.horizon.inventory.dto.NodeList;
import org.opennms.horizon.inventory.dto.SearchBy;
import org.opennms.horizon.inventory.dto.SearchIpInterfaceQuery;
import org.opennms.horizon.inventory.dto.TagCreateDTO;
import org.opennms.horizon.inventory.dto.TagEntityIdDTO;
import org.opennms.horizon.inventory.dto.TagListParamsDTO;
import org.opennms.horizon.shared.common.tag.proto.Operation;
import org.opennms.horizon.shared.common.tag.proto.TagOperationList;
import org.opennms.horizon.shared.common.tag.proto.TagOperationProto;
import org.opennms.inventory.types.ServiceType;
import org.opennms.node.scan.contract.IpInterfaceResult;
import org.opennms.node.scan.contract.NodeScanResult;
import org.opennms.node.scan.contract.ServiceResult;
import org.opennms.node.scan.contract.SnmpInterfaceResult;
import org.opennms.taskset.contract.DiscoveryScanResult;
import org.opennms.taskset.contract.MonitorType;
import org.opennms.taskset.contract.PingResponse;
import org.opennms.taskset.contract.ScannerResponse;
import org.opennms.taskset.contract.TaskResult;
import org.opennms.taskset.contract.TenantLocationSpecificTaskSetResults;
import org.opennms.taskset.service.contract.UpdateSingleTaskOp;
import org.opennms.taskset.service.contract.UpdateTasksRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InventoryProcessingStepDefinitions {
    private static final Logger LOG = LoggerFactory.getLogger(InventoryProcessingStepDefinitions.class);

    private InventoryBackgroundHelper backgroundHelper;

    private String label;
    private String newDeviceIpAddress;
    private String locationId;
    private String systemId;
    private boolean deviceDetectedInd;
    private String reason;

    private NodeDTO node;
    private NodeList nodeList;
    private Int64Value nodeIdCreated;
    private String taskIpAddress;
    private MonitorType monitorType;
    private KafkaConsumerRunner kafkaConsumerRunner;
    private final String tagTopic = "tag-operation";
    private NodeScanResult nodeScanResult;
    private NodeScanResult nodeScanForIpInterfaces;
    private IcmpActiveDiscoveryCreateDTO icmpDiscovery;
    private long activeDiscoveryId;
    private DiscoveryScanResult discoveryScanResult;

    @Then("Add a device with IP address = {string} with label {string}")
    public void addADeviceWithIPAddressWithLabel(String ipAddress, String label) {

        var nodeServiceBlockingStub = backgroundHelper.getNodeServiceBlockingStub();
        node = nodeServiceBlockingStub.createNode(NodeCreateDTO.newBuilder()
                .setLabel(label)
                .setLocationId(locationId)
                .setManagementIp(ipAddress)
                .build());
        assertNotNull(node);
    }

    public enum PublishType {
        UPDATE,
        REMOVE
    }

    // ========================================
    // Constructor
    // ----------------------------------------

    public InventoryProcessingStepDefinitions(InventoryBackgroundHelper inventoryBackgroundHelper) {
        this.backgroundHelper = inventoryBackgroundHelper;
    }

    // ========================================
    // Step Definitions
    // ----------------------------------------

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
    }

    @Given("Grpc location named {string}")
    public void grpcLocation(String location) {}

    @Given("Minion at location named {string} with system ID {string}")
    public void minionAtLocationWithSystemId(String location, String systemId) {
        Objects.requireNonNull(location);
        Objects.requireNonNull(systemId);
        this.locationId = backgroundHelper.findLocationId(location);
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
                            .setLocationId(locationId)
                            .setIdentity(
                                    Identity.newBuilder().setSystemId(systemId).build())
                            .setTimestamp(Timestamp.newBuilder()
                                    .setSeconds(millis / 1000)
                                    .setNanos((int) ((millis % 1000) * 1000000))
                                    .build())
                            .build();
            var producerRecord = new ProducerRecord<String, byte[]>(topic, heartbeatMessage.toByteArray());

            kafkaProducer.send(producerRecord);
        }
    }

    @Then("verify Monitoring system is created with system id {string} with location named {string}")
    public void verifyMonitoringSystemIsCreatedWithSystemIdWithLocationNamed(String systemId, String location) {
        var monitoringLocationStub = backgroundHelper.getMonitoringLocationStub();
        var monitoringLocation = monitoringLocationStub.getLocationByName(StringValue.of(location));
        assertEquals(location, monitoringLocation.getLocation());
        var monitoringSystemStub = backgroundHelper.getMonitoringSystemStub();
        await().pollInterval(1, TimeUnit.SECONDS)
                .atMost(15, TimeUnit.SECONDS)
                .until(
                        () -> {
                            try {
                                return monitoringSystemStub
                                        .getMonitoringSystemByQuery(MonitoringSystemQuery.newBuilder()
                                                .setLocation(location)
                                                .setSystemId(systemId)
                                                .build())
                                        .getSystemId();
                            } catch (Exception e) {
                                return "";
                            }
                        },
                        Matchers.equalTo(systemId));

        var systems = monitoringSystemStub
                .listMonitoringSystem(Empty.newBuilder().build())
                .getSystemsList();
        var monitoringSystems = systems.stream()
                .filter(msId -> msId.getSystemId().equals(systemId)
                        && msId.getMonitoringLocationId() == monitoringLocation.getId())
                .toList();
        assertFalse(monitoringSystems.isEmpty());
        var monitoringSystem = monitoringSystems.get(0);
        assertEquals(backgroundHelper.getTenantId(), monitoringSystem.getTenantId());
        assertEquals(location, monitoringLocation.getLocation());
    }

    @Then("verify Monitoring system is removed with system id {string}")
    public void verifyMonitoringSystemIsRemovedWithSystemId(String systemId) {
        await().atMost(30, TimeUnit.SECONDS)
                .pollDelay(10L, TimeUnit.MILLISECONDS)
                .until(() -> {
                    var monitoringSystemStub = backgroundHelper.getMonitoringSystemStub();
                    var systems =
                            monitoringSystemStub
                                    .listMonitoringSystem(Empty.newBuilder().build())
                                    .getSystemsList()
                                    .stream()
                                    .filter(s -> systemId.equals(s.getSystemId()))
                                    .toList();
                    assertEquals(0, systems.size());
                });
    }

    @Then("verify Monitoring location is created with location {string}")
    public void verifyMonitoringLocationIsCreatedWithLocation(String location) {
        var monitoringLocationStub = backgroundHelper.getMonitoringLocationStub();
        await().pollInterval(5, TimeUnit.SECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .until(
                        () -> monitoringLocationStub
                                .getLocationByName(StringValue.newBuilder()
                                        .setValue(location)
                                        .build())
                                .getLocation(),
                        Matchers.notNullValue());
        var locationDTO = monitoringLocationStub.getLocationByName(
                StringValue.newBuilder().setValue(location).build());
        assertEquals(location, locationDTO.getLocation());
        assertEquals(backgroundHelper.getTenantId(), locationDTO.getTenantId());
    }

    @Then("verify the device has an interface with the given IP address")
    public void verifyTheDeviceHasAnInterfaceWithTheGivenIPAddress() {
        NodeDTO nodeDTO = backgroundHelper
                .getNodeServiceBlockingStub()
                .withInterceptors()
                .getNodeById(Int64Value.of(node.getId()));

        assertNotNull(nodeDTO);
        assertTrue(nodeDTO.getIpInterfacesList().stream()
                .anyMatch((ele) -> ele.getIpAddress().equals(newDeviceIpAddress)));
    }

    @Given("Label {string}")
    public void label(String label) {
        this.label = label;
    }

    @Given("Device IP Address {string} in location named {string}")
    public void newDeviceIPAddress(String ipAddress, String location) {
        this.newDeviceIpAddress = ipAddress;
        this.locationId = backgroundHelper.findLocationId(location);
    }

    @Then("add a new device")
    public void addANewDevice() {
        var nodeServiceBlockingStub = backgroundHelper.getNodeServiceBlockingStub();
        node = nodeServiceBlockingStub.createNode(NodeCreateDTO.newBuilder()
                .setLabel(label)
                .setLocationId(locationId)
                .setManagementIp(newDeviceIpAddress)
                .build());
        assertNotNull(node);
    }

    @Then("remove the device")
    public void removeTheDevice() {
        var nodeServiceBlockingStub = backgroundHelper.getNodeServiceBlockingStub();

        var nodeId = nodeServiceBlockingStub.getNodeIdFromQuery(NodeIdQuery.newBuilder()
                .setIpAddress(newDeviceIpAddress)
                .setLocationId(locationId)
                .build());

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
        assertFalse(nodeList.getNodesList().isEmpty());

        var nodeOptional = nodeList.getNodesList().stream()
                .filter(nodeDTO -> ((nodeDTO.getNodeLabel().equals(label))
                        && (nodeDTO.getIpInterfacesList().stream()
                                .anyMatch(ipInterfaceDTO ->
                                        ipInterfaceDTO.getIpAddress().equals(newDeviceIpAddress)))))
                .findFirst();

        assertTrue(nodeOptional.isPresent());

        var node = nodeOptional.get();
        assertEquals(label, node.getNodeLabel());
    }

    @Given("add a new device with label {string} and ip address {string} and location named {string}")
    public void addANewDeviceWithLabelAndIpAddressAndLocation(String label, String ipAddress, String location) {
        var nodeServiceBlockingStub = backgroundHelper.getNodeServiceBlockingStub();
        var nodeDto = nodeServiceBlockingStub.createNode(NodeCreateDTO.newBuilder()
                .setLabel(label)
                .setLocationId(backgroundHelper.findLocationId(location))
                .setManagementIp(ipAddress)
                .build());
        assertNotNull(nodeDto);
    }

    @Then("verify that a new node is created with location named {string} and ip address {string}")
    public void verifyThatANewNodeIsCreatedWithLocationAndIpAddress(String location, String ipAddress) {
        var nodeServiceBlockingStub = backgroundHelper.getNodeServiceBlockingStub();
        var nodeId = nodeServiceBlockingStub.getNodeIdFromQuery(NodeIdQuery.newBuilder()
                .setIpAddress(ipAddress)
                .setLocationId(backgroundHelper.findLocationId(location))
                .build());
        assertNotNull(nodeId);
        var node = nodeServiceBlockingStub.getNodeById(nodeId);
        assertNotNull(node);
    }

    @Then("verify adding existing device with label {string} and ip address {string} and location {string} will fail")
    public void verifyAddingExistingDeviceWithLabelAndIpAddressAndLocationWillFail(
            String label, String ipAddress, String location) {
        var nodeServiceBlockingStub = backgroundHelper.getNodeServiceBlockingStub();
        try {
            var nodeDto = nodeServiceBlockingStub.createNode(NodeCreateDTO.newBuilder()
                    .setLabel(label)
                    .setLocationId(location)
                    .setManagementIp(ipAddress)
                    .build());
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
                .setIpAddress(ipAddress)
                .setLocationId(backgroundHelper.findLocationId(location))
                .build());
        nodeIdCreated = nodeId;
        assertNotNull(nodeId);

        node = nodeServiceBlockingStub.getNodeById(nodeId);

        assertNotNull(node);
    }

    @Then("send Device Detection to Kafka topic {string} for an ip address {string} at location {string}")
    public void sendDeviceDetectionToKafkaTopicForAnIpAddressAtLocation(
            String kafkaTopic, String ipAddress, String location) {
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
                            .setStatus(true)
                            .build())
                    .addDetectorResult(ServiceResult.newBuilder()
                            .setService(ServiceType.SNMP)
                            .setIpAddress(ipAddress)
                            .setStatus(true)
                            .build())
                    .build();

            TaskResult taskResult = TaskResult.newBuilder()
                    .setIdentity(org.opennms.taskset.contract.Identity.newBuilder()
                            .setSystemId(systemId)
                            .build())
                    .setScannerResponse(ScannerResponse.newBuilder()
                            .setResult(Any.pack(nodeScanResult))
                            .build())
                    .build();

            TenantLocationSpecificTaskSetResults taskSetResults = TenantLocationSpecificTaskSetResults.newBuilder()
                    .setTenantId(backgroundHelper.getTenantId())
                    .setLocationId(backgroundHelper.findLocationId(location))
                    .addResults(taskResult)
                    .build();

            var producerRecord = new ProducerRecord<String, byte[]>(kafkaTopic, taskSetResults.toByteArray());

            // producerRecord.headers().add(GrpcConstants.TENANT_ID_KEY,
            // backgroundHelper.getTenantId().getBytes(StandardCharsets.UTF_8));
            // producerRecord.headers().add(GrpcConstants.LOCATION_KEY,
            // backgroundHelper.getLocation().getBytes(StandardCharsets.UTF_8));

            kafkaProducer.send(producerRecord);
        }
    }

    @Given("Subscribe to kafka topic {string}")
    public void subscribeToKafkaTopic(String topic) {
        kafkaConsumerRunner = new KafkaConsumerRunner(backgroundHelper.getKafkaBootstrapUrl(), topic);
        Executors.newSingleThreadExecutor().execute(kafkaConsumerRunner);
    }

    @Then("verify the task set update is published for device with nodeScan within {int}ms")
    public void verifyTheTaskSetUpdateIsPublishedForDeviceWithNodeScanWithinMs(int timeout) {
        long nodeId = node.getId();
        String taskIdPattern = "nodeScan=node_id/" + nodeId;
        await().atMost(timeout, TimeUnit.MILLISECONDS)
                .until(() -> matchesTaskPatternForUpdate(taskIdPattern).get(), Matchers.is(true));
    }

    @Given("Device Task IP address = {string}")
    public void deviceTaskIPAddress(String ipAddress) {
        this.taskIpAddress = ipAddress;
    }

    @Given("Monitor Type {string}")
    public void monitorType(String monitorType) {
        switch (monitorType) {
            case "ICMP":
                this.monitorType = MonitorType.ICMP;
                break;

            case "SNMP":
                this.monitorType = MonitorType.SNMP;
                break;

            default:
                throw new RuntimeException("Unrecognized monitor type " + monitorType);
        }
    }

    @Then("verify the task set update is published for device with task suffix {string} within {int}ms")
    public void verifyTheTaskSetUpdateIsPublishedForDeviceWithTaskSuffixWithinMs(String taskNameSuffix, int timeout) {
        String taskIdPattern = "nodeId:\\d+/ip=" + taskIpAddress + "/" + taskNameSuffix;
        await().atMost(timeout, TimeUnit.MILLISECONDS)
                .pollDelay(2000, TimeUnit.MILLISECONDS)
                .pollInterval(2000, TimeUnit.MILLISECONDS)
                .until(() -> matchesTaskPatternForUpdate(taskIdPattern).get(), Matchers.is(true));
    }

    @Then("verify the task set update is published with removal of task with suffix {string} within {int}ms")
    public void verifyTheTaskSetUpdateIsPublishedWithRemovalOfTaskWithSuffixWithinMs(String taskSuffix, int timeout) {
        String taskIdPattern = "nodeId:\\d+/ip=" + taskIpAddress + "/" + taskSuffix;
        await().atMost(timeout, TimeUnit.MILLISECONDS)
                .pollDelay(2000, TimeUnit.MILLISECONDS)
                .pollInterval(2000, TimeUnit.MILLISECONDS)
                .until(() -> matchesTaskPatternForDelete(taskIdPattern).get(), Matchers.is(true));
    }

    private AtomicBoolean matchesTaskPattern(String taskIdPattern, PublishType publishType) {
        AtomicBoolean matched = new AtomicBoolean(false);
        var list = kafkaConsumerRunner.getValues();
        var tasks = new ArrayList<UpdateTasksRequest>();
        for (byte[] taskSet : list) {
            try {
                var taskDefPub = UpdateTasksRequest.parseFrom(taskSet);
                tasks.add(taskDefPub);
            } catch (InvalidProtocolBufferException ignored) {

            }
        }
        LOG.info("taskIdPattern = {}, publish type = {}, Tasks :  {}", taskIdPattern, publishType, tasks);
        for (UpdateTasksRequest task : tasks) {
            var addTasks = task.getUpdateList().stream()
                    .filter(UpdateSingleTaskOp::hasAddTask)
                    .collect(Collectors.toList());
            var removeTasks = task.getUpdateList().stream()
                    .filter(UpdateSingleTaskOp::hasRemoveTask)
                    .collect(Collectors.toList());
            if (publishType.equals(PublishType.UPDATE)) {
                boolean matchForTaskId = addTasks.stream().anyMatch(updateSingleTaskOp -> updateSingleTaskOp
                        .getAddTask()
                        .getTaskDefinition()
                        .getId()
                        .matches(taskIdPattern));
                if (matchForTaskId) {
                    matched.set(true);
                }
            }
            if (publishType.equals(PublishType.REMOVE)) {
                boolean matchForTaskId = removeTasks.stream()
                        .anyMatch(updateSingleTaskOp ->
                                updateSingleTaskOp.getRemoveTask().getTaskId().matches(taskIdPattern));
                if (matchForTaskId) {
                    matched.set(true);
                }
            }
        }
        return matched;
    }

    AtomicBoolean matchesTaskPatternForUpdate(String taskIdPattern) {
        return matchesTaskPattern(taskIdPattern, PublishType.UPDATE);
    }

    AtomicBoolean matchesTaskPatternForDelete(String taskIdPattern) {
        return matchesTaskPattern(taskIdPattern, PublishType.REMOVE);
    }

    @Then("shutdown kafka consumer")
    public void shutdownKafkaConsumer() {
        kafkaConsumerRunner.shutdown();
        await().atMost(3, TimeUnit.SECONDS)
                .until(() -> kafkaConsumerRunner.isShutdown().get(), Matchers.is(true));
    }

    @Given("A new monitoring policy with tags {string}")
    public void aNewMonitoringPolicyWithTags(final String tags) {
        final var tagService = this.backgroundHelper.getTagServiceBlockingStub();
        final var tagListBuilder = TagOperationList.newBuilder();
        for (final var tag : tags.split(",")) {
            var tagBuilder = TagOperationProto.newBuilder()
                    .setTenantId(backgroundHelper.getTenantId())
                    .setTagName(tag)
                    .setOperation(Operation.ASSIGN_TAG)
                    .addMonitoringPolicyId(9999);
            tagListBuilder.addTags(tagBuilder);
        }

        Properties producerConfig = new Properties();
        producerConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, backgroundHelper.getKafkaBootstrapUrl());
        producerConfig.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getCanonicalName());
        producerConfig.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getCanonicalName());

        try (KafkaProducer<String, byte[]> kafkaProducer = new KafkaProducer<>(producerConfig)) {
            var producerRecord = new ProducerRecord<String, byte[]>(
                    tagTopic, tagListBuilder.build().toByteArray());
            kafkaProducer.send(producerRecord);
        }
        var expectedTags = tags.split(",");
        await().atMost(10, TimeUnit.SECONDS).until(() -> {
            return tagService
                            .getTagsByEntityId(ListTagsByEntityIdParamsDTO.newBuilder()
                                    .setEntityId(TagEntityIdDTO.newBuilder()
                                            .setMonitoringPolicyId(9999)
                                            .build())
                                    .setParams(TagListParamsDTO.newBuilder()
                                            .setSearchTerm(expectedTags[0])
                                            .build())
                                    .build())
                            .getTagsList()
                            .size()
                    > 0;
        });
    }

    @Then("Add a device with IP address = {string} with label {string} with tags {string}")
    public void addADeviceWithIPAddressWithLabel(String ipAddress, String label, String tags) {

        var nodeServiceBlockingStub = backgroundHelper.getNodeServiceBlockingStub();
        var tagsList = tags.split(",");
        node = nodeServiceBlockingStub.createNode(NodeCreateDTO.newBuilder()
                .setLabel(label)
                .setLocationId(locationId)
                .setManagementIp(ipAddress)
                .addAllTags(Stream.of(tagsList)
                        .map(tag -> TagCreateDTO.newBuilder().setName(tag).build())
                        .toList())
                .build());
        assertNotNull(node);
    }

    @Then("verify the device has an interface with the IpAddress {string}")
    public void verifyTheDeviceHasAnInterfaceWithTheIpAddress(String ipAddress) {
        NodeDTO nodeDTO = backgroundHelper
                .getNodeServiceBlockingStub()
                .withInterceptors()
                .getNodeById(Int64Value.of(node.getId()));

        assertNotNull(nodeDTO);
        assertTrue(nodeDTO.getIpInterfacesList().stream()
                .anyMatch((ele) -> ele.getIpAddress().equals(ipAddress)));
    }

    @Given("Node Scan results with IpInterfaces {string} and SnmpInterfaces with ifName {string}")
    public void nodeScanResultsWithIpInterfacesAndSnmpInterfacesWithIfName(String ipAddress, String ifName) {

        nodeScanResult = NodeScanResult.newBuilder()
                .setNodeId(node.getId())
                .addIpInterfaces(
                        IpInterfaceResult.newBuilder().setIpAddress(ipAddress).build())
                .addSnmpInterfaces(SnmpInterfaceResult.newBuilder()
                        .setIfName(ifName)
                        .setIfAlias(ifName)
                        .setIfIndex(1)
                        .setIfType(1)
                        .setIfDescr(ifName)
                        .setIfSpeed(1000000)
                        .build())
                .build();
    }

    @Then("Send node scan results to kafka topic {string}")
    public void sendNodeScanResultsToKafkaTopic(String topic) {
        TaskResult taskResult = TaskResult.newBuilder()
                .setIdentity(org.opennms.taskset.contract.Identity.newBuilder()
                        .setSystemId(systemId)
                        .build())
                .setScannerResponse(ScannerResponse.newBuilder()
                        .setResult(Any.pack(nodeScanResult))
                        .build())
                .build();

        TenantLocationSpecificTaskSetResults taskSetResults = TenantLocationSpecificTaskSetResults.newBuilder()
                .setTenantId(backgroundHelper.getTenantId())
                .setLocationId(backgroundHelper.findLocationId(node.getLocation()))
                .addResults(taskResult)
                .build();

        var producerRecord = new ProducerRecord<String, byte[]>(topic, taskSetResults.toByteArray());

        Properties producerConfig = new Properties();
        producerConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, backgroundHelper.getKafkaBootstrapUrl());
        producerConfig.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getCanonicalName());
        producerConfig.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getCanonicalName());
        try (KafkaProducer<String, byte[]> kafkaProducer = new KafkaProducer<>(producerConfig)) {
            kafkaProducer.send(producerRecord);
        }
    }

    @Then("verify node has IpInterface {string} and SnmpInterface with ifName {string}")
    public void verifyNodeHasIpInterfaceAndSnmpInterfaceWithIfName(String ipAddress, String ifName) {

        var nodeServiceBlockingStub = backgroundHelper.getNodeServiceBlockingStub();
        await().atMost(10, TimeUnit.SECONDS)
                .pollDelay(1, TimeUnit.SECONDS)
                .pollInterval(2, TimeUnit.SECONDS)
                .until(
                        () ->
                                nodeServiceBlockingStub
                                        .getNodeById(Int64Value.of(node.getId()))
                                        .getIpInterfacesList()
                                        .stream()
                                        .anyMatch(ipInterfaceDTO ->
                                                ipInterfaceDTO.getIpAddress().equals(ipAddress)),
                        Matchers.is(true));

        assertTrue(nodeServiceBlockingStub.getNodeById(Int64Value.of(node.getId())).getSnmpInterfacesList().stream()
                .anyMatch(snmpInterfaceDTO -> snmpInterfaceDTO.getIfName().equals(ifName)));
    }

    @Then("verify node has IpInterface with ipAddress {string}")
    public void verifyNodeHasIpInterfaceWithIpAddress(String ipAddress) {

        var nodeServiceBlockingStub = backgroundHelper.getNodeServiceBlockingStub();
        await().atMost(10, TimeUnit.SECONDS)
                .pollDelay(1, TimeUnit.SECONDS)
                .pollInterval(2, TimeUnit.SECONDS)
                .until(() -> nodeServiceBlockingStub
                                .searchIpInterfaces(SearchIpInterfaceQuery.newBuilder()
                                        .setNodeId(node.getId())
                                        .setSearchTerm(ipAddress)
                                        .build())
                                .getIpInterfaceList()
                                .size()
                        > 0);
    }

    @Given("Node Scan results with IpInterfaces {string} and hostName {string}")
    public void nodeScanResultsWithIpInterfacesAndHostName(String ipAddress, String hostName) {

        nodeScanForIpInterfaces = NodeScanResult.newBuilder()
                .setNodeId(node.getId())
                .addIpInterfaces(IpInterfaceResult.newBuilder()
                        .setIpAddress(ipAddress)
                        .setIpHostName(hostName)
                        .build())
                .build();
    }

    @Then("verify node has IpInterface with hostName {string}")
    public void verifyNodeHasIpInterfaceWithHostName(String hostName) {
        var nodeServiceBlockingStub = backgroundHelper.getNodeServiceBlockingStub();
        await().atMost(10, TimeUnit.SECONDS)
                .pollDelay(1, TimeUnit.SECONDS)
                .pollInterval(2, TimeUnit.SECONDS)
                .until(() -> nodeServiceBlockingStub
                                .searchIpInterfaces(SearchIpInterfaceQuery.newBuilder()
                                        .setNodeId(node.getId())
                                        .setSearchTerm(hostName)
                                        .build())
                                .getIpInterfaceList()
                                .size()
                        > 0);
    }

    @Then("Send node scan results to kafka topic for hostName and ipAddress {string}")
    public void sendNodeScanResultsToKafkaTopicForHost(String topic) {
        TaskResult taskResult = TaskResult.newBuilder()
                .setIdentity(org.opennms.taskset.contract.Identity.newBuilder()
                        .setSystemId(systemId)
                        .build())
                .setScannerResponse(ScannerResponse.newBuilder()
                        .setResult(Any.pack(nodeScanForIpInterfaces))
                        .build())
                .build();

        TenantLocationSpecificTaskSetResults taskSetResults = TenantLocationSpecificTaskSetResults.newBuilder()
                .setTenantId(backgroundHelper.getTenantId())
                .setLocationId(backgroundHelper.findLocationId(node.getLocation()))
                .addResults(taskResult)
                .build();

        var producerRecord = new ProducerRecord<String, byte[]>(topic, taskSetResults.toByteArray());

        Properties producerConfig = new Properties();
        producerConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, backgroundHelper.getKafkaBootstrapUrl());
        producerConfig.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getCanonicalName());
        producerConfig.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getCanonicalName());
        try (KafkaProducer<String, byte[]> kafkaProducer = new KafkaProducer<>(producerConfig)) {
            kafkaProducer.send(producerRecord);
        }
    }

    @Then("verify node has SnmpInterface with ifName {string}")
    public void verifyNodeHasIpInterfaceAndSnmpInterfaceWithIfName(String ifName) {

        var nodeServiceBlockingStub = backgroundHelper.getNodeServiceBlockingStub();
        await().atMost(10, TimeUnit.SECONDS)
                .pollDelay(1, TimeUnit.SECONDS)
                .pollInterval(2, TimeUnit.SECONDS)
                .until(() -> nodeServiceBlockingStub
                                .listSnmpInterfaces(SearchBy.newBuilder()
                                        .setNodeId(node.getId())
                                        .setSearchTerm(ifName)
                                        .build())
                                .getSnmpInterfacesList()
                                .size()
                        > 0);
    }

    @Given(
            "New Active Discovery {string} with IpAddress {string} and SNMP community as {string} at location named {string}")
    public void newActiveDiscoveryWithIpAddressAndSNMPCommunityAsAtLocation(
            String name, String ipAddress, String snmpReadCommunity, String location) {
        icmpDiscovery = IcmpActiveDiscoveryCreateDTO.newBuilder()
                .setName(name)
                .addIpAddresses(ipAddress)
                .setSnmpConfig(SNMPConfigDTO.newBuilder()
                        .addReadCommunity(snmpReadCommunity)
                        .build())
                .setLocationId(backgroundHelper.findLocationId(location))
                .build();
    }

    @Then("create Active Discovery and validate it's created active discovery with given details.")
    public void createActiveDiscoveryAndValidateItSCreatedActiveDiscoveryWithGivenDetails() {
        var icmpDiscoveryDto =
                backgroundHelper.getIcmpActiveDiscoveryServiceBlockingStub().createDiscovery(icmpDiscovery);
        activeDiscoveryId = icmpDiscoveryDto.getId();
        Assertions.assertEquals(icmpDiscovery.getLocationId(), icmpDiscoveryDto.getLocationId());
        Assertions.assertEquals(icmpDiscovery.getIpAddresses(0), icmpDiscoveryDto.getIpAddresses(0));
        Assertions.assertEquals(
                icmpDiscovery.getSnmpConfig().getReadCommunity(0),
                icmpDiscoveryDto.getSnmpConfig().getReadCommunity(0));
    }

    @Given("Discovery Scan results with IpAddress {string}")
    public void discoveryScanResultsWithIpAddress(String ipAddress) {

        discoveryScanResult = DiscoveryScanResult.newBuilder()
                .addPingResponse(
                        PingResponse.newBuilder().setIpAddress(ipAddress).build())
                .setActiveDiscoveryId(activeDiscoveryId)
                .build();
    }

    @Then("Send discovery scan results to kafka topic {string} with location {string}")
    public void sendDiscoveryScanResultsToKafkaTopic(String topic, String location) {
        TaskResult taskResult = TaskResult.newBuilder()
                .setIdentity(org.opennms.taskset.contract.Identity.newBuilder()
                        .setSystemId(systemId)
                        .build())
                .setScannerResponse(ScannerResponse.newBuilder()
                        .setResult(Any.pack(discoveryScanResult))
                        .build())
                .build();

        TenantLocationSpecificTaskSetResults taskSetResults = TenantLocationSpecificTaskSetResults.newBuilder()
                .setTenantId(backgroundHelper.getTenantId())
                .setLocationId(backgroundHelper.findLocationId(location))
                .addResults(taskResult)
                .build();

        var producerRecord = new ProducerRecord<String, byte[]>(topic, taskSetResults.toByteArray());

        Properties producerConfig = new Properties();
        producerConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, backgroundHelper.getKafkaBootstrapUrl());
        producerConfig.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getCanonicalName());
        producerConfig.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getCanonicalName());
        try (KafkaProducer<String, byte[]> kafkaProducer = new KafkaProducer<>(producerConfig)) {
            kafkaProducer.send(producerRecord);
        }
    }

    @Then("verify that node is created for {string} and location named {string} with discoveryId")
    public void verifyThatNodeIsCreatedForAndLocationWithTheTagsInPreviousScenario(String ipAddress, String location) {
        String locationId = backgroundHelper.findLocationId(location);
        await().atMost(30, TimeUnit.SECONDS)
                .pollDelay(1, TimeUnit.SECONDS)
                .pollInterval(2, TimeUnit.SECONDS)
                .until(() -> {
                    try {
                        var nodeId = backgroundHelper
                                .getNodeServiceBlockingStub()
                                .getNodeIdFromQuery(NodeIdQuery.newBuilder()
                                        .setLocationId(locationId)
                                        .setIpAddress(ipAddress)
                                        .build());
                        return nodeId != null && nodeId.getValue() != 0;
                    } catch (Exception e) {
                        return false;
                    }
                });
        var nodeId = backgroundHelper
                .getNodeServiceBlockingStub()
                .getNodeIdFromQuery(NodeIdQuery.newBuilder()
                        .setLocationId(locationId)
                        .setIpAddress(ipAddress)
                        .build());
        var nodeDto = backgroundHelper.getNodeServiceBlockingStub().getNodeById(nodeId);
        // validate that node has discovery id
        Assertions.assertTrue(nodeDto.getDiscoveryIdsList().contains(activeDiscoveryId));
    }
}
