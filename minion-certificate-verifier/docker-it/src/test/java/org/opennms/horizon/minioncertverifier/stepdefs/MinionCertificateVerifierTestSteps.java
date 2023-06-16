/*
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
 */

package org.opennms.horizon.minioncertverifier.stepdefs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.opennms.horizon.minioncertverifier.MinionCertificateVerifierHttpClientUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinionCertificateVerifierTestSteps {
    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(MinionCertificateVerifierTestSteps.class);

    private final Logger LOG = DEFAULT_LOGGER;

    private final MinionCertificateVerifierHttpClientUtils clientUtils;

    private CompletableFuture<Map<String, List<String>>> request;

    //========================================
    // Lifecycle
    //========================================
    public MinionCertificateVerifierTestSteps(MinionCertificateVerifierHttpClientUtils clientUtils) {
        this.clientUtils = clientUtils;
    }

    //========================================
    // Gherkin Rules
    //========================================
    @Given("External HTTP port in system property {string}")
    public void externalGRPCPortInSystemProperty(String propertyName) {
        clientUtils.externalHttpPortInSystemProperty(propertyName);
    }

    @When("Request with {string} is made")
    public void whenRequestIsMade(String certificateDn) {
        request = clientUtils.validateCertificateData(certificateDn);
    }

    @Then("Within {int}s result headers are:")
    public void sendRequest(long timeoutSec, DataTable table) throws ExecutionException, InterruptedException {
        Map<String, List<String>> headers = request.orTimeout(timeoutSec, TimeUnit.SECONDS).get();

        Map<String, String> dataTable = table.entries().stream()
            .map(map -> Map.entry(map.get("header"), map.get("value")))
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

        for (Entry<String, String> entry : dataTable.entrySet()) {
            String headerName = entry.getKey();
            List<String> headerValues = headers.get(headerName);
            if (headerValues == null) {
                fail("Required header " + headerName + " not found");
            }
            if (headerValues.size() != 1) {
                fail("Required header " + headerName + " have multiple values");
            }

            assertEquals("Header did not match", entry.getValue(), headerValues.get(0));
        }
    }


    @Then("Within {int}s result fails")
    public void verifyFailedRequest(long timeoutSec) throws InterruptedException {
        try {
            request.orTimeout(timeoutSec, TimeUnit.SECONDS).get();
            fail("Request should fail");
        } catch (ExecutionException e) {
            LOG.debug("Expected exception caught", e);
        }
    }
}
