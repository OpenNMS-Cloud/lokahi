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
package org.opennms.horizon.minion.grpc.rpc;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcResponseProto;

public class ThreadedHandler implements RpcRequestHandler {

    private final RpcRequestHandler delegate;
    private final AtomicLong counter = new AtomicLong();
    private final ThreadFactory requestHandlerThreadFactory =
            (runnable) -> new Thread(runnable, "rpc-request-handler-" + counter.incrementAndGet());
    private final ExecutorService requestHandlerExecutor = Executors.newCachedThreadPool(requestHandlerThreadFactory);

    public ThreadedHandler(RpcRequestHandler delegate) {
        this.delegate = delegate;
    }

    @Override
    public CompletableFuture<RpcResponseProto> handle(RpcRequestProto request) {
        CompletableFuture<RpcResponseProto> future = new CompletableFuture<>();
        requestHandlerExecutor.submit(() -> delegate.handle(request).whenComplete((r, e) -> {
            if (e != null) {
                future.completeExceptionally(e);
                return;
            }
            future.complete(r);
        }));
        return future;
    }

    public void shutdown() {
        delegate.shutdown();
        requestHandlerExecutor.shutdown();
    }
}
