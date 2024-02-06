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

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.opennms.cloud.grpc.minion.MinionToCloudMessage;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcResponseProto;
import org.opennms.cloud.grpc.minion.SinkMessage;
import org.opennms.cloud.grpc.minion_gateway.GatewayRpcResponseProto;
import org.opennms.cloud.grpc.minion_gateway.MinionIdentity;
import org.opennms.horizon.grpc.heartbeat.contract.HeartbeatMessage;
import org.opennms.horizon.shared.grpc.common.GrpcIpcServer;
import org.opennms.horizon.shared.grpc.common.LocationServerInterceptor;
import org.opennms.horizon.shared.grpc.common.TenantIDGrpcServerInterceptor;
import org.opennms.horizon.shared.grpc.interceptor.InterceptorFactory;
import org.opennms.horizon.shared.grpc.interceptor.MeteringInterceptorFactory;
import org.opennms.horizon.shared.ipc.grpc.server.manager.MinionManager;
import org.opennms.horizon.shared.ipc.grpc.server.manager.OutgoingMessageHandler;
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

import com.google.common.base.Strings;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.protobuf.Empty;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

import io.grpc.BindableService;
import io.grpc.Context;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

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
    private MinionRpcStreamConnectionManager minionRpcStreamConnectionManager;
    private RpcRequestTimeoutManager rpcRequestTimeoutManager;
    private MinionManager minionManager;
    private TenantIDGrpcServerInterceptor tenantIDGrpcServerInterceptor;
    private LocationServerInterceptor locationServerInterceptor;
    private final boolean debugSpanFullMessage;
    private final boolean debugSpanContent;

    private final Map<String, Consumer<SinkMessage>> sinkDispatcherById = new ConcurrentHashMap<>();

    // Maintains the map of sink consumer executor and by module Id.
    private final Map<String, ExecutorService> sinkConsumersByModuleId = new ConcurrentHashMap<>();
    private BiConsumer<RpcRequestProto, StreamObserver<RpcResponseProto>> incomingRpcHandler;
    private OutgoingMessageHandler outgoingMessageHandler;

    private final Tracer tracer;

    private MeterRegistry meterRegistry;

//========================================
// Constructor
//----------------------------------------

    public OpennmsGrpcServer(GrpcIpcServer grpcIpcServer, final MeterRegistry meterRegistry, final Tracer tracer, boolean debugSpanFullMessage, boolean debugSpanContent) {
        this.grpcIpcServer = grpcIpcServer;
        this.interceptors = List.of(
            new MeteringInterceptorFactory(meterRegistry)
        );

        this.meterRegistry = Objects.requireNonNull(meterRegistry);
        this.tracer = tracer;
        this.debugSpanFullMessage = debugSpanFullMessage;
        this.debugSpanContent = debugSpanContent;
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
    public LocationServerInterceptor getLocationServerInterceptor() {
        return locationServerInterceptor;
    }

    public void setLocationServerInterceptor(LocationServerInterceptor locationServerInterceptor) {
        this.locationServerInterceptor = locationServerInterceptor;
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
            final var span = tracer.spanBuilder("dispatch " + module.getId())
                .setAttribute(SemanticAttributes.CODE_NAMESPACE, module.getClass().getName())
                .startSpan();

            try (var ss = span.makeCurrent()) {
                dispatch(module, message);
            } catch (Throwable throwable) {
                span.setStatus(StatusCode.ERROR, "Received exception during dispatch: " + throwable);
                span.recordException(throwable);
                throw new UndeclaredThrowableException(throwable);
            } finally {
                span.end();
            }
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

    @Override
    public <S extends Message, T extends Message> void dispatch(final SinkModule<S, T> module, final T message) {
        this.meterRegistry.timer("consumer.dispatch",
            "module", module.getId())
            .record(() -> super.dispatch(module, message));
    }

    //========================================
// Internals
//----------------------------------------

    private StreamObserver<MinionToCloudMessage> processSinkStreamingCall(StreamObserver<Empty> responseObserver) {
        final var streamSpan = Span.current(); // stash for linking future messages back to the stream
        final AtomicReference<Attributes> attributes = new AtomicReference<>(Attributes.builder()
            .put("user", tenantIDGrpcServerInterceptor.readCurrentContextTenantId())
            .put("location", locationServerInterceptor.readCurrentContextLocationId())
            // we don't have systemId yet -- we grab this from the first heartbeat message we see
            .build());
        final var haveSystemId = new AtomicBoolean(false);

        streamSpan.setAllAttributes(attributes.get()); // Add attributes to the stream span now

        return new StreamObserver<MinionToCloudMessage>() {
            @Override
            public void onNext(MinionToCloudMessage message) {
                // We don't know the message type yet, but will update later if we can
                final var span = tracer.spanBuilder("MinionToCloudMessage receive unknown")
                    .setSpanKind(SpanKind.CONSUMER)
                    .setAllAttributes(attributes.get())
                    .setAttribute("size", message.getSerializedSize())
                    .setNoParent() // we don't want each message to be lost in the long-running streaming trace
                    .addLink(Span.current().getSpanContext()) // but we do want to link to the long-running trace
                    .startSpan();

                try (var ss = span.makeCurrent()) {
                    if (message.hasSinkMessage()) {
                        SinkMessage sinkMessage = message.getSinkMessage();
                        span.updateName("MinionToCloudMessages receive " + sinkMessage.getModuleId());
                        span.setAttribute("moduleId", sinkMessage.getModuleId());
                        span.setAttribute("messageId", sinkMessage.getMessageId());
                        if (sinkMessage.hasIdentity()) {
                            span.setAttribute("identity", sinkMessage.getIdentity().toString());
                        }

                        if (debugSpanFullMessage) {
                            span.setAttribute("message", sinkMessage.toString());
                        }
                        if (debugSpanContent) {
                            span.setAttribute("content", sinkMessage.getContent().toString());
                        }

                        // We won't have the system ID until we receive the first heartbeat message, so
                        // once we get it, (1) we stash it with a full set of identity attributes for
                        // future message spans, (2) we set it on the long-running stream span, and
                        // (3) we set it on our current span. And we make sure to only do this once.
                        if (!haveSystemId.get() && "heartbeat".equals(sinkMessage.getModuleId())) {
                            try {
                                var heartbeatMessage = HeartbeatMessage.parseFrom(sinkMessage.getContent());
                                if (heartbeatMessage.getIdentity() != null) {
                                    var systemId = heartbeatMessage.getIdentity().getSystemId();
                                    attributes.set(Attributes.builder()
                                        .putAll(attributes.get())
                                        .put("systemId", systemId)
                                        .build());
                                    streamSpan.setAttribute("systemId", systemId);
                                    span.setAttribute("systemId", systemId);
                                    haveSystemId.set(true);
                                }
                            } catch (InvalidProtocolBufferException e) {
                                // ignore
                            }
                        }

                        if (!Strings.isNullOrEmpty(sinkMessage.getModuleId())) {
                            ExecutorService sinkModuleExecutor = sinkConsumersByModuleId.get(sinkMessage.getModuleId());
                            if (sinkModuleExecutor != null) {
                                // Schedule execution with the ExecutorService, with the current GRPC context active
                                Context.currentContextExecutor(sinkModuleExecutor)
                                    .execute(() -> dispatchSinkMessage(sinkMessage));
                            } else {
                                LOG.error("Ignoring sink message; no module executor registered: module-id={}; identity={}; message-id={}",
                                    sinkMessage.getModuleId(),
                                    sinkMessage.getIdentity(),
                                    sinkMessage.getMessageId()
                                );
                                span.setStatus(StatusCode.ERROR, "Ignoring sink message; no module executor registered");
                            }
                        } else {
                            LOG.error("Ignoring sink message with null or empty module-id: identity={}; message-id={}",
                                sinkMessage.getIdentity(),
                                sinkMessage.getMessageId()
                            );
                            span.setStatus(StatusCode.ERROR, "Ignoring sink message with null or empty module-id");
                        }
                    } else {
                        LOG.error("Unsupported message {}", message);
                        span.setStatus(StatusCode.ERROR, "Unsupported message (expecting SinkMessage)");
                        span.setAttribute("message", message.toString());
                    }
                } catch (Throwable throwable) {
                    span.setStatus(StatusCode.ERROR, "Received exception during dispatch: " + throwable);
                    span.recordException(throwable);
                    throw new UndeclaredThrowableException(throwable);
                } finally {
                    span.end();
                }
            }

            @Override
            public void onError(Throwable throwable) {
                if (throwable instanceof StatusRuntimeException statusRuntimeException
                    && statusRuntimeException.getStatus().getCode() == Status.Code.CANCELLED) {
                    LOG.warn("Got status code CANCELLED in sink streaming");
                } else {
                    LOG.error("Error in sink streaming", throwable);
                }
            }

            @Override
            public void onCompleted() {
                LOG.info("sink streaming stream completed");
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

    public void setOutgoingMessageHandler(OutgoingMessageHandler outgoingMessageHandler) {
        this.outgoingMessageHandler = outgoingMessageHandler;
    }

    @Override
    public CompletableFuture<GatewayRpcResponseProto> dispatch(String tenantId, String locationId, RpcRequestProto request) {
        StreamObserver<RpcRequestProto> rpcHandler = rpcConnectionTracker.lookupByLocationRoundRobin(tenantId, locationId);
        if (rpcHandler == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Unknown location " + locationId));
        }
        return dispatch(rpcHandler, locationId, request);
    }

    @Override
    public CompletableFuture<GatewayRpcResponseProto> dispatch(String tenantId, String locationId, String systemId, RpcRequestProto request) {
        StreamObserver<RpcRequestProto> rpcHandler = rpcConnectionTracker.lookupByMinionId(tenantId, systemId);
        if (rpcHandler == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Unknown system id " + systemId));
        }
        return dispatch(rpcHandler, locationId, request);
    }

    private CompletableFuture<GatewayRpcResponseProto> dispatch(StreamObserver<RpcRequestProto> rpcHandler, String location, RpcRequestProto request) {
        final var span = tracer.spanBuilder("CloudToMinionRPC dispatch " + request.getModuleId())
            .setSpanKind(SpanKind.CLIENT)
            .addLink(rpcConnectionTracker.getConnectionSpanContext(rpcHandler))
            .setAllAttributes(rpcConnectionTracker.getConnectionSpanAttributes(rpcHandler))
            .setAttribute("request_size", request.getSerializedSize())
            .setAttribute("rpcId", request.getRpcId())
            .setAttribute("expiration", request.getExpirationTime())
            .setAttribute("moduleId", request.getModuleId())
            .startSpan();

        if (debugSpanFullMessage) {
            span.setAttribute("request", request.toString());
        }
        if (debugSpanContent && request.hasPayload()) {
            span.setAttribute("request_payload", request.getPayload().toString());
        }

        try (var ss = span.makeCurrent()) {
            CompletableFuture<RpcResponseProto> future = new CompletableFuture<>();
            String rpcId = request.getRpcId();
            BasicRpcResponseHandler responseHandler = new BasicRpcResponseHandler(request.getExpirationTime(), rpcId, request.getModuleId(), future);
            rpcHandler.onNext(request);
            rpcRequestTracker.addRequest(rpcId, responseHandler);
            rpcRequestTimeoutManager.registerRequestTimeout(responseHandler);
            return future.whenComplete((r, e) -> {
                rpcRequestTracker.remove(rpcId);
                if (r != null) {
                    span.setAttribute("response_size", r.getSerializedSize());
                    if (debugSpanFullMessage) {
                        span.setAttribute("response", r.toString());
                    }
                    if (debugSpanContent) {
                        span.setAttribute("response_payload", r.getPayload().toString());
                    }
                }
                if (e != null) {
                    span.setStatus(StatusCode.ERROR, "Received exception during dispatch future: " + e);
                    span.recordException(e);
                }
                span.end();
            }).thenApply(response -> {
                return GatewayRpcResponseProto.newBuilder()
                    .setRpcId(response.getRpcId())
                    .setIdentity(MinionIdentity.newBuilder()
                        .setSystemId(response.getIdentity().getSystemId())
                        .setLocationId(location)
                    )
                    .setModuleId(response.getModuleId())
                    .setPayload(response.getPayload())
                    .build();
            });
        } catch (Throwable throwable) {
            span.setStatus(StatusCode.ERROR, "Received exception during dispatch: " + throwable);
            span.recordException(throwable);
            span.end();
            throw new UndeclaredThrowableException(throwable);
        } finally {
            // In the non-exception case the span is ended earlier in future's whenComplete action.
            // In the exception case, the span is ended in the catch clause earlier.
            // So, no span.end() here.
        }
    }

}
