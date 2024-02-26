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
package org.opennms.horizon.alertservice.stepdefs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.JsonFormat;
import io.cucumber.java.en.Then;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
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

@RequiredArgsConstructor
@Slf4j
public class AlertTestSteps {

    public static final int DEFAULT_HTTP_SOCKET_TIMEOUT = 15_000;

    //
    // Test Injectables
    //
    private final TenantSteps tenantSteps;
    private final RetryUtils retryUtils;
    private final KafkaTestHelper kafkaTestHelper;
    private final AlertGrpcClientUtils clientUtils;
    private final BackgroundSteps background;

    //
    // Test Runtime Data
    //
    private Response restAssuredResponse;
    private JsonPath parsedJsonResponse;
    private List<Alert> alertsFromLastResponse;
    private Alert firstAlertFromLastResponse;

    // ========================================
    // Gherkin Rules
    // ========================================
    @Then("List alerts for the tenant, until JSON response matches the following JSON path expressions")
    public void listAlertsForTenant(List<String> expectedJson) throws InterruptedException {
        listAlertsForTenant(
                tenantSteps.getTenantId(), (int) Duration.ofSeconds(5).toMillis(), expectedJson);
    }

    @Then(
            "List alerts for the tenant, with timeout {int}ms, until JSON response matches the following JSON path expressions")
    public void listAlertsForTenant(int timeout, List<String> expectedJson) throws InterruptedException {
        listAlertsForTenant(tenantSteps.getTenantId(), timeout, expectedJson);
    }

    @Then(
            "List alerts for tenant {string}, with timeout {int}ms, until JSON response matches the following JSON path expressions")
    public void listAlertsForTenant(String tenantId, int timeout, List<String> jsonPathExpressions)
            throws InterruptedException {
        var requestBuilder = ListAlertsRequest.newBuilder().setSortBy("id").setSortAscending(true);
        listAlertsForTenant(tenantId, timeout, jsonPathExpressions, requestBuilder);
    }

    @Then(
            "List alerts for the tenant and label {string}, with timeout {int}ms, until JSON response matches the following JSON path expressions")
    public void listAlertsForTenantByNode(String label, int timeout, List<String> jsonPathExpressions)
            throws InterruptedException {
        var filter = Filter.newBuilder().setNodeLabel(label).build();
        var requestBuilder = ListAlertsRequest.newBuilder()
                .addFilters(filter)
                .setSortBy("id")
                .setSortAscending(true);
        listAlertsForTenant(tenantSteps.getTenantId(), timeout, jsonPathExpressions, requestBuilder);
    }

    @Then(
            "List alerts for the tenant with hours {long}, with timeout {int}ms, until JSON response matches the following JSON path expressions")
    public void listAlertsForTenantFilteredByTime(long hours, int timeout, List<String> jsonPathExpressions)
            throws InterruptedException {
        var requestBuilder = ListAlertsRequest.newBuilder().setSortBy("id").setSortAscending(true);
        getTimeRangeFilter(hours, requestBuilder);
        listAlertsForTenant(tenantSteps.getTenantId(), timeout, jsonPathExpressions, requestBuilder);
    }

    @Then("Delete the alert")
    public void deleteTheAlert() {
        clientUtils
                .getAlertServiceStub()
                .deleteAlert(AlertRequest.newBuilder()
                        .addAlertId(firstAlertFromLastResponse.getDatabaseId())
                        .build());
    }

    @Then("Acknowledge the alert")
    public void acknowledgeTheAlert() {
        clientUtils
                .getAlertServiceStub()
                .acknowledgeAlert(AlertRequest.newBuilder()
                        .addAlertId(firstAlertFromLastResponse.getDatabaseId())
                        .build());
    }

    @Then("Unacknowledge the alert")
    public void unacknowledgeTheAlert() {
        clientUtils
                .getAlertServiceStub()
                .unacknowledgeAlert(AlertRequest.newBuilder()
                        .addAlertId(firstAlertFromLastResponse.getDatabaseId())
                        .build());
    }

    @Then(
            "Send GET request to application at path {string}, with timeout {int}ms, until JSON response matches the following JSON path expressions")
    public void sendGETRequestToApplicationAtPathUntilJSONResponseMatchesTheFollowingJSONPathExpressions(
            String path, int timeout, List<String> jsonPathExpressions) throws Exception {
        boolean success = retryUtils.retry(
                () -> this.processGetRequestThenCheckJsonPathMatch(path, jsonPathExpressions),
                result -> result,
                100,
                timeout,
                false);

        assertTrue("GET request expected to return JSON response matching JSON path expression(s)", success);
    }

    @Then("Verify alert topic has {int} messages for the tenant")
    public void verifyTopicContainsTenant(int expectedMessages) throws InterruptedException {
        verifyTopicContainsTenant(expectedMessages, tenantSteps.getTenantId());
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

    @Then(
            "List alerts for the tenant with page size {int}, with timeout {int}ms, until JSON response matches the following JSON path expressions")
    public void listAlertsForTenantWithPageSize(int pageSize, int timeout, List<String> jsonPathExpressions)
            throws InterruptedException {
        var requestBuilder = ListAlertsRequest.newBuilder().setPageSize(pageSize);
        listAlertsForTenant(tenantSteps.getTenantId(), timeout, jsonPathExpressions, requestBuilder);
    }

    @Then(
            "List alerts for the tenant sorted by {string} ascending {string}, with timeout {int}ms, until JSON response matches the following JSON path expressions")
    public void listAlertsForTenantSorted(
            String filter, String ascending, int timeout, List<String> jsonPathExpressions)
            throws InterruptedException {
        var requestBuilder =
                ListAlertsRequest.newBuilder().setSortBy(filter).setSortAscending(Boolean.parseBoolean(ascending));
        listAlertsForTenant(tenantSteps.getTenantId(), timeout, jsonPathExpressions, requestBuilder);
    }

    @Then(
            "List alerts for the tenant filtered by severity {string}, with timeout {int}ms, until JSON response matches the following JSON path expressions")
    public void listAlertsForTenantFilteredBySeverity(String severity, int timeout, List<String> jsonPathExpressions)
            throws InterruptedException {
        Filter filter =
                Filter.newBuilder().setSeverity(Severity.valueOf(severity)).build();
        var requestBuilder = ListAlertsRequest.newBuilder().addFilters(filter);
        listAlertsForTenant(tenantSteps.getTenantId(), timeout, jsonPathExpressions, requestBuilder);
    }

    @Then(
            "List alerts for the tenant filtered by severity {string} and {string}, with timeout {int}ms, until JSON response matches the following JSON path expressions")
    public void listAlertsForTenantFilteredBySeverities(
            String severity, String severity2, int timeout, List<String> jsonPathExpressions)
            throws InterruptedException {
        var requestBuilder = ListAlertsRequest.newBuilder()
                .addFilters(Filter.newBuilder()
                        .setSeverity(Severity.valueOf(severity))
                        .build())
                .addFilters(Filter.newBuilder()
                        .setSeverity(Severity.valueOf(severity2))
                        .build())
                .setSortBy("id")
                .setSortAscending(true);
        listAlertsForTenant(tenantSteps.getTenantId(), timeout, jsonPathExpressions, requestBuilder);
    }

    @Then(
            "List alerts for the tenant today, with timeout {int}ms, until JSON response matches the following JSON path expressions")
    public void listAlertsForTenantToday(int timeout, List<String> jsonPathExpressions) throws InterruptedException {
        final var requestBuilder =
                ListAlertsRequest.newBuilder().setSortBy("id").setSortAscending(true);
        getTimeRangeFilter(LocalTime.MIDNIGHT.until(LocalTime.now(), ChronoUnit.HOURS), requestBuilder);
        listAlertsForTenant(tenantSteps.getTenantId(), timeout, jsonPathExpressions, requestBuilder);
    }

    public void listAlertsForTenant(
            String tenantId, int timeout, List<String> jsonPathExpressions, ListAlertsRequest.Builder requestBuilder)
            throws InterruptedException {
        log.info("List for tenant {}, timeout {}ms, data {}", tenantId, timeout, jsonPathExpressions);
        Supplier<MessageOrBuilder> call = () -> {
            clientUtils.setTenantId(tenantId);
            ListAlertsResponse listAlertsResponse =
                    clientUtils.getAlertServiceStub().listAlerts(requestBuilder.build());
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

    @Then("Count alerts for the tenant, assert response is {int}")
    public void countAlertsForTenantWithTimeoutMsUntilJSONResponseMatchesTheFollowingJSONPathExpressions(int expected) {
        clientUtils.setTenantId(tenantSteps.getTenantId());
        ListAlertsRequest listAlertsRequest = ListAlertsRequest.newBuilder().build();
        var countAlertsResponse = clientUtils.getAlertServiceStub().countAlerts(listAlertsRequest);
        assertEquals(expected, countAlertsResponse.getCount());
    }

    @Then("Count alerts for the tenant, filtered by severity {string}, assert response is {int}")
    public void countAlertsForTenantFilteredBySeverity(String severity, int expected) {
        clientUtils.setTenantId(tenantSteps.getTenantId());
        ListAlertsRequest listAlertsRequest = ListAlertsRequest.newBuilder()
                .addFilters(Filter.newBuilder()
                        .setSeverity(Severity.valueOf(severity))
                        .build())
                .build();
        var countAlertsResponse = clientUtils.getAlertServiceStub().countAlerts(listAlertsRequest);
        assertEquals(expected, countAlertsResponse.getCount());
    }

    // ========================================
    // Internals
    // ----------------------------------------

    private void verifyJsonPathExpressionMatch(JsonPath jsonPath, String pathExpression) {
        String[] parts = pathExpression.split(" == ", 2);

        if (parts.length == 2) {
            // Expression and value to match - evaluate as a string and compare
            String actualValue = jsonPath.getString(parts[0]);
            String actualTrimmed;

            if (actualValue != null) {
                actualTrimmed = actualValue.trim();
            } else {
                actualTrimmed = null;
            }

            String expectedTrimmed = parts[1].trim();
            if ("BLANK".equals(expectedTrimmed)) {
                expectedTrimmed = "";
            }

            assertEquals("matching to JSON path " + parts[0], expectedTrimmed, actualTrimmed);
        } else {
            // Just an expression - evaluate as a boolean
            assertTrue("verifying JSON path expression " + pathExpression, jsonPath.getBoolean(pathExpression));
        }
    }

    private void commonSendGetRequestToApplication(String path) throws MalformedURLException {
        URL requestUrl = new URL(new URL(background.getApplicationBaseHttpUrl()), path);

        RestAssuredConfig restAssuredConfig = this.createRestAssuredTestConfig();

        RequestSpecification requestSpecification = RestAssured.given().config(restAssuredConfig);

        restAssuredResponse = requestSpecification.get(requestUrl).thenReturn();
    }

    private RestAssuredConfig createRestAssuredTestConfig() {
        return RestAssuredConfig.config()
                .httpClient(HttpClientConfig.httpClientConfig()
                        .setParam("http.connection.timeout", DEFAULT_HTTP_SOCKET_TIMEOUT)
                        .setParam("http.socket.timeout", DEFAULT_HTTP_SOCKET_TIMEOUT));
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
        } catch (Throwable thrown) { // Assertions extend Error
            throw new RuntimeException(thrown);
        }
    }

    private boolean doRequestThenCheckJsonPathMatch(
            Supplier<MessageOrBuilder> supplier, List<String> jsonPathExpressions) {
        log.debug("Running request with check; json-path-expressions={}", jsonPathExpressions);
        try {
            var message = supplier.get();
            var messageJson = JsonFormat.printer()
                    .sortingMapKeys()
                    .includingDefaultValueFields()
                    .print(message);
            parsedJsonResponse = JsonPath.from(messageJson);
            log.info("Json response: {}", messageJson);
            // commonParseJsonResponse();

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
        for (ConsumerRecord<String, byte[]> record : records) {
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
                .setNanos(nowTime.getNano())
                .build();

        Instant thenTime = nowTime.minus(hours, ChronoUnit.HOURS);
        Timestamp thenTimestamp = Timestamp.newBuilder()
                .setSeconds(thenTime.getEpochSecond())
                .setNanos(thenTime.getNano())
                .build();

        request.addFilters(Filter.newBuilder()
                .setTimeRange(
                        TimeRangeFilter.newBuilder().setStartTime(thenTimestamp).setEndTime(nowTimestamp))
                .build());
    }
}
