package org.opennms.miniongateway.grpc.server;

import java.util.concurrent.CompletableFuture;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcResponseProto;

public interface ServerHandler {

    String getId();

    CompletableFuture<RpcResponseProto> handle(RpcRequestProto requestProto);

}
