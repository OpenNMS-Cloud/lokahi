package org.opennms.horizon.shared.ipc.grpc.server.manager;

import org.opennms.horizon.shared.ipc.rpc.api.RpcResponseHandler;

public interface RpcRequestTimeoutManager {
    void start();
    void shutdown();

    void registerRequestTimeout(RpcResponseHandler rpcResponseHandler);
}
