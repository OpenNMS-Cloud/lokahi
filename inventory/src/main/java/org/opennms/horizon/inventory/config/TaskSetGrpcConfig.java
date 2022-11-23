package org.opennms.horizon.inventory.config;

import io.grpc.Channel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import org.opennms.taskset.service.contract.TaskSetServiceGrpc;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TaskSetGrpcConfig {

    @Value("${grpc.client.minion-gateway.host:localhost}")
    private String host;

    @Value("${grpc.client.minion-gateway.port:8990}")
    private int port;

    @Value("${grpc.client.minion-gateway.tlsEnabled:false}")
    private boolean tlsEnabled;

    @Value("${grpc.client.minion-gateway.maxMessageSize:10485760}")
    private int maxMessageSize;

    @Bean
    public TaskSetServiceGrpc.TaskSetServiceBlockingStub taskSetServiceBlockingStub() {
        NettyChannelBuilder channelBuilder = NettyChannelBuilder.forAddress(host, port)
            .keepAliveWithoutCalls(true)
            .maxInboundMessageSize(maxMessageSize);

        Channel channel;

        if (tlsEnabled) {
            throw new RuntimeException("TLS NOT YET IMPLEMENTED");
        } else {
            channel = channelBuilder.usePlaintext().build();
        }

        return TaskSetServiceGrpc.newBlockingStub(channel);
    }
}
