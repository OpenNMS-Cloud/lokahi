package org.opennms.miniongateway.detector.server;

import java.util.concurrent.CompletableFuture;
import org.opennms.cloud.grpc.minion_gateway.GatewayRpcRequestProto;
import org.opennms.cloud.grpc.minion_gateway.GatewayRpcResponseProto;

public interface IgniteRpcRequestDispatcher {

    CompletableFuture<GatewayRpcResponseProto> execute(GatewayRpcRequestProto request);

}
