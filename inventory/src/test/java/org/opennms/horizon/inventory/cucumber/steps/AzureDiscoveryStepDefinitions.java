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
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.opennms.horizon.inventory.cucumber.InventoryBackgroundHelper;
import org.opennms.horizon.inventory.dto.AzureCredentialCreateDTO;
import org.opennms.horizon.inventory.dto.AzureCredentialDTO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class AzureDiscoveryStepDefinitions {
    private static InventoryBackgroundHelper backgroundHelper;
    private AzureCredentialCreateDTO createCredentialsDto;
    private AzureCredentialDTO azureCredentialsDto;

    @BeforeAll
    public static void beforeAll() {
        backgroundHelper = new InventoryBackgroundHelper();
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
    @Given("Azure Test Credentials")
    public void generatedTestCredentials() {
        createCredentialsDto = AzureCredentialCreateDTO.newBuilder()
            .setLocation("Default")
            .setName("test-azure-discovery-name")
            .setClientId("test-client-id")
            .setClientSecret("test-client-secret")
            .setSubscriptionId("test-subscription-id")
            .setDirectoryId("test-directory-id")
            .build();
    }

    /*
     * SCENARIO WHEN
     * *********************************************************************************
     */
    @When("A GRPC request to create azure credentials")
    public void aGRPCRequestToCreateAzureCredentials() {
        var azureCredentialServiceBlockingStub = backgroundHelper.getAzureCredentialServiceBlockingStub();
        azureCredentialsDto = azureCredentialServiceBlockingStub.createCredentials(createCredentialsDto);
    }

    /*
     * SCENARIO THEN
     * *********************************************************************************
     */
    @Then("The response should assert for relevant fields")
    public void theResponseShouldAssertForRelevantFields() {
        assertTrue(azureCredentialsDto.getId() > 0);
        assertEquals(createCredentialsDto.getName(), azureCredentialsDto.getName());
        assertEquals(createCredentialsDto.getClientId(), azureCredentialsDto.getClientId());
        assertEquals(createCredentialsDto.getSubscriptionId(), azureCredentialsDto.getSubscriptionId());
        assertEquals(createCredentialsDto.getDirectoryId(), azureCredentialsDto.getDirectoryId());
        assertNotNull(azureCredentialsDto.getLocation());
        assertTrue(azureCredentialsDto.getCreateTimeMsec() > 0);
    }
}
