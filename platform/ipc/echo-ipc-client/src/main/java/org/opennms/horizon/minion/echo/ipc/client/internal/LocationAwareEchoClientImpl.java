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

package org.opennms.horizon.minion.echo.ipc.client.internal;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.opennms.horizon.grpc.echo.contract.EchoRequest;
import org.opennms.horizon.grpc.echo.contract.EchoResponse;
import org.opennms.horizon.minion.echo.ipc.client.LocationAwareEchoClient;
import org.opennms.horizon.shared.ipc.rpc.api.RequestBuilder;
import org.opennms.horizon.shared.ipc.rpc.api.RpcClient;
import org.opennms.horizon.shared.ipc.rpc.api.RpcClientFactory;

public class LocationAwareEchoClientImpl implements LocationAwareEchoClient {

    private final RpcClient<EchoResponse> client;

    public LocationAwareEchoClientImpl(RpcClientFactory rpcClientFactory) {
        this.client = rpcClientFactory.getClient(response -> response.getPayload().unpack(EchoResponse.class));
    }

    @Override
    public CompletableFuture<EchoResponse> execute(String systemId, String location, Long timeToLive, EchoRequest payload) {
        RequestBuilder request = client.builder("Echo")
            .withLocation(location)
            .withSystemId(systemId)
            .withPayload(payload);

        Optional.ofNullable(timeToLive).ifPresent(request::withExpirationTime);
        return client.execute(request.build());
    }

}
