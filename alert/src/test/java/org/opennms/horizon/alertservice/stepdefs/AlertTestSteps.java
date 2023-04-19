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

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.JsonFormat;
import io.cucumber.java.After;
import io.cucumber.java.en.Then;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.opennms.horizon.alerts.proto.Alert;
import org.opennms.horizon.alerts.proto.AlertRequest;
import org.opennms.horizon.alerts.proto.Filter;
import org.opennms.horizon.alerts.proto.ListAlertsRequest;
import org.opennms.horizon.alerts.proto.ListAlertsResponse;
import org.opennms.horizon.alerts.proto.Severity;
import org.opennms.horizon.alerts.proto.TimeRangeFilter;
import org.opennms.horizon.alertservice.AlertGrpcClientUtils;
import org.opennms.horizon.alertservice.RetryUtils;
import org.opennms.horizon.alertservice.kafkahelper.KafkaTestHelper;
import org.opennms.horizon.events.proto.Event;
import org.opennms.horizon.events.proto.EventLog;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Slf4j
public class AlertTestSteps {

    public static final int DEFAULT_HTTP_SOCKET_TIMEOUT = 15_000;

    //
    // Test Injectables
    //
    private RetryUtils retryUtils;
    private KafkaTestHelper kafkaTestHelper;
    private AlertGrpcClientUtils clientUtils;
    private BackgroundSteps background;

    //
    // Test Runtime Data
    //
    private Response restAssuredResponse;
    private JsonPath parsedJsonResponse;
    private List<Alert> alertsFromLastResponse;
    private Alert firstAlertFromLastResponse;
    private Long lastAlertId;
    private String testAlertReductionKey;

//========================================
// Constructor
//----------------------------------------

    public AlertTestSteps(RetryUtils retryUtils, KafkaTestHelper kafkaTestHelper, AlertGrpcClientUtils clientUtils, BackgroundSteps bgSteps) {
        this.retryUtils = retryUtils;
        this.kafkaTestHelper = kafkaTestHelper;
        this.clientUtils = clientUtils;
        this.background = bgSteps;
        initKafka();
    }

    private void initKafka() {
        kafkaTestHelper.setKafkaBootstrapUrl(background.getKafkaBootstrapUrl());
        kafkaTestHelper.startConsumerAndProducer(background.getEventTopic(), background.getEventTopic());
        kafkaTestHelper.startConsumerAndProducer(background.getAlertTopic(), background.getAlertTopic());
    }

    @After
    public void cleanData() {
        log.info("clean alert data");
        if(alertsFromLastResponse != null) {
            alertsFromLastResponse.forEach(alert -> {
                clientUtils.getAlertServiceStub()
                    .deleteAlert(AlertRequest.newBuilder().addAlertId(alert.getDatabaseId()).build());
            });
        }
    }

    //========================================
    // Gherkin Rules
    //========================================
    @Then("Send event with UEI {string} with tenant {string} with node {int}")
    public void sendMessageToKafkaAtTopicWithSeverity(String eventUei, String tenantId, int nodeId) {
        EventLog eventLog = EventLog.newBuilder()
            .setTenantId(tenantId)
            .addEvents(Event.newBuilder()
                .setTenantId(tenantId)
                .setProducedTimeMs(System.currentTimeMillis())
                .setNodeId(nodeId)
                .setUei(eventUei))
            .build();

        kafkaTestHelper.sendToTopic(background.getEventTopic(), eventLog.toByteArray(), tenantId);
    }

    @Then("Send event with UEI {string} with tenant {string} with node {int} at {long} minutes ago")
    public void sendMessageToKafkaAtTopicWithMockedTime(String eventUei, String tenantId, int nodeId, long minutes) {
        long current = System.currentTimeMillis();
        long eventTime = current - (minutes * 60000L);

        EventLog eventLog = EventLog.newBuilder()
            .setTenantId(tenantId)
            .addEvents(Event.newBuilder()
                .setTenantId(tenantId)
                .setProducedTimeMs(eventTime)
                .setNodeId(nodeId)
                .setUei(eventUei))
            .build();

        kafkaTestHelper.sendToTopic(background.getEventTopic(), eventLog.toByteArray(), tenantId);
    }

    @Then("Send event with UEI {string} with tenant {string} with node {int} with produced time 23h ago")
    public void sendMessageToKafkaAtTopicYesterday(String eventUei, String tenantId, int nodeId) {
        EventLog eventLog = EventLog.newBuilder()
            .setTenantId(tenantId)
            .addEvents(Event.newBuilder()
                .setTenantId(tenantId)
                .setProducedTimeMs(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(23))
                .setNodeId(nodeId)
                .setUei(eventUei))
            .build();

        kafkaTestHelper.sendToTopic(background.getEventTopic(), eventLog.toByteArray(), tenantId);
    }

    @Then("Send event with UEI {string} with tenant {string} with node {int} with with produced time 1h ago")
    public void sendMessageToKafkaAtTopic1hAgo(String eventUei, String tenantId, int nodeId) {
        EventLog eventLog = EventLog.newBuilder()
            .setTenantId(tenantId)
            .addEvents(Event.newBuilder()
                .setTenantId(tenantId)
                .setProducedTimeMs(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1))
                .setNodeId(nodeId)
                .setUei(eventUei))
            .build();

        kafkaTestHelper.sendToTopic(background.getEventTopic(), eventLog.toByteArray(), tenantId);
    }

    @Then("Send event with UEI {string} with tenant {string} with node {int} with produced time 8 days ago")
    public void sendMessageToKafkaAtTopicLastWeek(String eventUei, String tenantId, int nodeId) {
        EventLog eventLog = EventLog.newBuilder()
            .setTenantId(tenantId)
            .addEvents(Event.newBuilder()
                .setTenantId(tenantId)
                .setProducedTimeMs(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(8))
                .setNodeId(nodeId)
                .setUei(eventUei))
            .build();

        kafkaTestHelper.sendToTopic(background.getEventTopic(), eventLog.toByteArray(), tenantId);
    }

    @Then("Send event with UEI {string} with tenant {string} with node {int} with produced time last month")
    public void sendMessageToKafkaAtTopicLastMonth(String eventUei, String tenantId, int nodeId) {
        EventLog eventLog = EventLog.newBuilder()
            .setTenantId(tenantId)
            .addEvents(Event.newBuilder()
                .setTenantId(tenantId)
                .setProducedTimeMs(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000)
                .setNodeId(nodeId)
                .setUei(eventUei))
            .build();

        kafkaTestHelper.sendToTopic(background.getEventTopic(), eventLog.toByteArray(), tenantId);
    }

    @Then("List alerts for tenant {string}, with timeout {int}ms, until JSON response matches the following JSON path expressions")
    public void listAlertsForTenant(String tenantId, int timeout, List<String> jsonPathExpressions) throws Exception {
        log.info("List for tenant {}, timeout {}ms, data {}", tenantId, timeout, jsonPathExpressions);
        Supplier<MessageOrBuilder> call = () -> {
            clientUtils.setTenantId(tenantId);
            ListAlertsResponse listAlertsResponse = clientUtils.getAlertServiceStub()
                .listAlerts(ListAlertsRequest.newBuilder().setSortBy("id").setSortAscending(true).build());
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

    @Then("List alerts for tenant {string} with hours {long}, with timeout {int}ms, until JSON response matches the following JSON path expressions")
    public void listAlertsForTenantFilteredByTime(String tenantId, long hours, int timeout, List<String> jsonPathExpressions) throws Exception {
        final var request = ListAlertsRequest.newBuilder();
        request.setSortBy("id")
            .setSortAscending(true);
        getTimeRangeFilter(hours, request);
        Supplier<MessageOrBuilder> call = () -> {
            clientUtils.setTenantId(tenantId);
            ListAlertsResponse listAlertsResponse = clientUtils.getAlertServiceStub().listAlerts(request.build());
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
            .deleteAlert(AlertRequest.newBuilder().addAlertId(firstAlertFromLastResponse.getDatabaseId()).build());
    }

    @Then("Acknowledge the alert")
    public void acknowledgeTheAlert() {
        clientUtils.getAlertServiceStub()
            .acknowledgeAlert(AlertRequest.newBuilder().addAlertId(firstAlertFromLastResponse.getDatabaseId()).build());
    }

    @Then("Unacknowledge the alert")
    public void unacknowledgeTheAlert() {
        clientUtils.getAlertServiceStub()
            .unacknowledgeAlert(AlertRequest.newBuilder().addAlertId(firstAlertFromLastResponse.getDatabaseId()).build());
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

    @Then("List alerts for tenant {string} sorted by {string} ascending {string}, with timeout {int}ms, until JSON response matches the following JSON path expressions")
    public void listAlertsForTenantSorted(String tenantId, String filter, String ascending, int timeout, List<String> jsonPathExpressions) throws InterruptedException {
        Supplier<MessageOrBuilder> call = () -> {
            clientUtils.setTenantId(tenantId);
            ListAlertsResponse listAlertsResponse = clientUtils.getAlertServiceStub()
                .listAlerts(ListAlertsRequest.newBuilder().setSortBy(filter).setSortAscending(Boolean.parseBoolean(ascending)).build());
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

    @Then("List alerts for tenant {string} filtered by severity {string}, with timeout {int}ms, until JSON response matches the following JSON path expressions")
    public void listAlertsForTenantFilteredBySeverityWithTimeoutMsUntilJSONResponseMatchesTheFollowingJSONPathExpressions(String tenantId, String severity, int timeout, List<String> jsonPathExpressions) throws InterruptedException {
        Supplier<MessageOrBuilder> call = () -> {
            clientUtils.setTenantId(tenantId);
            ListAlertsResponse listAlertsResponse = clientUtils.getAlertServiceStub()
                .listAlerts(ListAlertsRequest.newBuilder().addFilters(Filter.newBuilder().setSeverity(Severity.valueOf(severity)).build()).build());
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

    @Then("List alerts for tenant {string} filtered by severity {string} and {string}, with timeout {int}ms, until JSON response matches the following JSON path expressions")
    public void listAlertsForTenantFilteredBySeverityWithTimeoutMsUntilJSONResponseMatchesTheFollowingJSONPathExpressions(String tenantId, String severity, String severity2, int timeout, List<String> jsonPathExpressions) throws InterruptedException {
        Supplier<MessageOrBuilder> call = () -> {
            clientUtils.setTenantId(tenantId);
            ListAlertsResponse listAlertsResponse = clientUtils.getAlertServiceStub()
                .listAlerts(ListAlertsRequest.newBuilder()
                    .addFilters(Filter.newBuilder().setSeverity(Severity.valueOf(severity)).build())
                    .addFilters(Filter.newBuilder().setSeverity(Severity.valueOf(severity2)).build())
                    .setSortBy("id").setSortAscending(true).build());
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

    @Then("List alerts for tenant {string} today , with timeout {int}ms, until JSON response matches the following JSON path expressions")
    public void listAlertsForTenantTodayWithTimeoutMsUntilJSONResponseMatchesTheFollowingJSONPathExpressions(String tenantId, int timeout, List<String> jsonPathExpressions) throws InterruptedException {
        final var request = ListAlertsRequest.newBuilder();
        request.setSortBy("id")
            .setSortAscending(true);
        getTimeRangeFilter(LocalTime.MIDNIGHT.until(LocalTime.now(), ChronoUnit.HOURS), request);
        Supplier<MessageOrBuilder> call = () -> {
            clientUtils.setTenantId(tenantId);
            ListAlertsResponse listAlertsResponse = clientUtils.getAlertServiceStub().listAlerts(request.build());
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

    @Then("Count alerts for tenant {string}, assert response is {int}")
    public void countAlertsForTenantWithTimeoutMsUntilJSONResponseMatchesTheFollowingJSONPathExpressions(String tenantId, int expected) {
        clientUtils.setTenantId(tenantId);
        ListAlertsRequest listAlertsRequest = ListAlertsRequest.newBuilder().build();
        var countAlertsResponse = clientUtils.getAlertServiceStub()
            .countAlerts(listAlertsRequest);
        assertEquals(expected, countAlertsResponse.getCount());

    }

    @Then("Count alerts for tenant {string} filtered by severity {string}, assert response is {int}")
    public void countAlertsForTenantFilteredBySeverity(String tenantId, String severity, int expected) {
        clientUtils.setTenantId(tenantId);
        ListAlertsRequest listAlertsRequest = ListAlertsRequest.newBuilder().addFilters(Filter.newBuilder().setSeverity(Severity.valueOf(severity)).build()).build();
        var countAlertsResponse = clientUtils.getAlertServiceStub()
            .countAlerts(listAlertsRequest);
        assertEquals(expected, countAlertsResponse.getCount());
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
        URL requestUrl = new URL(new URL(background.getApplicationBaseHttpUrl()), path);

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
        List<ConsumerRecord<String, byte[]>> records = kafkaTestHelper.getConsumedMessages(background.getAlertTopic());
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
        log.info("Found {} messages for tenant {}", foundMessages, tenant);
        return foundMessages == expectedMessages;
    }

    private static void getTimeRangeFilter(Long hours, ListAlertsRequest.Builder request) {
        Instant nowTime = Instant.now();
        Timestamp nowTimestamp = Timestamp.newBuilder()
            .setSeconds(nowTime.getEpochSecond())
            .setNanos(nowTime.getNano()).build();

        Instant thenTime = nowTime.minus(hours, ChronoUnit.HOURS);
        Timestamp thenTimestamp = Timestamp.newBuilder()
            .setSeconds(thenTime.getEpochSecond())
            .setNanos(thenTime.getNano()).build();

        request.addFilters(Filter.newBuilder().setTimeRange(TimeRangeFilter.newBuilder()
                .setStartTime(thenTimestamp)
                .setEndTime(nowTimestamp))
            .build());
    }
}
