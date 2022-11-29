package org.opennms.miniongateway.grpc.server.rpcrequest.impl;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Setter;
import org.apache.ignite.Ignite;
import org.apache.ignite.compute.ComputeTaskFuture;
import org.apache.ignite.lang.IgniteFuture;
import org.apache.ignite.resources.SpringResource;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcResponseProto;
import org.opennms.horizon.shared.grpc.common.TenantIDGrpcServerInterceptor;
import org.opennms.miniongateway.grpc.server.rpcrequest.RouterTaskData;
import org.opennms.miniongateway.grpc.server.rpcrequest.RpcRequestRouterIgniteTask;
import org.opennms.miniongateway.rpcrequest.RpcRequestRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class RpcRequestRouterImpl implements RpcRequestRouter {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(RpcRequestRouterImpl.class);

    @Setter
    private Logger log = DEFAULT_LOGGER;

    @Autowired
    @Setter
    private Ignite ignite;

    @Autowired
    @Setter
    private RpcRequestRouterIgniteTask rpcRequestRouterIgniteTask;

    @Autowired
    @Setter
    private TenantIDGrpcServerInterceptor tenantIDGrpcServerInterceptor;


    //========================================
// Interface: RpcRequestRouter
//----------------------------------------

    @Override
    public CompletableFuture<RpcResponseProto> routeRequest(RpcRequestProto request) {
        CompletableFuture<RpcResponseProto> resultFuture = new CompletableFuture<>();

        String tenantId = tenantIDGrpcServerInterceptor.readCurrentContextTenantId();
        RouterTaskData routerTaskData = new RouterTaskData(tenantId, request.toByteArray());

        ComputeTaskFuture<byte[]> igniteFuture =
            ignite.compute().executeAsync(rpcRequestRouterIgniteTask, routerTaskData);

        igniteFuture.listen((futureArg) -> this.processCompletedIgniteFuture(resultFuture, futureArg));

        return resultFuture;
    }

//========================================
// Internals
//----------------------------------------

    private void processCompletedIgniteFuture(CompletableFuture<RpcResponseProto> completableFuture, IgniteFuture<byte[]> igniteFuture) {
        assert(igniteFuture.isDone());

        try {
            RpcResponseProto result = RpcResponseProto.parseFrom(igniteFuture.get());

            completableFuture.complete(result);
        } catch (InvalidProtocolBufferException ipbExc) {
            log.error("failed to parse RPC response", ipbExc);
            completableFuture.completeExceptionally(ipbExc);
        } catch (Exception exc) {
            completableFuture.completeExceptionally(exc);
        }
    }
}
