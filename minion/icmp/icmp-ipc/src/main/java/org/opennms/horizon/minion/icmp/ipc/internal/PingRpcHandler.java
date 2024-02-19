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
package org.opennms.horizon.minion.icmp.ipc.internal;

import com.google.protobuf.InvalidProtocolBufferException;
import java.net.InetAddress;
import java.util.concurrent.CompletableFuture;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.horizon.grpc.ping.contract.PingRequest;
import org.opennms.horizon.grpc.ping.contract.PingResponse;
import org.opennms.horizon.shared.icmp.PingerFactory;
import org.opennms.horizon.shared.ipc.rpc.api.minion.RpcHandler;

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
                    })
                    .thenApply(ttl ->
                            PingResponse.newBuilder().setRtt(ttl.doubleValue()).build());
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
