package org.opennms.miniongateway.grpc.server;

import org.opennms.core.grpc.common.GrpcIpcServer;
import org.opennms.core.grpc.common.GrpcIpcServerBuilder;
import org.opennms.core.grpc.common.GrpcIpcUtils;
import org.opennms.core.grpc.interceptor.LoggingInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Properties;


@Configuration
public class GrpcIpcServerConfig {

    public static final String GRPC_MAX_INBOUND_SIZE = "max.message.size";
    public static final long DEFAULT_MAX_MESSAGE_SIZE = 100 * ( 1024 * 1024 );

    @Value("${" + GRPC_MAX_INBOUND_SIZE + ":" + DEFAULT_MAX_MESSAGE_SIZE + "}")
    private long maxMessageSize;

    @Bean(destroyMethod = "stopServer")
    public GrpcIpcServer prepareGrpcIpcServer() {
        Properties properties = new Properties();
        properties.setProperty(GrpcIpcUtils.GRPC_MAX_INBOUND_SIZE, Long.toString(maxMessageSize));

        return new GrpcIpcServerBuilder(properties, 8990, "PT10S", Arrays.asList(
            new LoggingInterceptor()
        ));
    }
}
