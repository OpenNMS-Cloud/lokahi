package org.opennms.miniongateway.grpc.server;

import org.opennms.horizon.shared.grpc.common.TenantIDGrpcServerInterceptor;
import org.opennms.miniongateway.grpc.twin.GrpcTwinPublisher;
import org.opennms.miniongateway.grpc.twin.TwinRpcHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcTwinPublisherConfig {

    private final GrpcTwinPublisher grpcTwinPublisher = new GrpcTwinPublisher();

    @Bean
    public ServerHandler serverHandler(TenantIDGrpcServerInterceptor interceptor) {
        return new TwinRpcHandler(grpcTwinPublisher, interceptor);
    }

    @Bean(initMethod = "start", destroyMethod = "close")
    public GrpcTwinPublisher grpcTwinPublisher() {
        return grpcTwinPublisher;
    }

}
