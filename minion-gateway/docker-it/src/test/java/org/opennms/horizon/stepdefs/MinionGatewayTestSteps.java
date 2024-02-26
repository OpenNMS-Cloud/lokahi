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
package org.opennms.horizon.stepdefs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.opennms.horizon.shared.ipc.rpc.api.RpcModule.MINION_HEADERS_MODULE;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.opennms.cloud.grpc.minion.CloudServiceGrpc;
import org.opennms.cloud.grpc.minion.CloudToMinionMessage;
import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.cloud.grpc.minion.MinionToCloudMessage;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcResponseProto;
import org.opennms.cloud.grpc.minion.SinkMessage;
import org.opennms.cloud.grpc.minion.TwinRequestProto;
import org.opennms.cloud.grpc.minion.TwinResponseProto;
import org.opennms.cloud.grpc.minion_gateway.GatewayRpcRequestProto;
import org.opennms.cloud.grpc.minion_gateway.GatewayRpcResponseProto;
import org.opennms.cloud.grpc.minion_gateway.RpcRequestServiceGrpc;
import org.opennms.horizon.RetryUtils;
import org.opennms.horizon.grpc.TestCloudServiceRpcRequestHandler;
import org.opennms.horizon.grpc.TestCloudToMinionMessageHandler;
import org.opennms.horizon.grpc.TestEmptyMessageHandler;
import org.opennms.horizon.kafkahelper.KafkaTestHelper;
import org.opennms.taskset.contract.MonitorResponse;
import org.opennms.taskset.contract.MonitorType;
import org.opennms.taskset.contract.TaskDefinition;
import org.opennms.taskset.contract.TaskResult;
import org.opennms.taskset.contract.TaskSetResults;
import org.opennms.taskset.contract.TenantLocationSpecificTaskSetResults;
import org.opennms.taskset.service.contract.AddSingleTaskOp;
import org.opennms.taskset.service.contract.TaskSetServiceGrpc;
import org.opennms.taskset.service.contract.UpdateSingleTaskOp;
import org.opennms.taskset.service.contract.UpdateTasksRequest;
import org.opennms.taskset.service.contract.UpdateTasksResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private TestCloudToMinionMessageHandler testCloudToMinionMessageHandler = new TestCloudToMinionMessageHandler();
    private TestEmptyMessageHandler testEmptyMessageHandler = new TestEmptyMessageHandler();

    //
    // Test Configuration
    //
    private int internalGrpcPort;
    private int externalGrpcPort;
    private String kafkaBootstrapUrl;
    private String mockLocationId;
    private String mockSystemId;
    private String applicationBaseUrl;
    private String mockTenantId;

    //
    // Test Runtime Data
    //
    private CloudServiceGrpc.CloudServiceStub minionServiceStub;
    private StreamObserver<RpcResponseProto> minionRpcStream;

    private StreamObserver<MinionToCloudMessage> minionToCloudMessageStream;
    private CloudServiceGrpc.CloudServiceFutureStub minionToCloudRpcStub;
    private ManagedChannel cloudRpcManagedChannel;

    private GatewayRpcRequestProto.Builder gatewayRpcRequestProtoBuilder;
    private RpcRequestProto.Builder rpcRequestProtoBuilder;
    private GatewayRpcResponseProto
            internalRpcResponseProto; // Cloud-internal: between gateway and other cloud services
    private RpcResponseProto externalRpcResponseProto; // Cloud-external: sent to the Minion
    private Exception rpcException;
    private UpdateTasksResponse updateTasksResponse;

    private ConsumerRecord<String, byte[]> matchedKafkaRecord;

    // ========================================
    // Lifecycle
    // ========================================

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

    // ========================================
    // Gherkin Rules
    // ========================================

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

    @Given("Generated RPC Request ID")
    public void generatedRPCRequestID() {
        ensureRpcRequest();

        String requestId = UUID.randomUUID().toString();
        gatewayRpcRequestProtoBuilder.setRpcId(requestId);
        rpcRequestProtoBuilder.setRpcId(requestId);
    }

    @Given("RPC Request System ID {string}")
    public void rpcRequestSystemID(String systemId) {
        ensureRpcRequest();
        gatewayRpcRequestProtoBuilder.getIdentityBuilder().setSystemId(systemId);
        rpcRequestProtoBuilder.getIdentityBuilder().setSystemId(systemId);
    }

    @Given("RPC Request Tenant ID {string}")
    public void rpcRequestTenantID(String tenantId) {
        ensureRpcRequest();
        gatewayRpcRequestProtoBuilder.getIdentityBuilder().setTenantId(tenantId);
    }

    @Given("RPC Request Location ID {string}")
    public void rpcRequestLocation(String location) {
        ensureRpcRequest();
        gatewayRpcRequestProtoBuilder.getIdentityBuilder().setLocationId(location);
    }

    @Given("RPC Request Module ID {string}")
    public void rpcRequestModuleID(String moduleId) {
        ensureRpcRequest();
        gatewayRpcRequestProtoBuilder.setModuleId(moduleId);
        rpcRequestProtoBuilder.setModuleId(moduleId);
    }

    @Given("RPC Request TTL {int}ms")
    public void rpcRequestTTLMs(int ttl) {
        ensureRpcRequest();
        gatewayRpcRequestProtoBuilder.setExpirationTime(System.currentTimeMillis() + ttl);
        rpcRequestProtoBuilder.setExpirationTime(System.currentTimeMillis() + ttl);
    }

    @Given("mock system id {string}")
    public void mockSystemId(String mockSystemId) {
        this.mockSystemId = mockSystemId;
    }

    @Given("mock location ID {string}")
    public void mockLocation(String mockLocation) {
        this.mockLocationId = mockLocation;
    }

    @Given("mock tenant ID {string}")
    public void tenantID(String tenantId) {
        this.mockTenantId = tenantId;
    }

    @Then("create Cloud RPC connection")
    public void createCloudRPCConnection() {
        NettyChannelBuilder channelBuilder = NettyChannelBuilder.forAddress("localhost", externalGrpcPort);

        cloudRpcManagedChannel = channelBuilder.usePlaintext().build();
        cloudRpcManagedChannel.getState(true);

        minionServiceStub = CloudServiceGrpc.newStub(cloudRpcManagedChannel);
        minionRpcStream = minionServiceStub
                .withInterceptors(prepareGrpcHeaderInterceptor())
                .cloudToMinionRPC(testCloudServiceRpcRequestHandler);

        RpcResponseProto rpcHeader = RpcResponseProto.newBuilder()
                .setIdentity(Identity.newBuilder().setSystemId(mockSystemId).build())
                .setModuleId(MINION_HEADERS_MODULE)
                .setRpcId(mockSystemId)
                .build();

        minionRpcStream.onNext(rpcHeader);
    }

    @Then("create Cloud-To-Minion Message connection")
    public void createCloudToMinionMessageConnection() {
        NettyChannelBuilder channelBuilder = NettyChannelBuilder.forAddress("localhost", externalGrpcPort);

        cloudRpcManagedChannel = channelBuilder.usePlaintext().build();
        cloudRpcManagedChannel.getState(true);

        Identity identity = Identity.newBuilder().setSystemId("x-system-001-x").build();

        minionServiceStub = CloudServiceGrpc.newStub(cloudRpcManagedChannel);
        minionServiceStub
                .withInterceptors(prepareGrpcHeaderInterceptor())
                .cloudToMinionMessages(identity, testCloudToMinionMessageHandler);
    }

    @Then("create Minion-to-Cloud Message connection")
    public void createMinionToCloudMessageConnection() {
        NettyChannelBuilder channelBuilder = NettyChannelBuilder.forAddress("localhost", externalGrpcPort);

        cloudRpcManagedChannel = channelBuilder.usePlaintext().build();
        cloudRpcManagedChannel.getState(true);

        minionServiceStub = CloudServiceGrpc.newStub(cloudRpcManagedChannel);
        minionToCloudMessageStream = minionServiceStub
                .withInterceptors(prepareGrpcHeaderInterceptor())
                .minionToCloudMessages(testEmptyMessageHandler);
    }

    @Then("create Minion-to-Cloud RPC Request connection")
    public void createMinionToCloudRPCRequestConnection() {
        NettyChannelBuilder channelBuilder = NettyChannelBuilder.forAddress("localhost", externalGrpcPort);

        cloudRpcManagedChannel = channelBuilder.usePlaintext().build();
        cloudRpcManagedChannel.getState(true);

        minionServiceStub = CloudServiceGrpc.newStub(cloudRpcManagedChannel);
        minionToCloudRpcStub = CloudServiceGrpc.newFutureStub(cloudRpcManagedChannel);
    }

    @Then("send task set to the Minion Gateway until successful with timeout {int}ms")
    public void sendTaskSetToTheMinionGatewayUntilSuccessfulWithTimeoutMs(int timeout) throws InterruptedException {
        long expirationNanoTime = System.nanoTime() + (((long) timeout) * 1_000_000L);

        Exception exc = retryUtils.retry(
                () -> {
                    long remainingTimeoutMs = (expirationNanoTime - System.nanoTime()) / 1_000_000;
                    if (remainingTimeoutMs >= 1) {
                        commonSendTaskSet(remainingTimeoutMs);
                    }
                    return rpcException;
                },
                Objects::isNull,
                250,
                timeout,
                new Exception("fail"));

        verifyRPCExceptionWasNOTReceived();
    }

    @Then("send RPC Request with timeout {int}ms")
    public void sendRPCRequest(long timeout) {
        commonSendRpcRequest(timeout);
    }

    @Then("send RPC Request until successful with timeout {int}ms")
    public void sendRPCRequestUntilSuccessfulWithTimeoutMs(int timeout) throws InterruptedException {
        long expirationNanoTime = System.nanoTime() + (((long) timeout) * 1_000_000L);

        Exception exc = retryUtils.retry(
                () -> {
                    long remainingTimeoutMs = (expirationNanoTime - System.nanoTime()) / 1_000_000;
                    if (remainingTimeoutMs >= 1) {
                        commonSendRpcRequest(remainingTimeoutMs);
                    }
                    return rpcException;
                },
                Objects::isNull,
                250,
                timeout,
                new Exception("fail"));

        verifyRPCExceptionWasNOTReceived();
    }

    @Then("send task set monitor result to the Minion Gateway until successful with timeout {int}ms")
    public void sendTaskSetResultToTheMinionGatewayUntilSuccessfulWithTimeoutMs(int timeout)
            throws InterruptedException {
        Exception exc = retryUtils.retry(
                () -> {
                    commonSendTaskSetMonitorResult();
                    return rpcException;
                },
                Objects::isNull,
                250,
                timeout,
                new Exception("fail"));

        assertNull(exc);
    }

    @Then("send twin update request to the Minion Gateway until successful with timeout {int}ms")
    public void sendTwinUpdateRequestToTheMinionGatewayUntilSuccessfulWithTimeoutMs(int timeout)
            throws InterruptedException {
        long expirationNanoTime = System.nanoTime() + (((long) timeout) * 1_000_000L);
        Exception exc = retryUtils.retry(
                () -> {
                    long remainingTimeoutMs = (expirationNanoTime - System.nanoTime()) / 1_000_000;
                    if (remainingTimeoutMs >= 1) {
                        commonSendTwinRegistrationRequest(remainingTimeoutMs);
                    }

                    return rpcException;
                },
                Objects::isNull,
                250,
                timeout,
                new Exception("fail"));

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

        Optional.ofNullable(mockLocationId)
                .ifPresent(location -> assertTrue(
                        "expecting exception message to contain " + location,
                        rpcException.getMessage().contains(location)));

        String systemId = gatewayRpcRequestProtoBuilder.getIdentity().getSystemId();
        assertTrue(
                "expecting exception message to contain " + systemId,
                rpcException.getMessage().contains(systemId));
    }

    @Then("verify RPC exception indicates missing connection for minion")
    public void verifyRPCExceptionIndicatesMissingConnectionForMinion() {
        LOG.info("RPC exception message: {}", rpcException.getMessage());

        assertTrue(rpcException.getMessage().contains("Could not find active connection for"));
    }

    @Then("verify RPC request was received by test rpc request server with timeout {int}ms")
    public void verifyRPCRequestWasReceivedByTestRpcRequestServerWithTimeout(int timeout) throws InterruptedException {
        RpcRequestProto request = retryUtils.retry(
                () -> this.findReceivedRpcProto(gatewayRpcRequestProtoBuilder.getRpcId()),
                Objects::nonNull,
                100,
                timeout,
                null);

        assertNotNull("request was received by the test rpc request server (i.e. test stub for minion)", request);
    }

    @Then("verify task set was received by cloud-to-minion message connection with timeout {int}ms")
    public void verifyTaskSetWasReceivedByCloudToMinionMessageConnectionWithTimeoutMs(int timeout)
            throws InterruptedException {
        CloudToMinionMessage message =
                retryUtils.retry(this::findReceivedTaskSetMessage, Objects::nonNull, 100, timeout, null);

        assertNotNull(
                "message was received by the test cloud-to-minion-message server (i.e. test stub for minion)", message);
    }

    @Then("verify task set result was published to Kafka with tenant id = {string} and timeout {int}ms")
    public void verifyTaskSetResultWasPublishedToKafkaWithTenantIdAndTimeoutMs(String expectedTenantId, int timeout)
            throws Exception {
        kafkaTestHelper.startConsumer("task-set.results");

        try {
            List<ConsumerRecord<String, byte[]>> records = retryUtils.retry(
                    () -> kafkaTestHelper.getConsumedMessages("task-set.results"),
                    (list) -> !list.isEmpty(),
                    250,
                    timeout,
                    Collections.EMPTY_LIST);

            assertTrue("verify at least 1 record was returned", !records.isEmpty());

            matchedKafkaRecord = records.get(0);
            TenantLocationSpecificTaskSetResults results =
                    TenantLocationSpecificTaskSetResults.parseFrom(matchedKafkaRecord.value());
            assertNotNull(results);
            assertEquals(1, results.getResultsCount());
            assertEquals(expectedTenantId, results.getTenantId());

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

    @Then("verify twin update response was received from the Minion Gateway")
    public void verifyTwinUpdateResponseWasReceivedFromTheMinionGateway() throws InvalidProtocolBufferException {
        assertNotNull(externalRpcResponseProto);

        assertEquals("twin", externalRpcResponseProto.getModuleId());
        assertEquals(gatewayRpcRequestProtoBuilder.getRpcId(), externalRpcResponseProto.getRpcId());

        // Check for a TwinResponseProto payload
        Any payload = externalRpcResponseProto.getPayload();
        assertTrue("Twin response payload must be a TwinResponseProto", payload.is(TwinResponseProto.class));

        TwinResponseProto twinResponseProto = payload.unpack(TwinResponseProto.class);
        assertEquals("x-consumer-key-x", twinResponseProto.getConsumerKey());
    }

    // ========================================
    // Utility Rules
    // ----------------------------------------

    @Then("delay {int}ms")
    public void delayMs(int ms) throws Exception {
        Thread.sleep(ms);
    }

    // ========================================
    // Internals
    // ========================================

    private void commonSendRpcRequest(long timeout) {
        NettyChannelBuilder channelBuilder = NettyChannelBuilder.forAddress("localhost", internalGrpcPort);

        ManagedChannel channel = channelBuilder.usePlaintext().build();
        channel.getState(true);

        RpcRequestServiceGrpc.RpcRequestServiceFutureStub stub = RpcRequestServiceGrpc.newFutureStub(channel);

        rpcException = null;
        try {
            GatewayRpcRequestProto msg = gatewayRpcRequestProtoBuilder.build();

            ListenableFuture<GatewayRpcResponseProto> future =
                    stub.withInterceptors(prepareGrpcHeaderInterceptor()).request(msg);

            internalRpcResponseProto = future.get(timeout, TimeUnit.MILLISECONDS);
        } catch (Exception exc) {
            rpcException = exc;
        }
    }

    private void commonSendTaskSet(long timeout) {
        NettyChannelBuilder channelBuilder =
                NettyChannelBuilder.forAddress("localhost", internalGrpcPort).keepAliveWithoutCalls(true);

        ManagedChannel channel = channelBuilder.usePlaintext().build();

        try {
            var taskSetServiceStub = TaskSetServiceGrpc.newFutureStub(channel);

            TaskDefinition taskDefinition = TaskDefinition.newBuilder()
                    .setPluginName("x-plugin-name-x")
                    .setId("x-task-id-x")
                    .build();

            AddSingleTaskOp addSingleTaskOp = AddSingleTaskOp.newBuilder()
                    .setTaskDefinition(taskDefinition)
                    .build();

            UpdateSingleTaskOp updateSingleTaskOp =
                    UpdateSingleTaskOp.newBuilder().setAddTask(addSingleTaskOp).build();

            UpdateTasksRequest updateTasksRequest = UpdateTasksRequest.newBuilder()
                    .setTenantId(mockTenantId)
                    .addUpdate(updateSingleTaskOp)
                    .build();

            rpcException = null;
            try {
                ListenableFuture<UpdateTasksResponse> future = taskSetServiceStub.updateTasks(updateTasksRequest);

                updateTasksResponse = future.get(timeout, TimeUnit.MILLISECONDS);
            } catch (Exception exc) {
                rpcException = exc;
            }
        } finally {
            safeChannelShutdown(channel);
        }
    }

    private void commonSendTaskSetMonitorResult() {
        MinionToCloudMessage minionToCloudMessage = prepareTaskSetMonitorResultMessage();

        rpcException = null;
        try {
            minionToCloudMessageStream.onNext(minionToCloudMessage);
        } catch (Exception exc) {
            rpcException = exc;
        }
    }

    private void commonSendTwinRegistrationRequest(long timeout) {
        TwinRequestProto twinRequestProto =
                TwinRequestProto.newBuilder().setConsumerKey("x-consumer-key-x").build();

        RpcRequestProto twinRegistrationRequest = rpcRequestProtoBuilder
                .setModuleId("twin")
                .setIdentity(Identity.newBuilder()
                        .setSystemId(gatewayRpcRequestProtoBuilder
                                .getIdentityBuilder()
                                .getSystemId()))
                .setPayload(Any.pack(twinRequestProto))
                .build();

        rpcException = null;
        try {
            ListenableFuture<RpcResponseProto> future = minionToCloudRpcStub
                    .withInterceptors(prepareGrpcHeaderInterceptor())
                    .minionToCloudRPC(twinRegistrationRequest);

            externalRpcResponseProto = future.get(timeout, TimeUnit.MILLISECONDS);
        } catch (Exception exc) {
            rpcException = exc;
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

    // TODO - consider splitting the internal and external builders to prevent confusion and make the code more obvious?
    //  note that this requires changes to the test steps definitions & updating the feature file
    private void ensureRpcRequest() {
        if (gatewayRpcRequestProtoBuilder == null) {
            gatewayRpcRequestProtoBuilder = GatewayRpcRequestProto.newBuilder();
        }

        if (rpcRequestProtoBuilder == null) {
            rpcRequestProtoBuilder = RpcRequestProto.newBuilder();
        }
    }

    private CloudToMinionMessage findReceivedTaskSetMessage() {
        CloudToMinionMessage[] messages = testCloudToMinionMessageHandler.getReceivedMessagesSnapshot();

        CloudToMinionMessage match = Arrays.asList(messages).stream()
                .filter((msg) -> (msg.hasTwinResponse()
                        && msg.getTwinResponse().getConsumerKey().equals("task-set")))
                .findFirst()
                .orElse(null);

        return match;
    }

    private void handleRpcResponse(RpcResponseProto rpcResponseProto) {
        if (minionRpcStream != null) {
            minionRpcStream.onNext(rpcResponseProto);
        } else {
            LOG.error("RPC RESPONSE - cannot send; stream is null");
        }
    }

    private MinionToCloudMessage prepareTaskSetMonitorResultMessage() {
        MonitorResponse monitorResponse = MonitorResponse.newBuilder()
                .setMonitorType(MonitorType.ICMP)
                .setResponseTimeMs(13.999)
                .setStatus("OK")
                .setIpAddress("127.0.0.1")
                .build();

        TaskResult taskResult =
                TaskResult.newBuilder().setMonitorResponse(monitorResponse).build();

        TaskSetResults taskSetResults =
                TaskSetResults.newBuilder().addResults(taskResult).build();

        SinkMessage sinkMessage = SinkMessage.newBuilder()
                .setModuleId("task-set-result")
                .setContent(taskSetResults.toByteString())
                .build();

        MinionToCloudMessage minionToCloudMessage =
                MinionToCloudMessage.newBuilder().setSinkMessage(sinkMessage).build();

        return minionToCloudMessage;
    }

    private RestAssuredConfig createRestAssuredTestConfig() {
        return RestAssuredConfig.config()
                .httpClient(HttpClientConfig.httpClientConfig()
                        .setParam("http.connection.timeout", DEFAULT_HTTP_SOCKET_TIMEOUT)
                        .setParam("http.socket.timeout", DEFAULT_HTTP_SOCKET_TIMEOUT));
    }

    private ClientInterceptor prepareGrpcHeaderInterceptor() {
        return MetadataUtils.newAttachHeadersInterceptor(prepareGrpcHeaders());
    }

    private Metadata prepareGrpcHeaders() {
        Metadata result = new Metadata();

        if (mockLocationId != null) {
            result.put(Metadata.Key.of("location-id", Metadata.ASCII_STRING_MARSHALLER), mockLocationId);
        }
        if (mockTenantId != null) {
            result.put(Metadata.Key.of("tenant-id", Metadata.ASCII_STRING_MARSHALLER), mockTenantId);
        }

        return result;
    }
}
