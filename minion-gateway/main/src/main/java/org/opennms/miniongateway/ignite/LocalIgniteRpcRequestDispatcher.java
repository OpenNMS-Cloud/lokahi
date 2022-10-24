package org.opennms.miniongateway.ignite;

import java.util.concurrent.CompletableFuture;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcResponseProto;
import org.opennms.horizon.shared.ipc.grpc.server.manager.RpcRequestDispatcher;
import org.opennms.miniongateway.detector.server.IgniteRpcRequestDispatcher;

public class LocalIgniteRpcRequestDispatcher implements IgniteRpcRequestDispatcher {

    private RpcRequestDispatcher rpcRequestDispatcher;

    public LocalIgniteRpcRequestDispatcher(RpcRequestDispatcher rpcRequestDispatcher) {
        this.rpcRequestDispatcher = rpcRequestDispatcher;
    }

    @Override
    public CompletableFuture<RpcResponseProto> execute(RpcRequestProto request) {
        if (request.getSystemId().isBlank()) {
            return rpcRequestDispatcher.dispatch(request.getLocation(), request);
        }
        return rpcRequestDispatcher.dispatch(request.getLocation(), request.getSystemId(), request);
    }

}
