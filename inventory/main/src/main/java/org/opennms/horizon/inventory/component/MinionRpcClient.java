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
package org.opennms.horizon.inventory.component;

import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.opennms.cloud.grpc.minion_gateway.GatewayRpcRequestProto;
import org.opennms.cloud.grpc.minion_gateway.GatewayRpcResponseProto;
import org.opennms.cloud.grpc.minion_gateway.RpcRequestServiceGrpc;
import org.opennms.horizon.inventory.grpc.TenantLookup;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.springframework.beans.factory.annotation.Qualifier;

public class MinionRpcClient {

    private final ManagedChannel channel;
    private final TenantLookup tenantLookup;
    private final long deadline;

    public MinionRpcClient(
            @Qualifier("minion-gateway") ManagedChannel channel, TenantLookup tenantLookup, long deadline) {
        this.channel = channel;
        this.tenantLookup = tenantLookup;
        this.deadline = deadline;
    }

    private RpcRequestServiceGrpc.RpcRequestServiceStub rpcStub;

    protected void init() {
        rpcStub = RpcRequestServiceGrpc.newStub(channel);
    }

    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
        }
    }

    public CompletableFuture<GatewayRpcResponseProto> sendRpcRequest(String tenantId, GatewayRpcRequestProto request) {
        CompletableFuture<GatewayRpcResponseProto> future = new CompletableFuture<>();
        try {
            Metadata metadata = new Metadata();
            metadata.put(GrpcConstants.TENANT_ID_REQUEST_KEY, tenantId);

            rpcStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
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
                        public void onCompleted() {}
                    });
        } catch (Exception e) {
            future.completeExceptionally(new RuntimeException("Failed to call minion", e));
        }
        return future;
    }
}
