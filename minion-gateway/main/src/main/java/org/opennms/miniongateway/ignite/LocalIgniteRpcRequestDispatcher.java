package org.opennms.miniongateway.ignite;

import java.util.concurrent.CompletableFuture;
import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcRequestProto.Builder;
import org.opennms.cloud.grpc.minion.RpcResponseProto;
import org.opennms.cloud.grpc.minion_gateway.GatewayRpcRequestProto;
import org.opennms.cloud.grpc.minion_gateway.GatewayRpcResponseProto;
import org.opennms.cloud.grpc.minion_gateway.MinionIdentity;
import org.opennms.horizon.shared.ipc.grpc.server.manager.RpcRequestDispatcher;
import org.opennms.miniongateway.detector.server.IgniteRpcRequestDispatcher;

public class LocalIgniteRpcRequestDispatcher implements IgniteRpcRequestDispatcher {

    private RpcRequestDispatcher rpcRequestDispatcher;

    public LocalIgniteRpcRequestDispatcher(RpcRequestDispatcher rpcRequestDispatcher) {
        this.rpcRequestDispatcher = rpcRequestDispatcher;
    }

    @Override
    public CompletableFuture<GatewayRpcResponseProto> execute(GatewayRpcRequestProto request) {
        MinionIdentity identity = request.getIdentity();

        Builder rpcRequest = RpcRequestProto.newBuilder()
            .setRpcId(request.getRpcId())
            .setModuleId(request.getModuleId())
            .setExpirationTime(request.getExpirationTime())
            .setPayload(request.getPayload());

        if (identity.getSystemId().isBlank()) {
            return rpcRequestDispatcher.dispatch(identity.getTenantId(), identity.getLocationId(), rpcRequest.build());
        }

        rpcRequest.setIdentity(Identity.newBuilder().setSystemId(identity.getSystemId()));  // Why replace the existing identity with a new one?
        return rpcRequestDispatcher.dispatch(identity.getTenantId(), identity.getLocationId(), identity.getSystemId(), rpcRequest.build());
    }

}
