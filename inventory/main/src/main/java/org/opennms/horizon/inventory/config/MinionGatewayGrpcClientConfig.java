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
package org.opennms.horizon.inventory.config;

import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import org.opennms.horizon.inventory.component.MinionRpcClient;
import org.opennms.horizon.inventory.exception.InventoryRuntimeException;
import org.opennms.horizon.inventory.grpc.TenantLookup;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinionGatewayGrpcClientConfig {

    public static final String MINION_GATEWAY_GRPC_CHANNEL = "minion-gateway";

    @Value("${grpc.client.minion-gateway.host:localhost}")
    private String host;

    @Value("${grpc.client.minion-gateway.port:8990}")
    private int port;

    @Value("${grpc.client.minion-gateway.tlsEnabled:false}")
    private boolean tlsEnabled;

    @Value("${grpc.client.minion-gateway.maxMessageSize:10485760}")
    private int maxMessageSize;

    @Value("${grpc.server.deadline:60000}")
    private long deadline;

    @Bean(name = MINION_GATEWAY_GRPC_CHANNEL)
    public ManagedChannel createGrpcChannel() {
        NettyChannelBuilder channelBuilder = NettyChannelBuilder.forAddress(host, port)
                .keepAliveWithoutCalls(true)
                .maxInboundMessageSize(maxMessageSize);

        ManagedChannel channel;

        if (tlsEnabled) {
            throw new InventoryRuntimeException("TLS NOT YET IMPLEMENTED");
        } else {
            channel = channelBuilder.usePlaintext().build();
        }
        return channel;
    }

    @Bean(initMethod = "init", destroyMethod = "shutdown")
    public MinionRpcClient createMinionRpcClient(
            @Qualifier(MINION_GATEWAY_GRPC_CHANNEL) ManagedChannel channel, TenantLookup tenantLookup) {
        return new MinionRpcClient(channel, tenantLookup, deadline);
    }
}
