package org.opennms.horizon.shared.ipc.grpc.server.manager;

import org.opennms.horizon.shared.ipc.rpc.api.RpcResponseHandler;

public interface RpcRequestTracker {
    void addRequest(String id, RpcResponseHandler responseHandler);
    RpcResponseHandler lookup(String id);
    void remove(String id);

    void clear();
}
