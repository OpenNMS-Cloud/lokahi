package org.opennms.poc.ignite.grpc.client.internal;

import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;

import java.io.IOException;
import java.util.List;

import org.opennms.cloud.grpc.minion.CloudServiceGrpc;
import org.opennms.cloud.grpc.minion.CloudServiceGrpc.CloudServiceBlockingStub;
import org.opennms.cloud.grpc.minion.CloudServiceGrpc.CloudServiceStub;
import org.opennms.poc.ignite.grpc.client.GrpcClient;
import org.opennms.poc.ignite.grpc.client.GrpcClientFactory;

// transformed from GrpcClientBuilder
public class DefaultGrpcClientFactory implements GrpcClientFactory {

    private final String minionServerId;
    private final String minionLocation;
    //private final Logger logger = LoggerFactory.getLogger(DefaultGrpcClientFactory.class);

    public DefaultGrpcClientFactory(String minionServerId, String minionLocation) {
        this.minionServerId = minionServerId;
        this.minionLocation = minionLocation;
    }

    @Override
    public GrpcClient create(String host, int port, List<ClientInterceptor> interceptors) throws Exception {
        ManagedChannel channel = getChannel(host, port, interceptors);

        CloudServiceStub asyncStub = CloudServiceGrpc.newStub(channel);
        CloudServiceBlockingStub blockingRpcStub = CloudServiceGrpc.newBlockingStub(channel);
        StubGrpcClient client = new StubGrpcClient(channel, asyncStub, blockingRpcStub, minionServerId, minionLocation);
        return client;
    }

    private ManagedChannel getChannel(String host, int port, List<ClientInterceptor> interceptors) throws IOException {
        int maxInboundMessageSize = DEFAULT_MESSAGE_SIZE;
        NettyChannelBuilder channelBuilder = NettyChannelBuilder.forAddress(host, port)
            .maxInboundMessageSize(maxInboundMessageSize)
            .intercept(new DelegatingClientInterceptor(interceptors))
            .keepAliveWithoutCalls(true);

        return channelBuilder.usePlaintext().build();
    }
}
