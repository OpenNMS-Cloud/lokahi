package org.opennms.horizon.inventory.testtool.miniongateway.wiremock.client;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Message;
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
import org.opennms.icmp.contract.IcmpDetectorRequest;
import org.opennms.icmp.contract.IcmpMonitorRequest;
import org.opennms.node.scan.contract.NodeScanRequest;
import org.opennms.sink.flows.contract.FlowsConfig;
import org.opennms.sink.traps.contract.TrapConfig;
import org.opennms.snmp.contract.SnmpCollectorRequest;
import org.opennms.snmp.contract.SnmpDetectorRequest;
import org.opennms.snmp.contract.SnmpMonitorRequest;
import org.opennms.taskset.contract.MonitorType;
import org.opennms.taskset.contract.TaskDefinition;
import org.opennms.taskset.service.contract.AddSingleTaskOp;
import org.opennms.taskset.service.contract.UpdateSingleTaskOp;
import org.opennms.taskset.service.contract.UpdateTasksRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static org.junit.Assert.*;

public class MinionGatewayWiremockTestSteps {

    public static final String GOOGLE_PROTOBUF_TYPE_PREFIX = "type.googleapis.com/";

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

    private Map<String, Message.Builder> googleTypeUrlMap =
        Map.of(
            GOOGLE_PROTOBUF_TYPE_PREFIX + IcmpMonitorRequest.getDescriptor().getFullName(), IcmpMonitorRequest.newBuilder(),
            GOOGLE_PROTOBUF_TYPE_PREFIX + SnmpMonitorRequest.getDescriptor().getFullName(), SnmpMonitorRequest.newBuilder(),

            GOOGLE_PROTOBUF_TYPE_PREFIX + IcmpDetectorRequest.getDescriptor().getFullName(), IcmpDetectorRequest.newBuilder(),
            GOOGLE_PROTOBUF_TYPE_PREFIX + SnmpDetectorRequest.getDescriptor().getFullName(), SnmpDetectorRequest.newBuilder(),

            GOOGLE_PROTOBUF_TYPE_PREFIX + SnmpCollectorRequest.getDescriptor().getFullName(), SnmpCollectorRequest.newBuilder(),

            GOOGLE_PROTOBUF_TYPE_PREFIX + NodeScanRequest.getDescriptor().getFullName(), NodeScanRequest.newBuilder(),
            GOOGLE_PROTOBUF_TYPE_PREFIX + FlowsConfig.getDescriptor().getFullName(), FlowsConfig.newBuilder(),
            GOOGLE_PROTOBUF_TYPE_PREFIX + TrapConfig.getDescriptor().getFullName(), TrapConfig.newBuilder()
        );


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
    private List<UpdateTasksRequest> restGetTaskSetUpdates() throws MalformedURLException {
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

        // The response must be a JSON array, with JSON object entries.
        List payloadList = restAssuredResponse.getBody().as(List.class);
        List<UpdateTasksRequest> result = new LinkedList<>();

        for (var entry : payloadList) {
            UpdateTasksRequest updateTasksRequest =
                (UpdateTasksRequest) unmarshalMapToProtobuf((Map) entry, UpdateTasksRequest.newBuilder());
            result.add(updateTasksRequest);
        }

        return result;
    }


//========================================
// PROTOBUF JSON DESERIALIZATION
//----------------------------------------

    /**
     * Unmarhsal the given JSON data, in a Map, into the protobuf message for which the builder has been given.
     *
     * @param jsonData JSON content describing the message, which must be a Map.
     * @param protobufBuilder protobuf builder for the message being unmarshalled.
     * @param <T> type of the protobuf message that will be generated by unmarshalling.
     * @return the protobuf message unmarshalled from the given JSON data.
     */
    private <T> T unmarshalMapToProtobuf(Map<String, Object> jsonData, Message.Builder protobufBuilder) {
        try {
            Message.Builder builder = protobufBuilder.clone();

            List<Descriptors.FieldDescriptor> fieldDescriptors = builder.getDescriptorForType().getFields();

            for (Descriptors.FieldDescriptor fieldDescriptor : fieldDescriptors) {
                var fieldName = fieldDescriptor.getName();

                // Process the field only if it exists
                if (jsonData.containsKey(fieldName)) {
                    Object jsonValue = jsonData.get(fieldName);
                    if (fieldDescriptor.isRepeated()) {
                        unmarshalRepeatedField(jsonValue, fieldDescriptor, builder);
                    } else if (fieldDescriptor.getJavaType() == Descriptors.FieldDescriptor.JavaType.ENUM) {
                        unmarshalEnumField(jsonValue, fieldDescriptor, builder);
                    } else if (fieldDescriptor.getJavaType() == Descriptors.FieldDescriptor.JavaType.MESSAGE) {
                        unmarshalProtobufMessageField(jsonValue, fieldDescriptor, builder);
                    } else {
                        // Field is not a protobuf-message or other special type, so set it normally.
                        builder.setField(fieldDescriptor, jsonData.get(fieldDescriptor.getName()));
                    }
                }
            }

            return (T) builder.build();
        } catch (Exception exc) {
            throw new RuntimeException("failed to decode protobuf message from jsonData as a map", exc);
        }
    }

    /**
     * Extract a Protobuf Message field with the given field name and value.
     * @param jsonValue value from the JSON to be unmarshalled into the protobuf field; must be a Map.
     * @param fieldDescriptor protobuf descriptor describing the content of the field.
     * @param builder protobuf message builder for the message that contains the field being unmarshalled.
     */
    private void unmarshalProtobufMessageField(Object jsonValue, Descriptors.FieldDescriptor fieldDescriptor, Message.Builder builder) {
        if (jsonValue instanceof Map) {
            Map mapValue = (Map) jsonValue;

            if (Objects.equals(fieldDescriptor.getFullName(), "google.protobuf.Any")) {
                var any = unmarshalMapToAny(mapValue);
                builder.setField(fieldDescriptor, any);
            } else {
                var message = unmarshalMapToProtobuf(mapValue, builder.getFieldBuilder(fieldDescriptor));
                builder.setField(fieldDescriptor, message);
            }
        } else {
            // JSON value is not a map - throw it
            String typeName = "<null>";
            if (jsonValue != null) {
                typeName = jsonValue.getClass().getName();
            }

            throw new RuntimeException("INVALID protobuf message json-data; must be a map: type=" + typeName);
        }
    }

    private void unmarshalEnumField(Object jsonValue, Descriptors.FieldDescriptor fieldDescriptor, Message.Builder builder) {
        String enumValue = (String) jsonValue;
        var enumValueDescriptor = fieldDescriptor.getEnumType().findValueByName(enumValue);
        builder.setField(fieldDescriptor, enumValueDescriptor);
    }


    /**
     * Extract a repeated field given the JSON value, protobuf field descriptor, and builder.
     *
     * @param jsonValue value to unmarshal from JSON back into the protobuf field.  Must be a List.
     * @param fieldDescriptor protobuf descriptor for the field.
     * @param builder protobuf message builder into which the field's value will be stored.
     */
    /** @noinspection rawtypes*/
    private void unmarshalRepeatedField(Object jsonValue, Descriptors.FieldDescriptor fieldDescriptor, Message.Builder builder) {
        // LIST
        List jsonList = (List) jsonValue;
        List resultList = new LinkedList();

        for (var listEntry : jsonList) {
            if (fieldDescriptor.getJavaType() == Descriptors.FieldDescriptor.JavaType.MESSAGE) {
                Message.Builder fieldBuilder = builder.newBuilderForField(fieldDescriptor);
                var convertedEntry =
                    unmarshalMapToProtobuf((Map) listEntry, fieldBuilder);

                resultList.add(convertedEntry);
            } else if (fieldDescriptor.getJavaType() == Descriptors.FieldDescriptor.JavaType.ENUM) {
                Message.Builder enumFieldBuilder = builder.newBuilderForField(fieldDescriptor);
                unmarshalEnumField(listEntry, fieldDescriptor, enumFieldBuilder);
            } else {
                resultList.add(listEntry);
            }
        }

        builder.setField(fieldDescriptor, resultList);
    }

    /**
     * Unmarshal the given json data in a Map into the associated protobuf Any with the wrapped message contained.
     *
     * NOTE: the nested protobuf message type MUST be registered in the googleTypeUrlMap to be properly decoded; otherwise,
     * the returned Any's content is stored without being unmarshalled first.
     *
     * @param jsonData the JSON data representing the Any field; must be a Map.
     * @return the unmarshalled Any data.
     */
    /** @noinspection rawtypes*/
    private Any unmarshalMapToAny(Map jsonData) {
        Any result;

        String typeUrl = (String) jsonData.get("typeUrl");

        if (jsonData.containsKey("__ANY__")) {
            byte[] raw = Base64.getDecoder().decode((String) jsonData.get("raw"));

            result =
                Any.newBuilder()
                    .setTypeUrl(typeUrl)
                    .setValue(ByteString.copyFrom(raw))
                    .build()
            ;
        } else {
            // Lookup the type and decode it, then pack into an "Any"
            var builder = googleTypeUrlMap.get(typeUrl);

            if (builder != null) {
                GeneratedMessageV3 protobufMessageValue = (GeneratedMessageV3) unmarshalMapToProtobuf(jsonData, builder);
                result = Any.pack(protobufMessageValue);
            } else {
                throw new RuntimeException("Unrecognized protobuf message type: type-url=" + typeUrl);
            }
        }

        return result;
    }
}
