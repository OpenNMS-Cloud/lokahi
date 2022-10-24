package org.opennms.miniongateway.grpc.server;

import org.opennms.horizon.shared.grpc.common.GrpcIpcServer;
import org.opennms.horizon.shared.grpc.common.GrpcIpcServerBuilder;
import org.opennms.horizon.shared.grpc.common.GrpcIpcUtils;
import org.opennms.horizon.shared.grpc.interceptor.LoggingInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Properties;


@Configuration
public class GrpcIpcServerConfig {

    public static final String GRPC_MAX_INBOUND_SIZE = "max.message.size";
    public static final long DEFAULT_MAX_MESSAGE_SIZE = 100 * ( 1024 * 1024 );
    public static final int DEFAULT_GRPC_PORT = 8990;

    @Value("${" + GRPC_MAX_INBOUND_SIZE + ":" + DEFAULT_MAX_MESSAGE_SIZE + "}")
    private long maxMessageSize;

    @Value("${grpc.port:" + DEFAULT_GRPC_PORT + "}")
    private int grpcPort;

    @Bean(destroyMethod = "stopServer")
    public GrpcIpcServer prepareGrpcIpcServer() {
        Properties properties = new Properties();
        properties.setProperty(GrpcIpcUtils.GRPC_MAX_INBOUND_SIZE, Long.toString(maxMessageSize));

        return new GrpcIpcServerBuilder(properties, grpcPort, "PT10S", Arrays.asList(
            new LoggingInterceptor()
        ));
    }
}
