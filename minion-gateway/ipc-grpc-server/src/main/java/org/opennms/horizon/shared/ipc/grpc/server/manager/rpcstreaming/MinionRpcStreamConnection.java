package org.opennms.horizon.shared.ipc.grpc.server.manager.rpcstreaming;


import org.opennms.cloud.grpc.minion.RpcResponseProto;

public interface MinionRpcStreamConnection {
    void handleRpcStreamInboundMessage(RpcResponseProto message);

    void handleRpcStreamInboundError(Throwable thrown);

    void handleRpcStreamInboundCompleted();
}
