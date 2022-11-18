package org.opennms.horizon.core.rpc.request.client;

import com.google.protobuf.Message;
import io.grpc.Channel;
import io.grpc.ClientInterceptor;
import io.grpc.Metadata;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.MetadataUtils;
import lombok.Getter;
import lombok.Setter;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcRequestServiceGrpc;
import org.opennms.cloud.grpc.minion.RpcResponseProto;
import org.opennms.horizon.shared.ipc.rpc.api.RequestBuilder;
import org.opennms.horizon.shared.ipc.rpc.api.RpcClient;
import org.opennms.horizon.shared.ipc.rpc.api.RpcClientFactory.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class RpcRequestGrpcClient<T extends Message> implements RpcClient<T> {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(RpcRequestGrpcClient.class);

    private static final Metadata.Key HEADER_KE = Metadata.Key.of("tenant-id", Metadata.ASCII_STRING_MARSHALLER);

    private Logger log = DEFAULT_LOGGER;


    private Channel channel;
    private RpcRequestServiceGrpc.RpcRequestServiceFutureStub rpcRequestServiceFutureStub;
    private Executor futureCompletionExecutor = Executors.newSingleThreadExecutor();

    private Deserializer<T> deserializer = (in) -> (T) in;

    private boolean tlsEnabled = false;
    private String host = RpcRequestGrpcClientFactory.DEFAULT_GRPC_HOSTNAME;
    private int port = RpcRequestGrpcClientFactory.DEFAULT_GRPC_PORT;
    private int maxMessageSize = RpcRequestGrpcClientFactory.DEFAULT_MAX_MESSAGE_SIZE;

    /**
     * TESTABILITY: enable injection of a mock to bypass the static method execution of NettyChannelBuilder.forAddress
     */
    @Setter
    private BiFunction<String, Integer, NettyChannelBuilder>
        nettyChannelBuilderForAddressFunction = this::callDefaultNettyChannelBuilderForAddress;

    /**
     * TESTABILITY: enable injection of a mock to bypass the static method execution of RpcRequestServiceGrpc.newFutureStub
     */
    @Setter
    private Function<Channel, RpcRequestServiceGrpc.RpcRequestServiceFutureStub>
        rpcRequestServiceNewFutureStubFunction = this::callDefaultRpcRequestServiceNewFutureStub;

    /**
     * TESTABILITY: enable injection of a mock to bypass the static method execution of MetadataUtils.newAttachHeadersInterceptor
     */
    @Setter
    private Function<Metadata, ClientInterceptor>
        newAttachHeadersInterceptorFunction = this::callDefaultAttachHeadersInterceptor;

//========================================
// Lifecycle
//----------------------------------------

    public void init() {
        NettyChannelBuilder channelBuilder = nettyChannelBuilderForAddressFunction.apply(host, port)
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

        rpcRequestServiceFutureStub = rpcRequestServiceNewFutureStubFunction.apply(channel);
    }

//========================================
// Getters and Setters
//----------------------------------------

    public Deserializer<T> getDeserializer() {
        return deserializer;
    }

    public void setDeserializer(Deserializer<T> deserializer) {
        this.deserializer = deserializer;
    }

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

    public Logger getLog() {
        return log;
    }

    public void setLog(Logger log) {
        this.log = log;
    }

//========================================
// RpcClient INTERFACE
//----------------------------------------

    /**
     * Execute the give request over GRPC and return a future tracking the completion of the request.
     *
     * TBD888
     *
     * DEVELOPER NOTE: using reflection here as a temporary work-around due to 2 different definitions of
     *  ListenableFuture which appears to be caused by the GRPC bundle being shaded.  Once this code is moved out of
     *  OSGi, this use of reflection can (and SHOULD) be undone.
     *
     *
     * @param tenantId
     * @param request
     * @return
     */
    @Override
    public CompletableFuture<T> execute(String tenantId, RpcRequestProto request) {
        CompletableFuture<T> resultFuture = new CompletableFuture<>();

        try {
            //     ListenableFuture<RpcResponseProto> listenableFuture = rpcRequestServiceFutureStub.request(request);
            //
            //     listenableFuture.addListener(
            //         () -> processRequestCompletion(listenableFuture, resultFuture),
            //         futureCompletionExecutor
            //     );

            Metadata metadata = new Metadata();
            metadata.put(HEADER_KE, tenantId);

            Object listenableFutureObject =
                rpcRequestServiceFutureStub
                    .withInterceptors(
                        newAttachHeadersInterceptorFunction.apply(metadata)
                    )
                    .request(request)
                ;

            Method method = listenableFutureObject.getClass().getMethod("addListener", Runnable.class, Executor.class);
            method.invoke(listenableFutureObject,
                    (Runnable) () -> processRequestCompletion(listenableFutureObject, resultFuture),
                    futureCompletionExecutor
                );
        } catch (Throwable thrown) {
            log.error("failed to execute RPC request", thrown);
            resultFuture.completeExceptionally(thrown);
        }

        return resultFuture;
    }

    @Override
    public RequestBuilder builder(String module) {
        return new RpcRequestBuilder(module);
    }

//========================================
//
//----------------------------------------

    /**
     * Process the completion of the request; the work here is chaining the different types of futures.
     *
     * TBD888: see the comment on execute() above
     *
     * @param listenableFutureObject
     * @param resultFuture
     */
    private void processRequestCompletion(Object listenableFutureObject, CompletableFuture<T> resultFuture) {
        try {
            //         RpcResponseProto response = listenableFuture.get();
            //         T finalResult = deserializer.deserialize(response);
            Method getMethod = listenableFutureObject.getClass().getMethod("get");
            RpcResponseProto response = (RpcResponseProto) getMethod.invoke(listenableFutureObject);

            T finalResult = deserializer.deserialize(response);
            resultFuture.complete(finalResult);
        } catch (Exception exc) {
            resultFuture.completeExceptionally(exc);
        }
    }

//========================================
// Testability
//----------------------------------------

    private NettyChannelBuilder callDefaultNettyChannelBuilderForAddress(String host, int port) {
        return
            NettyChannelBuilder.forAddress(host, port)
                ;
    }

    private RpcRequestServiceGrpc.RpcRequestServiceFutureStub callDefaultRpcRequestServiceNewFutureStub(Channel channel) {
        return RpcRequestServiceGrpc.newFutureStub(channel);
    }

    private ClientInterceptor callDefaultAttachHeadersInterceptor(Metadata headers) {
        return MetadataUtils.newAttachHeadersInterceptor(headers);
    }
}
