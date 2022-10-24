package org.opennms.horizon.shared.ipc.grpc.server.manager.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.opennms.horizon.shared.ipc.grpc.server.manager.RpcRequestTracker;
import org.opennms.horizon.shared.ipc.rpc.api.RpcResponseHandler;

public class RpcRequestTrackerImpl implements RpcRequestTracker {

    private final Map<String, RpcResponseHandler> requestMap = new ConcurrentHashMap<>();

    @Override
    public void addRequest(String id, RpcResponseHandler responseHandler) {
        this.requestMap.put(id, responseHandler);
    }

    @Override
    public RpcResponseHandler lookup(String id) {
        return this.requestMap.get(id);
    }

    @Override
    public void remove(String id) {
        this.requestMap.remove(id);
    }

    @Override
    public void clear() {
        this.requestMap.clear();
    }
}
