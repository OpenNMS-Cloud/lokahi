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

import io.cucumber.java.BeforeAll;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.opennms.horizon.inventory.cucumber.InventoryBackgroundHelper;
import org.opennms.horizon.inventory.dto.AzureActiveDiscoveryCreateDTO;
import org.opennms.horizon.inventory.dto.AzureActiveDiscoveryDTO;
import org.opennms.horizon.inventory.dto.ListTagsByEntityIdParamsDTO;
import org.opennms.horizon.inventory.dto.TagCreateDTO;
import org.opennms.horizon.inventory.dto.TagDTO;
import org.opennms.horizon.inventory.dto.TagEntityIdDTO;
import org.opennms.horizon.inventory.dto.TagListDTO;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class AzureDiscoveryStepDefinitions {
    private final InventoryBackgroundHelper backgroundHelper;
    private AzureActiveDiscoveryCreateDTO createDiscoveryDto;
    private AzureActiveDiscoveryDTO discoveryDto;
    private TagCreateDTO tagCreateDto1;
    private TagListDTO tagList;
    private Exception caught;

    public AzureDiscoveryStepDefinitions(InventoryBackgroundHelper backgroundHelper) {
        this.backgroundHelper = backgroundHelper;
    }

    /*
     * BACKGROUND GIVEN
     * *********************************************************************************
     */
    @Given("[Azure] External GRPC Port in system property {string}")
    public void externalGRPCPortInSystemProperty(String propertyName) {
        backgroundHelper.externalGRPCPortInSystemProperty(propertyName);
    }

    @Given("[Azure] Kafka Bootstrap URL in system property {string}")
    public void kafkaBootstrapURLInSystemProperty(String systemPropertyName) {
        backgroundHelper.kafkaBootstrapURLInSystemProperty(systemPropertyName);
    }

    @Given("[Azure] Grpc TenantId {string}")
    public void grpcTenantId(String tenantId) {
        backgroundHelper.grpcTenantId(tenantId);
    }

    @Given("[Azure] Grpc location {string}")
    public void grpcLocation(String location) {
        backgroundHelper.grpcLocation(location);
    }

    @Given("[Azure] Create Grpc Connection for Inventory")
    public void createGrpcConnectionForInventory() {
        backgroundHelper.createGrpcConnectionForInventory();
    }

    /*
     * SCENARIO GIVEN
     * *********************************************************************************
     */
    @Given("Azure Test Active Discovery")
    public void generatedTestActiveDiscovery() {
        tagCreateDto1 = TagCreateDTO.newBuilder()
            .setName("test-tag-name-1").build();
        createDiscoveryDto = AzureActiveDiscoveryCreateDTO.newBuilder()
            .setLocation("Default")
            .setName("test-azure-discovery-name")
            .setClientId("test-client-id")
            .setClientSecret("test-client-secret")
            .setSubscriptionId("test-subscription-id")
            .setDirectoryId("test-directory-id")
            .addAllTags(List.of(tagCreateDto1))
            .build();
    }

    @Given("Clear tenant id")
    public void clearTenantId() {
        backgroundHelper.clearTenantId();
    }

    /*
     * SCENARIO WHEN
     * *********************************************************************************
     */
    @When("A GRPC request to create azure active discovery")
    public void aGRPCRequestToCreateAzureActiveDiscovery() {
        var azureActiveDiscoveryServiceBlockingStub = backgroundHelper.getAzureActiveDiscoveryServiceBlockingStub();
        discoveryDto = azureActiveDiscoveryServiceBlockingStub.createDiscovery(createDiscoveryDto);
    }

    @When("A GRPC request to create azure active discovery with exception expected")
    public void aGRPCRequestToCreateAzureActiveDiscoveryWithException() {
        caught = null;

        try {
            var azureActiveDiscoveryServiceBlockingStub = backgroundHelper.getAzureActiveDiscoveryServiceBlockingStub();
            discoveryDto = azureActiveDiscoveryServiceBlockingStub.createDiscovery(createDiscoveryDto);
        } catch (Exception ex) {
            caught = ex;
        }
    }


    @And("A GRPC request to get tags for azure active discovery")
    public void aGRPCRequestToGetTagsForAzureActiveDiscovery() {
        var tagServiceBlockingStub = backgroundHelper.getTagServiceBlockingStub();
        ListTagsByEntityIdParamsDTO params = ListTagsByEntityIdParamsDTO.newBuilder()
            .setEntityId(TagEntityIdDTO.newBuilder()
                .setActiveDiscoveryId(discoveryDto.getId())).build();
        tagList = tagServiceBlockingStub.getTagsByEntityId(params);
    }

    /*
     * SCENARIO THEN
     * *********************************************************************************
     */
    @Then("The response should assert for relevant fields")
    public void theResponseShouldAssertForRelevantFields() {
        assertTrue(discoveryDto.getId() > 0);
        assertEquals(createDiscoveryDto.getName(), discoveryDto.getName());
        assertEquals(createDiscoveryDto.getClientId(), discoveryDto.getClientId());
        assertEquals(createDiscoveryDto.getSubscriptionId(), discoveryDto.getSubscriptionId());
        assertEquals(createDiscoveryDto.getDirectoryId(), discoveryDto.getDirectoryId());
        assertNotNull(discoveryDto.getLocation());
        assertTrue(discoveryDto.getCreateTimeMsec() > 0);

        assertEquals(1, tagList.getTagsCount());
        TagDTO tagDTO = tagList.getTags(0);
        assertEquals(tagCreateDto1.getName(), tagDTO.getName());
    }


    @Then("verify exception {string} thrown with message {string}")
    public void verifyException(String exceptionName, String message) {
        if (caught == null) {
            fail("No exception caught");
        } else {
            assertEquals(exceptionName, caught.getClass().getSimpleName(), "Exception mismatch");
            assertEquals(message, caught.getMessage());
        }
    }

}
