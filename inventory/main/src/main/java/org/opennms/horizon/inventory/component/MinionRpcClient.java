/*******************************************************************************
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
 *******************************************************************************/

package org.opennms.horizon.inventory.component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcResponseProto;
import org.opennms.cloud.grpc.minion_gateway.GatewayRpcRequestProto;
import org.opennms.cloud.grpc.minion_gateway.GatewayRpcResponseProto;
import org.opennms.cloud.grpc.minion_gateway.RpcRequestServiceGrpc;
import org.opennms.horizon.inventory.grpc.TenantLookup;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.springframework.beans.factory.annotation.Qualifier;

import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;

public class MinionRpcClient {

    private final ManagedChannel channel;
    private final TenantLookup tenantLookup;
    private final long deadline;

    public MinionRpcClient(@Qualifier("minion-gateway") ManagedChannel channel, TenantLookup tenantLookup, long deadline) {
        this.channel = channel;
        this.tenantLookup = tenantLookup;
        this.deadline = deadline;
    }

    private RpcRequestServiceGrpc.RpcRequestServiceStub rpcStub;

    protected void init() {
        rpcStub = RpcRequestServiceGrpc.newStub(channel);
    }

    public void shutdown() {
        if(channel != null && !channel.isShutdown()) {
            channel.shutdown();
        }
    }

    public CompletableFuture<GatewayRpcResponseProto> sendRpcRequest(String tenantId, GatewayRpcRequestProto request) {
        CompletableFuture<GatewayRpcResponseProto> future = new CompletableFuture<>();
        try {
            Metadata metadata = new Metadata();
            metadata.put(GrpcConstants.TENANT_ID_REQUEST_KEY, tenantId);

            rpcStub
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
                .withDeadlineAfter(deadline, TimeUnit.MILLISECONDS)
                .request(request, new StreamObserver<>() {
                    @Override
                    public void onNext(GatewayRpcResponseProto value) {
                        future.complete(value);
                    }
                    @Override
                    public void onError(Throwable t) {
                        future.completeExceptionally(t);
                    }
                    @Override
                    public void onCompleted() {
                    }
            });
        } catch (Exception e) {
            future.completeExceptionally(new RuntimeException("Failed to call minion", e));
        }
        return future;
    }
}
