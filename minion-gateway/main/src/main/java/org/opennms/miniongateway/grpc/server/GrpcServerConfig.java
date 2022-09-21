package org.opennms.miniongateway.grpc.server;

import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.grpc.stub.StreamObserver;
import org.opennms.cloud.grpc.minion.CloudToMinionMessage;
import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcResponseProto;
import org.opennms.core.grpc.common.GrpcIpcServer;
import org.opennms.core.grpc.interceptor.MeteringInterceptorFactory;
import org.opennms.core.ipc.grpc.server.OpennmsGrpcServer;
import org.opennms.core.ipc.grpc.server.manager.LocationIndependentRpcClientFactory;
import org.opennms.core.ipc.grpc.server.manager.MinionManager;
import org.opennms.core.ipc.grpc.server.manager.RpcConnectionTracker;
import org.opennms.core.ipc.grpc.server.manager.RpcRequestTimeoutManager;
import org.opennms.core.ipc.grpc.server.manager.RpcRequestTracker;
import org.opennms.core.ipc.grpc.server.manager.impl.RpcRequestTimeoutManagerImpl;
import org.opennms.core.ipc.grpc.server.manager.impl.RpcRequestTrackerImpl;
import org.opennms.core.ipc.grpc.server.manager.rpc.LocationIndependentRpcClientFactoryImpl;
import org.opennms.core.ipc.grpc.server.manager.rpcstreaming.MinionRpcStreamConnectionManager;
import org.opennms.core.ipc.grpc.server.manager.rpcstreaming.impl.MinionRpcStreamConnectionManagerImpl;
import org.opennms.miniongateway.grpc.server.heartbeat.HeartbeatKafkaForwarder;
import org.opennms.miniongateway.grpc.server.stub.StubCloudToMinionMessageProcessor;
import org.opennms.miniongateway.grpc.server.stub.StubMinionToCloudProcessor;
import org.opennms.miniongateway.grpc.server.tasktresults.TaskResultsKafkaForwarder;
import org.opennms.miniongateway.grpc.twin.GrpcTwinPublisher;
import org.opennms.taskset.service.api.TaskSetForwarder;
import org.opennms.taskset.service.api.TaskSetPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.function.BiConsumer;

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
    public StubMinionToCloudProcessor stubMinionToCloudRPCProcessor() {
        return new StubMinionToCloudProcessor();
    }

    @Bean("cloudToMinionMessageProcessor")
    public StubCloudToMinionMessageProcessor stubCloudToMinionMessageProcessor(
        @Qualifier("taskSetPublisher") TaskSetPublisher publisher,
        @Qualifier("taskSetForwarder") TaskSetForwarder forwarder,
        GrpcTwinPublisher grpcTwinPublisher) {
        return new StubCloudToMinionMessageProcessor(publisher, forwarder, grpcTwinPublisher);
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

        return result;
    }

    @Bean
    public OpennmsGrpcServer opennmsServer(
        @Autowired GrpcIpcServer serverBuilder,
        @Autowired MinionManager minionManager,
        @Autowired RpcConnectionTracker rpcConnectionTracker,
        @Autowired RpcRequestTracker rpcRequestTracker,
        @Autowired LocationIndependentRpcClientFactory locationIndependentRpcClientFactory,
        @Autowired MinionRpcStreamConnectionManager minionRpcStreamConnectionManager,
        @Autowired @Qualifier("minionToCloudRPCProcessor") BiConsumer<RpcRequestProto, StreamObserver<RpcResponseProto>> minionToCloudRPCProcessor,
        @Autowired @Qualifier("cloudToMinionMessageProcessor") BiConsumer<Identity, StreamObserver<CloudToMinionMessage>> cloudToMinionMessageProcessor,
        @Autowired TaskResultsKafkaForwarder taskResultsKafkaForwarder,
        @Autowired HeartbeatKafkaForwarder heartbeatKafkaForwarder,
        @Autowired RpcRequestTimeoutManager rpcRequestTimeoutManager
    ) throws Exception {

        OpennmsGrpcServer server = new OpennmsGrpcServer(serverBuilder, Arrays.asList(
            new MeteringInterceptorFactory(metricRegistry)
        ));


        server.setRpcConnectionTracker(rpcConnectionTracker);
        server.setRpcRequestTracker(rpcRequestTracker);
        server.setRpcRequestTimeoutManager(rpcRequestTimeoutManager);
        server.setMinionManager(minionManager);
        server.setLocationIndependentRpcClientFactory(locationIndependentRpcClientFactory);
        server.setMinionRpcStreamConnectionManager(minionRpcStreamConnectionManager);
        server.setIncomingRpcHandler(minionToCloudRPCProcessor);
        server.setOutgoingMessageHandler(cloudToMinionMessageProcessor);
        server.registerConsumer(taskResultsKafkaForwarder);
        server.registerConsumer(heartbeatKafkaForwarder);

        server.start();
        return server;
    }


}
