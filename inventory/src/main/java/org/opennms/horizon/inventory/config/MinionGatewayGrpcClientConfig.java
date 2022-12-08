package org.opennms.horizon.inventory.config;

import org.opennms.horizon.inventory.component.MinionRpcClient;
import org.opennms.horizon.inventory.exception.InventoryRuntimeException;
import org.opennms.taskset.service.contract.TaskSetServiceGrpc;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;

@Configuration
public class MinionGatewayGrpcClientConfig {

    @Value("${grpc.client.minion-gateway.host:localhost}")
    private String host;

    @Value("${grpc.client.minion-gateway.port:8990}")
    private int port;

    @Value("${grpc.client.minion-gateway.tlsEnabled:false}")
    private boolean tlsEnabled;

    @Value("${grpc.client.minion-gateway.maxMessageSize:10485760}")
    private int maxMessageSize;

    @Bean(name = "minion-gateway")
    public ManagedChannel createGrpcChannel() {
        NettyChannelBuilder channelBuilder = NettyChannelBuilder.forAddress(host, port)
            .keepAliveWithoutCalls(true)
            .maxInboundMessageSize(maxMessageSize);

        ManagedChannel channel;

        if (tlsEnabled) {
            throw new InventoryRuntimeException("TLS NOT YET IMPLEMENTED");
        } else {
            channel = channelBuilder.usePlaintext().build();
        }
        return channel;
    }

    @Bean
    public TaskSetServiceGrpc.TaskSetServiceBlockingStub taskSetServiceBlockingStub(@Qualifier("minion-gateway") ManagedChannel channel) {
        return TaskSetServiceGrpc.newBlockingStub(channel);
    }

    @Bean(initMethod = "init", destroyMethod = "shutdown")
    public MinionRpcClient createMinionRpcClient(@Qualifier("minion-gateway") ManagedChannel channel) {
        return new MinionRpcClient(channel);
    }
}
