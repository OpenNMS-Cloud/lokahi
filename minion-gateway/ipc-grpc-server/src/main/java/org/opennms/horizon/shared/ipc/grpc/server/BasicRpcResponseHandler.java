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
package org.opennms.horizon.shared.ipc.grpc.server;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.opennms.cloud.grpc.minion.RpcResponseProto;
import org.opennms.horizon.shared.ipc.rpc.api.RpcResponseHandler;

public class BasicRpcResponseHandler implements RpcResponseHandler {

    private final long expirationTime;
    private final String rpcId;
    private final String moduleId;
    private final CompletableFuture<RpcResponseProto> future;

    public BasicRpcResponseHandler(
            long expirationTime, String rpcId, String moduleId, CompletableFuture<RpcResponseProto> future) {
        this.expirationTime = expirationTime;
        this.rpcId = rpcId;
        this.moduleId = moduleId;
        this.future = future;
    }

    @Override
    public void sendResponse(RpcResponseProto response) {
        if (response == null) {
            future.completeExceptionally(new TimeoutException());
            return;
        }

        future.complete(response);
    }

    @Override
    public boolean isProcessed() {
        return future.isDone();
    }

    @Override
    public String getRpcId() {
        return rpcId;
    }

    @Override
    public String getRpcModuleId() {
        return moduleId;
    }

    @Override
    public int compareTo(Delayed other) {
        long myDelay = getDelay(TimeUnit.MILLISECONDS);
        long otherDelay = other.getDelay(TimeUnit.MILLISECONDS);
        return Long.compare(myDelay, otherDelay);
    }

    @Override
    public long getDelay(TimeUnit unit) {
        long now = System.currentTimeMillis();
        return unit.convert(expirationTime - now, TimeUnit.MILLISECONDS);
    }
}
