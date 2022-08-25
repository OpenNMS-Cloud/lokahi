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

package org.opennms.horizon.minion.icmp.proxy.impl;

import java.net.InetAddress;

import org.opennms.horizon.shared.ipc.rpc.api.RpcClient;
import org.opennms.horizon.shared.ipc.rpc.api.RpcClientFactory;
import org.opennms.netmgt.icmp.proxy.LocationAwarePingClient;
import org.opennms.netmgt.icmp.proxy.PingRequestBuilder;
import org.opennms.netmgt.icmp.proxy.PingSweepRequestBuilder;
import org.opennms.horizon.minion.icmp.proxy.common.PingProxyRpcModule;
import org.opennms.horizon.minion.icmp.proxy.common.PingRequestDTO;
import org.opennms.horizon.minion.icmp.proxy.common.PingResponseDTO;
import org.opennms.horizon.minion.icmp.proxy.common.PingSweepRequestDTO;
import org.opennms.horizon.minion.icmp.proxy.common.PingSweepResponseDTO;
import org.opennms.horizon.minion.icmp.proxy.common.PingSweepRpcModule;

public class LocationAwarePingClientImpl implements LocationAwarePingClient {

    private final RpcClient<PingRequestDTO, PingResponseDTO> pingProxyDelegate;

    private final RpcClient<PingSweepRequestDTO, PingSweepResponseDTO> pingSweepDelegate;

    public LocationAwarePingClientImpl(RpcClientFactory rpcClientFactory, PingProxyRpcModule pingProxyRpcModule, PingSweepRpcModule pingSweepRpcModule) {
        pingProxyDelegate = rpcClientFactory.getClient(pingProxyRpcModule);
        pingSweepDelegate = rpcClientFactory.getClient(pingSweepRpcModule);
    }

    @Override
    public PingRequestBuilder ping(InetAddress inetAddress) {
        return new PingRequestBuilderImpl(pingProxyDelegate).withInetAddress(inetAddress);
    }

    @Override
    public PingSweepRequestBuilder sweep() {
        return new PingSweepRequestBuilderImpl(pingSweepDelegate);
    }

}
