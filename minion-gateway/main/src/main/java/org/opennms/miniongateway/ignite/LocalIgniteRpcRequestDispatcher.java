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
package org.opennms.miniongateway.ignite;

import java.util.concurrent.CompletableFuture;
import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcRequestProto.Builder;
import org.opennms.cloud.grpc.minion_gateway.GatewayRpcRequestProto;
import org.opennms.cloud.grpc.minion_gateway.GatewayRpcResponseProto;
import org.opennms.cloud.grpc.minion_gateway.MinionIdentity;
import org.opennms.horizon.shared.ipc.grpc.server.manager.RpcRequestDispatcher;
import org.opennms.miniongateway.detector.server.IgniteRpcRequestDispatcher;

public class LocalIgniteRpcRequestDispatcher implements IgniteRpcRequestDispatcher {

    private RpcRequestDispatcher rpcRequestDispatcher;

    public LocalIgniteRpcRequestDispatcher(RpcRequestDispatcher rpcRequestDispatcher) {
        this.rpcRequestDispatcher = rpcRequestDispatcher;
    }

    @Override
    public CompletableFuture<GatewayRpcResponseProto> execute(GatewayRpcRequestProto request) {
        MinionIdentity identity = request.getIdentity();

        Builder rpcRequest = RpcRequestProto.newBuilder()
                .setRpcId(request.getRpcId())
                .setModuleId(request.getModuleId())
                .setExpirationTime(request.getExpirationTime())
                .setPayload(request.getPayload());

        if (identity.getSystemId().isBlank()) {
            return rpcRequestDispatcher.dispatch(identity.getTenantId(), identity.getLocationId(), rpcRequest.build());
        }

        rpcRequest.setIdentity(Identity.newBuilder()
                .setSystemId(identity.getSystemId())); // Why replace the existing identity with a new one?
        return rpcRequestDispatcher.dispatch(
                identity.getTenantId(), identity.getLocationId(), identity.getSystemId(), rpcRequest.build());
    }
}
