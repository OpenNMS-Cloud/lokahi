package org.opennms.core.ipc.grpc.server.manager;

import org.opennms.horizon.ipc.rpc.api.RpcResponseHandler;

public interface RpcRequestTracker {
    void addRequest(String id, RpcResponseHandler responseHandler);
    RpcResponseHandler lookup(String id);
    void remove(String id);

    void clear();
}
