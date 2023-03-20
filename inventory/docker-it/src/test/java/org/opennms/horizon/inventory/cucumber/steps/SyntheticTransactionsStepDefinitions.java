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

import static org.junit.Assert.fail;

import com.google.protobuf.Empty;
import io.cucumber.datatable.DataTable;
import io.cucumber.datatable.TableTransformer;
import io.cucumber.java.BeforeAll;
import io.cucumber.java.DataTableType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import org.opennms.horizon.inventory.cucumber.InventoryBackgroundHelper;
import org.opennms.horizon.inventory.dto.SyntheticTestCreateDTO;
import org.opennms.horizon.inventory.dto.SyntheticTestDTO;
import org.opennms.horizon.inventory.dto.SyntheticTestPluginConfigurationDTO;
import org.opennms.horizon.inventory.dto.SyntheticTestPluginResilienceDTO;
import org.opennms.horizon.inventory.dto.SyntheticTransactionCreateDTO;
import org.opennms.horizon.inventory.dto.SyntheticTransactionDTO;
import org.opennms.horizon.inventory.dto.SyntheticTransactionDTO.Builder;
import org.opennms.horizon.inventory.dto.SyntheticTransactionDTOArray;
import org.opennms.horizon.inventory.dto.SyntheticTransactionRequestDTO;
import org.opennms.horizon.inventory.dto.SyntheticTransactionServiceGrpc.SyntheticTransactionServiceBlockingStub;
import org.opennms.horizon.inventory.dto.SyntheticTransactionTestDTOArray;
import org.opennms.horizon.inventory.dto.TenantedId;
import org.opennms.horizon.inventory.testtool.miniongateway.wiremock.client.MinionGatewayWiremockTestSteps;


public class SyntheticTransactionsStepDefinitions {
    private final MinionGatewayWiremockTestSteps minionGatewayWiremockTestSteps;
    private final InventoryBackgroundHelper backgroundHelper;

    private Builder syntheticTransaction = SyntheticTransactionDTO.newBuilder();
    private SyntheticTestCreateDTO syntheticTransactionTest;

    public SyntheticTransactionsStepDefinitions(MinionGatewayWiremockTestSteps minionGatewayWiremockTestSteps,
        InventoryBackgroundHelper inventoryBackgroundHelper) {
        this.minionGatewayWiremockTestSteps = minionGatewayWiremockTestSteps;
        this.backgroundHelper = inventoryBackgroundHelper;
    }

    @Given("Synthetic transaction label {string}")
    public void synthetic_transaction_label(String label) {
        this.syntheticTransaction.setLabel(label);
    }

    // step definitions
    @When("Synthetic transaction is created")
    public void synthetic_transaction_is_created() {
        backgroundHelper.getSyntheticTransactionServiceBlockingStub().createSyntheticTransaction(SyntheticTransactionCreateDTO.newBuilder()
            .setLabel(syntheticTransaction.getLabel())
            .build()
        );
    }

    @Then("Synthetic transaction with label {string} exists")
    public void synthetic_transaction_exists(String label) {
        SyntheticTransactionServiceBlockingStub serviceStub = backgroundHelper.getSyntheticTransactionServiceBlockingStub();
        SyntheticTransactionDTOArray transactions = serviceStub.getSyntheticTransactions(Empty.getDefaultInstance());
        for (SyntheticTransactionDTO transactionDTO : transactions.getTransactionsList()) {
            if (label.equals(transactionDTO.getLabel())) {
                return;
            }
        }

        fail("Transaction " + label + " should exist");
    }

    @When("Synthetic transaction {string} is deleted")
    public void synthetic_transaction_is_deleted(String label) {
        SyntheticTransactionServiceBlockingStub serviceStub = backgroundHelper.getSyntheticTransactionServiceBlockingStub();
        SyntheticTransactionDTOArray transactions = serviceStub.getSyntheticTransactions(Empty.getDefaultInstance());
        for (SyntheticTransactionDTO transactionDTO : transactions.getTransactionsList()) {
            if (label.equals(transactionDTO.getLabel())) {
                serviceStub.deleteSyntheticTransaction(SyntheticTransactionRequestDTO.newBuilder()
                    .setId(transactionDTO.getId())
                    .build()
                );
            }
        }
    }

    @Then("Synthetic transaction with label {string} does not exist")
    public void synthetic_transaction_does_not_exist(String label) {
        SyntheticTransactionServiceBlockingStub serviceStub = backgroundHelper.getSyntheticTransactionServiceBlockingStub();
        SyntheticTransactionDTOArray transactions = serviceStub.getSyntheticTransactions(Empty.getDefaultInstance());
        for (SyntheticTransactionDTO transactionDTO : transactions.getTransactionsList()) {
            if (label.equals(transactionDTO.getLabel())) {
                fail("Transaction " + transactionDTO.getLabel() + " should not exist");
            }
        }
    }

    @Given("Synthetic transaction test associated with transaction {string}:")
    public void synthetic_transaction_test(String transaction, SyntheticTestCreateDTO request) {
        TenantedId transactionId = lookupTransactionId(transaction).orElseThrow(() -> new IllegalArgumentException("Could not find transaction " + transaction));

        this.syntheticTransactionTest = request.toBuilder().setSyntheticTransactionId(transactionId).build();
    }

    private Optional<TenantedId> lookupTransactionId(String label) {
        SyntheticTransactionServiceBlockingStub serviceStub = backgroundHelper.getSyntheticTransactionServiceBlockingStub();
        SyntheticTransactionDTOArray transactions = serviceStub.getSyntheticTransactions(Empty.getDefaultInstance());

        for (SyntheticTransactionDTO transactionDTO : transactions.getTransactionsList()) {
            if (label.equals(transactionDTO.getLabel())) {
                return Optional.of(transactionDTO.getId());
            }
        }

        return Optional.empty();
    }

    @When("Synthetic transaction test is created")
    public void synthetic_transaction_test_is_created() {
        backgroundHelper.getSyntheticTransactionServiceBlockingStub().createSyntheticTransactionTest(syntheticTransactionTest);
    }

    @Then("Synthetic transaction {string} contains test labelled {string}")
    public void synthetic_transaction_with_test_exists(String transaction, String test) {
        TenantedId transactionId = lookupTransactionId(transaction).orElseThrow(() -> new IllegalArgumentException("Could not find transaction " + transaction));

        SyntheticTransactionTestDTOArray transactionTests = backgroundHelper.getSyntheticTransactionServiceBlockingStub().getSyntheticTransactionTests(
            SyntheticTransactionRequestDTO.newBuilder().setId(transactionId).build()
        );

        for (SyntheticTestDTO transactionTest : transactionTests.getTestsList()) {
            if (test.equals(transactionTest.getLabel())) {
                return;
            }
        }

        fail("Transaction " + transaction + " does not contain test with label " + test);
    }

    // transformations

    @DataTableType
    public SyntheticTestCreateDTO defineType(DataTable table) {
        Map<String, String> dataTable = table.entries().stream()
          .map(map -> Map.entry(map.get("property"), map.get("value")))
          .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

        SyntheticTestCreateDTO.Builder builder = SyntheticTestCreateDTO.newBuilder();
        for (String key : dataTable.keySet()) {
            String value = dataTable.get(key);
            switch (key) {
                case "label" -> builder.setLabel(value);
                case "schedule" -> builder.setSchedule(value);
                case "locations" -> builder.addAllLocations(List.of(value.split(",")));
                default -> {
                    if (key.startsWith("config.")) {
                        String configKey = key.substring("config.".length());
                        SyntheticTestPluginConfigurationDTO.Builder pluginBuilder = builder.getPluginConfigurationBuilder();
                        switch (configKey) {
                            case "pluginName" -> pluginBuilder.setPluginName(value);
                            default -> pluginBuilder.getMutableConfig().put(configKey, value);
                        }

                    }
                    if (key.startsWith("resilience.")) {
                        String configKey = key.substring("resilience.".length());
                        SyntheticTestPluginResilienceDTO.Builder resilienceBuilder = builder.getPluginConfigurationBuilder()
                            .getResilienceBuilder();
                        switch (configKey) {
                            case "timeout" -> resilienceBuilder.setTimeout(Long.parseLong(value));
                            case "retries" -> resilienceBuilder.setRetries(Integer.parseInt(value));
                        }
                    }
                }
            }

        }

        return builder.build();
    }
}
