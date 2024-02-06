package org.opennms.miniongateway.grpc.server;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import org.opennms.horizon.shared.grpc.common.GrpcIpcServer;
import org.opennms.horizon.shared.grpc.common.LocationServerInterceptor;
import org.opennms.horizon.shared.grpc.common.TenantIDGrpcServerInterceptor;
import org.opennms.horizon.shared.ipc.grpc.server.OpennmsGrpcServer;
import org.opennms.horizon.shared.ipc.grpc.server.manager.MinionManager;
import org.opennms.horizon.shared.ipc.grpc.server.manager.OutgoingMessageFactory;
import org.opennms.horizon.shared.ipc.grpc.server.manager.OutgoingMessageHandler;
import org.opennms.horizon.shared.ipc.grpc.server.manager.RpcConnectionTracker;
import org.opennms.horizon.shared.ipc.grpc.server.manager.RpcRequestTimeoutManager;
import org.opennms.horizon.shared.ipc.grpc.server.manager.RpcRequestTracker;
import org.opennms.horizon.shared.ipc.grpc.server.manager.impl.RpcRequestTimeoutManagerImpl;
import org.opennms.horizon.shared.ipc.grpc.server.manager.impl.RpcRequestTrackerImpl;
import org.opennms.horizon.shared.ipc.grpc.server.manager.rpcstreaming.MinionRpcStreamConnectionManager;
import org.opennms.horizon.shared.ipc.grpc.server.manager.rpcstreaming.impl.MinionRpcStreamConnectionManagerImpl;
import org.opennms.horizon.shared.protobuf.mapper.TenantLocationSpecificTaskSetResultsMapper;
import org.opennms.horizon.shared.protobuf.mapper.impl.TenantLocationSpecificTaskSetResultsMapperImpl;
import org.opennms.miniongateway.grpc.server.flows.FlowKafkaForwarder;
import org.opennms.miniongateway.grpc.server.heartbeat.HeartbeatKafkaForwarder;
import org.opennms.miniongateway.grpc.server.tasktresults.TaskResultsKafkaForwarder;
import org.opennms.miniongateway.grpc.server.traps.TrapsKafkaForwarder;
import org.opennms.miniongateway.grpc.twin.TaskSetTwinMessageProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.OpenTelemetry;

@Configuration
public class GrpcServerConfig {
    @Value("${debug.span.full.message:false}")
    private boolean debugSpanFullMessage;

    @Value("${debug.span.content:false}")
    private boolean debugSpanContent;

    @Bean
    public TenantLocationSpecificTaskSetResultsMapper tenantLocationSpecificTaskSetResultsMapper() {
        return new TenantLocationSpecificTaskSetResultsMapperImpl();
    }

    @Bean
    public RpcConnectionTracker rpcConnectionTracker() {
        return new RpcConnectionTrackerImpl();
    }

    @Bean
    public RpcRequestTracker rpcRequestTracker() {
        return new RpcRequestTrackerImpl();
    }

    @Bean
    public MinionRpcStreamConnectionManager minionRpcStreamConnectionManager(
        @Autowired MinionManager minionManager,
        @Autowired RpcConnectionTracker rpcConnectionTracker,
        @Autowired RpcRequestTracker rpcRequestTracker,
        @Autowired TenantIDGrpcServerInterceptor tenantIDGrpcServerInterceptor,
        @Autowired LocationServerInterceptor locationServerInterceptor
        ) {
        ScheduledExecutorService responseHandlerExecutor = Executors.newSingleThreadScheduledExecutor();

        return new MinionRpcStreamConnectionManagerImpl(
            rpcConnectionTracker,
            rpcRequestTracker,
            minionManager,
            responseHandlerExecutor,
            tenantIDGrpcServerInterceptor,
            locationServerInterceptor
        );
    }

    @Bean("minionToCloudRPCProcessor")
    public IncomingRpcHandlerAdapter stubMinionToCloudRPCProcessor(List<ServerHandler> handlers) {
        return new IncomingRpcHandlerAdapter(handlers);
    }

    @Bean("cloudToMinionMessageProcessor")
    public TaskSetTwinMessageProcessor stubCloudToMinionMessageProcessor(
        TenantIDGrpcServerInterceptor tenantIDGrpcServerInterceptor,
        LocationServerInterceptor locationServerInterceptor,
        List<OutgoingMessageFactory> outgoingMessageFactoryList) {
        return new TaskSetTwinMessageProcessor(tenantIDGrpcServerInterceptor, locationServerInterceptor, outgoingMessageFactoryList, debugSpanFullMessage, debugSpanContent);
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
        @Autowired MinionRpcStreamConnectionManager minionRpcStreamConnectionManager,
        @Autowired @Qualifier("minionToCloudRPCProcessor") IncomingRpcHandlerAdapter incomingRpcHandlerAdapter,
        @Autowired @Qualifier("cloudToMinionMessageProcessor") OutgoingMessageHandler cloudToMinionMessageProcessor,
        @Autowired TaskResultsKafkaForwarder taskResultsKafkaForwarder,
        @Autowired HeartbeatKafkaForwarder heartbeatKafkaForwarder,
        @Autowired TrapsKafkaForwarder trapsKafkaForwarder,
        @Autowired FlowKafkaForwarder flowKafkaForwarder,
        @Autowired RpcRequestTimeoutManager rpcRequestTimeoutManager,
        @Autowired TenantIDGrpcServerInterceptor tenantIDGrpcServerInterceptor,
        @Autowired LocationServerInterceptor locationServerInterceptor,
        @Autowired MeterRegistry meterRegistry,
        @Autowired OpenTelemetry openTelemetry
        ) throws Exception {

        OpennmsGrpcServer server = new OpennmsGrpcServer(serverBuilder, meterRegistry, openTelemetry.getTracer(getClass().getName()), debugSpanFullMessage, debugSpanContent);

        server.setRpcConnectionTracker(rpcConnectionTracker);
        server.setRpcRequestTracker(rpcRequestTracker);
        server.setRpcRequestTimeoutManager(rpcRequestTimeoutManager);
        server.setTenantIDGrpcServerInterceptor(tenantIDGrpcServerInterceptor);
        server.setLocationServerInterceptor(locationServerInterceptor);
        server.setMinionManager(minionManager);
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
