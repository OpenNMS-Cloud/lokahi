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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import org.awaitility.Awaitility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InventoryHttpStepDefinitions {

    public static final int DEFAULT_HTTP_SOCKET_TIMEOUT = 15_000;

    public static final Logger LOG = LoggerFactory.getLogger(InventoryHttpStepDefinitions.class);

    private String baseHttpUrl;

    private String expectedHttpResponseLineRegex;
    private Response restAssuredResponse;

    @Given("Application external http endpoint base url in system property {string}")
    public void applicationExternalHttpEndpointBaseUrlInSystemProperty(String propertyName) {
        baseHttpUrl = System.getProperty(propertyName);

        assertNotNull(baseHttpUrl);
        assertFalse(baseHttpUrl.isEmpty());
    }

    @Given("Expected HTTP response line matching regex {string}")
    public void expectedHTTPResponseLineMatchingRegexTenantStream(String expectedHttpResponseLineRegex) {
        this.expectedHttpResponseLineRegex = expectedHttpResponseLineRegex;
    }

    @Then("Send GET request to application at path {string}, with timeout {int}ms, until successful response matches")
    public void sendGETRequestToApplicationAtPathWithTimeoutMsUntilResponseMatches(String path, int timeout)
            throws MalformedURLException {
        boolean completed = false;
        try {
            Awaitility.await()
                    .atMost(Duration.ofMillis(timeout))
                    .ignoreExceptions()
                    .until(() -> this.attemptGetForMatchingResponse(path));

            completed = true;
        } finally {
            if (completed) {
                LOG.debug(
                        "GET successful; LAST get response text: text={}",
                        restAssuredResponse.getBody().asString());
            } else {
                LOG.info(
                        "GET failed; LAST get response text: text={}",
                        restAssuredResponse.getBody().asString());
            }
        }
    }

    // ========================================
    // Internals
    // ----------------------------------------

    private URL formatHttpUrl(String path) throws MalformedURLException {
        return new URL(new URL(baseHttpUrl), path);
    }

    private boolean attemptGetForMatchingResponse(String path) throws MalformedURLException {
        URL url = formatHttpUrl(path);

        RestAssuredConfig restAssuredConfig = createRestAssuredTestConfig();
        RequestSpecification requestSpecification = RestAssured.given().config(restAssuredConfig);

        restAssuredResponse = requestSpecification.get(url);

        if ((restAssuredResponse.getStatusCode() >= 200) && (restAssuredResponse.getStatusCode() < 300)) {
            return lineInTextMatches(restAssuredResponse.getBody().asString(), expectedHttpResponseLineRegex);
        }

        return false;
    }

    private boolean lineInTextMatches(String text, String regex) {
        String[] lines = text.split("\\r?\\n");

        for (String oneLine : lines) {
            if (oneLine.matches(regex)) {
                return true;
            }
        }

        return false;
    }

    private RestAssuredConfig createRestAssuredTestConfig() {
        return RestAssuredConfig.config()
                .sslConfig(SSLConfig.sslConfig().relaxedHTTPSValidation("SSL"))
                .httpClient(HttpClientConfig.httpClientConfig()
                        .setParam("http.connection.timeout", DEFAULT_HTTP_SOCKET_TIMEOUT)
                        .setParam("http.socket.timeout", DEFAULT_HTTP_SOCKET_TIMEOUT));
    }
}
