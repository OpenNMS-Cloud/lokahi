/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2016 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2016 The OpenNMS Group, Inc.
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

package org.opennms.horizon.minion.ipc.echo;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.protobuf.InvalidProtocolBufferException;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.horizon.grpc.echo.contract.EchoRequest;
import org.opennms.horizon.grpc.echo.contract.EchoResponse;
import org.opennms.horizon.shared.ipc.rpc.api.minion.RpcHandler;

public class EchoRpcHandler implements RpcHandler<EchoRequest, EchoResponse> {

    public static final String RPC_MODULE_ID = "Echo";

    private static final Supplier<Timer> TIMER_SUPPLIER = Suppliers.memoize(() -> new Timer("EchoRpcModule"));

    @Override
    public CompletableFuture<EchoResponse> execute(final EchoRequest request) {
        final CompletableFuture<EchoResponse> future = new CompletableFuture<>();
        if (request.hasDelay()) {
            TIMER_SUPPLIER.get().schedule(new TimerTask() {
                @Override
                public void run() {
                    processRequest(request, future);
                }
            }, request.getDelay());
        } else {
            processRequest(request, future);
        }
        return future;
    }

    public void processRequest(EchoRequest request, CompletableFuture<EchoResponse> future) {
        if (request.getThrow()) {
            future.completeExceptionally(new MyEchoException(request.getMessage()));
        } else {
            EchoResponse response = EchoResponse.newBuilder()
                .setTime(request.getTime())
                .setMessage(request.getMessage())
                .setBody(request.getBody())
                .build();
            future.complete(response);
        }
    }

    @Override
    public String getId() {
        return RPC_MODULE_ID;
    }

    @Override
    public EchoRequest unmarshal(RpcRequestProto request) {
        try {
            return request.getPayload().unpack(EchoRequest.class);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }

}
