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
package org.opennms.horizon.grpc;

import io.grpc.stub.StreamObserver;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcResponseProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestCloudServiceRpcRequestHandler implements StreamObserver<RpcRequestProto> {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(TestCloudServiceRpcRequestHandler.class);

    private Logger LOG = DEFAULT_LOGGER;

    private final Object lock = new Object();

    private final Consumer<RpcResponseProto> onResponse;

    private List<RpcRequestProto> receivedRequests = new LinkedList<>();

    // ========================================
    // Constructor
    // ----------------------------------------

    public TestCloudServiceRpcRequestHandler(Consumer<RpcResponseProto> onResponse) {
        this.onResponse = onResponse;
    }

    // ========================================
    // StreamObserver Interface
    // ----------------------------------------

    @Override
    public void onNext(RpcRequestProto value) {
        LOG.info(
                "Have inbound RpcRequest: rpc-id={}; system-id={}; module-id={}; expiration-time={}; payload={}",
                value.getRpcId(),
                value.getIdentity().getSystemId(),
                value.getModuleId(),
                value.getExpirationTime(),
                value.getPayload());

        synchronized (lock) {
            receivedRequests.add(value);
        }

        if (onResponse != null) {
            RpcResponseProto.Builder rpcResponseProtoBuilder = RpcResponseProto.newBuilder();

            rpcResponseProtoBuilder.setRpcId(value.getRpcId());
            rpcResponseProtoBuilder.setIdentity(value.getIdentity());
            rpcResponseProtoBuilder.setModuleId(value.getModuleId());
            rpcResponseProtoBuilder.setPayload(value.getPayload());

            RpcResponseProto rpcResponseProto = rpcResponseProtoBuilder.build();

            LOG.info("Sending response for rpc request: id={}", rpcResponseProto.getRpcId());
            onResponse.accept(rpcResponseProto);
        }
    }

    @Override
    public void onError(Throwable t) {
        LOG.error("RPC error", t);
    }

    @Override
    public void onCompleted() {}

    // ========================================
    // Test Data Access
    // ----------------------------------------

    public RpcRequestProto[] getReceivedRequestsSnapshot() {
        synchronized (lock) {
            return receivedRequests.toArray(new RpcRequestProto[0]);
        }
    }
}
