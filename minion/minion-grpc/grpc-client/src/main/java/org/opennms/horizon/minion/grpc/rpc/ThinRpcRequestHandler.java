package org.opennms.horizon.minion.grpc.rpc;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcResponseProto;
import org.opennms.horizon.shared.ipc.rpc.IpcIdentity;
import org.opennms.horizon.shared.ipc.rpc.api.minion.RpcHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThinRpcRequestHandler implements RpcRequestHandler {

    private final Logger logger = LoggerFactory.getLogger(ThinRpcRequestHandler.class);

    private final AtomicLong counter = new AtomicLong();
    private final ThreadFactory requestHandlerThreadFactory = (runnable) -> new Thread(runnable, "rpc-request-handler-" + counter.incrementAndGet());
    private final ExecutorService requestHandlerExecutor = Executors.newCachedThreadPool(requestHandlerThreadFactory);
    // Maintain the map of RPC modules and their ID.
    private final Map<String, RpcHandler<Message, Message>> registeredModules = new ConcurrentHashMap<>();
    private final IpcIdentity ipcIdentity;

    public ThinRpcRequestHandler(IpcIdentity ipcIdentity) {
        this.ipcIdentity = ipcIdentity;
    }

    @Override
    public CompletableFuture<RpcResponseProto> handle(RpcRequestProto requestProto) {
        long currentTime = requestProto.getExpirationTime();
        if (requestProto.getExpirationTime() < currentTime) {
            logger.debug("ttl already expired for the request id = {}, won't process.", requestProto.getRpcId());
            return CompletableFuture.failedFuture(new TimeoutException());
        }
        String moduleId = requestProto.getModuleId();
        if (moduleId.isBlank()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Empty module id"));
        }
        logger.debug("Received RPC request with RpcID:{} for module {}", requestProto.getRpcId(), requestProto.getModuleId());
        RpcHandler<Message, Message> handler = registeredModules.get(moduleId);
        if (handler == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Could not find requested module id"));
        }

        CompletableFuture<Message> future = handler.execute(handler.unmarshal(requestProto));
        return future.thenApply((rpcResponse) -> {
            // Construct response using the same rpcId;
            RpcResponseProto responseProto = RpcResponseProto.newBuilder()
                .setRpcId(requestProto.getRpcId())
                .setIdentity(Identity.newBuilder().setSystemId(ipcIdentity.getId()).build())
                .setModuleId(requestProto.getModuleId())
                .setPayload(Any.pack(rpcResponse))
                .build();

            return responseProto;
        });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void bind(RpcHandler handler) throws Exception {
        if (handler != null) {
            if (registeredModules.containsKey(handler.getId())) {
                logger.warn(" {} handler is already registered", handler.getId());
            } else {
                registeredModules.put(handler.getId(), handler);
                logger.info("Registered handler {} with gRPC IPC client", handler.getId());
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void unbind(RpcHandler handler) throws Exception {
        if (handler != null) {
            if (registeredModules.remove(handler.getId()) != null) {
                logger.info("Removing handler {} from gRPC IPC client.", handler.getId());
            }
        }
    }

    public void shutdown() {
        requestHandlerExecutor.shutdown();
        registeredModules.clear();
    }
}
