package org.opennms.horizon.shared.ipc.grpc.server.manager.rpc;

import java.util.concurrent.CompletableFuture;
import org.opennms.horizon.shared.ipc.rpc.api.RpcRequest;

@SuppressWarnings("rawtypes")
public interface RemoteRegistrationHandler {
    /**
     * Register this remote call, with the given expiration time.
     *
     * @param request request that will be sent to the remote
     * @param expiration time at which the remote call should be timed out locally
     * @return identifier for the remote call
     */
    String registerRemoteCall(RpcRequest request, long expiration, CompletableFuture future);
}
