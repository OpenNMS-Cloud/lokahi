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

package org.opennms.core.ipc.grpc.client;

import static org.opennms.core.ipc.grpc.client.GrpcClientConstants.*;
import static org.opennms.horizon.shared.ipc.rpc.api.RpcModule.MINION_HEADERS_MODULE;

import com.codahale.metrics.MetricRegistry;
import com.google.protobuf.Message;
import io.opentracing.Tracer;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.atomic.AtomicLong;
import org.opennms.cloud.grpc.minion.CloudServiceGrpc;
import org.opennms.cloud.grpc.minion.CloudServiceGrpc.CloudServiceStub;
import org.opennms.cloud.grpc.minion.MinionToCloudMessage;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcResponseProto;
import org.opennms.cloud.grpc.minion.SinkMessage;
import org.opennms.horizon.shared.ipc.rpc.IpcIdentity;
import org.opennms.horizon.shared.ipc.rpc.api.RpcModule;
import org.opennms.horizon.shared.ipc.rpc.api.RpcRequest;
import org.opennms.horizon.shared.ipc.rpc.api.RpcResponse;
import org.opennms.horizon.shared.ipc.sink.api.MessageConsumerManager;
import org.opennms.horizon.shared.ipc.sink.api.SinkModule;
import org.opennms.horizon.shared.ipc.sink.common.AbstractMessageDispatcherFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;

import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NegotiationType;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import io.grpc.stub.StreamObserver;
import org.slf4j.MDC;
import org.slf4j.MDC.MDCCloseable;

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
public class MinionGrpcClient extends AbstractMessageDispatcherFactory<String> {

    static final String SINK_METRIC_PRODUCER_DOMAIN = "org.opennms.core.ipc.sink.producer";

    private static final Logger LOG = LoggerFactory.getLogger(MinionGrpcClient.class);
    private static final long SINK_BLOCKING_TIMEOUT = 1000;
    private static final int SINK_BLOCKING_THREAD_POOL_SIZE = 100;
    private ManagedChannel channel;
    private CloudServiceStub asyncStub;
    private Properties properties;
    private BundleContext bundleContext;
    private IpcIdentity ipcIdentity;
    private MetricRegistry metricRegistry;
    private StreamObserver<RpcResponseProto> rpcStream;
    private StreamObserver<MinionToCloudMessage> sinkStream;
    private ConnectivityState currentChannelState;

    private final AtomicLong counter = new AtomicLong();
    private final ThreadFactory requestHandlerThreadFactory = (runnable) -> new Thread(runnable, "rpc-request-handler-" + counter.incrementAndGet());
    private final ThreadFactory blockingSinkMessageThreadFactory = (runnable) -> new Thread(runnable, "blocking-sink-message-" + counter.incrementAndGet());
    // Each request is handled in a new thread which unmarshals and executes the request.
    private final ExecutorService requestHandlerExecutor = Executors.newCachedThreadPool(requestHandlerThreadFactory);
    // Maintain the map of RPC modules and their ID.
    private final Map<String, RpcModule<RpcRequest, RpcResponse>> registerdModules = new ConcurrentHashMap<>();
    // This maintains a blocking thread for each dispatch module when OpenNMS is not in active state.
    private final ScheduledExecutorService blockingSinkMessageScheduler = Executors.newScheduledThreadPool(SINK_BLOCKING_THREAD_POOL_SIZE,
            blockingSinkMessageThreadFactory);
    private ReconnectStrategy reconnectStrategy;
    private Tracer tracer;

    public MinionGrpcClient(IpcIdentity ipcIdentity, ConfigurationAdmin configAdmin) {
        this(ipcIdentity, ConfigUtils.getPropertiesFromConfig(configAdmin, GRPC_CLIENT_PID), new MetricRegistry(), null);
    }

    public MinionGrpcClient(IpcIdentity ipcIdentity, ConfigurationAdmin configAdmin, MetricRegistry metricRegistry, Tracer tracer) {
        this(ipcIdentity, ConfigUtils.getPropertiesFromConfig(configAdmin, GRPC_CLIENT_PID), metricRegistry, tracer);
    }

    public MinionGrpcClient(IpcIdentity ipcIdentity, Properties properties) {
        this(ipcIdentity, properties, new MetricRegistry(), null);
    }

    public MinionGrpcClient(IpcIdentity ipcIdentity, Properties properties, MetricRegistry metricRegistry, Tracer tracer) {
        this.ipcIdentity = ipcIdentity;
        this.properties = properties;
        this.metricRegistry = metricRegistry;
        this.tracer = tracer;
    }

    public void start() throws IOException {
        String host = PropertiesUtils.getProperty(properties, GRPC_HOST, DEFAULT_GRPC_HOST);
        int port = PropertiesUtils.getProperty(properties, GRPC_PORT, DEFAULT_GRPC_PORT);
        boolean tlsEnabled = PropertiesUtils.getProperty(properties, TLS_ENABLED, false);
        int maxInboundMessageSize = PropertiesUtils.getProperty(properties, GRPC_MAX_INBOUND_SIZE, DEFAULT_MESSAGE_SIZE);

        NettyChannelBuilder channelBuilder = NettyChannelBuilder.forAddress(host, port)
                .keepAliveWithoutCalls(true)
                .maxInboundMessageSize(maxInboundMessageSize);

        if (tlsEnabled) {
            channel = channelBuilder
                    .negotiationType(NegotiationType.TLS)
                    .sslContext(buildSslContext().build())
                    .build();
            LOG.info("TLS enabled for gRPC");
        } else {
            channel = channelBuilder.usePlaintext().build();
        }
        asyncStub = CloudServiceGrpc.newStub(channel);

        reconnectStrategy = new SimpleReconnectStrategy(channel, () -> {
            initializeRpcStub();
            initializeSinkStub();
        }, () -> {
            rpcStream = null;
            sinkStream = null;
        });
        reconnectStrategy.activate();

        LOG.info("Minion at location {} with systemId {} started", ipcIdentity.getLocation(), ipcIdentity.getId());

    }

    private SslContextBuilder buildSslContext() {
        SslContextBuilder builder = GrpcSslContexts.forClient();
        String clientCertChainFilePath = properties.getProperty(CLIENT_CERTIFICATE_FILE_PATH);
        String clientPrivateKeyFilePath = properties.getProperty(CLIENT_PRIVATE_KEY_FILE_PATH);
        String trustCertCollectionFilePath = properties.getProperty(TRUST_CERTIFICATE_FILE_PATH);

        if (trustCertCollectionFilePath != null) {
            builder.trustManager(new File(trustCertCollectionFilePath));
        }
        if (clientCertChainFilePath != null && clientPrivateKeyFilePath != null) {
            builder.keyManager(new File(clientCertChainFilePath), new File(clientPrivateKeyFilePath));
        }
        return builder;
    }

    private void initializeRpcStub() {
        rpcStream = asyncStub.cloudToMinionRPC(new RpcMessageHandler());
        // Need to send minion headers to gRPC server in order to register.
        sendMinionHeaders();
        LOG.info("Initialized RPC stream");
    }

    private void initializeSinkStub() {
        sinkStream = asyncStub.minionToCloudMessages(new EmptyMessageReceiver());
        LOG.info("Initialized Sink stream");
    }


    @SuppressWarnings({"rawtypes", "unchecked"})
    public void bind(RpcModule module) throws Exception {
        if (module != null) {
            final RpcModule<RpcRequest, RpcResponse> rpcModule = (RpcModule<RpcRequest, RpcResponse>) module;
            if (registerdModules.containsKey(rpcModule.getId())) {
                LOG.warn(" {} module is already registered", rpcModule.getId());
            } else {
                registerdModules.put(rpcModule.getId(), rpcModule);
                LOG.info("Registered module {} with gRPC IPC client", rpcModule.getId());
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void unbind(RpcModule module) throws Exception {
        if (module != null) {
            final RpcModule<RpcRequest, RpcResponse> rpcModule = (RpcModule<RpcRequest, RpcResponse>) module;
            registerdModules.remove(rpcModule.getId());
            LOG.info("Removing module {} from gRPC IPC client.", rpcModule.getId());
        }
    }

    private boolean hasChangedToReadyState() {
        ConnectivityState prevState = currentChannelState;
        return !prevState.equals(ConnectivityState.READY) && getChannelState().equals(ConnectivityState.READY);
    }

    public void shutdown() {
        requestHandlerExecutor.shutdown();
        blockingSinkMessageScheduler.shutdown();
        registerdModules.clear();
        if (rpcStream != null) {
            rpcStream.onCompleted();
        }
        channel.shutdown();
        LOG.info("Minion at location {} with systemId {} stopped", ipcIdentity.getLocation(), ipcIdentity.getId());
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }


    @Override
    public String getMetricDomain() {
        return SINK_METRIC_PRODUCER_DOMAIN;
    }

    @Override
    public BundleContext getBundleContext() {
        return bundleContext;
    }

    @Override
    public Tracer getTracer() {
        return tracer;
    }

    @Override
    public MetricRegistry getMetrics() {
        return metricRegistry;
    }

    ConnectivityState getChannelState() {
        return channel.getState(true);
    }

    @Override
    // public <S extends org.opennms.horizon.ipc.sink.api.Message, T extends org.opennms.horizon.ipc.sink.api.Message> void dispatch(SinkModule<S, T> module, String metadata, T message) {
    public <S extends Message, T extends Message> void dispatch(SinkModule<S, T> module, String metadata, T message) {

        try (MDCCloseable mdc = MDC.putCloseable("prefix", MessageConsumerManager.LOG_PREFIX)) {
            byte[] sinkMessageContent = module.marshal(message);
            String messageId = UUID.randomUUID().toString();
            SinkMessage.Builder sinkMessageBuilder = SinkMessage.newBuilder()
                    .setMessageId(messageId)
                    .setLocation(ipcIdentity.getLocation())
                    .setModuleId(module.getId())
                    .setContent(ByteString.copyFrom(sinkMessageContent));

            // If module has asyncpolicy, keep attempting to send message.
            if (module.getAsyncPolicy() != null) {
                sendBlockingSinkMessage(sinkMessageBuilder.build());
            } else {
                sendSinkMessage(sinkMessageBuilder.build());
            }
        }
    }

    private void sendBlockingSinkMessage(SinkMessage sinkMessage) {
        boolean succeeded = sendSinkMessage(sinkMessage);
        if (succeeded) {
            return;
        }
        //Recursively try to send sink message until it succeeds.
        scheduleSinkMessageAfterDelay(sinkMessage);
    }

    private boolean scheduleSinkMessageAfterDelay(SinkMessage sinkMessage) {
        ScheduledFuture<Boolean> future = blockingSinkMessageScheduler.schedule(
                () -> sendSinkMessage(sinkMessage), SINK_BLOCKING_TIMEOUT, TimeUnit.MILLISECONDS);
        try {
            boolean succeeded = future.get();
            if (succeeded) {
                return true;
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error while attempting to send sink message with id {} from module {} to gRPC IPC server",
                    sinkMessage.getMessageId(), sinkMessage.getModuleId(), e);
        }
        return scheduleSinkMessageAfterDelay(sinkMessage);
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
                LOG.error("Exception while sending sinkMessage to gRPC IPC server", e);
            }
        }
        return false;
    }


    private void sendMinionHeaders() {
        RpcResponseProto rpcHeader = RpcResponseProto.newBuilder()
                .setLocation(ipcIdentity.getLocation())
                .setSystemId(ipcIdentity.getId())
                .setModuleId(MINION_HEADERS_MODULE)
                .setRpcId(ipcIdentity.getId())
                .build();
        sendRpcResponse(rpcHeader);
        LOG.info("Sending Minion Headers from SystemId {} to gRPC server", ipcIdentity.getId());
    }

    private void processRpcRequest(RpcRequestProto requestProto) {
        long currentTime = requestProto.getExpirationTime();
        if (requestProto.getExpirationTime() < currentTime) {
            LOG.debug("ttl already expired for the request id = {}, won't process.", requestProto.getRpcId());
            return;
        }
        String moduleId = requestProto.getModuleId();
        if (moduleId.isBlank()) {
            return;
        }
        LOG.debug("Received RPC request with RpcID:{} for module {}", requestProto.getRpcId(), requestProto.getModuleId());
        RpcModule<RpcRequest, RpcResponse> rpcModule = registerdModules.get(moduleId);
        if (rpcModule == null) {
            return;
        }

        RpcRequest rpcRequest = rpcModule.unmarshalRequest(requestProto.getRpcContent().toStringUtf8());
        CompletableFuture<RpcResponse> future = rpcModule.execute(rpcRequest);
        future.whenComplete((res, ex) -> {
            final RpcResponse rpcResponse;
            if (ex != null) {
                // An exception occurred, store the exception in a new response
                LOG.warn("An error occured while executing a call in {}.", rpcModule.getId(), ex);
                rpcResponse = rpcModule.createResponseWithException(ex);
            } else {
                // No exception occurred, use the given response
                rpcResponse = res;
            }
            // Construct response using the same rpcId;
            String responseAsString = rpcModule.marshalResponse(rpcResponse);
            RpcResponseProto responseProto = RpcResponseProto.newBuilder()
                    .setRpcId(requestProto.getRpcId())
                    .setSystemId(ipcIdentity.getId())
                    .setLocation(requestProto.getLocation())
                    .setModuleId(requestProto.getModuleId())
                    .setRpcContent(ByteString.copyFrom(responseAsString, StandardCharsets.UTF_8))
                    .build();

            try {
                sendRpcResponse(responseProto);
                LOG.debug("Request with RpcId:{} for module {} handled successfully, and response was sent",
                        responseProto.getRpcId(), responseProto.getModuleId());
            } catch (Throwable e) {
                LOG.error("Error while sending RPC response {}", responseProto, e);
            }
        });
    }

    private synchronized void sendRpcResponse(RpcResponseProto rpcResponseProto) {
        if (rpcStream != null) {
            try {
                rpcStream.onNext(rpcResponseProto);
            } catch (Exception e) {
                LOG.error("Exception while sending RPC response : {}", rpcResponseProto);
            }
        } else {
            //throw new RuntimeException("RPC response handler not found");
            LOG.warn("gRPC IPC server is not in ready state");
        }
    }

    private class RpcMessageHandler implements StreamObserver<RpcRequestProto> {

        @Override
        public void onNext(RpcRequestProto rpcRequestProto) {

            try {
                // Run processing of RPC request in a different thread.
                requestHandlerExecutor.execute(() -> processRpcRequest(rpcRequestProto));
            } catch (Throwable e) {
                LOG.error("Error while processing the RPC Request {}", rpcRequestProto, e);
            }
        }

        @Override
        public void onError(Throwable throwable) {
            LOG.error("Error in RPC streaming", throwable);
            reconnectStrategy.activate();
        }

        @Override
        public void onCompleted() {
            LOG.error("Closing RPC message handler");
            reconnectStrategy.activate();
        }

    }

}
