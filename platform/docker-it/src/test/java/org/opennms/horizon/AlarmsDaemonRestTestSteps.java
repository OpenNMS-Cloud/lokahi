/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2012-2021 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2021 The OpenNMS Group, Inc.
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
 ******************************************************************************/

package org.opennms.horizon;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.internal.util.IOUtils;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AlarmsDaemonRestTestSteps {

    public static final int DEFAULT_HTTP_SOCKET_TIMEOUT = 15_000;
    public static final String KEYCLOAK_ADMIN_CLIENT_ID = "admin-cli";
    public static final String KEYCLOAK_ACCOUNT_CLIENT_ID = "account";

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(AlarmsDaemonRestTestSteps.class);

    private Logger log = DEFAULT_LOGGER;

    //
    // Injected Dependencies
    //
    private RetryUtils retryUtils;


    //
    // Test Configuration
    //
    private String databaseUrl;
    private String dbUsername;
    private String dbPassword;

    private String applicationBaseUrl;
    private String acceptEncoding;
    private String contentType;
    private String postPayload;

    private String keycloakUrl;
    private String keycloakAdminUser;
    private String keycloakAdminPassword;
    private String keycloakTestUser;
    private String keycloakTestPassword;
    private String keycloakTestRealm;

    //
    // Test Runtime Data
    //
    private Response restAssuredResponse;
    private JsonPath parsedJsonResponse;
    private String keycloakAdminToken;
    private String keycloakTestUserToken;

//========================================
// Constructor
//========================================

    public AlarmsDaemonRestTestSteps(RetryUtils retryUtils) {
        this.retryUtils = retryUtils;
    }


//========================================
// Gherkin Rules
//========================================


    @Given("^application base url in system property \"([^\"]*)\"$")
    public void applicationBaseUrlInSystemProperty(String systemProperty) throws Throwable {
        this.applicationBaseUrl = System.getProperty(systemProperty);

        this.log.info("Using BASE URL {}", this.applicationBaseUrl);
    }

    @Given("DB url in system property {string}")
    public void dbUrlInSystemProperty(String systemProperty) {
        this.databaseUrl = System.getProperty(systemProperty);

        this.log.info("Using DB URL {}", this.databaseUrl);
    }

    @Given("keycloak server URL in system property {string}")
    public void keycloakServerURLInSystemProperty(String systemProperty) {
        this.keycloakUrl = System.getProperty(systemProperty);

        this.log.info("Using KEYCLOAK URL {}", this.keycloakUrl);
    }

    @Given("keycloak admin user {string} with password {string}")
    public void keycloakAdminUserWithPassword(String adminUsername, String adminPassword) {
        this.keycloakAdminUser = adminUsername;
        this.keycloakAdminPassword = adminPassword;
    }

    @Given("keycloak test user {string} with password {string}")
    public void keycloakTestUserWithPassword(String testUsername, String testPassword) {
        this.keycloakTestUser = testUsername;
        this.keycloakTestPassword = testPassword;
    }

    @Given("keycloak test realm {string}")
    public void keycloakTestRealm(String realm) {
        this.keycloakTestRealm = realm;
    }

    @Then("login admin user with keycloak")
    public void loginAdminUserToKeycloak() throws Exception {
        this.keycloakAdminToken = this.loginToKeycloak(KEYCLOAK_ADMIN_CLIENT_ID, this.keycloakAdminUser, this.keycloakAdminPassword, "master");
    }

    @Then("login test user with keycloak")
    public void loginTestUserToKeycloak() throws Exception {
        this.keycloakTestUserToken = this.loginToKeycloak(KEYCLOAK_ADMIN_CLIENT_ID, this.keycloakTestUser, this.keycloakTestPassword, this.keycloakTestRealm);
    }

    @Then("add keycloak realm {string}")
    public void addKeycloakRealm(String realmName) throws Exception {
        //
        // Format a JSON body using the helper. Structure => { "realm": <realmName> }
        //
        String jsonText = this.formatJsonObjectText(
                (map) -> {
                    map.put("realm", realmName);
                    map.put("enabled", true);
                }
        );
        // Map<String, String> requestObj = new HashMap<>();
        // requestObj.put("realm", realmName);
        //
        // ObjectMapper objectMapper = new ObjectMapper();
        // String jsonText = objectMapper.writeValueAsString(requestObj);

        //
        // Prepare and execute the RestAssured call
        //
        RestAssuredConfig restAssuredConfig = this.createRestAssuredTestConfig();

        RequestSpecification requestSpecification =
                RestAssured
                        .given()
                        .config(restAssuredConfig)
                        .auth()
                        .preemptive()
                        .oauth2(this.keycloakAdminToken)
                        .header("Content-Type", "application/json")
                ;

        URL requestUrl = new URL(new URL(this.keycloakUrl), "/admin/realms");

        Response response =
                requestSpecification
                        .body(jsonText)
                        .post(requestUrl)
                        .thenReturn()
                ;

        // Verify the response
        assertEquals("Failed to add realm: response-text=" + response.getBody().asString(), 201, response.getStatusCode());
    }


    @Then("add keycloak user {string} with password {string} in realm {string}")
    public void addKeycloakUserWithPassword(String username, String password, String realm) throws Exception {
        String jsonText =
                this.formatJsonObjectText(
                        (map) -> {
                            map.put("username", username);

                            Map<String, Object> credMap = new HashMap<>();
                            credMap.put("type", "password");
                            credMap.put("temporary", false);
                            credMap.put("value", password);

                            List<Map<String, Object>> credsList = new LinkedList<>();
                            credsList.add(credMap);

                            map.put("credentials", credsList);
                            map.put("enabled", true);
                        });

        RestAssuredConfig restAssuredConfig = this.createRestAssuredTestConfig();

        RequestSpecification requestSpecification =
                RestAssured
                        .given()
                        .config(restAssuredConfig)
                        .auth()
                        .preemptive()
                        .oauth2(this.keycloakAdminToken)
                        .header("Content-Type", "application/json")
                ;

        URL requestUrl = new URL(new URL(this.keycloakUrl), "/admin/realms/" + realm + "/users");

        Response response =
                requestSpecification
                        .body(jsonText)
                        .post(requestUrl)
                        .thenReturn()
                ;

        // Verify the response
        assertEquals("Failed to add realm: response-text=" + response.getBody().asString(), 201, response.getStatusCode());
    }


    @Given("DB username {string} and password {string}")
    public void dbUsernameAndPassword(String username, String password) {
        this.dbUsername = username;
        this.dbPassword = password;
    }

    @Given("^JSON accept encoding$")
    public void jsonAcceptEncoding() throws Throwable {
        this.acceptEncoding = "application/json";
    }

    @Given("^JSON content type$")
    public void jsonContentType() throws Throwable {
        this.contentType = "application/json";
    }

    @Given("^XML content type$")
    public void xmlContentType() throws Throwable {
        this.contentType = "application/xml";
    }

    @Given("^POST request body in resource \"([^\"]*)\"$")
    public void postRequestBodyInResource(String path) throws Throwable {
        this.postPayload = this.loadResource(path);
    }

    @Then("execute SQL statement {string}")
    public void executeSQLStatement(String sqlStatement) throws SQLException {
        try (Connection connection = DriverManager.getConnection(this.databaseUrl, this.dbUsername, this.dbPassword)) {
            Statement statement = connection.createStatement();
            statement.execute(sqlStatement);
        }
    }

    @Then("send GET request at path {string} with retry timeout {int}")
    public void sendGETRequestAtPath(String path, int retryTimeout) throws Throwable {
        this.restAssuredResponse = this.sendGetRequestWithRetry(path, retryTimeout,
                //
                // Retry the operation until success or timeout
                //
                (response) -> ((response != null) && (response.getStatusCode() != 500) && (response.getStatusCode() != 404)));

        assertNotNull(this.restAssuredResponse);
    }

    @Then("^send POST request at path \"([^\"]*)\"$")
    public void sendPOSTRequestAtPath(String path) throws Throwable {
        URL requestUrl = new URL(new URL(this.applicationBaseUrl), path);

        RestAssuredConfig restAssuredConfig = this.createRestAssuredTestConfig();

        RequestSpecification requestSpecification =
                RestAssured
                        .given()
                        .config(restAssuredConfig)
                        ;

        if (this.keycloakTestUserToken != null) {
            requestSpecification =
                requestSpecification
                        .auth()
                        .preemptive()
                        .oauth2(this.keycloakTestUserToken)
                        ;
        }

        if (this.contentType!= null) {
            requestSpecification =
                    requestSpecification
                            .header("Content-Type", this.contentType)
                            ;
        }

        if (this.acceptEncoding != null) {
            requestSpecification =
                    requestSpecification
                            .header("Accept", this.acceptEncoding)
                            ;
        }

        this.restAssuredResponse =
            requestSpecification
                    .body(this.postPayload)
                    .post(requestUrl)
                    .thenReturn()
            ;

        assertNotNull(this.restAssuredResponse);
    }

    @Then("request non-empty alarm list with retry timeout {int}")
    public void requestAlarmListWithRetryTimeout(int retryTimeout) throws Throwable {
        this.restAssuredResponse = this.sendGetRequestWithRetry("/alarms/list", retryTimeout,
                //
                // Retry the operation until response body has an alarm or timeout
                //
                (response) -> {
                    if (response == null || response.getStatusCode() == 500 || response.getStatusCode() == 404) {
                        return false;
                    }

                    if (response.getStatusCode() == 200) {
                        JsonPath body = JsonPath.from(response.getBody().asString());
                        return (int) body.get("totalCount") > 0;
                    }

                    return false;
                });
    }

    @Then("^verify the response code (\\d+) was returned$")
    public void verifyTheResponseCodeWasReturned (int expectedResponseCode) {
        assertEquals(expectedResponseCode, this.restAssuredResponse.getStatusCode());
    }

    @Then("^parse the JSON response$")
    public void parseTheJsonResponse() {
        this.parsedJsonResponse = JsonPath.from((this.restAssuredResponse.getBody().asString()));
    }

    @Then("^verify JSON path expressions match$")
    public void verifyJsonPathExpressionsMatch(List<String> pathExpressions) {
        for (String onePathExpression : pathExpressions) {
            this.verifyJsonPathExpressionMatch(this.parsedJsonResponse, onePathExpression);
        }
    }

    @Then("verify the response body matches {string}")
    public void verifyTheResponseBodyMatches(String regex) {
        String bodyText = restAssuredResponse.getBody().asString();

        assertTrue(bodyText.matches(regex));
    }

//========================================
// Utility Rules
//----------------------------------------

    @Then("^DEBUG dump the response body$")
    public void debugDumpTheResponseBody() {
        this.log.info("RESPONSE BODY = {}", this.restAssuredResponse.getBody().asString());
    }

    @Then("delay {int}ms")
    public void delayMs(int ms) throws Exception {
        Thread.sleep(ms);
    }

//========================================
// Internals
//========================================

    private String loginToKeycloak(String keycloakClientId, String username, String password, String realm) throws Exception {

//keycloak> 2022-03-25 21:24:58,952 WARN  [org.keycloak.events] (executor-thread-0) type=LOGIN_ERROR, realmId=master, clientId=admin-cli, userId=null, ipAddress=192.168.176.1, error=user_not_found, auth_method=openid-connect, grant_type=password, client_auth_method=client-secret, username=t>

        URL requestUrl = new URL(new URL(this.keycloakUrl), "/realms/" + realm + "/protocol/openid-connect/token");

        RestAssuredConfig restAssuredConfig = this.createRestAssuredTestConfig();

        RequestSpecification requestSpecification =
                RestAssured
                        .given()
                        .config(restAssuredConfig)
                        // .auth()
                        // .preemptive()
                        // .basic(this.keycloakAdminUser, this.keycloakAdminPassword)
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .formParam("client_id", keycloakClientId)
                        .formParam("username", username)
                        .formParam("password", password)
                        .formParam("grant_type", "password")
                        .formParam("scope", "openid")
                ;

        Response response =
                requestSpecification
                        .post(requestUrl)
                        .thenReturn()
                ;

        String adminTokenResponseText = response.getBody().asString();

        log.info("Retrieve token for user {} response: {}", username, adminTokenResponseText);

        JsonPath adminTokenJsonPath = JsonPath.from(adminTokenResponseText);

        String token = adminTokenJsonPath.getString("access_token");

        assertNotNull(token);
        assertFalse(token.isBlank());

        return token;
    }

    private RestAssuredConfig createRestAssuredTestConfig() {
        return RestAssuredConfig.config()
                .httpClient(HttpClientConfig.httpClientConfig()
                        .setParam("http.connection.timeout", DEFAULT_HTTP_SOCKET_TIMEOUT)
                        .setParam("http.socket.timeout", DEFAULT_HTTP_SOCKET_TIMEOUT)
                );
    }

    private void verifyJsonPathExpressionMatch(JsonPath jsonPath, String pathExpression) {
        String[] parts = pathExpression.split(" == ", 2);

        if (parts.length == 2) {
            // Expression and value to match - evaluate as a string and compare
            String actualValue = jsonPath.getString(parts[0]);
            String actualTrimmed = actualValue.trim();

            String expectedTrimmed = parts[1].trim();

            assertEquals("matching to JSON path " + jsonPath, expectedTrimmed, actualTrimmed);
        } else {
            // Just an expression - evaluate as a boolean
            assertTrue("verifying JSON path expression " + pathExpression, jsonPath.getBoolean(pathExpression));
        }
    }

    private String loadResource(String path) throws IOException {
        try (InputStream inputStream = this.getClass().getResourceAsStream(path)) {
            byte[] payloadBytes = IOUtils.toByteArray(inputStream);

            return new String(payloadBytes, StandardCharsets.UTF_8);
        }
    }

    private Response sendGetRequestWithRetry(String path, int retryTimeout, Predicate<Response> completionPredicate) throws Throwable {
        URL requestUrl = new URL(new URL(this.applicationBaseUrl), path);

        RestAssuredConfig restAssuredConfig = this.createRestAssuredTestConfig();

        RequestSpecification requestSpecification =
                RestAssured
                        .given()
                        .config(restAssuredConfig)
                ;

        if (this.keycloakTestUserToken != null) {
            requestSpecification =
                    requestSpecification
                            .auth()
                            .preemptive()
                            .oauth2(this.keycloakTestUserToken)
            ;
        }

        if (this.acceptEncoding != null) {
            requestSpecification =
                    requestSpecification
                            .header("Accept", this.acceptEncoding)
            ;
        }

        final RequestSpecification finalRequestSpecification = requestSpecification;
        Supplier<Response> operation =
                () ->
                        finalRequestSpecification
                                .get(requestUrl)
                                .thenReturn();

        return this.retryUtils.retry(
                        operation,
                        completionPredicate,
                        1000,
                        retryTimeout,
                        null);
    }

    private String formatJsonObjectText(Consumer<Map<String, Object>> fieldAdder) throws Exception {
        Map<String, Object> jsonObject = new HashMap<>();

        fieldAdder.accept(jsonObject);

        ObjectMapper objectMapper = new ObjectMapper();
        String jsonText = objectMapper.writeValueAsString(jsonObject);

        return jsonText;
    }
}
