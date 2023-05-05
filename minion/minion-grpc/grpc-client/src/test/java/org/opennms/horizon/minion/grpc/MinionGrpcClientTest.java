/*
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
 *
 */

package org.opennms.horizon.minion.grpc;

import com.codahale.metrics.MetricRegistry;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import io.opentracing.Tracer;
import nl.altindag.log.LogCaptor;
import nl.altindag.log.model.LogEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.opennms.cloud.grpc.minion.CloudServiceGrpc;
import org.opennms.cloud.grpc.minion.CloudToMinionMessage;
import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.cloud.grpc.minion.MinionToCloudMessage;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcResponseProto;
import org.opennms.horizon.minion.grpc.rpc.RpcRequestHandler;
import org.opennms.horizon.minion.grpc.ssl.MinionGrpcSslContextBuilderFactory;
import org.opennms.horizon.shared.ipc.rpc.IpcIdentity;
import org.opennms.horizon.shared.ipc.sink.api.SendQueueFactory;

import javax.net.ssl.SSLContext;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WARNING: this test does not provide complete coverage of the MinionGrpcClient.
 *
 * Note too that this test currently does trigger GRPC to attempt to create connections, although it does not need the
 * connections to succeed.  Refactoring the MinionGrpcClient to be more test friendly would make the test code easier
 * to read, write and maintain.
 */
public class MinionGrpcClientTest {

    private MinionGrpcClient target;

    private MetricRegistry mockMetricRegistry;
    private Tracer mockTracer;

    private SendQueueFactory mockSendQueueFactory;
    private MinionGrpcSslContextBuilderFactory mockMinionGrpcSslContextBuilderFactory;
    private MinionGrpcClient.SimpleReconnectStrategyFactory mockSimpleReconnectStrategyFactory;
    private SimpleReconnectStrategy mockSimpleReconnectStrategy;
    private Function<ManagedChannel, CloudServiceGrpc.CloudServiceStub> mockNewStubOperation;
    private CloudServiceGrpc.CloudServiceStub mockAsyncStub;
    private SSLContext mockSslContext;
    private CloudMessageHandler mockCloudMessageHandler;
    private RpcRequestHandler mockRpcRequestHandler;

    private StreamObserver<RpcResponseProto> mockRpcStream;
    private StreamObserver<MinionToCloudMessage> mockSinkStream;

    private IpcIdentity testIpcIdentity;

    @BeforeEach
    public void setUp() {
        mockMetricRegistry = Mockito.mock(MetricRegistry.class);
        mockTracer = Mockito.mock(Tracer.class);
        mockSendQueueFactory = Mockito.mock(SendQueueFactory.class);
        mockMinionGrpcSslContextBuilderFactory = Mockito.mock(MinionGrpcSslContextBuilderFactory.class);
        mockSimpleReconnectStrategyFactory = Mockito.mock(MinionGrpcClient.SimpleReconnectStrategyFactory.class);
        mockSimpleReconnectStrategy = Mockito.mock(SimpleReconnectStrategy.class);
        mockNewStubOperation = Mockito.mock(Function.class);
        mockAsyncStub = Mockito.mock(CloudServiceGrpc.CloudServiceStub.class);
        mockSslContext = Mockito.mock(SSLContext.class);
        mockCloudMessageHandler = Mockito.mock(CloudMessageHandler.class);
        mockRpcRequestHandler = Mockito.mock(RpcRequestHandler.class);

        mockRpcStream = Mockito.mock(StreamObserver.class);
        mockSinkStream = Mockito.mock(StreamObserver.class);

        testIpcIdentity = new MinionIpcIdentity("x-system-id-x", "x-location-x");

        Mockito.when(mockMinionGrpcSslContextBuilderFactory.create()).thenReturn(mockSslContext);
        Mockito.when(mockSimpleReconnectStrategyFactory.create(Mockito.any(ManagedChannel.class), Mockito.any(Runnable.class), Mockito.any(Runnable.class))).thenReturn(mockSimpleReconnectStrategy);
        Mockito.when(mockNewStubOperation.apply(Mockito.any(ManagedChannel.class))).thenReturn(mockAsyncStub);

        target = new MinionGrpcClient(testIpcIdentity, mockMetricRegistry, mockTracer, mockSendQueueFactory, mockMinionGrpcSslContextBuilderFactory);
    }

    @Test
    void testStartTls() throws Exception {
        //
        // Setup Test Data and Interactions
        //
        try (var logCaptor = LogCaptor.forClass(MinionGrpcClient.class)) {
            //
            // Execute
            //
            target.setGrpcHost("x-grpc-host-x");
            target.setGrpcPort(1313);
            target.setTlsEnabled(true);
            target.start();

            //
            // Verify the Results
            //
            Predicate<LogEvent> matcher1 =
                (logEvent) ->
                    (
                        Objects.equals("TLS enabled for gRPC", logEvent.getMessage() )
                    );

            assertTrue(logCaptor.getLogEvents().stream().anyMatch(matcher1));

            Predicate<LogEvent> matcher2 =
                (logEvent) ->
                    (
                        Objects.equals("Minion at location {} with systemId {} started", logEvent.getMessage() ) &&
                        (logEvent.getArguments().size() == 2) &&
                        (logEvent.getArguments().get(0).equals("x-location-x")) &&
                        (logEvent.getArguments().get(1).equals("x-system-id-x"))
                    );

            assertTrue(logCaptor.getLogEvents().stream().anyMatch(matcher2));
        }
    }

    @Test
    void testStartTlsWithOverrideAuthority() throws Exception {
        //
        // Setup Test Data and Interactions
        //
        try (var logCaptor = LogCaptor.forClass(MinionGrpcClient.class)) {
            //
            // Execute
            //
            target.setOverrideAuthority("x-override-authority-x");
            target.setGrpcHost("x-grpc-host-x");
            target.setGrpcPort(1313);
            target.setTlsEnabled(true);
            target.start();

            //
            // Verify the Results
            //
            Predicate<LogEvent> matcher =
                (logEvent) ->
                    (
                        Objects.equals("Configuring GRPC override authority {}", logEvent.getMessage() ) &&
                        (logEvent.getArguments().size() == 1) &&
                        (logEvent.getArguments().get(0).equals("x-override-authority-x"))
                    );

            assertTrue(logCaptor.getLogEvents().stream().anyMatch(matcher));
        }
    }

    @Test
    void testStartPlainText() throws Exception {
        //
        // Setup Test Data and Interactions
        //
        try (var logCaptor = LogCaptor.forClass(MinionGrpcClient.class)) {
            //
            // Execute
            //
            target.setGrpcHost("x-grpc-host-x");
            target.setGrpcPort(1313);
            target.setTlsEnabled(false);
            target.start();

            //
            // Verify the Results
            //
            Predicate<LogEvent> matcher =
                (logEvent) ->
                    (
                        Objects.equals("Minion at location {} with systemId {} started", logEvent.getMessage() ) &&
                        (logEvent.getArguments().size() == 2) &&
                        (logEvent.getArguments().get(0).equals("x-location-x")) &&
                        (logEvent.getArguments().get(1).equals("x-system-id-x"))
                    );

            assertTrue(logCaptor.getLogEvents().stream().anyMatch(matcher));
        }
    }

    @Test
    void testShutdownNotStarted() throws Exception {
        //
        // Setup Test Data and Interactions
        //
        try (var logCaptor = LogCaptor.forClass(MinionGrpcClient.class)) {
            //
            // Execute
            //
            target.setGrpcHost("x-grpc-host-x");
            target.setGrpcPort(1313);
            target.setTlsEnabled(false);
            target.shutdown();

            //
            // Verify the Results
            //
            Predicate<LogEvent> matcher =
                (logEvent) ->
                    (
                        Objects.equals("Minion at location {} with systemId {} stopped", logEvent.getMessage() ) &&
                        (logEvent.getArguments().size() == 2) &&
                        (logEvent.getArguments().get(0).equals("x-location-x")) &&
                        (logEvent.getArguments().get(1).equals("x-system-id-x"))
                    );

            assertTrue(logCaptor.getLogEvents().stream().anyMatch(matcher));
        }
    }

    @Test
    void testShutdownAfterStarted() throws Exception {
        //
        // Setup Test Data and Interactions
        //
        try (var logCaptor = LogCaptor.forClass(MinionGrpcClient.class)) {
            //
            // Execute
            //
            target.setGrpcHost("x-grpc-host-x");
            target.setGrpcPort(1313);
            target.setTlsEnabled(false);
            target.start();
            target.shutdown();

            //
            // Verify the Results
            //
            Predicate<LogEvent> matcher =
                (logEvent) ->
                    (
                        Objects.equals("Minion at location {} with systemId {} stopped", logEvent.getMessage() ) &&
                        (logEvent.getArguments().size() == 2) &&
                        (logEvent.getArguments().get(0).equals("x-location-x")) &&
                        (logEvent.getArguments().get(1).equals("x-system-id-x"))
                    );

            assertTrue(logCaptor.getLogEvents().stream().anyMatch(matcher));
        }
    }

    @Test
    void testReconnectStrategy() throws Exception {
        //
        // Setup Test Data and Interactions
        //

        //
        // Execute
        //
        target.setGrpcHost("x-grpc-host-x");
        target.setGrpcPort(1313);
        target.setTlsEnabled(true);
        target.setSimpleReconnectStrategyFactory(mockSimpleReconnectStrategyFactory);
        target.setNewStubOperation(mockNewStubOperation);
        target.start();

        //
        // Verify the Results
        //
        var onConnectHandler = ArgumentCaptor.forClass(Runnable.class);
        var onDisconnectHandler = ArgumentCaptor.forClass(Runnable.class);
        Mockito.verify(mockSimpleReconnectStrategyFactory).create(Mockito.any(ManagedChannel.class), onConnectHandler.capture(), onDisconnectHandler.capture());
        Mockito.verify(mockSimpleReconnectStrategy).activate();

        verifyOnConnectHandler(onConnectHandler.getValue());
        verifyOnDisconnectHandler(onDisconnectHandler.getValue());
    }

    @Test
    void testRpcMessageHandler() throws Exception {
        //
        // Setup Test Data and Interactions
        //
        Mockito.when(mockAsyncStub.cloudToMinionRPC(Mockito.any(StreamObserver.class))).thenReturn(mockRpcStream);



        //
        // Execute
        //
        target.setGrpcHost("x-grpc-host-x");
        target.setGrpcPort(1313);
        target.setTlsEnabled(false);
        target.setSimpleReconnectStrategyFactory(mockSimpleReconnectStrategyFactory);
        target.setNewStubOperation(mockNewStubOperation);
        target.setRpcRequestHandler(mockRpcRequestHandler);
        target.start();

        //
        // Verify the Results
        //
        var onConnectHandler = ArgumentCaptor.forClass(Runnable.class);
        var onDisconnectHandler = ArgumentCaptor.forClass(Runnable.class);
        Mockito.verify(mockSimpleReconnectStrategyFactory).create(Mockito.any(ManagedChannel.class), onConnectHandler.capture(), onDisconnectHandler.capture());
        Mockito.verify(mockSimpleReconnectStrategy).activate();

        //
        // Run the onConnect handler to trigger the GRPC calls
        //
        onConnectHandler.getValue().run();

        // Verify the GRPC call
        var rpcMessageHandlerCaptor = ArgumentCaptor.forClass(StreamObserver.class);
        Mockito.verify(mockAsyncStub).cloudToMinionRPC(rpcMessageHandlerCaptor.capture());
        verifyRpcMessageHandler(rpcMessageHandlerCaptor.getValue());
    }

    @Test
    void testCloudMessageObserver() throws Exception {
        //
        // Setup Test Data and Interactions
        //

        //
        // Execute
        //
        target.setGrpcHost("x-grpc-host-x");
        target.setGrpcPort(1313);
        target.setTlsEnabled(false);
        target.setSimpleReconnectStrategyFactory(mockSimpleReconnectStrategyFactory);
        target.setNewStubOperation(mockNewStubOperation);
        target.setCloudMessageHandler(mockCloudMessageHandler);
        target.start();

        //
        // Verify the Results
        //
        var onConnectHandler = ArgumentCaptor.forClass(Runnable.class);
        var onDisconnectHandler = ArgumentCaptor.forClass(Runnable.class);
        Mockito.verify(mockSimpleReconnectStrategyFactory).create(Mockito.any(ManagedChannel.class), onConnectHandler.capture(), onDisconnectHandler.capture());
        Mockito.verify(mockSimpleReconnectStrategy).activate();

        //
        // Run the onConnect handler to trigger the GRPC calls
        //
        onConnectHandler.getValue().run();

        // Verify the GRPC call
        var cloudMessageObserverCaptor = ArgumentCaptor.forClass(StreamObserver.class);
        Mockito.verify(mockAsyncStub).cloudToMinionMessages(Mockito.any(Identity.class), cloudMessageObserverCaptor.capture());
        verifyCloudMessageObserver(cloudMessageObserverCaptor.getValue());
    }

//========================================
// Internals
//----------------------------------------

    private void verifyOnConnectHandler(Runnable onConnectHandler) {
        //
        // Setup Test Data and Interactions
        //
        Mockito.when(mockAsyncStub.cloudToMinionRPC(Mockito.any(StreamObserver.class))).thenReturn(mockRpcStream);
        Mockito.when(mockAsyncStub.minionToCloudMessages(Mockito.any(StreamObserver.class))).thenReturn(mockSinkStream);

        //
        // Execute
        //
        onConnectHandler.run();

        //
        // Verify the Results
        //
        Mockito.verify(mockAsyncStub).cloudToMinionRPC(Mockito.any(StreamObserver.class));
        Mockito.verify(mockAsyncStub).minionToCloudMessages(Mockito.any(StreamObserver.class));
        Mockito.verify(mockAsyncStub).cloudToMinionMessages(Mockito.any(Identity.class), Mockito.any(StreamObserver.class));
    }

    private void verifyOnDisconnectHandler(Runnable onDisconnectHandler) {
        onDisconnectHandler.run();

        Mockito.verify(mockRpcStream).onCompleted();
        Mockito.verify(mockSinkStream).onCompleted();
    }

    private void verifyCloudMessageObserver(StreamObserver streamObserver) {
        CloudToMinionMessage cloudToMinionMessage =
            CloudToMinionMessage.newBuilder()
                .build();

        streamObserver.onNext(cloudToMinionMessage);
        Mockito.verify(mockCloudMessageHandler).handle(cloudToMinionMessage);

        Mockito.reset(mockSimpleReconnectStrategy);
        streamObserver.onCompleted();
        Mockito.verify(mockSimpleReconnectStrategy).activate();

        Mockito.reset(mockSimpleReconnectStrategy);
        Exception testException = new Exception("x-test-exception-x");
        streamObserver.onError(testException);
        Mockito.verify(mockSimpleReconnectStrategy).activate();
    }

    private void verifyRpcMessageHandler(StreamObserver streamObserver) {
        RpcRequestProto rpcRequest =
            RpcRequestProto.newBuilder()
                .build();

        CompletableFuture mockCompletableFuture = Mockito.mock(CompletableFuture.class);

        Mockito.when(mockRpcRequestHandler.handle(rpcRequest)).thenReturn(mockCompletableFuture);

        streamObserver.onNext(rpcRequest);
        Mockito.verify(mockRpcRequestHandler).handle(rpcRequest);
        var futureCaptor = ArgumentCaptor.forClass(BiConsumer.class);
        Mockito.verify(mockCompletableFuture).whenComplete(futureCaptor.capture());
        verifyRpcRequestCompletion(futureCaptor.getValue());

        Mockito.reset(mockSimpleReconnectStrategy);
        streamObserver.onCompleted();
        Mockito.verify(mockSimpleReconnectStrategy).activate();

        Mockito.reset(mockSimpleReconnectStrategy);
        Exception testException = new Exception("x-test-exception-x");
        streamObserver.onError(testException);
        Mockito.verify(mockSimpleReconnectStrategy).activate();
    }

    private void verifyRpcRequestCompletion(BiConsumer<RpcResponseProto, Throwable> completionOp) {
        RpcResponseProto rpcResponse =
            RpcResponseProto.newBuilder()
                .build();

        completionOp.accept(rpcResponse, null);
        Mockito.verify(mockRpcStream).onNext(rpcResponse);

        Exception testException = new Exception("x-test-exception-x");
        completionOp.accept(rpcResponse, testException);
        Mockito.verify(mockRpcStream).onError(testException);
    }
}
