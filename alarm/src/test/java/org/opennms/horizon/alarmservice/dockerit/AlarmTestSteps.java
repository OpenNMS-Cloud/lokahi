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

package org.opennms.horizon.alarmservice.dockerit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import javax.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.alarmservice.model.AlarmDTO;
import org.opennms.horizon.alarmservice.model.AlarmSeverity;
import org.opennms.horizon.alarmservice.rest.AlarmCollectionDTO;
import org.opennms.horizon.alarmservice.rest.support.MultivaluedMapImpl;
import org.opennms.horizon.events.proto.AlarmData;
import org.opennms.horizon.events.proto.Event;

@Slf4j
public class AlarmTestSteps {

    public static final int DEFAULT_HTTP_SOCKET_TIMEOUT = 15_000;

    //
    // Test Injectables
    //
    private RetryUtils retryUtils;

    //
    // Test Configuration
    //
    private String applicationBaseUrl;
    private String kafkaRestBaseUrl;

    //
    // Test Runtime Data
    //
    private Response restAssuredResponse;
    private JsonPath parsedJsonResponse;
    private Long lastAlarmId;
    private String testAlarmReductionKey;

//========================================
// Constructor
//----------------------------------------

    public AlarmTestSteps(RetryUtils retryUtils) {
        this.retryUtils = retryUtils;
    }


//========================================
// Gherkin Rules
//========================================

    @Given("Application Base URL in system property {string}")
    public void applicationBaseURLInSystemProperty(String systemProperty) throws Exception {
        this.applicationBaseUrl = System.getProperty(systemProperty);

        log.info("Using BASE URL {}", this.applicationBaseUrl);
    }

    @Given("Kafka Rest Server URL in system property {string}")
    public void kafkaRestServerURLInSystemProperty(String systemProperty) throws Exception {
        this.kafkaRestBaseUrl = System.getProperty(systemProperty);

        log.info("Using KAFKA REST BASE URL {}", this.kafkaRestBaseUrl);
    }

    @Then("Send POST request to application at path {string}")
    public void sendPOSTRequestToApplicationAtPath(String path) throws Exception {
        commonSendPOSTRequestToApplication(path);
    }

    @Then("Send POST request to clear alarm at path {string}")
    public void sendPOSTRequestToClearAlarmAtPath(String path) throws Exception {
        AlarmCollectionDTO alarmCollectionDTO = restAssuredResponse.getBody().as(AlarmCollectionDTO.class);
        List<AlarmDTO> alarmDTOList = alarmCollectionDTO.getAlarms();
        AlarmDTO alarmDTO = findCurrentTestAlarm(alarmDTOList);
        commonSendPOSTRequestToApplication(path+"/"+ alarmDTO.getAlarmId());
    }

    @Then("Send PUT request to add memo at path {string}")
    public void sendPUTRequestToAddMemoAtPath(String path) throws Exception {
        AlarmCollectionDTO alarmCollectionDTO = restAssuredResponse.getBody().as(AlarmCollectionDTO.class);
        List<AlarmDTO> alarmDTOList = alarmCollectionDTO.getAlarms();
        AlarmDTO alarmDTO = findCurrentTestAlarm(alarmDTOList);
        MultivaluedMapImpl multiValuedMap = new MultivaluedMapImpl();
        multiValuedMap.putSingle("body", "blahNobody");
        commonSendPUTRequestWithBodyToApplication(path+"/"+ alarmDTO.getAlarmId(), multiValuedMap);
    }

    @Then("Send POST request to acknowledge alarm at path {string}")
    public void sendPOSTRequestToAckAlarmAtPath(String path) throws Exception {
        AlarmCollectionDTO alarmCollectionDTO = restAssuredResponse.getBody().as(AlarmCollectionDTO.class);
        List<AlarmDTO> alarmDTOList = alarmCollectionDTO.getAlarms();
        AlarmDTO alarmDTO = findCurrentTestAlarm(alarmDTOList);
        commonSendPOSTRequestToApplication(path+"/"+ alarmDTO.getAlarmId()+"/blahUserId");
    }

    @Then("Send POST request to unacknowledge alarm at path {string}")
    public void sendPOSTRequestToUnAckAlarmAtPath(String path) throws Exception {
        AlarmCollectionDTO alarmCollectionDTO = restAssuredResponse.getBody().as(AlarmCollectionDTO.class);
        List<AlarmDTO> alarmDTOList = alarmCollectionDTO.getAlarms();
        AlarmDTO alarmDTO = findCurrentTestAlarm(alarmDTOList);
        commonSendPOSTRequestToApplication(path+"/"+ alarmDTO.getAlarmId());
    }

    @Then("Send POST request to set alarm severity at path {string}")
    public void sendPOSTRequestToSetAlarmSeverityAtPath(String path) throws Exception {
        AlarmCollectionDTO alarmCollectionDTO = restAssuredResponse.getBody().as(AlarmCollectionDTO.class);
        List<AlarmDTO> alarmDTOList = alarmCollectionDTO.getAlarms();
        AlarmDTO alarmDTO = findCurrentTestAlarm(alarmDTOList);
        commonSendPOSTRequestToApplication(path+"/"+ alarmDTO.getAlarmId()+"/1");
    }

    @Then("Send POST request to escalate alarm severity at path {string}")
    public void sendPOSTRequestToEscalateAlarmSeverityAtPath(String path) throws Exception {
        AlarmCollectionDTO alarmCollectionDTO = restAssuredResponse.getBody().as(AlarmCollectionDTO.class);
        List<AlarmDTO> alarmDTOList = alarmCollectionDTO.getAlarms();
        AlarmDTO alarmDTO = findCurrentTestAlarm(alarmDTOList);
        commonSendPOSTRequestToApplication(path+"/"+ alarmDTO.getAlarmId());
    }

    @Then("Send Event message to Kafka at topic {string} with alarm reduction key {string}")
    public void sendMessageToKafkaAtTopic(String topic, String alarmReductionKey) throws Exception {
        URL requestUrl = new URL(new URL(this.kafkaRestBaseUrl), "/topics/" + topic);

        testAlarmReductionKey = alarmReductionKey;

        RestAssuredConfig restAssuredConfig = this.createRestAssuredTestConfig();

        RequestSpecification requestSpecification =
            RestAssured
                .given()
                .config(restAssuredConfig);

        AlarmData alarmData =
            AlarmData.newBuilder()
                .setReductionKey(alarmReductionKey)
                .build()
            ;

        Event event =
            Event.newBuilder()
                .setNodeId(10L)
                .setUei("BlahUEI")
                .setAlarmData(alarmData)
                .build();

        String body = formatKafkaRestProducerMessageBody(event.toByteArray());

        requestSpecification =
            requestSpecification
                .body(body)
                .header("Content-Type", "application/vnd.kafka.binary.v2+json")
                .header("Accept", "application/vnd.kafka.v2+json")
                ;

        restAssuredResponse =
            requestSpecification
                .post(requestUrl)
                .thenReturn()
        ;
    }

    @Then("send GET request to application at path {string}, with timeout {int}ms, until JSON response matches the following JSON path expressions")
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

    @Then("delay")
    public void delay() throws InterruptedException{
        Thread.sleep(10000);
    }

    @Then("Verify the HTTP response code is {int}")
    public void verifyTheHTTPResponseCodeIs(int expectedStatusCode) {
        assertEquals(expectedStatusCode, restAssuredResponse.getStatusCode());
    }

//    @Then("delay {int}ms")
//    public void delayMs(int ms) throws Exception {
//        Thread.sleep(ms);
//    }

    @Then("Send GET request to application at path {string}")
    public void sendGETRequestToApplicationAtPath(String path) throws Exception {
        commonSendGetRequestToApplication(path);
    }

    @Then("Send DELETE request to application at path {string}")
    public void sendDELETERequestToApplicationAtPath(String path) throws Exception {
        log.info("####### sending POST to clear alarm {}", lastAlarmId);
        commonSendDELETERequestToApplication(path+"/"+lastAlarmId);
    }

    @Then("Send DELETE request to remove memo at path {string}")
    public void sendDELETERequestToRemoveMemoAtPath(String path) throws Exception {
        log.info("####### sending POST to remove memo {}", lastAlarmId);
        commonSendDELETERequestToApplication(path+"/"+lastAlarmId);
    }

    @Then("^parse the JSON response$")
    public void parseTheJsonResponse() throws Exception {
        commonParseJsonResponse();
    }

    @Then("Verify JSON path expressions match$")
    public void verifyJsonPathExpressionsMatch(List<String> pathExpressions) throws Exception {
        for (String onePathExpression : pathExpressions) {
            verifyJsonPathExpressionMatch(parsedJsonResponse, onePathExpression);
        }
    }

    @Then("Verify alarm was cleared")
    public void verifyAlarmWasCleared() {
        AlarmCollectionDTO alarmCollectionDTO = restAssuredResponse.getBody().as(AlarmCollectionDTO.class);

        List<AlarmDTO> alarmDTOList = alarmCollectionDTO.getAlarms();
        AlarmDTO alarmDTO = findCurrentTestAlarm(alarmDTOList);

        assertEquals(AlarmSeverity.CLEARED, alarmDTO.getSeverity());
    }

    @Then("Verify alarm was acknowledged")
    public void verifyAlarmWasAcked() {

        AlarmCollectionDTO alarmCollectionDTO = restAssuredResponse.getBody().as(AlarmCollectionDTO.class);
        List<AlarmDTO> alarmDTOList = alarmCollectionDTO.getAlarms();
        AlarmDTO alarmDTO = findCurrentTestAlarm(alarmDTOList);
        assertNotNull(alarmDTO.getAlarmAckTime());
        assertNotNull(alarmDTO.getAlarmAckUser());
    }

    @Then("Verify alarm was unacknowledged")
    public void verifyAlarmWasUnAcked() {

        AlarmCollectionDTO alarmCollectionDTO = restAssuredResponse.getBody().as(AlarmCollectionDTO.class);
        List<AlarmDTO> alarmDTOList = alarmCollectionDTO.getAlarms();
        AlarmDTO alarmDTO = findCurrentTestAlarm(alarmDTOList);
        assertNull(alarmDTO.getAlarmAckTime());
        assertNull(alarmDTO.getAlarmAckUser());
    }

    @Then("Verify alarm severity was escalated")
    public void verifyAlarmWasEscalated() {

        AlarmCollectionDTO alarmCollectionDTO = restAssuredResponse.getBody().as(AlarmCollectionDTO.class);
        List<AlarmDTO> alarmDTOList = alarmCollectionDTO.getAlarms();
        AlarmDTO alarmDTO = findCurrentTestAlarm(alarmDTOList);
        assertTrue(alarmDTO.getSeverity().isGreaterThan(AlarmSeverity.INDETERMINATE));
    }

    @Then("Verify alarm was uncleared")
    public void verifyAlarmWasUncleared() {

        AlarmCollectionDTO alarmCollectionDTO = restAssuredResponse.getBody().as(AlarmCollectionDTO.class);
        List<AlarmDTO> alarmDTOList = alarmCollectionDTO.getAlarms();
        AlarmDTO alarmDTO = findCurrentTestAlarm(alarmDTOList);
        assertEquals(alarmDTO.getLastEventSeverity(), alarmDTO.getSeverity());
    }

    @Then("Remember alarm id")
    public void rememberAlarmId() {
        AlarmCollectionDTO alarmCollectionDTO = restAssuredResponse.getBody().as(AlarmCollectionDTO.class);
        List<AlarmDTO> alarmDTOList = alarmCollectionDTO.getAlarms();
        AlarmDTO alarmDTO = findCurrentTestAlarm(alarmDTOList);
        this.lastAlarmId = alarmDTO.getAlarmId();
    }

//========================================
// Utility Rules
//----------------------------------------

    @Then("^DEBUG dump the response body$")
    public void debugDumpTheResponseBody() {
        log.info("RESPONSE BODY = {}", restAssuredResponse.getBody().asString());
    }

//========================================
// Internals
//----------------------------------------

    private AlarmDTO findCurrentTestAlarm(List<AlarmDTO> alarmList) {
        return
            alarmList
                .stream()
                .filter((alarmDTO) -> Objects.equals(testAlarmReductionKey, alarmDTO.getReductionKey()))
                .findFirst()
                .orElse(null)
            ;
    }

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
        URL requestUrl = new URL(new URL(this.applicationBaseUrl), path);

        RestAssuredConfig restAssuredConfig = this.createRestAssuredTestConfig();

        RequestSpecification requestSpecification =
            RestAssured
                .given()
                .config(restAssuredConfig);

        restAssuredResponse =
            requestSpecification
                .get(requestUrl)
                .thenReturn()
        ;
    }
    private RestAssuredConfig createRestAssuredTestConfig() {
        return RestAssuredConfig.config()
            .httpClient(HttpClientConfig.httpClientConfig()
                .setParam("http.connection.timeout", DEFAULT_HTTP_SOCKET_TIMEOUT)
                .setParam("http.socket.timeout", DEFAULT_HTTP_SOCKET_TIMEOUT)
            );
    }

    private void commonSendPOSTRequestToApplication(String path) throws MalformedURLException {
        URL requestUrl = new URL(new URL(this.applicationBaseUrl), path);

        RestAssuredConfig restAssuredConfig = this.createRestAssuredTestConfig();

        RequestSpecification requestSpecification =
            RestAssured
                .given()
                .config(restAssuredConfig);

        restAssuredResponse =
            requestSpecification
                .post(requestUrl)
                .thenReturn();
    }

    private void commonSendPUTRequestWithBodyToApplication(String path, MultivaluedMapImpl body) throws MalformedURLException {
        URL requestUrl = new URL(new URL(this.applicationBaseUrl), path);

        RestAssuredConfig restAssuredConfig = this.createRestAssuredTestConfig();

        RequestSpecification requestSpecification =
            RestAssured
                .given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new Gson().toJson(body))
                .config(restAssuredConfig);

        restAssuredResponse =
            requestSpecification
                .put(requestUrl)
                .thenReturn();
    }

    private void commonSendDELETERequestToApplication(String path) throws MalformedURLException {
        URL requestUrl = new URL(new URL(this.applicationBaseUrl), path);

        RestAssuredConfig restAssuredConfig = this.createRestAssuredTestConfig();

        RequestSpecification requestSpecification =
            RestAssured
                .given()
                .config(restAssuredConfig);

        restAssuredResponse =
            requestSpecification
                .delete(requestUrl)
                .thenReturn();
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

    private String formatKafkaRestProducerMessageBody(byte[] payload) throws JsonProcessingException {
        KafkaRestRecord record = new KafkaRestRecord();

        byte[] encoded = Base64.getEncoder().encode(payload);
        record.setValue(new String(encoded, StandardCharsets.UTF_8));

        KafkaRestProducerRequest request = new KafkaRestProducerRequest();
        request.setRecords(new KafkaRestRecord[] { record });

        return new ObjectMapper().writeValueAsString(request);
    }

    private static class KafkaRestProducerRequest {
        private KafkaRestRecord[] records;

        public KafkaRestRecord[] getRecords() {
            return records;
        }

        public void setRecords(KafkaRestRecord[] records) {
            this.records = records;
        }
    }

    private static class KafkaRestRecord {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
