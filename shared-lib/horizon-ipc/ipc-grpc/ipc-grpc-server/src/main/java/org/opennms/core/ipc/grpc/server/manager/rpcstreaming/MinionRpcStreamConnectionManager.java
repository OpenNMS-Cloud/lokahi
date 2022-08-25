package org.opennms.core.ipc.grpc.server.manager.rpcstreaming;

import io.grpc.stub.StreamObserver;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.core.ipc.grpc.server.manager.adapter.InboundRpcAdapter;

public interface MinionRpcStreamConnectionManager {
    InboundRpcAdapter startRpcStreaming(StreamObserver<RpcRequestProto> requestObserver);
}
