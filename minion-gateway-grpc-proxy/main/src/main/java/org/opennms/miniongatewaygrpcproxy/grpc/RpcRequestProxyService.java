package org.opennms.miniongatewaygrpcproxy.grpc;

import io.grpc.Channel;
import io.grpc.Context;
import io.grpc.Metadata;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcRequestServiceGrpc;
import org.opennms.cloud.grpc.minion.RpcResponseProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class RpcRequestProxyService extends RpcRequestServiceGrpc.RpcRequestServiceImplBase {

    public static final int DEFAULT_MAX_MESSAGE_SIZE = 1_0485_760;

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(RpcRequestProxyService.class);

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
    private RpcRequestServiceGrpc.RpcRequestServiceBlockingStub rpcRequestServiceStub;


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

        rpcRequestServiceStub = RpcRequestServiceGrpc.newBlockingStub(channel);
    }

//========================================
// GRPC Request
//----------------------------------------

    @Override
    public void request(RpcRequestProto request, StreamObserver<RpcResponseProto> responseObserver) {
        try {
            Metadata inboundHeaders = GrpcHeaderCaptureInterceptor.INBOUND_HEADERS_CONTEXT_KEY.get();

            Metadata metadata = new Metadata();
            metadata.merge(inboundHeaders);
            metadata.put(TENANT_ID_METADATA_KEY, injectHeaderValue);

            // TODO: grab inbound headers

            RpcResponseProto response =
                rpcRequestServiceStub
                    .withInterceptors(
                        MetadataUtils.newAttachHeadersInterceptor(
                            metadata
                        )
                    )
                    .request(request);

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Throwable exc) {
            LOG.error("forwarded request failed", exc);
            responseObserver.onError(exc);
        }
    }
}
