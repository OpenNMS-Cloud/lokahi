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

import io.grpc.Context;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcRequestServiceGrpc;
import org.opennms.cloud.grpc.minion.RpcRequestServiceGrpc.RpcRequestServiceStub;
import org.opennms.cloud.grpc.minion.RpcResponseProto;
import org.opennms.horizon.inventory.Constants;
import org.opennms.horizon.inventory.grpc.TenantIdClientInterceptor;
import org.opennms.horizon.inventory.grpc.TenantLookup;
import org.springframework.beans.factory.annotation.Qualifier;

import io.grpc.ManagedChannel;

public class MinionRpcClient {

    private final ManagedChannel channel;
    private final TenantLookup tenantLookup;

    public MinionRpcClient(@Qualifier("minion-gateway") ManagedChannel channel, TenantLookup tenantLookup) {
        this.channel = channel;
        this.tenantLookup = tenantLookup;
    }

    private RpcRequestServiceStub rpcStub;

    protected void init() {
        rpcStub = RpcRequestServiceGrpc.newStub(channel)
            .withInterceptors(new TenantIdClientInterceptor(tenantLookup));
    }

    public void shutdown() {
        if(channel != null && !channel.isShutdown()) {
            channel.shutdown();
        }
    }

    public CompletableFuture<RpcResponseProto> sendRpcRequest(String tenantId, RpcRequestProto request) {
        CompletableFuture<RpcResponseProto> future = new CompletableFuture<>();
        Context withCredential = Context.current().withValue(Constants.TENANT_ID_CONTEXT_KEY, tenantId);
        try {
            withCredential.run(() -> {
                rpcStub.request(request, new StreamObserver<>() {
                    @Override
                    public void onNext(RpcResponseProto value) {
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
            });
        } catch (Exception e) {
            throw new RuntimeException("Failed to call minion", e);
        }
        return future.orTimeout(60, TimeUnit.SECONDS);
    }
}
