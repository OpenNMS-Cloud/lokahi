package org.opennms.miniongateway.grpc.server;

import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.grpc.stub.StreamObserver;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.function.BiConsumer;
import org.opennms.cloud.grpc.minion.CloudServiceGrpc.CloudServiceImplBase;
import org.opennms.cloud.grpc.minion.CloudToMinionMessage;
import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.horizon.shared.grpc.common.GrpcIpcServer;
import org.opennms.horizon.shared.grpc.interceptor.MeteringInterceptorFactory;
import org.opennms.horizon.shared.ipc.grpc.server.OpennmsGrpcServer;
import org.opennms.horizon.shared.ipc.grpc.server.manager.LocationIndependentRpcClientFactory;
import org.opennms.horizon.shared.ipc.grpc.server.manager.MinionManager;
import org.opennms.horizon.shared.ipc.grpc.server.manager.RpcConnectionTracker;
import org.opennms.horizon.shared.ipc.grpc.server.manager.RpcRequestTimeoutManager;
import org.opennms.horizon.shared.ipc.grpc.server.manager.RpcRequestTracker;
import org.opennms.horizon.shared.ipc.grpc.server.manager.adapter.MinionRSTransportAdapter;
import org.opennms.horizon.shared.ipc.grpc.server.manager.adapter.SinkAdapter;
import org.opennms.horizon.shared.ipc.grpc.server.manager.impl.RpcRequestTimeoutManagerImpl;
import org.opennms.horizon.shared.ipc.grpc.server.manager.impl.RpcRequestTrackerImpl;
import org.opennms.horizon.shared.ipc.grpc.server.manager.rpc.LocationIndependentRpcClientFactoryImpl;
import org.opennms.horizon.shared.ipc.grpc.server.manager.rpcstreaming.MinionRpcStreamConnectionManager;
import org.opennms.horizon.shared.ipc.grpc.server.manager.rpcstreaming.impl.MinionRpcStreamConnectionManagerImpl;
import org.opennms.miniongateway.grpc.server.heartbeat.HeartbeatKafkaForwarder;
import org.opennms.miniongateway.grpc.twin.TaskSetTwinMessageProcessor;
import org.opennms.miniongateway.grpc.server.tasktresults.TaskResultsKafkaForwarder;
import org.opennms.miniongateway.grpc.twin.GrpcTwinPublisher;
import org.opennms.taskset.service.api.TaskSetForwarder;
import org.opennms.taskset.service.api.TaskSetPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcServerConfig {

    @Autowired
    private MetricRegistry metricRegistry;

    @Bean
    public RpcConnectionTracker rpcConnectionTracker() {
        return new RpcConnectionTrackerImpl();
    }

    @Bean
    public RpcRequestTracker rpcRequestTracker() {
        return new RpcRequestTrackerImpl();
    }

    @Bean
    public LocationIndependentRpcClientFactory locationIndependentRpcClientFactory() {
        return new LocationIndependentRpcClientFactoryImpl();
    }

    @Bean
    public MinionRpcStreamConnectionManager minionRpcStreamConnectionManager(
        @Autowired MinionManager minionManager,
        @Autowired RpcConnectionTracker rpcConnectionTracker,
        @Autowired RpcRequestTracker rpcRequestTracker
    ) {
        ScheduledExecutorService responseHandlerExecutor = Executors.newSingleThreadScheduledExecutor();

        return new MinionRpcStreamConnectionManagerImpl(
            rpcConnectionTracker, rpcRequestTracker, minionManager, responseHandlerExecutor
        );
    }

    @Bean("minionToCloudRPCProcessor")
    public IncomingRpcHandlerAdapter stubMinionToCloudRPCProcessor(List<ServerHandler> handlers) {
        return new IncomingRpcHandlerAdapter(handlers);
    }

    @Bean("cloudToMinionMessageProcessor")
    public TaskSetTwinMessageProcessor stubCloudToMinionMessageProcessor(
        @Qualifier("taskSetPublisher") TaskSetPublisher publisher,
        @Qualifier("taskSetForwarder") TaskSetForwarder forwarder,
        GrpcTwinPublisher grpcTwinPublisher) {
        return new TaskSetTwinMessageProcessor(publisher, forwarder, grpcTwinPublisher);
    }

    @Bean
    public RpcRequestTimeoutManager requestTimeoutManager() {
        ThreadFactory timerThreadFactory = new ThreadFactoryBuilder()
            .setNameFormat("rpc-timeout-tracker-%d")
            .build();

        // RPC timeout executor thread retrieves elements from delay queue used to timeout rpc requests.
        ExecutorService rpcTimeoutExecutor = Executors.newSingleThreadExecutor(timerThreadFactory);

        RpcRequestTimeoutManagerImpl result = new RpcRequestTimeoutManagerImpl();
        result.setRpcTimeoutExecutor(rpcTimeoutExecutor);
        result.setResponseHandlerExecutor(rpcTimeoutExecutor);

        return result;
    }

    @Bean
    public SinkAdapter sinkAdapter(
        @Autowired TaskResultsKafkaForwarder taskResultsKafkaForwarder,
        @Autowired HeartbeatKafkaForwarder heartbeatKafkaForwarder
    ) throws Exception {
        SinkAdapter sinkAdapter = new SinkAdapter();
        sinkAdapter.registerConsumer(taskResultsKafkaForwarder);
        sinkAdapter.registerConsumer(heartbeatKafkaForwarder);
        return sinkAdapter;
    }

    @Bean
    public CloudServiceImplBase transportAdapter(
        @Autowired MinionRpcStreamConnectionManager minionRpcStreamConnectionManager,
        @Autowired @Qualifier("minionToCloudRPCProcessor") IncomingRpcHandlerAdapter incomingRpcHandlerAdapter,
        @Autowired @Qualifier("cloudToMinionMessageProcessor") BiConsumer<Identity, StreamObserver<CloudToMinionMessage>> cloudToMinionMessageProcessor,
        @Autowired SinkAdapter sinkAdapter
    ) {
        MinionRSTransportAdapter adapter = new MinionRSTransportAdapter(
            minionRpcStreamConnectionManager::startRpcStreaming,
            cloudToMinionMessageProcessor,
            incomingRpcHandlerAdapter,
            (emptyStream) -> sinkAdapter
        );
        return adapter;
    }

    @Bean
    public OpennmsGrpcServer opennmsServer(
        @Autowired GrpcIpcServer serverBuilder,
        @Autowired CloudServiceImplBase transportAdapter,
        @Autowired MinionManager minionManager,
        @Autowired RpcConnectionTracker rpcConnectionTracker,
        @Autowired RpcRequestTracker rpcRequestTracker,
        @Autowired LocationIndependentRpcClientFactory locationIndependentRpcClientFactory,
        @Autowired RpcRequestTimeoutManager rpcRequestTimeoutManager
    ) throws Exception {

        OpennmsGrpcServer server = new OpennmsGrpcServer(serverBuilder, Arrays.asList(
            new MeteringInterceptorFactory(metricRegistry)
        ));

        server.setTransportAdapter(transportAdapter);
        server.setRpcConnectionTracker(rpcConnectionTracker);
        server.setRpcRequestTracker(rpcRequestTracker);
        server.setRpcRequestTimeoutManager(rpcRequestTimeoutManager);
        server.setMinionManager(minionManager);
        server.setLocationIndependentRpcClientFactory(locationIndependentRpcClientFactory);

        server.start();
        return server;
    }


}
