package org.opennms.miniongateway.detector.server;

import java.util.concurrent.CompletableFuture;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcResponseProto;

public interface IgniteRpcRequestDispatcher {

    CompletableFuture<RpcResponseProto> execute(RpcRequestProto request);

}
