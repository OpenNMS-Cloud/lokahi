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
import com.google.protobuf.StringValue;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.opennms.horizon.events.proto.Event;
import org.opennms.horizon.events.proto.EventLog;
import org.opennms.horizon.inventory.cucumber.InventoryBackgroundHelper;
import org.opennms.horizon.inventory.dto.ListTagsByEntityIdParamsDTO;
import org.opennms.horizon.inventory.dto.NodeIdQuery;
import org.opennms.horizon.inventory.dto.PassiveDiscoveryDTO;
import org.opennms.horizon.inventory.dto.PassiveDiscoveryListDTO;
import org.opennms.horizon.inventory.dto.PassiveDiscoveryServiceGrpc;
import org.opennms.horizon.inventory.dto.PassiveDiscoveryToggleDTO;
import org.opennms.horizon.inventory.dto.PassiveDiscoveryUpsertDTO;
import org.opennms.horizon.inventory.dto.TagCreateDTO;
import org.opennms.horizon.inventory.dto.TagEntityIdDTO;
import org.opennms.horizon.inventory.dto.TagListDTO;
import org.opennms.horizon.inventory.dto.TagServiceGrpc;
import org.opennms.horizon.shared.events.EventConstants;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class PassiveDiscoveryStepDefinitions {
    private final InventoryBackgroundHelper backgroundHelper;
    private PassiveDiscoveryUpsertDTO passiveDiscoveryUpsertDTO;
    private PassiveDiscoveryDTO upsertedDiscovery;
    private PassiveDiscoveryListDTO fetchedPassiveDiscoveryList;
    private TagListDTO tagList;
    private long fetchedId;
    private final String internalEventTopic  = "internal-event";

    public PassiveDiscoveryStepDefinitions(InventoryBackgroundHelper backgroundHelper) {
        this.backgroundHelper = backgroundHelper;
    }

    /*
     * BACKGROUND GIVEN
     * *********************************************************************************
     */
    @Given("[Passive] External GRPC Port in system property {string}")
    public void externalGRPCPortInSystemProperty(String propertyName) {
        backgroundHelper.externalGRPCPortInSystemProperty(propertyName);
    }

    @Given("[Passive] Kafka Bootstrap URL in system property {string}")
    public void kafkaBootstrapURLInSystemProperty(String systemPropertyName) {
        backgroundHelper.kafkaBootstrapURLInSystemProperty(systemPropertyName);
    }

    @Given("[Passive] Grpc TenantId {string}")
    public void grpcTenantId(String tenantId) {
        backgroundHelper.grpcTenantId(tenantId);
    }

    @Given("[Passive] Create Grpc Connection for Inventory")
    public void createGrpcConnectionForInventory() {
        backgroundHelper.createGrpcConnectionForInventory();
    }

    /*
     * SCENARIO GIVEN
     * *********************************************************************************
     */
    @Given("Passive Discovery cleared")
    public void passiveDiscoveryCleared() {
        deleteAllPassiveDiscovery();
    }

    @Given("Passive Discovery fields to persist for location named {string}")
    public void passiveDiscoveryFieldsToPersist(String location) {
        passiveDiscoveryUpsertDTO = PassiveDiscoveryUpsertDTO.newBuilder()
            .setLocationId(backgroundHelper.findLocationId(location))
            .addCommunities("public")
            .addPorts(161)
            .addTags(TagCreateDTO.newBuilder().setName("tag-name").build())
            .build();
    }

    @Given("Passive Discovery fields to update for location named {string}")
    public void passiveDiscoveryFieldsToUpdate(String location) {
        passiveDiscoveryUpsertDTO = PassiveDiscoveryUpsertDTO.newBuilder()
            .setId(fetchedId)
            .setLocationId(backgroundHelper.findLocationId(location))
            .addCommunities("other")
            .addPorts(161)
            .addTags(TagCreateDTO.newBuilder().setName("tag-name").build())
            .build();
    }

    /*
     * SCENARIO WHEN
     * *********************************************************************************
     */
    @When("A GRPC request to upsert a passive discovery")
    public void aGRPCRequestToCreateANewPassiveDiscovery() {
        PassiveDiscoveryServiceGrpc.PassiveDiscoveryServiceBlockingStub stub
            = backgroundHelper.getPassiveDiscoveryServiceBlockingStub();
        upsertedDiscovery = stub.upsertDiscovery(passiveDiscoveryUpsertDTO);
    }

    @When("A GRPC request to toggle a passive discovery")
    public void aGRPCRequestToToggleAPassiveDiscovery() {
        PassiveDiscoveryToggleDTO toggleDTO = PassiveDiscoveryToggleDTO.newBuilder()
            .setId(fetchedId)
            .setToggle(false)
            .build();

        PassiveDiscoveryServiceGrpc.PassiveDiscoveryServiceBlockingStub stub
            = backgroundHelper.getPassiveDiscoveryServiceBlockingStub();
        upsertedDiscovery = stub.toggleDiscovery(toggleDTO);
    }

    @And("A GRPC request to get passive discovery list")
    public void aGRPCRequestToGetPassiveDiscoveryList() {
        PassiveDiscoveryServiceGrpc.PassiveDiscoveryServiceBlockingStub stub
            = backgroundHelper.getPassiveDiscoveryServiceBlockingStub();
        fetchedPassiveDiscoveryList = stub.listAllDiscoveries(Empty.getDefaultInstance());
    }

    @And("A GRPC request to get tags for passive discovery")
    public void aGRPCRequestToGetTagsForPassiveDiscovery() {
        TagServiceGrpc.TagServiceBlockingStub stub = backgroundHelper.getTagServiceBlockingStub();
        tagList = stub.getTagsByEntityId(ListTagsByEntityIdParamsDTO.newBuilder()
            .setEntityId(TagEntityIdDTO.newBuilder()
                .setPassiveDiscoveryId(upsertedDiscovery.getId())).build());
    }

    /*
     * SCENARIO THEN
     * *********************************************************************************
     */
    @Then("The upserted and the get of passive discovery should be the same")
    public void theCreationAndTheGetOfPassiveDiscoveryShouldBeTheSame() {
        assertEquals(1, fetchedPassiveDiscoveryList.getDiscoveriesList().size());
        PassiveDiscoveryDTO fetchedDiscovery = fetchedPassiveDiscoveryList.getDiscoveries(0);
        fetchedId = fetchedDiscovery.getId();

        assertEquals(passiveDiscoveryUpsertDTO.getLocationId(), upsertedDiscovery.getLocationId());
        assertEquals(passiveDiscoveryUpsertDTO.getLocationId(), fetchedDiscovery.getLocationId());

        assertEquals(passiveDiscoveryUpsertDTO.getPortsCount(), upsertedDiscovery.getPortsCount());
        assertEquals(passiveDiscoveryUpsertDTO.getPortsCount(), fetchedDiscovery.getPortsCount());
        assertEquals(passiveDiscoveryUpsertDTO.getPorts(0), upsertedDiscovery.getPorts(0));
        assertEquals(passiveDiscoveryUpsertDTO.getPorts(0), fetchedDiscovery.getPorts(0));

        assertEquals(passiveDiscoveryUpsertDTO.getCommunitiesCount(), upsertedDiscovery.getCommunitiesCount());
        assertEquals(passiveDiscoveryUpsertDTO.getCommunitiesCount(), fetchedDiscovery.getCommunitiesCount());
        assertEquals(passiveDiscoveryUpsertDTO.getCommunities(0), upsertedDiscovery.getCommunities(0));
        assertEquals(passiveDiscoveryUpsertDTO.getCommunities(0), fetchedDiscovery.getCommunities(0));
        assertTrue(fetchedDiscovery.getToggle());
    }

    @Then("The passive discovery toggle should be false")
    public void theToggleShouldBeFalse() {
        assertEquals(1, fetchedPassiveDiscoveryList.getDiscoveriesList().size());
        PassiveDiscoveryDTO fetchedDiscovery = fetchedPassiveDiscoveryList.getDiscoveries(0);
        assertFalse(fetchedDiscovery.getToggle());
    }

    @Then("the tags for passive discovery match what it was created with")
    public void theTagsForPassiveDiscoveryMatchWhatItWasCreatedWith() {
        assertEquals(passiveDiscoveryUpsertDTO.getTagsCount(), tagList.getTagsCount());
        assertEquals(passiveDiscoveryUpsertDTO.getTags(0).getName(), tagList.getTags(0).getName());
    }

    /*
     * INTERNAL
     * *********************************************************************************
     */
    private void deleteAllPassiveDiscovery() {
        var passiveDiscoveryServiceBlockingStub = backgroundHelper.getPassiveDiscoveryServiceBlockingStub();
        for (PassiveDiscoveryDTO discoveryDTO : passiveDiscoveryServiceBlockingStub.listAllDiscoveries(Empty.newBuilder().build()).getDiscoveriesList()) {
            passiveDiscoveryServiceBlockingStub.deleteDiscovery(Int64Value.of(discoveryDTO.getId()));
        }
    }

    @Given("A GRPC request to enable a passive discovery")
    public void aGRPCRequestToEnableAPassiveDiscovery() {
        PassiveDiscoveryToggleDTO toggleDTO = PassiveDiscoveryToggleDTO.newBuilder()
            .setId(fetchedId)
            .setToggle(true)
            .build();

        PassiveDiscoveryServiceGrpc.PassiveDiscoveryServiceBlockingStub stub
            = backgroundHelper.getPassiveDiscoveryServiceBlockingStub();
        upsertedDiscovery = stub.toggleDiscovery(toggleDTO);
    }

    @When("A new suspect event sent to Inventory for location named {string} and IP Address {string}")
    public void aNewSuspectEventSentToInventoryForLocationNamedAndIPAddress(String location , String ipAddress) throws InterruptedException {

        var locationDTO = backgroundHelper.getMonitoringLocationStub().getLocationByName(StringValue.of(location));
        Properties producerConfig = new Properties();
        producerConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, backgroundHelper.getKafkaBootstrapUrl());
        producerConfig.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getCanonicalName());
        producerConfig.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getCanonicalName());

        var eventBuilder = Event.newBuilder().setUei(EventConstants.NEW_SUSPECT_INTERFACE_EVENT_UEI)
            .setLocationId(String.valueOf(locationDTO.getId()))
            .setIpAddress(ipAddress)
            .setTenantId(backgroundHelper.getTenantId());
        var eventLog = EventLog.newBuilder().addEvents(eventBuilder.build()).build();
        try (KafkaProducer<String, byte[]> kafkaProducer = new KafkaProducer<>(producerConfig)) {
            var producerRecord = new ProducerRecord<String, byte[]>(internalEventTopic, eventLog.toByteArray());
            kafkaProducer.send(producerRecord);
        }

    }


    @Then("A new node should be created with location {string} and IP Address {string}")
    public void aNewNodeShouldBeCreatedWithLocationAndIPAddress(String location, String ipAddress) {

        var locationDTO = backgroundHelper.getMonitoringLocationStub().getLocationByName(StringValue.of(location));

        await().atMost(10, TimeUnit.SECONDS).until(() -> {
            try {
                var result = backgroundHelper.getNodeServiceBlockingStub().getNodeIdFromQuery(NodeIdQuery.newBuilder()
                    .setIpAddress(ipAddress).setLocationId(String.valueOf(locationDTO.getId())).build());
                var nodeDTO = backgroundHelper.getNodeServiceBlockingStub().getNodeById(result);
                return nodeDTO.getIpInterfacesList().get(0).getIpAddress().equals(ipAddress);
            } catch (Exception e) {
                return false;
            }
        });
    }


}
