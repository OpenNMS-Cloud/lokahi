package org.opennms.horizon.shared.ipc.grpc.server.manager;

import java.util.concurrent.CompletableFuture;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion_gateway.GatewayRpcResponseProto;

/**
 * High level interface for dispatching requests.
 *
 * Implementer of this type might decide to validate request and received response after execution of rpc operation.
 * However, it is not determined by interface contract.
 */
public interface RpcRequestDispatcher {

    CompletableFuture<GatewayRpcResponseProto> dispatch(String tenantId, String locationId, RpcRequestProto request);
    CompletableFuture<GatewayRpcResponseProto> dispatch(String tenantId, String locationId, String systemId, RpcRequestProto request);

}
