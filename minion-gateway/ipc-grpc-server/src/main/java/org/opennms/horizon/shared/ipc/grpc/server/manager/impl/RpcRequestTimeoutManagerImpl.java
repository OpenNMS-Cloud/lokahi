/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
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

    // ========================================
    // Setters and Getters
    // ----------------------------------------

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

    // ========================================
    // Operations
    // ----------------------------------------

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

    // ========================================
    // Internals
    // ----------------------------------------

    private void handleRpcTimeouts() {
        while (!shutdown.get()) {
            try {
                RpcResponseHandler responseHandler = rpcTimeoutQueue.take();
                if (!responseHandler.isProcessed()) {
                    responseHandlerExecutor.execute(() -> {
                        try {
                            responseHandler.sendResponse(null);
                        } catch (Throwable throwable) {
                            log.error("ERROR sending RPC Request Timeout", throwable);
                        }
                    });
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
