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

import static org.junit.jupiter.api.Assertions.*;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.List;
import org.opennms.horizon.inventory.cucumber.InventoryBackgroundHelper;
import org.opennms.horizon.inventory.dto.AzureActiveDiscoveryCreateDTO;
import org.opennms.horizon.inventory.dto.AzureActiveDiscoveryDTO;
import org.opennms.horizon.inventory.dto.ListTagsByEntityIdParamsDTO;
import org.opennms.horizon.inventory.dto.TagCreateDTO;
import org.opennms.horizon.inventory.dto.TagDTO;
import org.opennms.horizon.inventory.dto.TagEntityIdDTO;
import org.opennms.horizon.inventory.dto.TagListDTO;

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

    @Given("[Azure] Create Grpc Connection for Inventory")
    public void createGrpcConnectionForInventory() {
        backgroundHelper.createGrpcConnectionForInventory();
    }

    /*
     * SCENARIO GIVEN
     * *********************************************************************************
     */
    @Given("Azure Test Active Discovery {string} subscription {string} for location named {string}")
    public void generatedTestActiveDiscovery(String name, String subscriptionId, String location) {
        tagCreateDto1 = TagCreateDTO.newBuilder().setName("test-tag-name-1").build();
        createDiscoveryDto = AzureActiveDiscoveryCreateDTO.newBuilder()
                .setLocationId(backgroundHelper.findLocationId(location))
                .setName(name)
                .setClientId("test-client-id")
                .setClientSecret("test-client-secret")
                .setSubscriptionId(subscriptionId)
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
                .setEntityId(TagEntityIdDTO.newBuilder().setActiveDiscoveryId(discoveryDto.getId()))
                .build();
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
        assertNotNull(discoveryDto.getLocationId());
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
