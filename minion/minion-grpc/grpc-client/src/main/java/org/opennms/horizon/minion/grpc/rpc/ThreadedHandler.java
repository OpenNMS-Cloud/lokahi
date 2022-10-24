package org.opennms.horizon.minion.grpc.rpc;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcResponseProto;

public class ThreadedHandler implements RpcRequestHandler {

    private final RpcRequestHandler delegate;
    private final AtomicLong counter = new AtomicLong();
    private final ThreadFactory requestHandlerThreadFactory = (runnable) -> new Thread(runnable, "rpc-request-handler-" + counter.incrementAndGet());
    private final ExecutorService requestHandlerExecutor = Executors.newCachedThreadPool(requestHandlerThreadFactory);

    public ThreadedHandler(RpcRequestHandler delegate) {
        this.delegate = delegate;
    }

    @Override
    public CompletableFuture<RpcResponseProto> handle(RpcRequestProto request) {
        CompletableFuture<RpcResponseProto> future = new CompletableFuture<>();
        requestHandlerExecutor.submit(() -> delegate.handle(request).whenComplete((r, e) -> {
            if (e != null) {
                future.completeExceptionally(e);
                return;
            }
            future.complete(r);
        }));
        return future;
    }

    public void shutdown() {
        delegate.shutdown();
        requestHandlerExecutor.shutdown();
    }
}
