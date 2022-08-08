package org.opennms.core.ipc.grpc.server.manager;

import org.opennms.horizon.ipc.rpc.api.RpcResponseHandler;

public interface RpcRequestTimeoutManager {
    void start();
    void shutdown();

    void registerRequestTimeout(RpcResponseHandler rpcResponseHandler);
}
