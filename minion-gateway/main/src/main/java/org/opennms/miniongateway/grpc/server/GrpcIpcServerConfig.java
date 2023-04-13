package org.opennms.miniongateway.grpc.server;

import org.opennms.horizon.shared.grpc.common.GrpcIpcServer;
import org.opennms.horizon.shared.grpc.common.GrpcIpcServerBuilder;
import org.opennms.horizon.shared.grpc.common.GrpcIpcUtils;
import org.opennms.horizon.shared.grpc.common.LocationServerInterceptor;
import org.opennms.horizon.shared.grpc.common.TenantIDGrpcServerInterceptor;
import org.opennms.horizon.shared.grpc.interceptor.LoggingInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Properties;


@Configuration
public class GrpcIpcServerConfig {

    public static final String GRPC_MAX_INBOUND_SIZE = "max.message.size";
    public static final long DEFAULT_MAX_MESSAGE_SIZE = 100 * ( 1024 * 1024 );
    public static final int DEFAULT_EXTERNAL_GRPC_PORT = 8990;
    public static final int DEFAULT_INTERNAL_GRPC_PORT = 8991;

    @Value("${" + GRPC_MAX_INBOUND_SIZE + ":" + DEFAULT_MAX_MESSAGE_SIZE + "}")
    private long maxMessageSize;

    @Value("${grpc.port:" + DEFAULT_EXTERNAL_GRPC_PORT + "}")
    private int externalGrpcPort;

    @Value("${grpc.internal.port:" + DEFAULT_INTERNAL_GRPC_PORT + "}")
    private int internalGrpcPort;

//========================================
// BEAN REGISTRATION
//----------------------------------------

    @Bean
    public TenantIDGrpcServerInterceptor prepareTenantIDGrpcInterceptor() {
        return new TenantIDGrpcServerInterceptor();
    }

    @Bean
    public LocationServerInterceptor prepareLocationInterceptor() {
        return new LocationServerInterceptor();
    }

    /**
     * External GRPC service for handling
     *
     * @return
     */
    @Bean(name = "externalGrpcIpcServer", destroyMethod = "stopServer")
    public GrpcIpcServer prepareExternalGrpcIpcServer(
        @Autowired TenantIDGrpcServerInterceptor tenantIDGrpcServerInterceptor,
        @Autowired LocationServerInterceptor locationServerInterceptor
    ) {
        Properties properties = new Properties();
        properties.setProperty(GrpcIpcUtils.GRPC_MAX_INBOUND_SIZE, Long.toString(maxMessageSize));

        return new GrpcIpcServerBuilder(properties, externalGrpcPort, "PT10S", Arrays.asList(
            new LoggingInterceptor(),
            tenantIDGrpcServerInterceptor,
            locationServerInterceptor
        ));
    }

    @Bean(name = "internalGrpcIpcServer", destroyMethod = "stopServer")
    public GrpcIpcServer prepareInternalGrpcIpcServerTenantIdInterceptor() {
        Properties properties = new Properties();
        properties.setProperty(GrpcIpcUtils.GRPC_MAX_INBOUND_SIZE, Long.toString(maxMessageSize));

        return new GrpcIpcServerBuilder(properties, internalGrpcPort, "PT10S", Arrays.asList(
            new LoggingInterceptor()
        ));
    }
}
