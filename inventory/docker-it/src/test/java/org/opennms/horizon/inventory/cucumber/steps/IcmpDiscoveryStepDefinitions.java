/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
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
import com.google.protobuf.Empty;
import com.google.protobuf.Int64Value;
import io.cucumber.java.BeforeAll;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.opennms.horizon.inventory.cucumber.InventoryBackgroundHelper;
import org.opennms.horizon.inventory.discovery.IcmpActiveDiscoveryCreateDTO;
import org.opennms.horizon.inventory.discovery.IcmpActiveDiscoveryList;
import org.opennms.horizon.inventory.discovery.SNMPConfigDTO;
import org.opennms.horizon.inventory.dto.ListTagsByEntityIdParamsDTO;
import org.opennms.horizon.inventory.dto.NodeIdQuery;
import org.opennms.horizon.inventory.dto.TagCreateDTO;
import org.opennms.horizon.inventory.dto.TagDTO;
import org.opennms.horizon.inventory.dto.TagEntityIdDTO;
import org.opennms.taskset.contract.DiscoveryScanResult;
import org.opennms.taskset.contract.PingResponse;
import org.opennms.taskset.contract.ScannerResponse;
import org.opennms.taskset.contract.TaskResult;
import org.opennms.taskset.contract.TenantLocationSpecificTaskSetResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class IcmpDiscoveryStepDefinitions {

    private static final Logger LOG = LoggerFactory.getLogger(InventoryProcessingStepDefinitions.class);
    private final InventoryBackgroundHelper backgroundHelper;
    private IcmpActiveDiscoveryCreateDTO icmpDiscovery;
    private long activeDiscoveryId;

    public IcmpDiscoveryStepDefinitions(InventoryBackgroundHelper backgroundHelper) {
        this.backgroundHelper = backgroundHelper;
    }

    @Given("[ICMP Discovery] External GRPC Port in system property {string}")
    public void icmpDiscoveryExternalGRPCPortInSystemProperty(String propertyName) {
        backgroundHelper.externalGRPCPortInSystemProperty(propertyName);
    }

    @Given("[ICMP Discovery] Kafka Bootstrap URL in system property {string}")
    public void icmpDiscoveryKafkaBootstrapURLInSystemProperty(String systemPropertyName) {
        backgroundHelper.kafkaBootstrapURLInSystemProperty(systemPropertyName);
    }

    @Given("[ICMP Discovery] MOCK Minion Gateway Base URL in system property {string}")
    public void icmpDiscoveryMOCKMinionGatewayBaseURLInSystemProperty(String arg0) {
    }

    @Given("[ICMP Discovery] Grpc TenantId {string}")
    public void icmpDiscoveryGrpcTenantId(String tenantId) {
        backgroundHelper.grpcTenantId(tenantId);
    }

    @Given("[ICMP Discovery] Grpc location {string}")
    public void grpcLocation(String location) {
        backgroundHelper.grpcLocation(location);
    }

    @Given("[ICMP Discovery] Create Grpc Connection for Inventory")
    public void icmpDiscoveryCreateGrpcConnectionForInventory() {
        backgroundHelper.createGrpcConnectionForInventory();
    }

    @Given("New Active Discovery with IpAddresses {string} and SNMP community as {string} at location {string}")
    public void newActiveDiscoveryWithIpAddressesAndSNMPCommunityAsAtLocation(String ipAddressStrings, String snmpReadCommunity, String location) {
        icmpDiscovery = IcmpActiveDiscoveryCreateDTO.newBuilder()
            .addIpAddresses(ipAddressStrings).setSnmpConf(SNMPConfigDTO.newBuilder().addReadCommunity(snmpReadCommunity).build())
            .setLocation(location).build();
    }



    @Given("New Active Discovery with IpAddresses {string} and SNMP community as {string} at location {string} with tags {string}")
    public void newActiveDiscoveryWithIpAddressesAndSNMPCommunityAsAtLocationWithTags(String ipAddressStrings, String snmpReadCommunity,
                                                                                      String location, String tags) {
        var tagsList = tags.split(",");
        icmpDiscovery = IcmpActiveDiscoveryCreateDTO.newBuilder()
            .addIpAddresses(ipAddressStrings).setSnmpConf(SNMPConfigDTO.newBuilder()
                .addReadCommunity(snmpReadCommunity).build())
            .addAllTags(Stream.of(tagsList).map(tag -> TagCreateDTO.newBuilder().setName(tag).build()).toList())
            .setLocation(location).build();
    }

    @Then("create Active Discovery and validate it's created active discovery with above details.")
    public void createActiveDiscoveryAndValidateItSCreatedActiveDiscoveryWithAboveDetails() {
        var icmpDiscoveryDto = backgroundHelper.getIcmpActiveDiscoveryServiceBlockingStub().createDiscovery(icmpDiscovery);
        activeDiscoveryId = icmpDiscoveryDto.getId();
        Assertions.assertEquals(icmpDiscovery.getLocation(), icmpDiscoveryDto.getLocation());
        Assertions.assertEquals(icmpDiscovery.getIpAddresses(0), icmpDiscoveryDto.getIpAddresses(0));
        Assertions.assertEquals(icmpDiscovery.getSnmpConf().getReadCommunity(0), icmpDiscoveryDto.getSnmpConf().getReadCommunity(0));
        var tagListQuery = ListTagsByEntityIdParamsDTO.newBuilder()
            .setEntityId(TagEntityIdDTO.newBuilder().setActiveDiscoveryId(icmpDiscoveryDto.getId()).build())
            .build();
        var tagList = backgroundHelper.getTagServiceBlockingStub().getTagsByEntityId(tagListQuery);
        Assertions.assertEquals(icmpDiscovery.getTagsCount(), tagList.getTagsCount());
        var tagsCreated = icmpDiscovery.getTagsList().stream().map(TagCreateDTO::getName).toList();
        // Take one tag and validate if it exists on the discovery.
        var tag = tagsCreated.get(0);
        Assertions.assertTrue(tagList.getTagsList().stream().map(TagDTO::getName).toList().contains(tag));
    }

    @Then("verify get active discovery with above details.")
    public void GetActiveDiscoveryWithAboveDetails() {
        var icmpDiscoveryDto = backgroundHelper.getIcmpActiveDiscoveryServiceBlockingStub().getDiscoveryById(Int64Value.of(activeDiscoveryId));
        Assertions.assertEquals(icmpDiscovery.getLocation(), icmpDiscoveryDto.getLocation());
        Assertions.assertEquals(icmpDiscovery.getIpAddresses(0), icmpDiscoveryDto.getIpAddresses(0));
        Assertions.assertEquals(icmpDiscovery.getSnmpConf().getReadCommunity(0), icmpDiscoveryDto.getSnmpConf().getReadCommunity(0));
        var tagListQuery = ListTagsByEntityIdParamsDTO.newBuilder()
            .setEntityId(TagEntityIdDTO.newBuilder().setActiveDiscoveryId(icmpDiscoveryDto.getId()).build())
            .build();
        var tagList = backgroundHelper.getTagServiceBlockingStub().getTagsByEntityId(tagListQuery);
        Assertions.assertEquals(icmpDiscovery.getTagsCount(), tagList.getTagsCount());
        var tagsCreated = icmpDiscovery.getTagsList().stream().map(TagCreateDTO::getName).toList();
        // Take one tag and validate if it exists on the discovery.
        var tag = tagsCreated.get(0);
        Assertions.assertTrue(tagList.getTagsList().stream().map(TagDTO::getName).toList().contains(tag));
    }

    @Then("send discovery ping results for {string} to Kafka topic {string}")
    public void sendDiscoveryPingResultsForToKafkaTopic(String ipAddress, String topic) {
        Properties producerConfig = new Properties();
        producerConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, backgroundHelper.getKafkaBootstrapUrl());
        producerConfig.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getCanonicalName());
        producerConfig.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getCanonicalName());
        try (KafkaProducer<String, byte[]> kafkaProducer = new KafkaProducer<>(producerConfig)) {
            var scanResult = DiscoveryScanResult.newBuilder().setActiveDiscoveryId(activeDiscoveryId)
                .addPingResponse(PingResponse.newBuilder().setIpAddress(ipAddress).build()).build();

            TaskResult taskResult =
                TaskResult.newBuilder()
                    .setScannerResponse(ScannerResponse.newBuilder().setResult(Any.pack(scanResult)).build())
                    .build();

            TenantLocationSpecificTaskSetResults taskSetResults =
                TenantLocationSpecificTaskSetResults.newBuilder()
                    .setTenantId(backgroundHelper.getTenantId())
                    .setLocation(icmpDiscovery.getLocation())
                    .addResults(taskResult)
                    .build();
            var producerRecord = new ProducerRecord<String, byte[]>(topic, taskSetResults.toByteArray());
            Map<String, String> grpcHeaders = backgroundHelper.getGrpcHeaders();
            grpcHeaders.forEach((key, value) -> producerRecord.headers().add(key, value.getBytes(StandardCharsets.UTF_8)));
            kafkaProducer.send(producerRecord);
        }
    }

    @Then("verify list has {int} items")
    public void verifyListSize(int items) {
        IcmpActiveDiscoveryList list = backgroundHelper.getIcmpActiveDiscoveryServiceBlockingStub().listDiscoveries(Empty.getDefaultInstance());
        Assertions.assertEquals(items, list.getDiscoveriesCount());
    }

    @Then("verify that node is created for {string} and location {string} with same tags within {int}ms")
    public void verifyThatNodeIsCreatedForAndLocationWithTheTagsInPreviousScenario(String ipAddress, String location, int timeout) {

        Awaitility.await().pollInterval(2000, TimeUnit.MILLISECONDS).atMost(timeout, TimeUnit.MILLISECONDS).until(() -> {
            try {
                var nodeId = backgroundHelper.getNodeServiceBlockingStub().
                    getNodeIdFromQuery(NodeIdQuery.newBuilder().setLocation(location).setIpAddress(ipAddress).build());
                return nodeId != null && nodeId.getValue() != 0;
            } catch (Exception e) {
                return false;
            }
        });
        var nodeId = backgroundHelper.getNodeServiceBlockingStub().
            getNodeIdFromQuery(NodeIdQuery.newBuilder().setLocation(location).setIpAddress(ipAddress).build());
        var nodeDto = backgroundHelper.getNodeServiceBlockingStub().getNodeById(nodeId);
        Assertions.assertTrue(nodeDto.getIpInterfacesList().stream().anyMatch(ipInterfaceDTO -> ipInterfaceDTO.getIpAddress().equals(ipAddress)));
        var tagListQuery = ListTagsByEntityIdParamsDTO.newBuilder()
            .setEntityId(TagEntityIdDTO.newBuilder().setNodeId(nodeDto.getId()).build())
            .build();
        var tagList = backgroundHelper.getTagServiceBlockingStub().getTagsByEntityId(tagListQuery);
        Assertions.assertEquals(icmpDiscovery.getTagsCount(), tagList.getTagsCount());
        var tagsCreated = icmpDiscovery.getTagsList().stream().map(TagCreateDTO::getName).toList();
        // Take one tag and validate if it exists on the node.
        var tag = tagsCreated.get(0);
        Assertions.assertTrue(tagList.getTagsList().stream().map(TagDTO::getName).toList().contains(tag));

    }


}
