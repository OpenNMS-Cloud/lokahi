package org.opennms.horizon.inventory.testtool.miniongateway.wiremock.client;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import com.swrve.ratelimitedlogger.RateLimitedLog;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.IOUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.protocol.HTTP;
import org.awaitility.Awaitility;
import org.opennms.horizon.inventory.testtool.miniongateway.wiremock.api.ProtobufConstants;
import org.opennms.taskset.contract.MonitorType;
import org.opennms.taskset.contract.TaskDefinition;
import org.opennms.taskset.service.contract.AddSingleTaskOp;
import org.opennms.taskset.service.contract.UpdateSingleTaskOp;
import org.opennms.taskset.service.contract.UpdateTasksRequest;
import org.opennms.taskset.service.contract.UpdateTasksRequestList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static org.junit.Assert.*;

public class MinionGatewayWiremockTestSteps {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(MinionGatewayWiremockTestSteps.class);

    private Logger LOG = DEFAULT_LOGGER;
    private RateLimitedLog rateLimitedLog =
        RateLimitedLog
            .withRateLimit(LOG)
            .maxRate(3).every(Duration.ofSeconds(10))
            .build();

    public static int DEFAULT_HTTP_SOCKET_TIMEOUT = 30_000;

    private String baseUrl;

    @Getter
    private MonitorType monitorType;

    @Getter
    private String taskIpAddress;

    @Setter
    private String taskLocation = "Default";

    @Setter
    private long nodeId;

    private Response restAssuredResponse;

//========================================
// Cucumber Step Definitions
//----------------------------------------

    @Given("MOCK Minion Gateway Base URL in system property {string}")
    public void minionGatewayBaseURLInSystemProperty(String systemProperty) {
        baseUrl = System.getProperty(systemProperty);

        LOG.info("Using BASE URL {}", baseUrl);
    }

    @Given("Device Task IP address = {string}")
    public void deviceTaskIPAddress(String ipAddress) {
        this.taskIpAddress = ipAddress;
    }

    @Given("The taskset at location {string}")
    public void theTasksetAtLocation(String taskLocation) {
        this.taskLocation = taskLocation;
    }

    @Given("Monitor Type {string}")
    public void monitorType(String monitorType) {
        switch (monitorType) {
            case "ICMP":
                this.monitorType = MonitorType.ICMP;
                break;

            case "SNMP":
                this.monitorType = MonitorType.SNMP;
                break;

            default:
                throw new RuntimeException("Unrecognized monitor type " + monitorType);
        }
    }

    @Then("verify the task set update is published for device with task suffix {string} within {int}ms")
    public void verifyTheTaskSetUpdateIsPublishedForDeviceWithTaskSuffixWithinMs(String taskNameSuffix, int timeout) {
        commonVerifyTaskSetUpdate(record -> isExpectedDeviceCollectorTaskSetUpdate(record, taskNameSuffix), timeout);
    }

    @Then("verify the task set update is published for device with nodeScan within {int}ms")
    public void verifyTheTaskSetUpdateIsPublishedForDeviceWithNodeScanWithinMs(int timeout) {
        String taskIdPattern = "nodeScan=node_id/" + nodeId;

        commonVerifyTaskSetUpdate((record) -> isMatchingAddTask(record, taskLocation, taskIdPattern), timeout);
    }

    @Then("verify the task set update is published with device monitor within {int}ms")
    public void verifyTheTaskSetUpdateIsPublishedWithDeviceMonitorWithinMs(int timeout) {
        String monitorName = monitorType.toString().toLowerCase() + "-detector";
        commonVerifyTaskSetUpdate(record -> isExpectedDeviceCollectorTaskSetUpdate(record, monitorName), timeout);
    }

    @Then("verify the task set update is published with device collector within {int}ms")
    public void verifyTheTaskSetUpdateIsPublishedWithDeviceCollectorWithinMs(int timeout) {
        String collectorName = monitorType.toString().toLowerCase() + "-collector";
        commonVerifyTaskSetUpdate(record -> isExpectedDeviceCollectorTaskSetUpdate(record, collectorName), timeout);
    }

    @Then("verify the task set update is published with removal of task with suffix {string} within {int}ms")
    public void verifyTheTaskSetUpdateIsPublishedWithRemovalOfTaskWithSuffixWithinMs(String taskSuffix, int timeout) {
        String taskIdPattern = "nodeId:\\d+/ip=" + taskIpAddress + "/" + taskSuffix;
        commonVerifyTaskSetUpdate((record) -> isMatchingRemoveTask(record, taskLocation, taskIdPattern), timeout);
    }

    @Then("verify the task set update is published for icmp discovery within {int}ms")
    public void verifyTheTaskSetUpdateIsPublishedForIcmpDiscoveryWithinMs(int timeout) {
        String taskIdPattern = "discovery:\\d+/" + taskLocation;
        commonVerifyTaskSetUpdate((record) -> isMatchingAddTask(record, taskLocation, taskIdPattern), timeout);
    }


//========================================
// Internals
//----------------------------------------

    private boolean isMatchingAddTask(UpdateTasksRequest record, String location,  String taskIdPattern) {
        if (Objects.equals(record.getLocation(), location)) {
            for (var update : record.getUpdateList()) {
                if (update.hasAddTask()) {
                    if (update.getAddTask().hasTaskDefinition()) {
                        return update.getAddTask().getTaskDefinition().getId().matches(taskIdPattern);
                    }
                }
            }
        }

        return false;
    }

    private boolean isMatchingRemoveTask(UpdateTasksRequest record, String location, String taskIdPattern) {
        if (Objects.equals(record.getLocation(), location)) {
            for (var update : record.getUpdateList()) {
                if (update.hasRemoveTask()) {
                    if (update.getRemoveTask().getTaskId().matches(taskIdPattern)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private void commonVerifyTaskSetUpdate(Predicate<UpdateTasksRequest> predicate, int timeout) {
        // Wait up until the timeout for the Mock Minion Gateway to report it has received a task set update that
        //  matches the given predicate, checking once every 0.1 second.
        List<UpdateTasksRequest> updateList =
            Awaitility.await()
                .ignoreExceptions()
                .timeout(timeout, TimeUnit.MILLISECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(
                    this::restGetTaskSetUpdates,
                    (candidateUpdateList) -> candidateUpdateList.stream().anyMatch(predicate)
                )
            ;

        boolean matched = updateList.stream().anyMatch(predicate);

        assertTrue(matched);
    }

    private String readResourceFile(String path) throws Exception {
        String content = "";

        try (InputStream inputStream = this.getClass().getResourceAsStream(path)) {
            content = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        }

        return content;
    }

    private RestAssuredConfig createRestAssuredTestConfig() {
        return RestAssuredConfig.config()
            .httpClient(HttpClientConfig.httpClientConfig()
                .setParam("http.connection.timeout", DEFAULT_HTTP_SOCKET_TIMEOUT)
                .setParam("http.socket.timeout", DEFAULT_HTTP_SOCKET_TIMEOUT)
            );
    }

    private boolean isExpectedDeviceCollectorTaskSetUpdate(UpdateTasksRequest record, String collectorName) {
        return commonIsExpectedDeviceTaskSetUpdate(
            record,
            updateSingleTaskOp -> commonIsExpectedDeviceUpdateSingleTaskOp(updateSingleTaskOp, collectorName));
    }

    private boolean commonIsExpectedDeviceTaskSetUpdate(UpdateTasksRequest record, Predicate<UpdateSingleTaskOp> predicate) {
        List<UpdateSingleTaskOp> updates = record.getUpdateList();
        Optional<UpdateSingleTaskOp> matchedOp = updates.stream().filter(predicate).findAny();

        return (matchedOp.isPresent());
    }

    private boolean commonIsExpectedDeviceUpdateSingleTaskOp(UpdateSingleTaskOp updateSingleTaskOp, String name) {
        if (updateSingleTaskOp.hasAddTask()) {
            AddSingleTaskOp addSingleTaskOp = updateSingleTaskOp.getAddTask();
            TaskDefinition addTaskDefinition = addSingleTaskOp.getTaskDefinition();

            // Verify the ID matches
            if (addTaskDefinition.getId().matches("nodeId:\\d+/ip=" + taskIpAddress + "/" + name)) {
                return true;
            }
        }

        return false;
    }

    private URL formatUrl(String path) throws MalformedURLException {
        return new URL(new URL(baseUrl), path);
    }

    /**
     * Retrieve the current list of Task Set Updates known to the Mock via it's HTTP endpoint.
     * @return the list of task set updates known to the mock.
     * @throws MalformedURLException
     */
    /** @noinspection rawtypes*/
    private List<UpdateTasksRequest> restGetTaskSetUpdates() throws MalformedURLException, InvalidProtocolBufferException {
        URL requestUrl = formatUrl("/api/taskset/updates");

        RestAssuredConfig restAssuredConfig = createRestAssuredTestConfig();

        RequestSpecification requestSpecification =
            RestAssured
                .given()
                .config(restAssuredConfig)
            ;

        restAssuredResponse =
            requestSpecification
                .header(HTTP.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
                .get(requestUrl)
                .thenReturn()
        ;

        rateLimitedLog.info("MOCK get TaskSet updates: status-code={}; body={}", restAssuredResponse.getStatusCode(), restAssuredResponse.getBody().asString());

        String jsonText = restAssuredResponse.getBody().asString();

        // Unmarshal from the JSON text back into an UpdateTasksRequestList
        UpdateTasksRequestList.Builder builder = UpdateTasksRequestList.newBuilder();
        unmarshalJsonToProtobufBuilder(jsonText, builder);
        UpdateTasksRequestList updateTasksRequestList = builder.build();

        // Return the list of UpdateTaskRequest
        return updateTasksRequestList.getUpdateTasksRequestList();
    }


//========================================
// Internals
//----------------------------------------

    private void unmarshalJsonToProtobufBuilder(String jsonText, Message.Builder builder) throws InvalidProtocolBufferException {
        JsonFormat.TypeRegistry typeRegistry =
            JsonFormat.TypeRegistry.newBuilder()
                .add(ProtobufConstants.PROTOBUF_TYPE_LIST)
                .build();

        JsonFormat.parser().usingTypeRegistry(typeRegistry).merge(jsonText, builder);
    }


}
