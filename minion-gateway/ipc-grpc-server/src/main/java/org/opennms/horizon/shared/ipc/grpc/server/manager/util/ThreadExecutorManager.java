package org.opennms.horizon.shared.ipc.grpc.server.manager.util;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class ThreadExecutorManager {
    private final ThreadFactory timerThreadFactory = new ThreadFactoryBuilder()
            .setNameFormat("rpc-timeout-tracker-%d")
            .build();

    // RPC timeout executor thread retrieves elements from delay queue used to timeout rpc requests.
    private final ExecutorService rpcTimeoutExecutor = Executors.newSingleThreadExecutor(timerThreadFactory);

    private final ThreadFactory responseHandlerThreadFactory = new ThreadFactoryBuilder()
            .setNameFormat("rpc-response-handler-%d")
            .build();
    private final ExecutorService responseHandlerExecutor =
            Executors.newCachedThreadPool(responseHandlerThreadFactory);


//========================================
// Getters
//----------------------------------------

    public ExecutorService getRpcTimeoutExecutor() {
        return rpcTimeoutExecutor;
    }

    public ExecutorService getResponseHandlerExecutor() {
        return responseHandlerExecutor;
    }
}
