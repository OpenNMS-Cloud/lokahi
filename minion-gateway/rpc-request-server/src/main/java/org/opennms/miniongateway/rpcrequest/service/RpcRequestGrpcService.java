package org.opennms.miniongateway.rpcrequest.service;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.Setter;
import org.opennms.cloud.grpc.minion_gateway.GatewayRpcRequestProto;
import org.opennms.cloud.grpc.minion_gateway.RpcRequestServiceGrpc;
import org.opennms.cloud.grpc.minion_gateway.GatewayRpcResponseProto;
import org.opennms.horizon.shared.grpc.common.GrpcIpcServer;
import org.opennms.horizon.shared.grpc.common.TenantIDGrpcServerInterceptor;
import org.opennms.miniongateway.rpcrequest.RpcRequestRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Component
public class RpcRequestGrpcService extends RpcRequestServiceGrpc.RpcRequestServiceImplBase {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(RpcRequestGrpcService.class);

    private Logger log = DEFAULT_LOGGER;

    @Autowired
    @Setter
    private RpcRequestRouter rpcRequestRouter;

    @Autowired
    @Qualifier("internalGrpcIpcServer")
    @Setter
    private GrpcIpcServer grpcIpcServer;


//========================================
// Lifecycle
//----------------------------------------

    @PostConstruct
    public void start() throws IOException {
        // TODO: use explicit tenant-id handling
        grpcIpcServer.startServer(this);
        log.info("Initiated RPC-Request GRPC Service");
    }

//========================================
// Service API
//----------------------------------------

    @Override
    public void request(GatewayRpcRequestProto request, StreamObserver<GatewayRpcResponseProto> responseObserver) {
        CompletableFuture<GatewayRpcResponseProto> future = rpcRequestRouter.routeRequest(request);

        future.whenComplete(
            (response, exception) -> handleCompletedRequest(response, exception, responseObserver)
        );
    }

//========================================
// Internals
//----------------------------------------

    private void handleCompletedRequest(GatewayRpcResponseProto response, Throwable exception, StreamObserver<GatewayRpcResponseProto> responseObserver) {
        if (exception != null) {
            Status status = Status.UNAVAILABLE.withDescription(exception.getMessage());
            responseObserver.onError(status.asRuntimeException());
        } else {
            responseObserver.onNext(response);
        }

        responseObserver.onCompleted();
    }
}
