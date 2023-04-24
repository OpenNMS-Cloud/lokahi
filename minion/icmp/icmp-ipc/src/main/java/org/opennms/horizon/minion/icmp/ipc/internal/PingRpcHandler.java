/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022-2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
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

package org.opennms.horizon.minion.icmp.ipc.internal;

import com.google.protobuf.InvalidProtocolBufferException;
import java.net.InetAddress;
import java.util.concurrent.CompletableFuture;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.horizon.grpc.ping.contract.PingRequest;
import org.opennms.horizon.grpc.ping.contract.PingResponse;
import org.opennms.horizon.shared.ipc.rpc.api.minion.RpcHandler;
import org.opennms.horizon.shared.icmp.PingerFactory;

public class PingRpcHandler implements RpcHandler<PingRequest, PingResponse> {

    public static final String RPC_MODULE_ID = "PING";

    private final PingerFactory pingerFactory;

    public PingRpcHandler(PingerFactory pingerFactory) {
        this.pingerFactory = pingerFactory;
    }

    @Override
    public CompletableFuture<PingResponse> execute(PingRequest request) {
        try {
            InetAddress address = InetAddress.getByName(request.getInetAddress());
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return pingerFactory.getInstance().ping(address);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).thenApply(ttl -> PingResponse.newBuilder()
                .setRtt(ttl.doubleValue())
                .build());
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public String getId() {
        return RPC_MODULE_ID;
    }

    @Override
    public PingRequest unmarshal(RpcRequestProto request) {
        try {
            return request.getPayload().unpack(PingRequest.class);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }
}
