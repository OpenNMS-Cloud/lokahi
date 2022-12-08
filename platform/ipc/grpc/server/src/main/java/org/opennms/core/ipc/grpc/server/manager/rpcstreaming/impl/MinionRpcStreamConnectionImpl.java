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

package org.opennms.core.ipc.grpc.server.manager.rpcstreaming.impl;

import com.google.common.base.Strings;
import io.grpc.stub.StreamObserver;
import org.opennms.core.ipc.grpc.common.RpcRequestProto;
import org.opennms.core.ipc.grpc.common.RpcResponseProto;
import org.opennms.core.ipc.grpc.server.manager.MinionInfo;
import org.opennms.core.ipc.grpc.server.manager.MinionManager;
import org.opennms.core.ipc.grpc.server.manager.RpcConnectionTracker;
import org.opennms.core.ipc.grpc.server.manager.RpcRequestTracker;
import org.opennms.core.ipc.grpc.server.manager.rpcstreaming.MinionRpcStreamConnection;
import org.opennms.horizon.ipc.rpc.api.RpcResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import static org.opennms.horizon.ipc.rpc.api.RpcModule.MINION_HEADERS_MODULE;

/**
 * RPC Streaming connection from a Minion.  Note that the Minion identity is expected to be the first message received,
 * so this connection needs to notify the Minion Manager of the minion's identity, and initiate wiring to the internal
 * connection tracker so requests to the minion can find this connection.
 */
public class MinionRpcStreamConnectionImpl implements MinionRpcStreamConnection {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(MinionRpcStreamConnectionImpl.class);

    private Logger log = DEFAULT_LOGGER;

    private final StreamObserver<RpcRequestProto> streamObserver;
    private final Consumer<StreamObserver<RpcRequestProto>> onCompleted;
    private final RpcConnectionTracker rpcConnectionTracker;
    private final RpcRequestTracker rpcRequestTracker;
    private final ExecutorService responseHandlerExecutor;
    private final MinionManager minionManager;

    public MinionRpcStreamConnectionImpl(
            StreamObserver<RpcRequestProto> streamObserver,
            Consumer<StreamObserver<RpcRequestProto>> onCompleted,
            RpcConnectionTracker rpcConnectionTracker,
            RpcRequestTracker rpcRequestTracker,
            ExecutorService responseHandlerExecutor,
            MinionManager minionManager
            ) {

        this.streamObserver = streamObserver;
        this.onCompleted = onCompleted;
        this.rpcConnectionTracker = rpcConnectionTracker;
        this.rpcRequestTracker = rpcRequestTracker;
        this.responseHandlerExecutor = responseHandlerExecutor;
        this.minionManager = minionManager;
    }

    private boolean isMinionIndentityHeaders(RpcResponseProto rpcMessage) {
        return Objects.equals(MINION_HEADERS_MODULE, rpcMessage.getModuleId());
    }

    @Override
    public void handleRpcStreamInboundMessage(RpcResponseProto message) {
        if (isMinionIndentityHeaders(message)) {
            String location = message.getLocation();
            String systemId = message.getSystemId();

            if (Strings.isNullOrEmpty(location) || Strings.isNullOrEmpty(systemId)) {
                log.error("Invalid metadata received with location = {} , systemId = {}", location, systemId);
                return;
            }

            // Register the Minion
            boolean added = rpcConnectionTracker.addConnection(message.getLocation(), message.getSystemId(), streamObserver);

            if (added) {
                log.info("Added RPC handler for minion {} at location {}", systemId, location);

                // Notify the MinionManager of the addition
                MinionInfo minionInfo = new MinionInfo();
                minionInfo.setId(systemId);
                minionInfo.setLocation(location);
                minionManager.addMinion(minionInfo);
            }
        } else {
            // Schedule processing of the message which is expected to be a response to a past request sent to the
            //  Minion
            asyncQueueHandleResponse(message);
        }
    }

    @Override
    public void handleRpcStreamInboundError(Throwable thrown) {
        log.error("Error in rpc streaming", thrown);
    }

    @Override
    public void handleRpcStreamInboundCompleted() {
        onCompleted.accept(streamObserver);
    }

//========================================
// Internals
//----------------------------------------

    private void asyncQueueHandleResponse(RpcResponseProto message) {
        responseHandlerExecutor.execute(() -> syncHandleResponse(message));
    }

    private void syncHandleResponse(RpcResponseProto message) {
        if (Strings.isNullOrEmpty(message.getRpcId())) {
            return;
        }

        // Handle response from the Minion.
        RpcResponseHandler responseHandler = rpcRequestTracker.lookup(message.getRpcId());

        if (responseHandler != null && message.getRpcContent() != null) {
            responseHandler.sendResponse(message.getRpcContent().toStringUtf8());
        } else {
            log.debug("Received a response for request for module: {} with RpcId:{}, but no outstanding request was found with this id." +
                    "The request may have timed out", message.getModuleId(), message.getRpcId());
        }
    }
}
