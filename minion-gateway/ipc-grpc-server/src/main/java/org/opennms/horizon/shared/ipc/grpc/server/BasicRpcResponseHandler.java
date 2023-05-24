package org.opennms.horizon.shared.ipc.grpc.server;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.opennms.cloud.grpc.minion.RpcResponseProto;
import org.opennms.cloud.grpc.minion_gateway.GatewayRpcResponseProto;
import org.opennms.horizon.shared.ipc.rpc.api.RpcResponseHandler;

public class BasicRpcResponseHandler implements RpcResponseHandler {

    private final long expirationTime;
    private final String rpcId;
    private final String moduleId;
    private final CompletableFuture<RpcResponseProto> future;

    public BasicRpcResponseHandler(long expirationTime, String rpcId, String moduleId, CompletableFuture<RpcResponseProto> future) {
        this.expirationTime = expirationTime;
        this.rpcId = rpcId;
        this.moduleId = moduleId;
        this.future = future;
    }

    @Override
    public void sendResponse(RpcResponseProto response) {
        if (response == null) {
            future.completeExceptionally(new TimeoutException());
            return;
        }

        future.complete(response);
    }

    @Override
    public boolean isProcessed() {
        return future.isDone();
    }

    @Override
    public String getRpcId() {
        return rpcId;
    }

    @Override
    public String getRpcModuleId() {
        return moduleId;
    }

    @Override
    public int compareTo(Delayed other) {
        long myDelay = getDelay(TimeUnit.MILLISECONDS);
        long otherDelay = other.getDelay(TimeUnit.MILLISECONDS);
        return Long.compare(myDelay, otherDelay);
    }

    @Override
    public long getDelay(TimeUnit unit) {
        long now = System.currentTimeMillis();
        return unit.convert(expirationTime - now, TimeUnit.MILLISECONDS);
    }

}
