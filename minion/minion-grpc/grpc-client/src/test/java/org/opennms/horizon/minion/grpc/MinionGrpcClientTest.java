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
package org.opennms.horizon.minion.grpc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.codahale.metrics.MetricRegistry;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.opentracing.Tracer;
import java.io.Closeable;
import java.io.IOException;
import java.security.cert.CertificateExpiredException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opennms.cloud.grpc.minion.CloudServiceGrpc;
import org.opennms.cloud.grpc.minion.CloudToMinionMessage;
import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcResponseProto;
import org.opennms.horizon.minion.grpc.channel.ManagedChannelFactory;
import org.opennms.horizon.minion.grpc.rpc.RpcRequestHandler;
import org.opennms.horizon.shared.ipc.rpc.IpcIdentity;
import org.opennms.horizon.shared.ipc.sink.api.SendQueueFactory;

/**
 * WARNING: this test does not provide complete coverage of the MinionGrpcClient.
 *
 * Note too that this test currently does trigger GRPC to attempt to create connections, although it does not need the
 * connections to succeed.  Refactoring the MinionGrpcClient to be more test friendly would make the test code easier
 * to read, write and maintain.
 */
@ExtendWith(MockitoExtension.class)
public class MinionGrpcClientTest {

    private MinionGrpcClient target;

    @Mock
    private MetricRegistry mockMetricRegistry;

    @Mock
    private Tracer mockTracer;

    @Mock
    private SendQueueFactory mockSendQueueFactory;

    @Mock
    private ManagedChannel mockManagedChannel;

    @Mock
    private ManagedChannelFactory managedChannelFactory;

    @Mock
    private MinionGrpcClient.SimpleReconnectStrategyFactory mockSimpleReconnectStrategyFactory;

    @Mock
    private SimpleReconnectStrategy mockSimpleReconnectStrategy;

    @Mock
    private Function<ManagedChannel, CloudServiceGrpc.CloudServiceStub> mockNewStubOperation;

    @Mock
    private CloudServiceGrpc.CloudServiceStub mockAsyncStub;

    @Mock
    private CloudMessageHandler mockCloudMessageHandler;

    @Mock
    private RpcRequestHandler mockRpcRequestHandler;

    @Mock
    private StreamObserver mockRpcStream;

    @Mock
    private StreamObserver mockSinkStream;

    @Mock
    private GrpcShutdownHandler mockGrpcShutdownHandler;

    private IpcIdentity testIpcIdentity;

    @BeforeEach
    public void setUp() {
        mockMetricRegistry = Mockito.mock(MetricRegistry.class);
        mockTracer = Mockito.mock(Tracer.class);
        mockSendQueueFactory = Mockito.mock(SendQueueFactory.class);
        mockSimpleReconnectStrategyFactory = Mockito.mock(MinionGrpcClient.SimpleReconnectStrategyFactory.class);
        mockSimpleReconnectStrategy = Mockito.mock(SimpleReconnectStrategy.class);
        mockNewStubOperation = Mockito.mock(Function.class);
        mockAsyncStub = Mockito.mock(CloudServiceGrpc.CloudServiceStub.class);
        mockCloudMessageHandler = Mockito.mock(CloudMessageHandler.class);
        mockRpcRequestHandler = Mockito.mock(RpcRequestHandler.class);

        mockRpcStream = Mockito.mock(StreamObserver.class);
        mockSinkStream = Mockito.mock(StreamObserver.class);

        testIpcIdentity = new MinionIpcIdentity("x-system-id-x");

        mockGrpcShutdownHandler = Mockito.mock(GrpcShutdownHandler.class);

        target = new MinionGrpcClient(
                testIpcIdentity,
                mockMetricRegistry,
                mockTracer,
                mockSendQueueFactory,
                managedChannelFactory,
                mockGrpcShutdownHandler);
        target.setSimpleReconnectStrategyFactory(mockSimpleReconnectStrategyFactory);
    }

    @Test
    void testStartTheConnection() throws Exception {
        //
        // Setup Test Data and Interactions
        //
        try (var ignored = expect(connectionCall("x-grpc-host-x", 1313, null)).with(reconnectStrategyFactoryCall())) {
            //
            // Execute
            //
            target.setGrpcHost("x-grpc-host-x");
            target.setGrpcPort(1313);
            target.start();

            //
            // Verify the Results
            //
        }
    }

    @Test
    void testStartTlsWithOverrideAuthority() throws Exception {
        try (var ignored = expect(connectionCall("x-grpc-host-x", 1313, "x-override-authority-x"))
                .with(reconnectStrategyFactoryCall())) {
            target.setOverrideAuthority("x-override-authority-x");
            target.setGrpcHost("x-grpc-host-x");
            target.setGrpcPort(1313);
            target.start();
        }
    }

    @Test
    void testShutdownNotStarted() throws Exception {
        //
        // Setup Test Data and Interactions
        //
        try (var ignored = expect(() -> {})) {
            //
            // Execute
            //
            target.setGrpcHost("x-grpc-host-x");
            target.setGrpcPort(1313);

            target.shutdown();
            verifyNoInteractions(mockManagedChannel, mockSimpleReconnectStrategyFactory, mockNewStubOperation);
        }
    }

    @Test
    void testCertificateException() throws IOException {
        //
        // Setup Test Data and Interactions
        //
        try (var ignored = expect(connectionCall("x-grpc-host-x", 1313, null))
                .with(reconnectStrategyFactoryCall())
                .with(stubFactoryCall())) {
            //
            // Execute
            //
            target.setGrpcHost("x-grpc-host-x");
            target.setGrpcPort(1313);
            target.start();
        }

        var request = RpcRequestProto.getDefaultInstance();
        target.call(RpcRequestProto.getDefaultInstance());

        var responseObserverCaptor = ArgumentCaptor.forClass(StreamObserver.class);
        verify(mockAsyncStub).minionToCloudRPC(eq(request), responseObserverCaptor.capture());

        CertificateExpiredException expiredException = new CertificateExpiredException("expired");
        RuntimeException runtimeException = new RuntimeException("runtime", expiredException);
        responseObserverCaptor.getValue().onError(runtimeException);
        verify(mockGrpcShutdownHandler, times(1)).shutdown(expiredException);
        target.shutdown();
    }

    @Test
    void testShutdownAfterStarted() throws Exception {
        //
        // Setup Test Data and Interactions
        //
        try (var ignored = expect(connectionCall("x-grpc-host-x", 1313, null)).with(reconnectStrategyFactoryCall())) {
            //
            // Execute
            //
            target.setGrpcHost("x-grpc-host-x");
            target.setGrpcPort(1313);
            target.start();
            target.shutdown();

            //
            // Verify the Results
            //
            verify(mockManagedChannel).shutdown();
        }
    }

    @Test
    void testReconnectStrategy() throws Exception {
        //
        // Setup Test Data and Interactions
        //
        try (var ignored = expect(connectionCall("x-grpc-host-x", 1313, null))
                .with(reconnectStrategyFactoryCall())
                .with(stubFactoryCall())) {
            //
            // Execute
            //
            target.setGrpcHost("x-grpc-host-x");
            target.setGrpcPort(1313);
            target.start();
            target.shutdown();
        }

        //
        // Verify the Results
        //
        var onConnectHandler = ArgumentCaptor.forClass(Runnable.class);
        var onDisconnectHandler = ArgumentCaptor.forClass(Runnable.class);
        verify(mockSimpleReconnectStrategyFactory)
                .create(eq(mockManagedChannel), onConnectHandler.capture(), onDisconnectHandler.capture());
        verify(mockSimpleReconnectStrategy).activate();

        verifyOnConnectHandler(onConnectHandler.getValue());
        verifyOnDisconnectHandler(onDisconnectHandler.getValue());
    }

    @Test
    void testRpcMessageHandler() throws Exception {
        //
        // Setup Test Data and Interactions
        //
        when(mockAsyncStub.cloudToMinionRPC(any(StreamObserver.class))).thenReturn(mockRpcStream);

        try (var ignored = expect(connectionCall("x-grpc-host-x", 1313, "abc"))
                .with(reconnectStrategyFactoryCall())
                .with(stubFactoryCall())) {
            //
            // Execute
            //
            target.setGrpcHost("x-grpc-host-x");
            target.setGrpcPort(1313);
            target.setRpcRequestHandler(mockRpcRequestHandler);
            target.start();
        }

        //
        // Verify the Results
        //
        var onConnectHandler = ArgumentCaptor.forClass(Runnable.class);
        var onDisconnectHandler = ArgumentCaptor.forClass(Runnable.class);
        verify(mockSimpleReconnectStrategyFactory)
                .create(eq(mockManagedChannel), onConnectHandler.capture(), onDisconnectHandler.capture());
        verify(mockSimpleReconnectStrategy).activate();

        //
        // Run the onConnect handler to trigger the GRPC calls
        //
        onConnectHandler.getValue().run();

        // Verify the GRPC call
        var rpcMessageHandlerCaptor = ArgumentCaptor.forClass(StreamObserver.class);
        verify(mockAsyncStub).cloudToMinionRPC(rpcMessageHandlerCaptor.capture());
        verifyRpcMessageHandler(rpcMessageHandlerCaptor.getValue());
    }

    @Test
    void testCloudMessageObserver() throws Exception {
        //
        // Setup Test Data and Interactions
        //
        connectionCall("x-grpc-host-x", 1313, null);
        reconnectStrategyFactoryCall();
        stubFactoryCall();

        //
        // Execute
        //
        target.setGrpcHost("x-grpc-host-x");
        target.setGrpcPort(1313);
        target.setCloudMessageHandler(mockCloudMessageHandler);
        target.start();

        //
        // Verify the Results
        //
        var onConnectHandler = ArgumentCaptor.forClass(Runnable.class);
        var onDisconnectHandler = ArgumentCaptor.forClass(Runnable.class);
        verify(mockSimpleReconnectStrategyFactory)
                .create(any(ManagedChannel.class), onConnectHandler.capture(), onDisconnectHandler.capture());
        verify(mockSimpleReconnectStrategy).activate();

        //
        // Run the onConnect handler to trigger the GRPC calls
        //
        onConnectHandler.getValue().run();

        // Verify the GRPC call
        var cloudMessageObserverCaptor = ArgumentCaptor.forClass(StreamObserver.class);
        verify(mockAsyncStub).cloudToMinionMessages(any(Identity.class), cloudMessageObserverCaptor.capture());
        verifyCloudMessageObserver(cloudMessageObserverCaptor.getValue());
    }

    // ========================================
    // Internals
    // ----------------------------------------

    private void verifyOnConnectHandler(Runnable onConnectHandler) {
        //
        // Setup Test Data and Interactions
        //
        when(mockAsyncStub.cloudToMinionRPC(any(StreamObserver.class))).thenReturn(mockRpcStream);
        when(mockAsyncStub.minionToCloudMessages(any(StreamObserver.class))).thenReturn(mockSinkStream);

        //
        // Execute
        //
        onConnectHandler.run();

        //
        // Verify the Results
        //
        verify(mockAsyncStub).cloudToMinionRPC(any(StreamObserver.class));
        verify(mockAsyncStub).minionToCloudMessages(any(StreamObserver.class));
        verify(mockAsyncStub).cloudToMinionMessages(any(Identity.class), any(StreamObserver.class));
    }

    private void verifyOnDisconnectHandler(Runnable onDisconnectHandler) {
        onDisconnectHandler.run();

        verify(mockRpcStream).onCompleted();
        verify(mockSinkStream).onCompleted();
    }

    private void verifyCloudMessageObserver(StreamObserver streamObserver) {
        CloudToMinionMessage cloudToMinionMessage =
                CloudToMinionMessage.newBuilder().build();

        streamObserver.onNext(cloudToMinionMessage);
        verify(mockCloudMessageHandler).handle(cloudToMinionMessage);

        Mockito.reset(mockSimpleReconnectStrategy);
        streamObserver.onCompleted();
        verify(mockSimpleReconnectStrategy).activate();

        Mockito.reset(mockSimpleReconnectStrategy);
        Exception testException = new Exception("x-test-exception-x");
        streamObserver.onError(testException);
        verify(mockSimpleReconnectStrategy).activate();
    }

    private void verifyRpcMessageHandler(StreamObserver streamObserver) {
        RpcRequestProto rpcRequest = RpcRequestProto.newBuilder().build();

        CompletableFuture mockCompletableFuture = Mockito.mock(CompletableFuture.class);

        when(mockRpcRequestHandler.handle(rpcRequest)).thenReturn(mockCompletableFuture);

        streamObserver.onNext(rpcRequest);
        verify(mockRpcRequestHandler).handle(rpcRequest);
        var futureCaptor = ArgumentCaptor.forClass(BiConsumer.class);
        verify(mockCompletableFuture).whenComplete(futureCaptor.capture());
        verifyRpcRequestCompletion(futureCaptor.getValue());

        Mockito.reset(mockSimpleReconnectStrategy);
        streamObserver.onCompleted();
        verify(mockSimpleReconnectStrategy).activate();

        Mockito.reset(mockSimpleReconnectStrategy);
        Exception testException = new Exception("x-test-exception-x");
        streamObserver.onError(testException);
        verify(mockSimpleReconnectStrategy).activate();

        StatusRuntimeException statusRuntimeException =
                new StatusRuntimeException(Status.fromCode(Status.Code.UNAUTHENTICATED));
        streamObserver.onError(statusRuntimeException);
        verify(mockGrpcShutdownHandler).shutdown(GrpcErrorMessages.UNAUTHENTICATED);
    }

    private void verifyRpcRequestCompletion(BiConsumer<RpcResponseProto, Throwable> completionOp) {
        RpcResponseProto rpcResponse = RpcResponseProto.newBuilder().build();

        completionOp.accept(rpcResponse, null);
        verify(mockRpcStream).onNext(rpcResponse);

        Exception testException = new Exception("x-test-exception-x");
        completionOp.accept(rpcResponse, testException);
        verify(mockRpcStream).onError(testException);
    }

    private Expectation expect(Runnable runnable) {
        return new Expectation(runnable);
    }

    private Runnable connectionCall(String hostname, int port, String authority) {
        when(managedChannelFactory.create(hostname, port, authority)).thenReturn(mockManagedChannel);
        if (authority != null) {
            target.setOverrideAuthority(authority);
        }

        return () -> verify(managedChannelFactory).create(hostname, port, authority);
    }

    private Runnable reconnectStrategyFactoryCall() {
        when(mockSimpleReconnectStrategyFactory.create(
                        eq(mockManagedChannel), any(Runnable.class), any(Runnable.class)))
                .thenReturn(mockSimpleReconnectStrategy);

        target.setSimpleReconnectStrategyFactory(mockSimpleReconnectStrategyFactory);
        return () -> verify(mockSimpleReconnectStrategyFactory)
                .create(eq(mockManagedChannel), any(Runnable.class), any(Runnable.class));
    }

    private Runnable reconnection() {
        return () -> verify(mockSimpleReconnectStrategy).activate();
    }

    private Runnable stubFactoryCall() {
        when(mockNewStubOperation.apply(mockManagedChannel)).thenReturn(mockAsyncStub);
        target.setNewStubOperation(mockNewStubOperation);

        return () -> verify(mockNewStubOperation).apply(mockManagedChannel);
    }

    static class Expectation implements Closeable {
        private final List<Runnable> closures = new ArrayList<>();

        public Expectation(Runnable firstExpectation) {
            closures.add(firstExpectation);
        }

        public Expectation with(Runnable runnable) {
            closures.add(runnable);
            return this;
        }

        @Override
        public void close() throws IOException {
            for (Runnable closure : closures) {
                closure.run();
            }
        }
    }
}
