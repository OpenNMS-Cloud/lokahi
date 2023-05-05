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

package org.opennms.horizon.shared.ipc.grpc.server;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.grpc.BindableService;
import io.grpc.Context;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.opennms.cloud.grpc.minion.CloudToMinionMessage;
import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.cloud.grpc.minion.MinionToCloudMessage;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcResponseProto;
import org.opennms.cloud.grpc.minion.SinkMessage;
import org.opennms.horizon.shared.grpc.common.GrpcIpcServer;
import org.opennms.horizon.shared.grpc.common.TenantIDGrpcServerInterceptor;
import org.opennms.horizon.shared.grpc.interceptor.InterceptorFactory;
import org.opennms.horizon.shared.ipc.grpc.server.manager.LocationIndependentRpcClientFactory;
import org.opennms.horizon.shared.ipc.grpc.server.manager.MinionManager;
import org.opennms.horizon.shared.ipc.grpc.server.manager.RpcConnectionTracker;
import org.opennms.horizon.shared.ipc.grpc.server.manager.RpcRequestDispatcher;
import org.opennms.horizon.shared.ipc.grpc.server.manager.RpcRequestTimeoutManager;
import org.opennms.horizon.shared.ipc.grpc.server.manager.RpcRequestTracker;
import org.opennms.horizon.shared.ipc.grpc.server.manager.adapter.MinionRSTransportAdapter;
import org.opennms.horizon.shared.ipc.grpc.server.manager.rpcstreaming.MinionRpcStreamConnectionManager;
import org.opennms.horizon.shared.ipc.rpc.api.RpcClientFactory;
import org.opennms.horizon.shared.ipc.sink.api.SinkModule;
import org.opennms.horizon.shared.ipc.sink.common.AbstractMessageConsumerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.MDC.MDCCloseable;

/**
 * OpenNMS GRPC Server runs as OSGI bundle and it runs both RPC/Sink together.
 * gRPC runs in a typical web server/client mode, so gRPC client runs on each minion and gRPC server runs on OpenNMS.
 * Server initializes and creates two observers (RPC/Sink) that receive messages from the client (Minion).
 * <p>
 * RPC : RPC runs in bi-directional streaming mode. OpenNMS needs a client(minion) handle for sending RPC request
 * so minion always sends it's headers (SystemId/location) when it initializes. This Server maintains a list of
 * client(minion) handles and sends RPC request to each minion in round-robin fashion. When it is directed RPC, server
 * invokes specific minion handle directly.
 * For each RPC request received, server creates a rpcId and maintains the state of this request in the concurrent map.
 * The request is also added to a delay queue which can timeout the request if response is not received within expiration
 * time. RPC responses are received in the observers that are created at start. Each response handling is done in a
 * separate thread which may be used by rpc module to process the response.
 * <p>
 * Sink: Sink runs in uni-directional streaming mode. OpenNMS receives sink messages from client and they are dispatched
 * in the consumer threads that are initialized at start.
 */

@SuppressWarnings("rawtypes")
public class OpennmsGrpcServer extends AbstractMessageConsumerManager implements RpcRequestDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(OpennmsGrpcServer.class);

    private final GrpcIpcServer grpcIpcServer;
    private final List<InterceptorFactory> interceptors;
    private String location;
    private Properties properties;
    private AtomicBoolean closed = new AtomicBoolean(false);
    private final ThreadFactory sinkConsumerThreadFactory = new ThreadFactoryBuilder()
            .setNameFormat("sink-consumer-%d")
            .build();

    private RpcConnectionTracker rpcConnectionTracker;
    private RpcRequestTracker rpcRequestTracker;
    private LocationIndependentRpcClientFactory locationIndependentRpcClientFactory;
    private MinionRpcStreamConnectionManager minionRpcStreamConnectionManager;
    private RpcRequestTimeoutManager rpcRequestTimeoutManager;
    private MinionManager minionManager;
    private TenantIDGrpcServerInterceptor tenantIDGrpcServerInterceptor;

    private final Map<String, Consumer<SinkMessage>> sinkDispatcherById = new ConcurrentHashMap<>();

    // Maintains the map of sink consumer executor and by module Id.
    private final Map<String, ExecutorService> sinkConsumersByModuleId = new ConcurrentHashMap<>();
    private BiConsumer<RpcRequestProto, StreamObserver<RpcResponseProto>> incomingRpcHandler;
    private BiConsumer<Identity, StreamObserver<CloudToMinionMessage>> outgoingMessageHandler;

//========================================
// Constructor
//----------------------------------------

    public OpennmsGrpcServer(GrpcIpcServer grpcIpcServer) {
        this(grpcIpcServer, Collections.emptyList());
    }

    public OpennmsGrpcServer(GrpcIpcServer grpcIpcServer, List<InterceptorFactory> interceptors) {
        this.grpcIpcServer = grpcIpcServer;
        this.interceptors = interceptors;
    }

//========================================
// Lifecycle
//----------------------------------------

    public void start() throws IOException {
        try (MDCCloseable mdc = MDC.putCloseable("prefix", RpcClientFactory.LOG_PREFIX)) {

            MinionRSTransportAdapter adapter = new MinionRSTransportAdapter(
                minionRpcStreamConnectionManager::startRpcStreaming,
                this.outgoingMessageHandler,
                this.incomingRpcHandler,
                this::processSinkStreamingCall
            );

            BindableService service = adapter;
            if (!interceptors.isEmpty()) {
                for (InterceptorFactory interceptorFactory : interceptors) {
                    service = interceptorFactory.create(service);
                }
            }
            grpcIpcServer.startServer(service);
            LOG.info("Added RPC/Sink Service to OpenNMS IPC Grpc Server");
        }
    }

    public void shutdown() {
        closed.set(true);
        rpcConnectionTracker.clear();

        rpcRequestTracker.clear();
        sinkDispatcherById.clear();
        grpcIpcServer.stopServer();
        LOG.info("OpenNMS gRPC server stopped");
    }

//========================================
// Getters and Setters
//----------------------------------------

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public MinionManager getMinionManager() {
        return minionManager;
    }

    public void setMinionManager(MinionManager minionManager) {
        this.minionManager = minionManager;
    }

    public LocationIndependentRpcClientFactory getLocationIndependentRpcClientFactory() {
        return locationIndependentRpcClientFactory;
    }

    public void setLocationIndependentRpcClientFactory(LocationIndependentRpcClientFactory locationIndependentRpcClientFactory) {
        this.locationIndependentRpcClientFactory = locationIndependentRpcClientFactory;
    }

    public RpcConnectionTracker getRpcConnectionTracker() {
        return rpcConnectionTracker;
    }

    public void setRpcConnectionTracker(RpcConnectionTracker rpcConnectionTracker) {
        this.rpcConnectionTracker = rpcConnectionTracker;
    }

    public RpcRequestTracker getRpcRequestTracker() {
        return rpcRequestTracker;
    }

    public void setRpcRequestTracker(RpcRequestTracker rpcRequestTracker) {
        this.rpcRequestTracker = rpcRequestTracker;
    }

    public MinionRpcStreamConnectionManager getMinionRpcStreamConnectionManager() {
        return minionRpcStreamConnectionManager;
    }

    public void setMinionRpcStreamConnectionManager(MinionRpcStreamConnectionManager minionRpcStreamConnectionManager) {
        this.minionRpcStreamConnectionManager = minionRpcStreamConnectionManager;
    }

    public RpcRequestTimeoutManager getRpcRequestTimeoutManager() {
        return rpcRequestTimeoutManager;
    }

    public void setRpcRequestTimeoutManager(RpcRequestTimeoutManager rpcRequestTimeoutManager) {
        this.rpcRequestTimeoutManager = rpcRequestTimeoutManager;
    }

    public TenantIDGrpcServerInterceptor getTenantIDGrpcServerInterceptor() {
        return tenantIDGrpcServerInterceptor;
    }

    public void setTenantIDGrpcServerInterceptor(TenantIDGrpcServerInterceptor tenantIDGrpcServerInterceptor) {
        this.tenantIDGrpcServerInterceptor = tenantIDGrpcServerInterceptor;
    }

//========================================
// Operations
//----------------------------------------

//========================================
// Message Consumer Manager
//----------------------------------------

    @Override
    protected <S extends Message, T extends Message> void startConsumingForModule(SinkModule<S, T> module) {
        if (sinkConsumersByModuleId.get(module.getId()) == null) {
            int numOfThreads = getNumConsumerThreads(module);
            ExecutorService executor = Executors.newFixedThreadPool(numOfThreads, sinkConsumerThreadFactory);
            sinkConsumersByModuleId.put(module.getId(), executor);
            LOG.info("Adding {} consumers for module: {}", numOfThreads, module.getId());
        }
        sinkDispatcherById.computeIfAbsent(module.getId(), id -> sinkMessage -> {
            final T message = module.unmarshal(sinkMessage.getContent().toByteArray());
            dispatch(module, message);
        });
    }

    @Override
    protected void stopConsumingForModule(SinkModule<? extends Message, ? extends Message> module) {
        ExecutorService executor = sinkConsumersByModuleId.get(module.getId());
        if (executor != null) {
            executor.shutdownNow();
        }
        LOG.info("Stopped consumers for module: {}", module.getId());
        sinkDispatcherById.remove(module.getId());
    }

//========================================
// Internals
//----------------------------------------

    private StreamObserver<MinionToCloudMessage> processSinkStreamingCall(StreamObserver<Empty> responseObserver) {
        return new StreamObserver<>() {

            @Override
            public void onNext(MinionToCloudMessage message) {
                if (message.hasSinkMessage()) {
                    SinkMessage sinkMessage = message.getSinkMessage();
                    if (!Strings.isNullOrEmpty(sinkMessage.getModuleId())) {
                        ExecutorService sinkModuleExecutor = sinkConsumersByModuleId.get(sinkMessage.getModuleId());
                        if (sinkModuleExecutor != null) {
                            // Schedule execution with the ExecutorService, with the current GRPC context active
                            Context.currentContextExecutor(sinkModuleExecutor)
                                .execute(() -> dispatchSinkMessage(sinkMessage));
                        } else {
                            LOG.error("Ignoring sink message; no module executor registered: module-id={}; location={}; system-id={}; message-id={}",
                                sinkMessage.getModuleId(),
                                sinkMessage.getLocation(),
                                sinkMessage.getSystemId(),
                                sinkMessage.getMessageId()
                            );
                        }
                    } else {
                        LOG.error("Ignoring sink message with null or empty module-id: location={}; system-id={}; message-id={}",
                            sinkMessage.getLocation(),
                            sinkMessage.getSystemId(),
                            sinkMessage.getMessageId()
                        );
                    }
                } else {
                    LOG.error("Unsupported message {}", message);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                LOG.error("Error in sink streaming", throwable);
            }

            @Override
            public void onCompleted() {

            }
        };
    }

    private void dispatchSinkMessage(SinkMessage sinkMessage) {
        final var dispatcher = this.sinkDispatcherById.get(sinkMessage.getModuleId());
        if (dispatcher == null) {
            return;
        }

        dispatcher.accept(sinkMessage);
    }

    public void setIncomingRpcHandler(BiConsumer<RpcRequestProto, StreamObserver<RpcResponseProto>> handler) {
        this.incomingRpcHandler = handler;
    }

    public void setOutgoingMessageHandler(BiConsumer<Identity, StreamObserver<CloudToMinionMessage>> outgoingMessageHandler) {
        this.outgoingMessageHandler = outgoingMessageHandler;
    }

    @Override
    public CompletableFuture<RpcResponseProto> dispatch(String tenantId, String location, RpcRequestProto request) {
        StreamObserver<RpcRequestProto> rpcHandler = rpcConnectionTracker.lookupByLocationRoundRobin(tenantId, location);
        if (rpcHandler == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Unknown location " + location));
        }
        return dispatch(rpcHandler, request);
    }

    @Override
    public CompletableFuture<RpcResponseProto> dispatch(String tenantId, String location, String systemId, RpcRequestProto request) {
        StreamObserver<RpcRequestProto> rpcHandler = rpcConnectionTracker.lookupByMinionId(tenantId, systemId);
        if (rpcHandler == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Unknown system id " + systemId));
        }
        return dispatch(rpcHandler, request);
    }

    private CompletableFuture<RpcResponseProto> dispatch(StreamObserver<RpcRequestProto> rpcHandler, RpcRequestProto request) {
        CompletableFuture future = new CompletableFuture();
        String rpcId = request.getRpcId();
        BasicRpcResponseHandler responseHandler = new BasicRpcResponseHandler(request.getExpirationTime(), rpcId, request.getModuleId(), future);
        rpcHandler.onNext(request);
        rpcRequestTracker.addRequest(rpcId, responseHandler);
        rpcRequestTimeoutManager.registerRequestTimeout(responseHandler);
        return future.whenComplete((r, e) -> {
            rpcRequestTracker.remove(rpcId);
        });
    }


}
