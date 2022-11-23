package org.opennms.miniongatewaygrpcproxy.grpc;

import com.google.protobuf.Empty;
import io.grpc.Channel;
import io.grpc.Metadata;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import org.opennms.cloud.grpc.minion.CloudServiceGrpc;
import org.opennms.cloud.grpc.minion.CloudToMinionMessage;
import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.cloud.grpc.minion.MinionToCloudMessage;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcRequestServiceGrpc;
import org.opennms.cloud.grpc.minion.RpcResponseProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.function.Function;

@Component
public class CloudServiceProxyImpl extends CloudServiceGrpc.CloudServiceImplBase {

    public static final int DEFAULT_MAX_MESSAGE_SIZE = 1_0485_760;

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(CloudServiceProxyImpl.class);

    private Logger LOG = DEFAULT_LOGGER;


    private Metadata.Key TENANT_ID_METADATA_KEY;

    @Value("${grpc.downstream.host}")
    private String downstreamHost;

    @Value("${grpc.downstream.port}")
    private int downstreamPort;

    @Value("${grpc.downstream.max-message-size:" + DEFAULT_MAX_MESSAGE_SIZE + "}")
    private int maxMessageSize;

    @Value("${grpc.downstream.tls-enabled:false}")
    private boolean tlsEnabled;

    @Value("${grpc.downstream.inject-header-name:tenant-id}")
    private String injectHeaderName;

    @Value("${grpc.downstream.inject-header-value:YYY-opennms-prime}")
    private String injectHeaderValue;

    private Channel channel;
    private CloudServiceGrpc.CloudServiceStub cloudServiceStub;


//========================================
// Lifecycle
//----------------------------------------

    @PostConstruct
    public void init() {
        TENANT_ID_METADATA_KEY = Metadata.Key.of(injectHeaderName, Metadata.ASCII_STRING_MARSHALLER);

        NettyChannelBuilder channelBuilder = NettyChannelBuilder.forAddress(downstreamHost, downstreamPort)
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

        cloudServiceStub = CloudServiceGrpc.newStub(channel);
    }

//========================================
// GRPC Request
//----------------------------------------

    @Override
    public StreamObserver<RpcResponseProto> cloudToMinionRPC(StreamObserver<RpcRequestProto> responseObserver) {
        return commonProcessCall((stub) -> stub.cloudToMinionRPC(responseObserver));
    }

    @Override
    public void cloudToMinionMessages(Identity request, StreamObserver<CloudToMinionMessage> responseObserver) {
        // "return null" in the lambda to conform to the common code's return handling
        commonProcessCall((stub) -> { stub.cloudToMinionMessages(request, responseObserver); return null; });
    }

    @Override
    public void minionToCloudRPC(RpcRequestProto request, StreamObserver<RpcResponseProto> responseObserver) {
        // "return null" in the lambda to conform to the common code's return handling
        commonProcessCall((stub) -> { stub.minionToCloudRPC(request, responseObserver); return null; });
    }

    @Override
    public StreamObserver<MinionToCloudMessage> minionToCloudMessages(StreamObserver<Empty> responseObserver) {
        return commonProcessCall((stub) -> stub.minionToCloudMessages(responseObserver));
    }

//========================================
// Internals
//----------------------------------------

    private <T> T commonProcessCall(Function<CloudServiceGrpc.CloudServiceStub, T> call) {
        Metadata inboundHeaders = GrpcHeaderCaptureInterceptor.INBOUND_HEADERS_CONTEXT_KEY.get();

        Metadata metadata = new Metadata();
        metadata.merge(inboundHeaders);
        metadata.put(TENANT_ID_METADATA_KEY, injectHeaderValue);

        CloudServiceGrpc.CloudServiceStub stubWithInterceptors =
            cloudServiceStub
                .withInterceptors(
                    MetadataUtils.newAttachHeadersInterceptor(
                        metadata
                    )
                );

        T result = call.apply(stubWithInterceptors);

        return result;
    }
}
