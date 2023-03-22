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
 ******************************************************************************/

package org.opennms.horizon.alertservice.stepdefs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.UInt64Value;
import com.google.protobuf.util.JsonFormat;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.opennms.horizon.alerts.proto.Alert;
import org.opennms.horizon.alerts.proto.ListAlertsRequest;
import org.opennms.horizon.alerts.proto.ListAlertsResponse;
import org.opennms.horizon.alertservice.AlertGrpcClientUtils;
import org.opennms.horizon.alertservice.RetryUtils;
import org.opennms.horizon.alertservice.kafkahelper.KafkaTestHelper;
import org.opennms.horizon.events.proto.Event;
import org.opennms.horizon.events.proto.EventLog;
import org.opennms.horizon.model.common.proto.Severity;

@Slf4j
public class AlertTestSteps {

    public static final int DEFAULT_HTTP_SOCKET_TIMEOUT = 15_000;

    //
    // Test Injectables
    //
    private RetryUtils retryUtils;
    private KafkaTestHelper kafkaTestHelper;
    private AlertGrpcClientUtils clientUtils;

    //
    // Test Configuration
    //
    private String applicationBaseHttpUrl;
    private String applicationBaseGrpcUrl;
    private String kafkaBootstrapUrl;


    //
    // Test Runtime Data
    //
    private Response restAssuredResponse;
    private JsonPath parsedJsonResponse;
    private List<Alert> alertsFromLastResponse;
    private Alert firstAlertFromLastResponse;
    private Long lastAlertId;
    private String testAlertReductionKey;
    private String eventTopic;
    private String alertTopic;

//========================================
// Constructor
//----------------------------------------

    public AlertTestSteps(RetryUtils retryUtils, KafkaTestHelper kafkaTestHelper, AlertGrpcClientUtils clientUtils) {
        this.retryUtils = retryUtils;
        this.kafkaTestHelper = kafkaTestHelper;
        this.clientUtils = clientUtils;
    }

//========================================
// Gherkin Rules
//========================================

    @Given("Kafka event topic {string}")
    public void createKafkaTopicForEvents(String eventTopic) {
        kafkaTestHelper.startConsumerAndProducer(eventTopic, eventTopic);
        this.eventTopic = eventTopic;
    }

    @Given("Kafka alert topic {string}")
    public void createKafkaTopicForAlerts(String alertTopic) {
        kafkaTestHelper.startConsumerAndProducer(alertTopic, alertTopic);
        this.alertTopic = alertTopic;
    }

    @Given("Application base HTTP URL in system property {string}")
    public void applicationBaseHttpUrlInSystemProperty(String systemProperty) {
        this.applicationBaseHttpUrl = System.getProperty(systemProperty);

        log.info("Using base HTTP URL {}", this.applicationBaseHttpUrl);
    }

    @Given("Application base gRPC URL in system property {string}")
    public void applicationBaseGrpcUrlInSystemProperty(String systemProperty) {
        this.applicationBaseGrpcUrl = System.getProperty(systemProperty);

        log.info("Using base gRPC URL: {}", this.applicationBaseGrpcUrl);
    }

    @Given("Kafka bootstrap URL in system property {string}")
    public void kafkaRestServerURLInSystemProperty(String systemProperty) {
        this.kafkaBootstrapUrl = System.getProperty(systemProperty);
        this.kafkaTestHelper.setKafkaBootstrapUrl(kafkaBootstrapUrl);

        log.info("Using Kafka base URL: {}", this.kafkaBootstrapUrl);
    }

    @Then("Send event with UEI {string} with tenant {string} with node {int}")
    public void sendMessageToKafkaAtTopic(String eventUei, String tenantId, int nodeId) {
        EventLog eventLog = EventLog.newBuilder()
            .setTenantId(tenantId)
            .addEvents(Event.newBuilder()
                .setTenantId(tenantId)
                .setSeverity(Severity.MINOR)
                .setProducedTimeMs(System.currentTimeMillis())
                .setNodeId(nodeId)
                .setUei(eventUei))
            .build();

        kafkaTestHelper.sendToTopic(eventTopic, eventLog.toByteArray());
    }

    @Then("List alerts for tenant {string}, with timeout {int}ms, until JSON response matches the following JSON path expressions")
    public void listAlertsForTenant(String tenantId, int timeout, List<String> jsonPathExpressions) throws Exception {
        Supplier<MessageOrBuilder> call = () -> {
            clientUtils.setTenantId(tenantId);
            ListAlertsResponse listAlertsResponse = clientUtils.getAlertServiceStub()
                .listAlerts(ListAlertsRequest.newBuilder().build());
            alertsFromLastResponse = listAlertsResponse.getAlertsList();
            return listAlertsResponse;
        };
        boolean success = retryUtils.retry(
                () -> this.doRequestThenCheckJsonPathMatch(call, jsonPathExpressions),
                result -> result,
                100,
                timeout,
                false);
        assertTrue("GET request expected to return JSON response matching JSON path expression(s)", success);
    }

    @Then("Delete the alert")
    public void deleteTheAlert() {
        clientUtils.getAlertServiceStub()
            .deleteAlert(UInt64Value.of(firstAlertFromLastResponse.getDatabaseId()))
            .getValue();
    }

    @Then("Acknowledge the alert")
    public void acknowledgeTheAlert() {
        clientUtils.getAlertServiceStub()
            .acknowledgeAlert(UInt64Value.of(firstAlertFromLastResponse.getDatabaseId()));
    }

    @Then("Unacknowledge the alert")
    public void unacknowledgeTheAlert() {
        clientUtils.getAlertServiceStub()
            .unacknowledgeAlert(UInt64Value.of(firstAlertFromLastResponse.getDatabaseId()));
    }

    @Then("Send GET request to application at path {string}, with timeout {int}ms, until JSON response matches the following JSON path expressions")
    public void sendGETRequestToApplicationAtPathUntilJSONResponseMatchesTheFollowingJSONPathExpressions(String path, int timeout, List<String> jsonPathExpressions) throws Exception {
        boolean success =
            retryUtils.retry(
                () -> this.processGetRequestThenCheckJsonPathMatch(path, jsonPathExpressions),
                result -> result,
                100,
                timeout,
                false
            );

        assertTrue("GET request expected to return JSON response matching JSON path expression(s)", success);
    }

    @Then("Verify alert topic has {int} messages with tenant {string}")
    public void verifyTopicContainsTenant(int expectedMessages, String tenant) throws InterruptedException {
         boolean success = retryUtils.retry(
            () -> this.checkNumberOfMessageForOneTenant(tenant, expectedMessages),
            result -> result,
            100,
            10000,
            false);

        assertTrue("Verify alert topic has the right number of message(s)", success);
    }

    @Then("Remember the first alert from the last response")
    public void rememberFirstAlertFromLastResponse() {
        if (alertsFromLastResponse.size() > 0) {
            firstAlertFromLastResponse = alertsFromLastResponse.get(0);
        } else {
            firstAlertFromLastResponse = null;
        }
    }

    @Then("List alerts for tenant {string} with page size {int}, with timeout {int}ms, until JSON response matches the following JSON path expressions")
    public void listAlertsForTenantWithPageSize(String tenantId, int pageSize, int timeout, List<String> jsonPathExpressions) throws InterruptedException {
        Supplier<MessageOrBuilder> call = () -> {
            clientUtils.setTenantId(tenantId);
            ListAlertsResponse listAlertsResponse = clientUtils.getAlertServiceStub()
                .listAlerts(ListAlertsRequest.newBuilder().setPageSize(pageSize).build());
            alertsFromLastResponse = listAlertsResponse.getAlertsList();
            return listAlertsResponse;
        };
        boolean success = retryUtils.retry(
            () -> this.doRequestThenCheckJsonPathMatch(call, jsonPathExpressions),
            result -> result,
            100,
            timeout,
            false);
        assertTrue("GET request expected to return JSON response matching JSON path expression(s)", success);
    }

//========================================
// Internals
//----------------------------------------

    private void verifyJsonPathExpressionMatch(JsonPath jsonPath, String pathExpression) {
        String[] parts = pathExpression.split(" == ", 2);

        if (parts.length == 2) {
            // Expression and value to match - evaluate as a string and compare
            String actualValue = jsonPath.getString(parts[0]);
            String actualTrimmed;

            if (actualValue != null) {
                actualTrimmed = actualValue.trim();
            }  else {
                actualTrimmed = null;
            }

            String expectedTrimmed = parts[1].trim();

            assertEquals("matching to JSON path " + parts[0], expectedTrimmed, actualTrimmed);
        } else {
            // Just an expression - evaluate as a boolean
            assertTrue("verifying JSON path expression " + pathExpression, jsonPath.getBoolean(pathExpression));
        }
    }

    private void commonSendGetRequestToApplication(String path) throws MalformedURLException {
        URL requestUrl = new URL(new URL(this.applicationBaseHttpUrl), path);

        RestAssuredConfig restAssuredConfig = this.createRestAssuredTestConfig();

        RequestSpecification requestSpecification =
            RestAssured
                .given()
                .config(restAssuredConfig);

        restAssuredResponse = requestSpecification
                .get(requestUrl)
                .thenReturn();
    }
    private RestAssuredConfig createRestAssuredTestConfig() {
        return RestAssuredConfig.config()
            .httpClient(HttpClientConfig.httpClientConfig()
                .setParam("http.connection.timeout", DEFAULT_HTTP_SOCKET_TIMEOUT)
                .setParam("http.socket.timeout", DEFAULT_HTTP_SOCKET_TIMEOUT)
            );
    }

    private void commonParseJsonResponse() {
        parsedJsonResponse = JsonPath.from((this.restAssuredResponse.getBody().asString()));
    }

    private boolean processGetRequestThenCheckJsonPathMatch(String path, List<String> jsonPathExpressions) {
        log.debug("running get with check; path={}; json-path-expressions={}", path, jsonPathExpressions);
        try {
            commonSendGetRequestToApplication(path);
            commonParseJsonResponse();

            log.debug("checking json path expressions");
            for (String onePathExpression : jsonPathExpressions) {
                verifyJsonPathExpressionMatch(parsedJsonResponse, onePathExpression);
            }

            log.debug("finished get with check; path={}; json-path-expressions={}", path, jsonPathExpressions);
            return true;
        } catch (Throwable thrown) {    // Assertions extend Error
            throw new RuntimeException(thrown);
        }
    }

    private boolean doRequestThenCheckJsonPathMatch(Supplier<MessageOrBuilder> supplier, List<String> jsonPathExpressions) {
        log.debug("Running request with check; json-path-expressions={}", jsonPathExpressions);
        try {
            var message = supplier.get();
            var messageJson = JsonFormat.printer()
                    .sortingMapKeys().includingDefaultValueFields()
                    .print(message);
            parsedJsonResponse = JsonPath.from(messageJson);
            log.info("Json response: {}", messageJson);
            //commonParseJsonResponse();

            log.debug("Checking json path expressions");
            for (String onePathExpression : jsonPathExpressions) {
                verifyJsonPathExpressionMatch(parsedJsonResponse, onePathExpression);
            }

            log.debug("Finished request with check; json-path-expressions={}", jsonPathExpressions);
            return true;
        } catch (Throwable thrown) { // Assertions extend Error
            throw new RuntimeException(thrown);
        }
    }

    private boolean checkNumberOfMessageForOneTenant(String tenant, int expectedMessages) {
        int foundMessages = 0;
        List<ConsumerRecord<String, byte[]>> records = kafkaTestHelper.getConsumedMessages(alertTopic);
        for (ConsumerRecord<String, byte[]> record: records) {
            if (record.value() == null) {
                continue;
            }
            Alert alert;
            try {
                alert = Alert.parseFrom(record.value());
            } catch (InvalidProtocolBufferException e) {
                throw new RuntimeException(e);
            }

            if (tenant.equals(alert.getTenantId())) {
                foundMessages++;
            }
        }
        return foundMessages == expectedMessages;
    }
}
