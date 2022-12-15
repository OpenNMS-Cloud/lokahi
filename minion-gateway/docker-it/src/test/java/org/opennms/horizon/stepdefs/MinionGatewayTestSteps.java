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

package org.opennms.horizon.stepdefs;

import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.internal.util.IOUtils;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.opennms.cloud.grpc.minion.CloudServiceGrpc;
import org.opennms.cloud.grpc.minion.CloudToMinionMessage;
import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.cloud.grpc.minion.MinionToCloudMessage;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcRequestServiceGrpc;
import org.opennms.cloud.grpc.minion.RpcResponseProto;
import org.opennms.cloud.grpc.minion.SinkMessage;
import org.opennms.horizon.RetryUtils;
import org.opennms.horizon.grpc.TestCloudServiceRpcRequestHandler;
import org.opennms.horizon.grpc.TestCloudToMinionMessageHandler;
import org.opennms.horizon.grpc.TestEmptyMessageHandler;
import org.opennms.horizon.kafkahelper.KafkaTestHelper;
import org.opennms.taskset.contract.MonitorResponse;
import org.opennms.taskset.contract.MonitorType;
import org.opennms.taskset.contract.TaskDefinition;
import org.opennms.taskset.contract.TaskResult;
import org.opennms.taskset.contract.TaskSet;
import org.opennms.taskset.contract.TaskSetResults;
import org.opennms.taskset.service.contract.PublishTaskSetRequest;
import org.opennms.taskset.service.contract.PublishTaskSetResponse;
import org.opennms.taskset.service.contract.TaskSetServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.opennms.horizon.shared.ipc.rpc.api.RpcModule.MINION_HEADERS_MODULE;

public class MinionGatewayTestSteps {
    public static final int DEFAULT_HTTP_SOCKET_TIMEOUT = 15_000;

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(MinionGatewayTestSteps.class);

    private Logger LOG = DEFAULT_LOGGER;

    //
    // Injected Dependencies
    //
    private RetryUtils retryUtils;
    private KafkaTestHelper kafkaTestHelper;

    //
    // Test Direct-Wired Dependencies
    //
    private TestCloudServiceRpcRequestHandler testCloudServiceRpcRequestHandler =
        new TestCloudServiceRpcRequestHandler(this::handleRpcResponse);
    private TestCloudToMinionMessageHandler testCloudToMinionMessageHandler =
        new TestCloudToMinionMessageHandler();
    private TestEmptyMessageHandler testEmptyMessageHandler =
        new TestEmptyMessageHandler();


    //
    // Test Configuration
    //
    private int internalGrpcPort;
    private int externalGrpcPort;
    private String kafkaBootstrapUrl;
    private String mockLocation;
    private String mockSystemId;
    private String applicationBaseUrl;
    private Map<String, String> grpcHeaders = new TreeMap<>();


    //
    // Test Runtime Data
    //
    private CloudServiceGrpc.CloudServiceStub minionServiceStub;
    private StreamObserver<RpcResponseProto> minionRpcStream;

    private StreamObserver<MinionToCloudMessage> minionToCloudMessageStream;
    private ManagedChannel cloudRpcManagedChannel;

    private RpcRequestProto.Builder rpcRequestProtoBuilder;
    private RpcResponseProto rpcResponseProto;
    private Exception rpcException;
    private PublishTaskSetResponse publishTaskSetResponse;
    private JsonPath parsedJsonResponse;

    private Response restAssuredResponse;
    private ConsumerRecord<String, byte[]> matchedKafkaRecord;

//========================================
// Lifecycle
//========================================

    public MinionGatewayTestSteps(RetryUtils retryUtils, KafkaTestHelper kafkaTestHelper) {
        this.retryUtils = retryUtils;
        this.kafkaTestHelper = kafkaTestHelper;
    }

    @After
    public void cleanupTest() {
        if (minionRpcStream != null) {
            minionRpcStream.onCompleted();
        }

        if (cloudRpcManagedChannel != null) {
            cloudRpcManagedChannel.shutdownNow();
            try {
                cloudRpcManagedChannel.awaitTermination(1000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException interruptedException) {
                LOG.debug("channel shutdown failed", interruptedException);
            }
        }
    }

//========================================
// Gherkin Rules
//========================================


    @Given("^application base url in system property \"([^\"]*)\"$")
    public void applicationBaseUrlInSystemProperty(String systemProperty) throws Throwable {
        applicationBaseUrl = System.getProperty(systemProperty);

        LOG.info("Using BASE URL {}", applicationBaseUrl);
    }

    @Given("External GRPC Port in system property {string}")
    public void externalGRPCPortInSystemProperty(String propertyName) {
        String value = System.getProperty(propertyName);
        externalGrpcPort = Integer.parseInt(value);

        LOG.info("Using EXTERNAL GRPC PORT {}", externalGrpcPort);
    }

    @Given("Internal GRPC Port in system property {string}")
    public void internalGRPCPortInSystemProperty(String propertyName) {
        String value = System.getProperty(propertyName);
        internalGrpcPort = Integer.parseInt(value);

        LOG.info("Using INTERNAL GRPC PORT {}", internalGrpcPort);
    }

    @Given("Kafka Bootstrap URL in system property {string}")
    public void kafkaBootstrapURLInSystemProperty(String systemPropertyName) {
        String value = System.getProperty(systemPropertyName);
        kafkaBootstrapUrl = value;

        this.kafkaTestHelper.setKafkaBootstrapUrl(kafkaBootstrapUrl);

        LOG.info("USING KAFKA BOOTSTRAP URL {}", kafkaBootstrapUrl);
    }

    @Then("^verify JSON path expressions match$")
    public void verifyJsonPathExpressionsMatch(List<String> pathExpressions) {
        for (String onePathExpression : pathExpressions) {
            verifyJsonPathExpressionMatch(parsedJsonResponse, onePathExpression);
        }
    }

    @Given("Generated RPC Request ID")
    public void generatedRPCRequestID() {
        ensureRpcRequest();
        rpcRequestProtoBuilder.setRpcId(UUID.randomUUID().toString());
    }

    @Given("RPC Request System ID {string}")
    public void rpcRequestSystemID(String systemId) {
        ensureRpcRequest();
        rpcRequestProtoBuilder.setSystemId(systemId);
    }

    @Given("RPC Request Location {string}")
    public void rpcRequestLocation(String location) {
        ensureRpcRequest();
        rpcRequestProtoBuilder.setLocation(location);
    }

    @Given("RPC Request Module ID {string}")
    public void rpcRequestModuleID(String moduleId) {
        ensureRpcRequest();
        rpcRequestProtoBuilder.setModuleId(moduleId);
    }

    @Given("RPC Request TTL {int}ms")
    public void rpcRequestTTLMs(int ttl) {
        ensureRpcRequest();
        rpcRequestProtoBuilder.setExpirationTime(System.currentTimeMillis() + ttl);
    }

    @Given("mock system id {string}")
    public void mockSystemId(String mockSystemId) {
        this.mockSystemId = mockSystemId;
    }

    @Given("mock location {string}")
    public void mockLocation(String mockLocation) {
        this.mockLocation = mockLocation;
    }

    @Given("GRPC header {string} = {string}")
    public void grpcHeader(String headerName, String headerValue) {
        grpcHeaders.put(headerName, headerValue);
    }

    @Then("create Cloud RPC connection")
    public void createCloudRPCConnection() {
        NettyChannelBuilder channelBuilder =
            NettyChannelBuilder.forAddress("localhost", externalGrpcPort);

        cloudRpcManagedChannel = channelBuilder.usePlaintext().build();
        cloudRpcManagedChannel.getState(true);

        minionServiceStub = CloudServiceGrpc.newStub(cloudRpcManagedChannel);
        minionRpcStream =
            minionServiceStub
                .withInterceptors(prepareGrpcHeaderInterceptor())
                .cloudToMinionRPC(testCloudServiceRpcRequestHandler)
        ;

        RpcResponseProto rpcHeader = RpcResponseProto.newBuilder()
            .setLocation(mockLocation)
            .setSystemId(mockSystemId)
            .setModuleId(MINION_HEADERS_MODULE)
            .setRpcId(mockSystemId)
            .build();

        minionRpcStream.onNext(rpcHeader);
    }

    @Then("create Cloud-To-Minion Message connection")
    public void createCloudToMinionMessageConnection() {
        NettyChannelBuilder channelBuilder =
            NettyChannelBuilder.forAddress("localhost", externalGrpcPort);

        cloudRpcManagedChannel = channelBuilder.usePlaintext().build();
        cloudRpcManagedChannel.getState(true);

        Identity identity =
            Identity.newBuilder()
                .setLocation("x-location-001-x")
                .setSystemId("x-system-001-x")
                .build();

        minionServiceStub = CloudServiceGrpc.newStub(cloudRpcManagedChannel);
        minionServiceStub
            .withInterceptors(prepareGrpcHeaderInterceptor())
            .cloudToMinionMessages(identity, testCloudToMinionMessageHandler)
        ;
    }

    @Then("create Minion-to-Cloud Message connection")
    public void createMinionToCloudMessageConnection() {
        NettyChannelBuilder channelBuilder =
            NettyChannelBuilder.forAddress("localhost", externalGrpcPort);

        cloudRpcManagedChannel = channelBuilder.usePlaintext().build();
        cloudRpcManagedChannel.getState(true);

        minionServiceStub = CloudServiceGrpc.newStub(cloudRpcManagedChannel);
        minionToCloudMessageStream =
            minionServiceStub
                .withInterceptors(prepareGrpcHeaderInterceptor())
                .minionToCloudMessages(testEmptyMessageHandler)
        ;
    }

    @Then("send task set to the Minion Gateway until successful with timeout {int}ms")
    public void sendTaskSetToTheMinionGatewayUntilSuccessfulWithTimeoutMs(int timeout) throws InterruptedException {
        Exception exc =
            retryUtils.retry(
                () -> {
                    commonSendTaskSet();
                    return rpcException;
                },
                Objects::isNull,
                250,
                timeout,
                new Exception("fail")
            );

        verifyRPCExceptionWasNOTReceived();
    }

    @Then("send RPC Request")
    public void sendRPCRequest() {
        commonSendRpcRequest();
    }

    @Then("send RPC Request until successful with timeout {int}ms")
    public void sendRPCRequestUntilSuccessfulWithTimeoutMs(int timeout) throws InterruptedException {
        Exception exc =
            retryUtils.retry(
                () -> {
                    commonSendRpcRequest();
                    return rpcException;
                },
                Objects::isNull,
                250,
                timeout,
                new Exception("fail")
            );

        verifyRPCExceptionWasNOTReceived();
    }

    @Then("send task set monitor result to the Minion Gateway until successful with timeout {int}ms")
    public void sendTaskSetResultToTheMinionGatewayUntilSuccessfulWithTimeoutMs(int timeout) throws InterruptedException {
        Exception exc =
            retryUtils.retry(
                () -> {
                    commonSendTaskSetMonitorResult();
                    return rpcException;
                },
                Objects::isNull,
                250,
                timeout,
                new Exception("fail")
            );

        assertNull(exc);
    }

    @Then("verify RPC exception was received")
    public void verifyRPCExceptionWasReceived() {
        assertNotNull(rpcException);
    }

    @Then("verify RPC exception was NOT received")
    public void verifyRPCExceptionWasNOTReceived() {
        if (rpcException != null) {
            LOG.error("Unexpected RPC Exception", rpcException);
            fail("Have unexpected RPC Exception");
        }
    }

    @Then("verify RPC exception states active connection could not be found")
    public void verifyRPCExceptionStatesActiveConnectionCouldNotBeFound() {
        LOG.info("RPC exception message: {}", rpcException.getMessage());

        assertTrue(rpcException.getMessage().contains("Could not find active connection"));

        Optional.ofNullable(rpcRequestProtoBuilder.getLocation()).ifPresent(
            location -> assertTrue("expecting exception message to contain location=" + location, rpcException.getMessage().contains("location=" + location)));

        Optional.ofNullable(rpcRequestProtoBuilder.getSystemId()).ifPresent(
            systemId -> assertTrue("expecting exception message to contain systemId=" + systemId, rpcException.getMessage().contains("systemId=" + systemId)));
    }


    @Then("verify RPC request was received by test rpc request server with timeout {int}ms")
    public void verifyRPCRequestWasReceivedByTestRpcRequestServerWithTimeout(int timeout) throws InterruptedException {
        RpcRequestProto request =
            retryUtils.retry(
                () -> this.findReceivedRpcProto(rpcRequestProtoBuilder.getRpcId()),
                Objects::nonNull,
                100,
                timeout,
                null
            );

        assertNotNull("request was received by the test rpc request server (i.e. test stub for minion)", request);
    }

    @Then("verify task set was received by cloud-to-minion message connection with timeout {int}ms")
    public void verifyTaskSetWasReceivedByCloudToMinionMessageConnectionWithTimeoutMs(int timeout) throws InterruptedException {
        CloudToMinionMessage message =
            retryUtils.retry(
                this::findReceivedTaskSetMessage,
                Objects::nonNull,
                100,
                timeout,
                null
            );

        assertNotNull("message was received by the test cloud-to-minion-message server (i.e. test stub for minion)", message);
    }

    @Then("verify task set result was published to Kafka with timeout {int}ms")
    public void verifyTaskSetResultWasPublishedToKafkaWithTimeoutMs(int timeout) throws Exception {
        kafkaTestHelper.startConsumer("task-set.results");

        try {
            List<ConsumerRecord<String, byte[]>> records =
                retryUtils.retry(
                    () -> kafkaTestHelper.getConsumedMessages("task-set.results"),
                    (list) -> ! list.isEmpty(),
                    250,
                    timeout,
                    Collections.EMPTY_LIST
                );

            assertTrue("verify at least 1 record was returned", ! records.isEmpty());

            matchedKafkaRecord = records.get(0);
            TaskSetResults results = TaskSetResults.parseFrom(matchedKafkaRecord.value());
            assertNotNull(results);
            assertEquals(1, results.getResultsCount());

            TaskResult taskResult = results.getResults(0);

            assertNotNull("TaskResult is not null", taskResult);
            assertNotNull("TaskResult contains a monitor response", taskResult.getMonitorResponse());
            assertEquals("127.0.0.1", taskResult.getMonitorResponse().getIpAddress());
            assertEquals("OK", taskResult.getMonitorResponse().getStatus());
            assertEquals(13.999, taskResult.getMonitorResponse().getResponseTimeMs(), 0.00000001);
        } finally {
            kafkaTestHelper.removeConsumer("task-set.results");
        }
    }

    @Then("verify the {string} header on Kafka = {string}")
    public void verifyTheHeaderOnKafka(String headerName, String execpted) {
        String actual = new String(matchedKafkaRecord.headers().lastHeader(headerName).value(), StandardCharsets.UTF_8);
        assertEquals(execpted, actual);
    }


//========================================
// Utility Rules
//----------------------------------------

    @Then("delay {int}ms")
    public void delayMs(int ms) throws Exception {
        Thread.sleep(ms);
    }

//========================================
// Internals
//========================================

    private void commonSendRpcRequest() {
        NettyChannelBuilder channelBuilder =
            NettyChannelBuilder.forAddress("localhost", internalGrpcPort);

        ManagedChannel channel = channelBuilder.usePlaintext().build();
        channel.getState(true);

        RpcRequestServiceGrpc.RpcRequestServiceBlockingStub stub = RpcRequestServiceGrpc.newBlockingStub(channel);

        rpcException = null;
        try {
            RpcRequestProto msg = rpcRequestProtoBuilder.build();
            rpcResponseProto = stub.request(msg);
        } catch (Exception exc) {
            rpcException = exc;
        }
    }

    private void commonSendTaskSet() {
        NettyChannelBuilder channelBuilder = NettyChannelBuilder.forAddress("localhost", internalGrpcPort)
            .keepAliveWithoutCalls(true);

        ManagedChannel channel = channelBuilder.usePlaintext().build();

        try {
            var taskSetServiceStub = TaskSetServiceGrpc.newBlockingStub(channel);

            TaskDefinition taskDefinition =
                TaskDefinition.newBuilder()
                    .setPluginName("x-plugin-name-x")
                    .setId("x-task-id-x")
                    .build();

            TaskSet taskSet =
                TaskSet.newBuilder()
                    .addTaskDefinition(taskDefinition)
                    .build();

            PublishTaskSetRequest publishTaskSetRequest =
                PublishTaskSetRequest.newBuilder()
                    .setTaskSet(taskSet)
                    .setLocation("x-location-001-x")
                    .build();

            rpcException = null;
            try {
                Metadata headers = new Metadata();
                headers.put(Metadata.Key.of("tenant-id", Metadata.ASCII_STRING_MARSHALLER), "opennms-prime");

                publishTaskSetResponse =
                    taskSetServiceStub
                        .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers))
                        .publishTaskSet(publishTaskSetRequest)
                ;
            } catch (Exception exc) {
                rpcException = exc;
            }
        } finally {
            safeChannelShutdown(channel);
        }
    }

    private void commonSendTaskSetMonitorResult() {
        MinionToCloudMessage minionToCloudMessage = prepareTaskSetMonitorResultMessage();
        NettyChannelBuilder channelBuilder = NettyChannelBuilder.forAddress("localhost", internalGrpcPort)
            .keepAliveWithoutCalls(true);

        ManagedChannel channel = channelBuilder.usePlaintext().build();

        try {
            rpcException = null;
            try {
                minionToCloudMessageStream.onNext(minionToCloudMessage);
            } catch (Exception exc) {
                rpcException = exc;
            }
        } finally {
            safeChannelShutdown(channel);
        }
    }

    private void safeChannelShutdown(ManagedChannel channel) {
        channel.shutdownNow();
        try {
            channel.awaitTermination(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException interruptedException) {
            LOG.debug("channel cleanup error", interruptedException);
        }
    }

    private RpcRequestProto findReceivedRpcProto(String requestId) {
        RpcRequestProto[] receivedRequests = testCloudServiceRpcRequestHandler.getReceivedRequestsSnapshot();
        for (RpcRequestProto oneRequest : receivedRequests) {
            if (requestId.equals(oneRequest.getRpcId())) {
                return oneRequest;
            }
        }

        return null;
    }

    private void ensureRpcRequest() {
        if (rpcRequestProtoBuilder == null) {
            rpcRequestProtoBuilder = RpcRequestProto.newBuilder();
        }
    }

    private CloudToMinionMessage findReceivedTaskSetMessage() {
        CloudToMinionMessage[] messages = testCloudToMinionMessageHandler.getReceivedMessagesSnapshot();

        CloudToMinionMessage match =
            Arrays.asList(messages).stream()
                .filter((msg) -> (msg.hasTwinResponse() && msg.getTwinResponse().getConsumerKey().equals("task-set")))
                .findFirst()
                .orElse(null);

        return match;
    }

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

            assertEquals("matching to JSON path " + parts[0], expectedTrimmed, actualTrimmed);
        } else {
            // Just an expression - evaluate as a boolean
            assertTrue("verifying JSON path expression " + pathExpression, jsonPath.getBoolean(pathExpression));
        }
    }

    private String loadResource(String path) throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(path)) {
            byte[] payloadBytes = IOUtils.toByteArray(inputStream);

            return new String(payloadBytes, StandardCharsets.UTF_8);
        }
    }

    private void handleRpcResponse(RpcResponseProto rpcResponseProto) {
        if (minionRpcStream != null) {
            minionRpcStream.onNext(rpcResponseProto);
        } else {
            LOG.error("RPC RESPONSE - cannot send; stream is null");
        }
    }

    private MinionToCloudMessage prepareTaskSetMonitorResultMessage() {
        MonitorResponse monitorResponse =
            MonitorResponse.newBuilder()
                .setMonitorType(MonitorType.ICMP)
                .setResponseTimeMs(13.999)
                .setStatus("OK")
                .setIpAddress("127.0.0.1")
                .build();

        TaskResult taskResult =
            TaskResult.newBuilder()
                .setMonitorResponse(monitorResponse)
                .build();

        TaskSetResults taskSetResults =
            TaskSetResults.newBuilder()
                .addResults(taskResult)
                .build();

        SinkMessage sinkMessage =
            SinkMessage.newBuilder()
                .setModuleId("task-set-result")
                .setContent(taskSetResults.toByteString())
                .build();

        MinionToCloudMessage minionToCloudMessage =
            MinionToCloudMessage.newBuilder()
                .setSinkMessage(sinkMessage)
                .build();

        return minionToCloudMessage;
    }

    private RestAssuredConfig createRestAssuredTestConfig() {
        return RestAssuredConfig.config()
            .httpClient(HttpClientConfig.httpClientConfig()
                .setParam("http.connection.timeout", DEFAULT_HTTP_SOCKET_TIMEOUT)
                .setParam("http.socket.timeout", DEFAULT_HTTP_SOCKET_TIMEOUT)
            );
    }

    private ClientInterceptor prepareGrpcHeaderInterceptor() {
        return MetadataUtils.newAttachHeadersInterceptor(prepareGrpcHeaders());
    }

    private Metadata prepareGrpcHeaders() {
        Metadata result = new Metadata();

        grpcHeaders.forEach((key, value) -> result.put(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER), value));

        return result;
    }
}
