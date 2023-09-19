/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2019 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2019 The OpenNMS Group, Inc.
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
 *******************************************************************************/

package org.opennms.horizon.minion.grpc;

import com.codahale.metrics.MetricRegistry;
import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.grpc.Context;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.opentracing.Tracer;
import lombok.Setter;
import org.opennms.cloud.grpc.minion.CloudServiceGrpc;
import org.opennms.cloud.grpc.minion.CloudServiceGrpc.CloudServiceStub;
import org.opennms.cloud.grpc.minion.CloudToMinionMessage;
import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.cloud.grpc.minion.MinionToCloudMessage;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcResponseProto;
import org.opennms.cloud.grpc.minion.SinkMessage;
import org.opennms.horizon.minion.grpc.channel.ManagedChannelFactory;
import org.opennms.horizon.minion.grpc.rpc.RpcRequestHandler;
import org.opennms.horizon.shared.ipc.rpc.IpcIdentity;
import org.opennms.horizon.shared.ipc.rpc.api.minion.ClientRequestDispatcher;
import org.opennms.horizon.shared.ipc.sink.api.MessageConsumerManager;
import org.opennms.horizon.shared.ipc.sink.api.SendQueueFactory;
import org.opennms.horizon.shared.ipc.sink.api.SinkModule;
import org.opennms.horizon.shared.ipc.sink.common.AbstractMessageDispatcherFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.MDC.MDCCloseable;

import java.io.IOException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import static org.opennms.horizon.shared.ipc.rpc.api.RpcModule.MINION_HEADERS_MODULE;

/**
 * Minion GRPC client runs both RPC/Sink together.
 * gRPC runs in a typical web server/client mode, so gRPC client runs on each minion and gRPC server runs on OpenNMS.
 * Minion GRPC Client tries to get two stubs/streams (RPC/Sink) from OpenNMS server when it is in active state.
 * It also initializes RPC observer/handler to receive requests from OpenNMS.
 * <p>
 * RPC : RPC runs in bi-directional streaming mode. Minion gets each request and it handles each request in a separate
 * thread. Once the request is executed, the response sender call is synchronized as writing to observer is not thread-safe.
 * Minion also sends it's headers (SystemId/location) to OpenNMS whenever the stub is initialized.
 * <p>
 * Sink: Sink runs in uni-directional streaming mode. If the sink module is async and OpenNMS Server is not active, the
 * messages are buffered and blocked till minion is able to connect to OpenNMS.
 */
public class MinionGrpcClient extends AbstractMessageDispatcherFactory<String> implements ClientRequestDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(MinionGrpcClient.class);
    private static final long SINK_BLOCKING_TIMEOUT = 1000;
    private static final int SINK_BLOCKING_THREAD_POOL_SIZE = 100;
    private ManagedChannel channel;
    private CloudServiceStub asyncStub;
    private IpcIdentity ipcIdentity;
    private MetricRegistry metricRegistry;
    private StreamObserver<RpcResponseProto> rpcStream;
    private StreamObserver<MinionToCloudMessage> sinkStream;
    private Context.CancellableContext cloudToMinionStreamCancellableContext;

    private final AtomicLong counter = new AtomicLong();
    private final ThreadFactory blockingSinkMessageThreadFactory = (runnable) -> new Thread(runnable, "blocking-sink-message-" + counter.incrementAndGet());
    // Each request is handled in a new thread which unmarshals and executes the request.
    // This maintains a blocking thread for each dispatch module when OpenNMS is not in active state.

    private final ScheduledExecutorService blockingSinkMessageScheduler = Executors.newScheduledThreadPool(SINK_BLOCKING_THREAD_POOL_SIZE,
            blockingSinkMessageThreadFactory);
    private final Map<SinkMessage, ScheduledFuture<?>> pendingMessages = new ConcurrentHashMap<>(2000);
    private final Tracer tracer;
    private final ManagedChannelFactory managedChannelFactory;

    private final GrpcShutdownHandler grpcShutdownHandler;

    private ReconnectStrategy reconnectStrategy;
    @Setter
    private RpcRequestHandler rpcRequestHandler;
    @Setter
    private CloudMessageHandler cloudMessageHandler;

    @Setter
    private SimpleReconnectStrategyFactory simpleReconnectStrategyFactory = SimpleReconnectStrategy::new;
    @Setter
    private Function<ManagedChannel, CloudServiceStub> newStubOperation = CloudServiceGrpc::newStub;

    @Setter
    private String grpcHost;
    @Setter
    private int grpcPort;
    @Setter
    private String overrideAuthority;

    private final SendQueueFactory sendQueueFactory;

//========================================
// Constructor
//----------------------------------------

    public MinionGrpcClient(
        IpcIdentity ipcIdentity,
        MetricRegistry metricRegistry,
        Tracer tracer,
        SendQueueFactory sendQueueFactory,
        ManagedChannelFactory managedChannelFactory,
        GrpcShutdownHandler grpcShutdownHandler) {

        this.ipcIdentity = ipcIdentity;
        this.metricRegistry = metricRegistry;
        this.tracer = tracer;
        this.sendQueueFactory = Objects.requireNonNull(sendQueueFactory);
        this.managedChannelFactory = managedChannelFactory;
        this.grpcShutdownHandler = grpcShutdownHandler;
    }

//========================================
// Lifecycle
//----------------------------------------

    public void start() throws IOException {
        channel = managedChannelFactory.create(grpcHost, grpcPort, overrideAuthority);
        asyncStub = newStubOperation.apply(channel);

        reconnectStrategy = simpleReconnectStrategyFactory.create(channel, this::handleReconnect, this::handleDisconnect);
        reconnectStrategy.activate();

        LOG.info("Minion with systemId {} started", ipcIdentity.getId());

    }


    public void shutdown() {
        blockingSinkMessageScheduler.shutdown();
        if (rpcStream != null) {
            rpcStream.onCompleted();
        }
        if (sinkStream != null) {
            sinkStream.onCompleted();
        }
        if (channel != null) {
            channel.shutdown();
        }
        LOG.info("Minion with systemId {} stopped", ipcIdentity.getId());

    }


//========================================
// Misc
//----------------------------------------

    @Override
    public Tracer getTracer() {
        return tracer;
    }

    @Override
    public MetricRegistry getMetrics() {
        return metricRegistry;
    }

    @Override
    protected SendQueueFactory getSendQueueFactory() {
        return this.sendQueueFactory;
    }

//========================================
// Processing
//----------------------------------------

    @Override
    // public <S extends org.opennms.horizon.ipc.sink.api.Message, T extends org.opennms.horizon.ipc.sink.api.Message> void dispatch(SinkModule<S, T> module, String metadata, T message) {
    public <S extends Message, T extends Message> void dispatch(SinkModule<S, T> module, String metadata, byte[] message) {
        try (MDCCloseable mdc = MDC.putCloseable("prefix", MessageConsumerManager.LOG_PREFIX)) {
            String messageId = UUID.randomUUID().toString();
            SinkMessage.Builder sinkMessageBuilder = SinkMessage.newBuilder()
                    .setMessageId(messageId)
                    .setModuleId(module.getId())
                    .setContent(ByteString.copyFrom(message));

            // If module has asyncpolicy, keep attempting to send message.
            if (module.getAsyncPolicy() != null) {
                sendBlockingSinkMessage(sinkMessageBuilder.build());
            } else {
                sendSinkMessage(sinkMessageBuilder.build());
            }
        }
    }

    @Override
    public CompletableFuture<RpcResponseProto> call(RpcRequestProto requestProto) {
        CompletableFuture<RpcResponseProto> future = new CompletableFuture<>();
        asyncStub.minionToCloudRPC(requestProto, new StreamObserver<>() {
            @Override
            public void onNext(RpcResponseProto value) {
                future.complete(value);
            }

            @Override
            public void onError(Throwable throwable) {
                var rootCause = findRootCause(throwable);
                // sun.security.provider.certpath.SunCertPathBuilderException is not visible by default
                if (rootCause.getClass().getName().contains("SunCertPathBuilderException")
                    || rootCause instanceof CertificateExpiredException
                    || rootCause instanceof CertificateNotYetValidException) {
                    LOG.error("Client received error {}", rootCause.getMessage());
                    shutdown();
                    reconnectStrategy.shutdown();
                    grpcShutdownHandler.shutdown(rootCause);
                } else {
                    future.completeExceptionally(throwable);
                }
            }

            @Override
            public void onCompleted() {
                if (!future.isDone() && !future.isCancelled()) {
                    future.cancel(false);
                }
            }
        });
        return future;
    }


//========================================
// Internals
//----------------------------------------
    private Throwable findRootCause(Throwable t) {
        Throwable rootCause = t;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        return rootCause;
    }
    private void handleReconnect() {
        initializeRpcStub();
        initializeSinkStub();
        initializeCloudReceiver();
    }

    private void handleDisconnect() {
        if (rpcStream != null) {
            try {
                rpcStream.onCompleted();
            } catch (Exception exc) {
                LOG.debug("Error on cleanup of existing rpc stream", exc);
            }

            rpcStream = null;
        }

        if (sinkStream != null) {
            try {
                sinkStream.onCompleted();
            } catch (Exception exc) {
                LOG.debug("Error on cleanup of existing sink stream", exc);
            }

            sinkStream = null;
        }

        if (cloudToMinionStreamCancellableContext != null) {
            try {
                cloudToMinionStreamCancellableContext.cancel(new Exception("cleanup on handleDisconnect"));
            } catch (Exception exc) {
                LOG.debug("Error on cleanup of existing cloud-to-minion stream", exc);
            }

            cloudToMinionStreamCancellableContext = null;
        }
    }

    private void initializeRpcStub() {
        rpcStream = asyncStub.cloudToMinionRPC(new RpcMessageHandler());
        // Need to send minion headers to gRPC server in order to register.
        sendMinionHeaders();
        LOG.info("Initialized RPC stream");
    }

    private void initializeSinkStub() {
        sinkStream = asyncStub.minionToCloudMessages(new MinionToCloudHandler());
        LOG.info("Initialized Sink stream");
    }

    private void initializeCloudReceiver() {
        Identity identity = Identity.newBuilder()
            .setSystemId(ipcIdentity.getId())
            .build();

        cloudToMinionStreamCancellableContext = Context.current().withCancellation();
        cloudToMinionStreamCancellableContext.run(
            () -> {
                asyncStub.cloudToMinionMessages(identity, new CloudMessageObserver(cloudMessageHandler));
                LOG.info("Initialized cloud receiver stream");
            });

    }

    private void sendBlockingSinkMessage(SinkMessage sinkMessage) {
        boolean succeeded = sendSinkMessage(sinkMessage);
        if (succeeded) {
            return;
        }
        //Recursively try to send sink message until it succeeds.
        scheduleSinkMessageAfterDelay(sinkMessage);
    }

    private void scheduleSinkMessageAfterDelay(SinkMessage sinkMessage) {
        // try until we get a successful send
        ScheduledFuture<?> future = blockingSinkMessageScheduler.scheduleWithFixedDelay(() -> {
            if (sendSinkMessage(sinkMessage)) {
                // canceling myself?
                pendingMessages.remove(sinkMessage).cancel(false);
            }
        }, SINK_BLOCKING_TIMEOUT, SINK_BLOCKING_TIMEOUT, TimeUnit.MILLISECONDS);
        pendingMessages.put(sinkMessage, future);
    }


    private synchronized boolean sendSinkMessage(SinkMessage sinkMessage) {
        if (sinkStream != null) {
            try {
                sinkStream.onNext(MinionToCloudMessage.newBuilder()
                    .setSinkMessage(sinkMessage)
                    .build()
                );
                return true;
            } catch (Throwable e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Exception while sending sinkMessage to gRPC IPC server", e);
                } else {
                    LOG.error("Exception while sending sinkMessage to gRPC IPC server {}", e.getMessage());
                }
            }
        }
        return false;
    }


    private void sendMinionHeaders() {
        RpcResponseProto rpcHeader = RpcResponseProto.newBuilder()
                .setIdentity(Identity.newBuilder().setSystemId(ipcIdentity.getId()))
                .setModuleId(MINION_HEADERS_MODULE)
                .setRpcId(ipcIdentity.getId())
                .build();
        sendRpcResponse(rpcHeader);
        LOG.info("Sending Minion Headers from SystemId {} to gRPC server", ipcIdentity.getId());
    }

    private void processRpcRequest(RpcRequestProto requestProto) {
        if (rpcRequestHandler != null) {
            rpcRequestHandler.handle(requestProto).whenComplete((response, error) -> {
                if (error != null) {
                    LOG.warn("Failed to handle request {}", requestProto, error);
                    rpcStream.onError(error);
                    return;
                }
                sendRpcResponse(response);
            });
            return;
        }
        LOG.warn("Ignored RPC request {}, client has no rpc request handler set", requestProto);
    }

    private synchronized void sendRpcResponse(RpcResponseProto rpcResponseProto) {
        if (rpcStream != null) {
            try {
                rpcStream.onNext(rpcResponseProto);
            } catch (Exception e) {
                LOG.debug("Exception while sending RPC response : response = {}, message = {}",
                    rpcResponseProto, e.getMessage());
            }
        } else {
            LOG.warn("gRPC IPC server is not in ready state");
        }
    }

    private class RpcMessageHandler implements StreamObserver<RpcRequestProto> {

        @Override
        public void onNext(RpcRequestProto rpcRequestProto) {
            processRpcRequest(rpcRequestProto);
        }

        @Override
        public void onError(Throwable throwable) {
            if (throwable instanceof StatusRuntimeException statusRuntimeException
                && (statusRuntimeException.getStatus().getCode() == Status.Code.UNAUTHENTICATED)) {
                LOG.error("Client received error {}", Status.Code.UNAUTHENTICATED);
                shutdown();
                reconnectStrategy.shutdown();
                grpcShutdownHandler.shutdown(GrpcErrorMessages.UNAUTHENTICATED);
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Error in RPC streaming", throwable);
                } else {
                    LOG.error("Error in RPC streaming {}", throwable.getMessage());
                }
                reconnectStrategy.activate();
                LOG.info("Client closed the connection, will reconnect");
            }
        }

        @Override
        public void onCompleted() {
            LOG.error("Closing RPC message handler");
            reconnectStrategy.activate();
            LOG.info("Client closed the connection, will reconnect");
        }

    }

    private class CloudMessageObserver implements StreamObserver<CloudToMinionMessage> {

        private final CloudMessageHandler cloudRequestHandler;

        public CloudMessageObserver(CloudMessageHandler cloudRequestHandler) {
            this.cloudRequestHandler = cloudRequestHandler;
        }

        @Override
        public void onNext(CloudToMinionMessage value) {
            cloudRequestHandler.handle(value);
        }

        @Override
        public void onError(Throwable throwable) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Error in cloud message receiver", throwable);
            } else {
                LOG.error("Error in cloud message receiver {} ", throwable.getMessage());
            }
            reconnectStrategy.activate();
            LOG.info("Client closed the connection, will reconnect");
        }

        @Override
        public void onCompleted() {
            LOG.debug("Closing cloud message receiver");
            reconnectStrategy.activate();
            LOG.info("Client closed the connection, will reconnect");
        }
    }

    private class MinionToCloudHandler implements StreamObserver<Empty> {

        @Override
        public void onNext(Empty message) {
            LOG.warn("Received unexpected message from MinionToCloudMessages: {}", message);
        }

        @Override
        public void onError(Throwable throwable) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Error in MinionToCloudMessages streaming", throwable);
            } else {
                LOG.error("Error in MinionToCloudMessages streaming {}", throwable.getMessage());
            }
            reconnectStrategy.activate();
            LOG.info("Client closed the connection, will reconnect");
        }

        @Override
        public void onCompleted() {
            LOG.debug("Closing MinionToCloudMessages message handler");
            reconnectStrategy.activate();
            LOG.info("Client closed the connection, will reconnect");
        }

    }
    public interface SimpleReconnectStrategyFactory {
        SimpleReconnectStrategy create(ManagedChannel channel, Runnable onConnect, Runnable onDisconnect);
    }

}
