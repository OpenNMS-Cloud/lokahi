package org.opennms.miniongateway.rpcrequest;

import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcResponseProto;

import java.util.concurrent.CompletableFuture;
import org.opennms.cloud.grpc.minion_gateway.GatewayRpcRequestProto;
import org.opennms.cloud.grpc.minion_gateway.GatewayRpcResponseProto;

public interface RpcRequestRouter {
    CompletableFuture<GatewayRpcResponseProto> routeRequest(GatewayRpcRequestProto request);
}
