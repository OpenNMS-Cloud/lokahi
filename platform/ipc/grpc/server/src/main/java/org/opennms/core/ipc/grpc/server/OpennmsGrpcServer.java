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

package org.opennms.core.ipc.grpc.server;

import static org.opennms.core.ipc.grpc.server.GrpcServerConstants.DEFAULT_GRPC_TTL;
import static org.opennms.core.ipc.grpc.server.GrpcServerConstants.GRPC_TTL_PROPERTY;
import static org.opennms.core.tracing.api.TracerConstants.TAG_TIMEOUT;
import static org.opennms.horizon.ipc.sink.api.Message.SINK_METRIC_CONSUMER_DOMAIN;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.opennms.core.grpc.common.GrpcIpcServer;
import org.opennms.core.ipc.grpc.common.Empty;
import org.opennms.core.ipc.grpc.common.SinkMessage;
import org.opennms.core.ipc.grpc.server.manager.LocationIndependentRpcClientFactory;
import org.opennms.core.ipc.grpc.server.manager.MinionManager;
import org.opennms.core.ipc.grpc.server.manager.RpcRequestTimeoutManager;
import org.opennms.core.ipc.grpc.server.manager.RpcConnectionTracker;
import org.opennms.core.ipc.grpc.server.manager.RpcRequestTracker;
import org.opennms.core.ipc.grpc.server.manager.adapter.MinionRSTransportAdapter;
import org.opennms.core.ipc.grpc.server.manager.rpc.RemoteRegistrationHandler;
import org.opennms.core.ipc.grpc.server.manager.rpcstreaming.MinionRpcStreamConnectionManager;
import org.opennms.core.tracing.api.TracerConstants;
import org.opennms.core.tracing.api.TracerRegistry;
import org.opennms.horizon.core.identity.Identity;
import org.opennms.horizon.core.lib.Logging;
import org.opennms.horizon.core.lib.PropertiesUtils;
import org.opennms.horizon.ipc.rpc.api.RemoteExecutionException;
import org.opennms.horizon.ipc.rpc.api.RequestTimedOutException;
import org.opennms.horizon.ipc.rpc.api.RpcClient;
import org.opennms.horizon.ipc.rpc.api.RpcClientFactory;
import org.opennms.horizon.ipc.rpc.api.RpcModule;
import org.opennms.horizon.ipc.rpc.api.RpcRequest;
import org.opennms.horizon.ipc.rpc.api.RpcResponse;
import org.opennms.horizon.ipc.rpc.api.RpcResponseHandler;
import org.opennms.horizon.ipc.sink.api.Message;
import org.opennms.horizon.ipc.sink.api.MessageConsumerManager;
import org.opennms.horizon.ipc.sink.api.SinkModule;
import org.opennms.horizon.ipc.sink.common.AbstractMessageConsumerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import io.grpc.stub.StreamObserver;
import io.opentracing.References;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapExtractAdapter;
import io.opentracing.util.GlobalTracer;

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
public class OpennmsGrpcServer extends AbstractMessageConsumerManager implements RpcClientFactory {

    private static final Logger LOG = LoggerFactory.getLogger(OpennmsGrpcServer.class);
    private final GrpcIpcServer grpcIpcServer;
    private String location;
    private Identity identity;
    private Properties properties;
    private MetricRegistry rpcMetrics;
    private MetricRegistry sinkMetrics;
    private JmxReporter rpcMetricsReporter;
    private JmxReporter sinkMetricsReporter;
    private TracerRegistry tracerRegistry;
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

    // Maintains the map of sink modules by it's id.
    private final Map<String, SinkModule<?, Message>> sinkModulesById = new ConcurrentHashMap<>();
    // Maintains the map of sink consumer executor and by module Id.
    private final Map<String, ExecutorService> sinkConsumersByModuleId = new ConcurrentHashMap<>();

//========================================
// Constructor
//----------------------------------------

    public OpennmsGrpcServer(GrpcIpcServer grpcIpcServer) {
        this.grpcIpcServer = grpcIpcServer;
    }


//========================================
// Lifecycle
//----------------------------------------

    public void start() throws IOException {
        try (Logging.MDCCloseable mdc = Logging.withPrefixCloseable(RpcClientFactory.LOG_PREFIX)) {

            grpcIpcServer.startServer(
                    new MinionRSTransportAdapter(minionRpcStreamConnectionManager::startRpcStreaming, this::processSinkStreamingCall)
            );

            LOG.info("Added RPC/Sink Service to OpenNMS IPC Grpc Server");

            properties = grpcIpcServer.getProperties();
            rpcMetricsReporter = JmxReporter.forRegistry(getRpcMetrics())
                    .inDomain(JMX_DOMAIN_RPC)
                    .build();
            rpcMetricsReporter.start();
            sinkMetricsReporter = JmxReporter.forRegistry(getRpcMetrics())
                    .inDomain(SINK_METRIC_CONSUMER_DOMAIN)
                    .build();
            sinkMetricsReporter.start();
            // Initialize tracer from tracer registry.
            if (tracerRegistry != null) {
                tracerRegistry.init(identity.getId());
            }
        }
    }

    public void shutdown() {
        closed.set(true);
        rpcConnectionTracker.clear();

        rpcRequestTracker.clear();
        sinkModulesById.clear();
        if (rpcMetricsReporter != null) {
            rpcMetricsReporter.close();
        }
        if (sinkMetricsReporter != null) {
            sinkMetricsReporter.close();
        }
        grpcIpcServer.stopServer();
        LOG.info("OpenNMS gRPC server stopped");
    }

//========================================
// Getters and Setters
//----------------------------------------

    public String getLocation() {
        if (location == null && getIdentity() != null) {
            return getIdentity().getLocation();
        }
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Identity getIdentity() {
        return identity;
    }

    public void setIdentity(Identity identity) {
        this.identity = identity;
    }

    private MetricRegistry getRpcMetrics() {
        if (rpcMetrics == null) {
            rpcMetrics = new MetricRegistry();
        }
        return rpcMetrics;
    }

    public void setRpcMetrics(MetricRegistry metricRegistry) {
        this.rpcMetrics = metricRegistry;
    }

    public MetricRegistry getSinkMetrics() {

        if (sinkMetrics == null) {
            sinkMetrics = new MetricRegistry();
        }
        return sinkMetrics;
    }

    public void setSinkMetrics(MetricRegistry sinkMetrics) {
        this.sinkMetrics = sinkMetrics;
    }

    public TracerRegistry getTracerRegistry() {
        return tracerRegistry;
    }

    public void setTracerRegistry(TracerRegistry tracerRegistry) {
        this.tracerRegistry = tracerRegistry;
    }

    public Tracer getTracer() {
        if (tracerRegistry != null) {
            return tracerRegistry.getTracer();
        }
        return GlobalTracer.get();
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


//========================================
// Operations
//----------------------------------------

    @Override
    public <S extends RpcRequest, T extends RpcResponse> RpcClient<S, T> getClient(RpcModule<S, T> module) {

        RemoteRegistrationHandler remoteRegistrationHandler =
                (request, timeout, span, future) -> registerRemoteCall(request, timeout, future, module, span);

        return locationIndependentRpcClientFactory.createClient(module, remoteRegistrationHandler);
    }


//========================================
// Message Consumer Manager
//----------------------------------------

    @Override
    protected void startConsumingForModule(SinkModule<?, Message> module) throws Exception {
        if (sinkConsumersByModuleId.get(module.getId()) == null) {
            int numOfThreads = getNumConsumerThreads(module);
            ExecutorService executor = Executors.newFixedThreadPool(numOfThreads, sinkConsumerThreadFactory);
            sinkConsumersByModuleId.put(module.getId(), executor);
            LOG.info("Adding {} consumers for module: {}", numOfThreads, module.getId());
        }
        sinkModulesById.putIfAbsent(module.getId(), module);
    }

    @Override
    protected void stopConsumingForModule(SinkModule<?, Message> module) throws Exception {

        ExecutorService executor = sinkConsumersByModuleId.get(module.getId());
        if (executor != null) {
            executor.shutdownNow();
        }
        LOG.info("Stopped consumers for module: {}", module.getId());
        sinkModulesById.remove(module.getId());
    }

//========================================
// Internals
//----------------------------------------

    private String registerRemoteCall(RpcRequest request, long expiration, CompletableFuture future, RpcModule localModule, Span span) {
        String rpcId = UUID.randomUUID().toString();

        Map<String, String> loggingContext = Logging.getCopyOfContextMap();

        RpcResponseHandlerImpl responseHandler =
                new RpcResponseHandlerImpl(future, localModule, rpcId, request.getLocation(), expiration, span, loggingContext);

        rpcRequestTracker.addRequest(rpcId, responseHandler);
        rpcRequestTimeoutManager.registerRequestTimeout(responseHandler);

        return rpcId;
    }

    private StreamObserver<SinkMessage> processSinkStreamingCall(StreamObserver<Empty> responseObserver) {
        return new StreamObserver<SinkMessage>() {

            @Override
            public void onNext(SinkMessage sinkMessage) {

                if (!Strings.isNullOrEmpty(sinkMessage.getModuleId())) {
                    ExecutorService sinkModuleExecutor = sinkConsumersByModuleId.get(sinkMessage.getModuleId());
                    if(sinkModuleExecutor != null) {
                        sinkModuleExecutor.execute(() -> dispatchSinkMessage(sinkMessage));
                    }
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
        SinkModule<?, Message> sinkModule = sinkModulesById.get(sinkMessage.getModuleId());
        if (sinkModule != null && sinkMessage.getContent() != null) {
            Message message = sinkModule.unmarshal(sinkMessage.getContent().toByteArray());

            MessageConsumerManager.updateMessageSize(getSinkMetrics(), sinkMessage.getLocation(),
                    sinkMessage.getModuleId(), sinkMessage.getSerializedSize());
            Timer dispatchTime = MessageConsumerManager.getDispatchTimerMetric(getSinkMetrics(),
                    sinkMessage.getLocation(), sinkMessage.getModuleId());

            Tracer.SpanBuilder spanBuilder = buildSpanFromSinkMessage(sinkMessage);

            try (Scope scope = spanBuilder.startActive(true);
                 Timer.Context context = dispatchTime.time()) {
                scope.span().setTag(TracerConstants.TAG_MESSAGE_SIZE, sinkMessage.getSerializedSize());
                scope.span().setTag(TracerConstants.TAG_THREAD, Thread.currentThread().getName());
                dispatch(sinkModule, message);
            }
        }
    }

    private Tracer.SpanBuilder buildSpanFromSinkMessage(SinkMessage sinkMessage) {

        Tracer tracer = getTracer();
        Tracer.SpanBuilder spanBuilder;
        Map<String, String> tracingInfoMap = new HashMap<>();
        sinkMessage.getTracingInfoMap().forEach(tracingInfoMap::put);
        SpanContext context = tracer.extract(Format.Builtin.TEXT_MAP, new TextMapExtractAdapter(tracingInfoMap));
        if (context != null) {
            // Span on consumer side will follow the span from producer (minion).
            spanBuilder = tracer.buildSpan(sinkMessage.getModuleId()).addReference(References.FOLLOWS_FROM, context);
        } else {
            spanBuilder = tracer.buildSpan(sinkMessage.getModuleId());
        }
        return spanBuilder;
    }

    // TODO: move to top-level class
    private class RpcResponseHandlerImpl<S extends RpcRequest, T extends RpcResponse> implements RpcResponseHandler {

        private final CompletableFuture<T> responseFuture;
        private final RpcModule<S, T> rpcModule;
        private final String rpcId;
        private final String location;
        private final long expirationTime;
        private final Map<String, String> loggingContext;
        private boolean isProcessed = false;
        private final Long requestCreationTime;
        private Span span;

        private RpcResponseHandlerImpl(CompletableFuture<T> responseFuture, RpcModule<S, T> rpcModule, String rpcId,
                                       String location, long timeout, Span span, Map<String, String> loggingContext) {
            this.responseFuture = responseFuture;
            this.rpcModule = rpcModule;
            this.rpcId = rpcId;
            this.location = location;
            this.expirationTime = timeout;
            this.loggingContext = loggingContext;
            this.span = span;
            this.requestCreationTime = System.currentTimeMillis();
        }

        @Override
        public void sendResponse(String message) {

            try (Logging.MDCCloseable mdc = Logging.withContextMapCloseable(loggingContext)) {
                if (message != null) {
                    T response = rpcModule.unmarshalResponse(message);
                    if (response.getErrorMessage() != null) {
                        span.log(response.getErrorMessage());
                        RpcClientFactory.markFailed(getRpcMetrics(), this.location, rpcModule.getId());
                        responseFuture.completeExceptionally(new RemoteExecutionException(response.getErrorMessage()));
                    } else {
                        responseFuture.complete(response);
                    }
                    isProcessed = true;
                    RpcClientFactory.updateResponseSize(getRpcMetrics(), this.location, rpcModule.getId(), message.getBytes().length);
                } else {
                    span.setTag(TAG_TIMEOUT, "true");
                    RpcClientFactory.markFailed(getRpcMetrics(), this.location, rpcModule.getId());
                    responseFuture.completeExceptionally(new RequestTimedOutException(new TimeoutException()));
                }
                RpcClientFactory.updateDuration(getRpcMetrics(), this.location, rpcModule.getId(), System.currentTimeMillis() - requestCreationTime);
                rpcRequestTracker.remove(rpcId);
                span.finish();
            } catch (Throwable e) {
                LOG.error("Error while processing RPC response {}", message, e);
            }
            if (isProcessed) {
                LOG.debug("RPC Response from module: {} handled successfully for RpcId:{}.", rpcId, rpcModule.getId());
            }
        }

        @Override
        public boolean isProcessed() {
            return isProcessed;
        }

        @Override
        public String getRpcId() {
            return rpcId;
        }


        @Override
        public int compareTo(Delayed other) {
            long myDelay = getDelay(TimeUnit.MILLISECONDS);
            long otherDelay = other.getDelay(TimeUnit.MILLISECONDS);
            return Long.compare(myDelay, otherDelay);
        }

        @Override
        public long getDelay(TimeUnit unit) {
            long now = System.currentTimeMillis();
            return unit.convert(expirationTime - now, TimeUnit.MILLISECONDS);
        }

        public RpcModule<S, T> getRpcModule() {
            return rpcModule;
        }
    }

}
