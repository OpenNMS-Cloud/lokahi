/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2016-2022 The OpenNMS Group, Inc.
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

package org.opennms.horizon.icmp.ipc.client.internal;

import java.net.InetAddress;
import java.util.concurrent.CompletableFuture;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.horizon.grpc.ping.contract.PingRequest;
import org.opennms.horizon.grpc.ping.contract.PingResponse;
import org.opennms.horizon.shared.ipc.rpc.api.RpcClient;
import org.opennms.horizon.shared.ipc.rpc.api.RpcClientFactory;
import org.opennms.horizon.shared.icmp.proxy.LocationAwarePingClient;
import org.opennms.horizon.shared.icmp.proxy.PingRequestBuilder;
import org.opennms.horizon.shared.icmp.proxy.PingSweepRequestBuilder;

public class LocationAwarePingClientImpl implements LocationAwarePingClient {

    private final RpcClient<PingResponse> client;

    public LocationAwarePingClientImpl(RpcClientFactory rpcClientFactory) {
        this.client = rpcClientFactory.getClient(response -> response.getPayload().unpack(PingResponse.class));
    }

    @Override
    public PingRequestBuilder ping(InetAddress inetAddress) {
        return new PingRequestBuilderImpl(this).withInetAddress(inetAddress);
    }

    @Override
    public PingSweepRequestBuilder sweep() {
        throw new UnsupportedOperationException("This RPC operation is currently not supported");
    }

    CompletableFuture<PingResponse> execute(String systemId, String location, PingRequest payload) {
        RpcRequestProto request = client.builder("PING")
            .withLocation(location)
            .withSystemId(systemId)
            .withPayload(payload)
            .build();
        return client.execute(request);
    }
}
