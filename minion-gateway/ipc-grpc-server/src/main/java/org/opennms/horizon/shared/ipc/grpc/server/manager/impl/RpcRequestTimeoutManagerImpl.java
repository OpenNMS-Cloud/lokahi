package org.opennms.horizon.shared.ipc.grpc.server.manager.impl;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.PostConstruct;
import org.opennms.horizon.shared.ipc.grpc.server.manager.RpcRequestTimeoutManager;
import org.opennms.horizon.shared.ipc.rpc.api.RpcResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcRequestTimeoutManagerImpl implements RpcRequestTimeoutManager {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(RpcRequestTimeoutManagerImpl.class);

    private Logger log = DEFAULT_LOGGER;

    // RPC timeout executor thread retrieves elements from delay queue used to timeout rpc requests.
    private ExecutorService rpcTimeoutExecutor;
    private ExecutorService responseHandlerExecutor;

    private DelayQueue<RpcResponseHandler> rpcTimeoutQueue = new DelayQueue<>();
    private AtomicBoolean shutdown = new AtomicBoolean(false);

//========================================
// Setters and Getters
//----------------------------------------

    public ExecutorService getRpcTimeoutExecutor() {
        return rpcTimeoutExecutor;
    }

    public void setRpcTimeoutExecutor(ExecutorService rpcTimeoutExecutor) {
        this.rpcTimeoutExecutor = rpcTimeoutExecutor;
    }

    public ExecutorService getResponseHandlerExecutor() {
        return responseHandlerExecutor;
    }

    public void setResponseHandlerExecutor(ExecutorService responseHandlerExecutor) {
        this.responseHandlerExecutor = responseHandlerExecutor;
    }


//========================================
// Operations
//----------------------------------------

    @Override
    @PostConstruct
    public void start() {
        rpcTimeoutExecutor.execute(this::handleRpcTimeouts);
    }

    @Override
    public void shutdown() {
        shutdown.set(true);
        rpcTimeoutExecutor.shutdownNow();
    }

    @Override
    public void registerRequestTimeout(RpcResponseHandler rpcResponseHandler) {
        rpcTimeoutQueue.offer(rpcResponseHandler);
    }

//========================================
// Internals
//----------------------------------------

    private void handleRpcTimeouts() {
        while (!shutdown.get()) {
            try {
                RpcResponseHandler responseHandler = rpcTimeoutQueue.take();
                if (!responseHandler.isProcessed()) {
                    log.warn("RPC request from module: {} with RpcId:{} timedout ", responseHandler.getRpcModuleId(), responseHandler.getRpcId());
                    responseHandlerExecutor.execute(() -> responseHandler.sendResponse(null));
                }
            } catch (InterruptedException e) {
                log.info("interrupted while waiting for an element from rpcTimeoutQueue", e);
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.warn("error while sending response from timeout handler", e);
            }
        }
    }
}
