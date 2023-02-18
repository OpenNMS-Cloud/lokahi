package org.opennms.miniongateway.grpc.server;

import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.grpc.stub.StreamObserver;
import org.opennms.cloud.grpc.minion.CloudToMinionMessage;
import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.horizon.shared.grpc.common.GrpcIpcServer;
import org.opennms.horizon.shared.grpc.common.TenantIDGrpcServerInterceptor;
import org.opennms.horizon.shared.grpc.interceptor.MeteringInterceptorFactory;
import org.opennms.horizon.shared.ipc.grpc.server.OpennmsGrpcServer;
import org.opennms.horizon.shared.ipc.grpc.server.manager.LocationIndependentRpcClientFactory;
import org.opennms.horizon.shared.ipc.grpc.server.manager.MinionManager;
import org.opennms.horizon.shared.ipc.grpc.server.manager.RpcConnectionTracker;
import org.opennms.horizon.shared.ipc.grpc.server.manager.RpcRequestTimeoutManager;
import org.opennms.horizon.shared.ipc.grpc.server.manager.RpcRequestTracker;
import org.opennms.horizon.shared.ipc.grpc.server.manager.impl.RpcRequestTimeoutManagerImpl;
import org.opennms.horizon.shared.ipc.grpc.server.manager.impl.RpcRequestTrackerImpl;
import org.opennms.horizon.shared.ipc.grpc.server.manager.rpc.LocationIndependentRpcClientFactoryImpl;
import org.opennms.horizon.shared.ipc.grpc.server.manager.rpcstreaming.MinionRpcStreamConnectionManager;
import org.opennms.horizon.shared.ipc.grpc.server.manager.rpcstreaming.impl.MinionRpcStreamConnectionManagerImpl;
import org.opennms.miniongateway.grpc.server.heartbeat.HeartbeatKafkaForwarder;
import org.opennms.miniongateway.grpc.server.flows.FlowKafkaForwarder;
import org.opennms.miniongateway.grpc.server.tasktresults.TaskResultsKafkaForwarder;
import org.opennms.miniongateway.grpc.server.traps.TrapsKafkaForwarder;
import org.opennms.miniongateway.grpc.twin.GrpcTwinPublisher;
import org.opennms.miniongateway.grpc.twin.TaskSetTwinMessageProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;
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
        @Autowired RpcRequestTracker rpcRequestTracker,
        @Autowired TenantIDGrpcServerInterceptor tenantIDGrpcServerInterceptor
        ) {
        ScheduledExecutorService responseHandlerExecutor = Executors.newSingleThreadScheduledExecutor();

        return new MinionRpcStreamConnectionManagerImpl(
            rpcConnectionTracker,
            rpcRequestTracker,
            minionManager,
            responseHandlerExecutor,
            tenantIDGrpcServerInterceptor
        );
    }

    @Bean("minionToCloudRPCProcessor")
    public IncomingRpcHandlerAdapter stubMinionToCloudRPCProcessor(List<ServerHandler> handlers) {
        return new IncomingRpcHandlerAdapter(handlers);
    }

    @Bean("cloudToMinionMessageProcessor")
    public TaskSetTwinMessageProcessor stubCloudToMinionMessageProcessor(
        GrpcTwinPublisher grpcTwinPublisher,
        TenantIDGrpcServerInterceptor tenantIDGrpcServerInterceptor) {
        return new TaskSetTwinMessageProcessor(grpcTwinPublisher, tenantIDGrpcServerInterceptor);
    }

    @Bean
    public RpcRequestTimeoutManager requestTimeoutManager() {
        ThreadFactory timerThreadFactory = new ThreadFactoryBuilder()
            .setNameFormat("rpc-timeout-tracker-%d")
            .build();

        // RPC timeout executor thread retrieves elements from delay queue used to timeout rpc requests.
        ExecutorService rpcTimeoutExecutor = Executors.newFixedThreadPool(3, timerThreadFactory);

        RpcRequestTimeoutManagerImpl result = new RpcRequestTimeoutManagerImpl();
        result.setRpcTimeoutExecutor(rpcTimeoutExecutor);
        result.setResponseHandlerExecutor(rpcTimeoutExecutor);

        return result;
    }

    @Bean
    public OpennmsGrpcServer opennmsServer(
        @Autowired @Qualifier("externalGrpcIpcServer") GrpcIpcServer serverBuilder,
        @Autowired MinionManager minionManager,
        @Autowired RpcConnectionTracker rpcConnectionTracker,
        @Autowired RpcRequestTracker rpcRequestTracker,
        @Autowired LocationIndependentRpcClientFactory locationIndependentRpcClientFactory,
        @Autowired MinionRpcStreamConnectionManager minionRpcStreamConnectionManager,
        @Autowired @Qualifier("minionToCloudRPCProcessor") IncomingRpcHandlerAdapter incomingRpcHandlerAdapter,
        @Autowired @Qualifier("cloudToMinionMessageProcessor") BiConsumer<Identity, StreamObserver<CloudToMinionMessage>> cloudToMinionMessageProcessor,
        @Autowired TaskResultsKafkaForwarder taskResultsKafkaForwarder,
        @Autowired HeartbeatKafkaForwarder heartbeatKafkaForwarder,
        @Autowired TrapsKafkaForwarder trapsKafkaForwarder,
        @Autowired FlowKafkaForwarder flowKafkaForwarder,
        @Autowired RpcRequestTimeoutManager rpcRequestTimeoutManager,
        @Autowired TenantIDGrpcServerInterceptor tenantIDGrpcServerInterceptor
    ) throws Exception {

        OpennmsGrpcServer server = new OpennmsGrpcServer(serverBuilder, Arrays.asList(
            new MeteringInterceptorFactory(metricRegistry)
        ));

        server.setRpcConnectionTracker(rpcConnectionTracker);
        server.setRpcRequestTracker(rpcRequestTracker);
        server.setRpcRequestTimeoutManager(rpcRequestTimeoutManager);
        server.setTenantIDGrpcServerInterceptor(tenantIDGrpcServerInterceptor);
        server.setMinionManager(minionManager);
        server.setLocationIndependentRpcClientFactory(locationIndependentRpcClientFactory);
        server.setMinionRpcStreamConnectionManager(minionRpcStreamConnectionManager);
        server.setIncomingRpcHandler(incomingRpcHandlerAdapter);
        server.setOutgoingMessageHandler(cloudToMinionMessageProcessor);
        server.registerConsumer(taskResultsKafkaForwarder);
        server.registerConsumer(heartbeatKafkaForwarder);
        server.registerConsumer(trapsKafkaForwarder);
        server.registerConsumer(flowKafkaForwarder);

        server.start();
        return server;
    }


}
