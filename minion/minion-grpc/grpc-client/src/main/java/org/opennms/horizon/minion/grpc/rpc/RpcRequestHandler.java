package org.opennms.horizon.minion.grpc.rpc;

import java.util.concurrent.CompletableFuture;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcResponseProto;

public interface RpcRequestHandler {

    CompletableFuture<RpcResponseProto> handle(RpcRequestProto request);

    default void shutdown() {};

}
