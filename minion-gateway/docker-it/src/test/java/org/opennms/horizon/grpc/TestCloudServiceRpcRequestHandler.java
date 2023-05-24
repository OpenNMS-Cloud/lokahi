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

package org.opennms.horizon.grpc;

import io.grpc.stub.StreamObserver;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcResponseProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class TestCloudServiceRpcRequestHandler implements StreamObserver<RpcRequestProto> {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(TestCloudServiceRpcRequestHandler.class);

    private Logger LOG = DEFAULT_LOGGER;

    private final Object lock = new Object();

    private final Consumer<RpcResponseProto> onResponse;

    private List<RpcRequestProto> receivedRequests = new LinkedList<>();

//========================================
// Constructor
//----------------------------------------

    public TestCloudServiceRpcRequestHandler(Consumer<RpcResponseProto> onResponse) {
        this.onResponse = onResponse;
    }

//========================================
// StreamObserver Interface
//----------------------------------------

    @Override
    public void onNext(RpcRequestProto value) {
        LOG.info("Have inbound RpcRequest: rpc-id={}; system-id={}; module-id={}; expiration-time={}; payload={}",
            value.getRpcId(),
            value.getIdentity().getSystemId(),
            value.getModuleId(),
            value.getExpirationTime(),
            value.getPayload()
        );

        synchronized (lock) {
            receivedRequests.add(value);
        }

        if (onResponse != null) {
            RpcResponseProto.Builder rpcResponseProtoBuilder =
                RpcResponseProto.newBuilder();

            rpcResponseProtoBuilder.setRpcId(value.getRpcId());
            rpcResponseProtoBuilder.setIdentity(value.getIdentity());
            rpcResponseProtoBuilder.setModuleId(value.getModuleId());
            rpcResponseProtoBuilder.setPayload(value.getPayload());

            RpcResponseProto rpcResponseProto = rpcResponseProtoBuilder.build();

            LOG.info("Sending response for rpc request: id={}", rpcResponseProto.getRpcId());
            onResponse.accept(rpcResponseProto);
        }
    }

    @Override
    public void onError(Throwable t) {
        LOG.error("RPC error", t);
    }

    @Override
    public void onCompleted() {
    }

//========================================
// Test Data Access
//----------------------------------------

    public RpcRequestProto[] getReceivedRequestsSnapshot() {
        synchronized (lock) {
            return receivedRequests.toArray(new RpcRequestProto[0]);
        }
    }
}
