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
package org.opennms.horizon.shared.ipc.grpc.server.manager.adapter;

import io.grpc.stub.StreamObserver;
import java.util.function.Consumer;
import org.opennms.cloud.grpc.minion.RpcResponseProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stream Observer that handles inbound RPC calls initiated by the Minion.
 */
public class InboundRpcAdapter implements StreamObserver<RpcResponseProto> {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(InboundRpcAdapter.class);

    private Logger log = DEFAULT_LOGGER;

    private final Consumer<RpcResponseProto> onMessage;
    private final Consumer<Throwable> onError;
    private final Runnable onCompleted;

    public InboundRpcAdapter(Consumer<RpcResponseProto> onMessage, Consumer<Throwable> onError, Runnable onCompleted) {
        this.onMessage = onMessage;
        this.onError = onError;
        this.onCompleted = onCompleted;
    }

    @Override
    public void onNext(RpcResponseProto rpcResponseProto) {
        onMessage.accept(rpcResponseProto);
    }

    @Override
    public void onError(Throwable thrown) {
        onError.accept(thrown);
    }

    @Override
    public void onCompleted() {
        onCompleted.run();
    }
}
