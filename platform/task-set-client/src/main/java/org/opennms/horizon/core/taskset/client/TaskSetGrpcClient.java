package org.opennms.horizon.core.taskset.client;

import io.grpc.Channel;
import io.grpc.Metadata;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.MetadataUtils;
import org.opennms.taskset.contract.TaskSet;
import org.opennms.taskset.service.api.TaskSetPublisher;
import org.opennms.taskset.service.contract.PublishTaskSetRequest;
import org.opennms.taskset.service.contract.PublishTaskSetResponse;
import org.opennms.taskset.service.contract.TaskSetServiceGrpc;
import org.slf4j.Logger;

public class TaskSetGrpcClient implements TaskSetPublisher {

    public static final String DEFAULT_GRPC_HOSTNAME = "localhost";
    public static final int DEFAULT_GRPC_PORT = 8990;
    public static final int DEFAULT_MAX_MESSAGE_SIZE = 1_0485_760;

    private static final Metadata.Key HEADER_KE = Metadata.Key.of("tenant-id", Metadata.ASCII_STRING_MARSHALLER);

    private static final Logger DEFAULT_LOGGER = org.slf4j.LoggerFactory.getLogger(TaskSetGrpcClient.class);

    private Logger log = DEFAULT_LOGGER;

    private boolean tlsEnabled = false;
    private String host = DEFAULT_GRPC_HOSTNAME;
    private int port = DEFAULT_GRPC_PORT;
    private int maxMessageSize = DEFAULT_MAX_MESSAGE_SIZE;

    private Channel channel;
    private TaskSetServiceGrpc.TaskSetServiceBlockingStub taskSetServiceStub;

//========================================
// Getters and Setters
//----------------------------------------

    public boolean isTlsEnabled() {
        return tlsEnabled;
    }

    public void setTlsEnabled(boolean tlsEnabled) {
        this.tlsEnabled = tlsEnabled;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getMaxMessageSize() {
        return maxMessageSize;
    }

    public void setMaxMessageSize(int maxMessageSize) {
        this.maxMessageSize = maxMessageSize;
    }

//========================================
// Lifecycle
//----------------------------------------

    public void init() {
        NettyChannelBuilder channelBuilder = NettyChannelBuilder.forAddress(host, port)
            .keepAliveWithoutCalls(true)
            .maxInboundMessageSize(maxMessageSize);

        if (tlsEnabled) {
            throw new RuntimeException("TLS NOT YET IMPLEMENTED");
            // channel = channelBuilder
            //     .negotiationType(NegotiationType.TLS)
            //     .sslContext(buildSslContext().build())
            //     .build();
            // log.info("TLS enabled for TaskSet gRPC");
        } else {
            channel = channelBuilder.usePlaintext().build();
        }

        taskSetServiceStub = TaskSetServiceGrpc.newBlockingStub(channel);
    }

//========================================
// Operations
//----------------------------------------

    @Override
    public void publishTaskSet(String tenantId, String location, TaskSet taskSet) {

        try {
            PublishTaskSetRequest request =
                PublishTaskSetRequest.newBuilder()
                    .setLocation(location)
                    .setTaskSet(taskSet)
                    .build()
                ;

            Metadata metadata = new Metadata();
            metadata.put(HEADER_KE, tenantId);

            PublishTaskSetResponse unused =
                taskSetServiceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata)).publishTaskSet(request);

            log.debug("PUBLISH task set complete: location={}", location);
        } catch (Exception exc) {
            log.error("Error publishing taskset", exc);
            throw new RuntimeException("failed to publish taskset", exc);
        }
    }
}
