package org.opennms.horizon.shared.ipc.rpc.api.minion;

import java.util.concurrent.CompletableFuture;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcResponseProto;

public interface ClientRequestDispatcher {

    CompletableFuture<RpcResponseProto> call(RpcRequestProto responseProto);

}
