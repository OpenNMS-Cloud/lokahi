/*
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 */

package org.opennms.core.ipc.grpc.server.manager.impl;

import org.opennms.core.ipc.grpc.server.manager.RpcRequestTimeoutManager;
import org.opennms.horizon.ipc.rpc.api.RpcResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

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
                    log.warn("RPC request from module: {} with RpcId:{} timedout ", responseHandler.getRpcModule().getId(),
                            responseHandler.getRpcId());
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
