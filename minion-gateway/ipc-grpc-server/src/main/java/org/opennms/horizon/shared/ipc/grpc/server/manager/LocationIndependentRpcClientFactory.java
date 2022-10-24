package org.opennms.horizon.shared.ipc.grpc.server.manager;

import org.opennms.horizon.shared.ipc.grpc.server.manager.rpc.LocationIndependentRpcClient;
import org.opennms.horizon.shared.ipc.grpc.server.manager.rpc.RemoteRegistrationHandler;
import org.opennms.horizon.shared.ipc.rpc.api.RpcModule;
import org.opennms.horizon.shared.ipc.rpc.api.RpcRequest;
import org.opennms.horizon.shared.ipc.rpc.api.RpcResponse;

public interface LocationIndependentRpcClientFactory {
    <REQUEST extends RpcRequest, RESPONSE extends RpcResponse>
    LocationIndependentRpcClient<REQUEST, RESPONSE>
    createClient(
            RpcModule<REQUEST, RESPONSE> localModule,
            RemoteRegistrationHandler remoteRegistrationHandler
    );
}
