package org.opennms.horizon.shared.ipc.grpc.server.manager.adapter;

import io.grpc.stub.StreamObserver;
import java.util.function.Consumer;
import org.opennms.cloud.grpc.minion.RpcResponseProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stream Observer that handles inbound RPC calls initiated by the Minion.
 */
public class InboundRpcAdapter implements StreamObserver<RpcResponseProto> {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(InboundRpcAdapter.class);

    private Logger log = DEFAULT_LOGGER;

    private final Consumer<RpcResponseProto> onMessage;
    private final Consumer<Throwable> onError;
    private final Runnable onCompleted;

    public InboundRpcAdapter(Consumer<RpcResponseProto> onMessage, Consumer<Throwable> onError, Runnable onCompleted) {
        this.onMessage = onMessage;
        this.onError = onError;
        this.onCompleted = onCompleted;
    }

    @Override
    public void onNext(RpcResponseProto rpcResponseProto) {
        onMessage.accept(rpcResponseProto);
    }

    @Override
    public void onError(Throwable thrown) {
        onError.accept(thrown);
    }

    @Override
    public void onCompleted() {
        onCompleted.run();
    }
}
