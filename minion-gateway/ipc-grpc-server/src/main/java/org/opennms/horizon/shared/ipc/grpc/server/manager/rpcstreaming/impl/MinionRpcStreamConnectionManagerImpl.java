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
package org.opennms.horizon.shared.ipc.grpc.server.manager.rpcstreaming.impl;

import io.grpc.stub.StreamObserver;
import java.util.concurrent.ExecutorService;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.horizon.shared.grpc.common.LocationServerInterceptor;
import org.opennms.horizon.shared.grpc.common.TenantIDGrpcServerInterceptor;
import org.opennms.horizon.shared.ipc.grpc.server.manager.MinionInfo;
import org.opennms.horizon.shared.ipc.grpc.server.manager.MinionManager;
import org.opennms.horizon.shared.ipc.grpc.server.manager.RpcConnectionTracker;
import org.opennms.horizon.shared.ipc.grpc.server.manager.RpcRequestTracker;
import org.opennms.horizon.shared.ipc.grpc.server.manager.adapter.InboundRpcAdapter;
import org.opennms.horizon.shared.ipc.grpc.server.manager.rpcstreaming.MinionRpcStreamConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinionRpcStreamConnectionManagerImpl implements MinionRpcStreamConnectionManager {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(MinionRpcStreamConnectionManagerImpl.class);

    private Logger log = DEFAULT_LOGGER;

    private final RpcConnectionTracker rpcConnectionTracker;
    private final RpcRequestTracker rpcRequestTracker;
    private final MinionManager minionManager;
    private final ExecutorService responseHandlerExecutor;
    private final TenantIDGrpcServerInterceptor tenantIDGrpcServerInterceptor;
    private final LocationServerInterceptor locationGrpcServerInterceptor;

    // ========================================
    // Constructor
    // ----------------------------------------

    public MinionRpcStreamConnectionManagerImpl(
            RpcConnectionTracker rpcConnectionTracker,
            RpcRequestTracker rpcRequestTracker,
            MinionManager minionManager,
            ExecutorService responseHandlerExecutor,
            TenantIDGrpcServerInterceptor tenantIDGrpcServerInterceptor,
            LocationServerInterceptor locationGrpcServerInterceptor) {

        this.rpcConnectionTracker = rpcConnectionTracker;
        this.rpcRequestTracker = rpcRequestTracker;
        this.minionManager = minionManager;
        this.responseHandlerExecutor = responseHandlerExecutor;
        this.tenantIDGrpcServerInterceptor = tenantIDGrpcServerInterceptor;
        this.locationGrpcServerInterceptor = locationGrpcServerInterceptor;
    }

    // ========================================
    // Lifecycle
    // ----------------------------------------

    public void shutdown() {
        responseHandlerExecutor.shutdown();
    }

    // ========================================
    // Processing
    // ----------------------------------------

    @Override
    public InboundRpcAdapter startRpcStreaming(StreamObserver<RpcRequestProto> requestObserver) {
        MinionRpcStreamConnectionImpl connection = new MinionRpcStreamConnectionImpl(
                requestObserver,
                this::onConnectionCompleted,
                this::onError,
                rpcConnectionTracker,
                rpcRequestTracker,
                responseHandlerExecutor,
                minionManager,
                tenantIDGrpcServerInterceptor,
                locationGrpcServerInterceptor);

        InboundRpcAdapter result = new InboundRpcAdapter(
                connection::handleRpcStreamInboundMessage,
                connection::handleRpcStreamInboundError,
                connection::handleRpcStreamInboundCompleted);

        return result;
    }

    private void onError(StreamObserver<RpcRequestProto> streamObserver, Throwable throwable) {
        log.info("Minion RPC handler reported an error");
        MinionInfo removedMinionInfo = rpcConnectionTracker.removeConnection(streamObserver);

        // Notify the MinionManager of the removal
        if (removedMinionInfo.getId() != null) {
            minionManager.removeMinion(removedMinionInfo);
        }
    }

    // ========================================
    // Internals
    // ----------------------------------------

    private void onConnectionCompleted(StreamObserver<RpcRequestProto> streamObserver) {
        log.info("Minion RPC handler closed");
        MinionInfo removedMinionInfo = rpcConnectionTracker.removeConnection(streamObserver);

        // Notify the MinionManager of the removal
        if (removedMinionInfo.getId() != null) {
            minionManager.removeMinion(removedMinionInfo);
        }
    }
}
