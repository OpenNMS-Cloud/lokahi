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

    // ========================================
    // Lifecycle
    // ========================================
    public MinionCertificateVerifierTestSteps(MinionCertificateVerifierHttpClientUtils clientUtils) {
        this.clientUtils = clientUtils;
    }

    // ========================================
    // Gherkin Rules
    // ========================================
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
        Map<String, List<String>> headers =
                request.orTimeout(timeoutSec, TimeUnit.SECONDS).get();

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
