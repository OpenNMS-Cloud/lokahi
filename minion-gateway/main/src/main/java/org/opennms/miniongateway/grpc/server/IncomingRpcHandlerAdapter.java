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
package org.opennms.miniongateway.grpc.server;

import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcResponseProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IncomingRpcHandlerAdapter implements BiConsumer<RpcRequestProto, StreamObserver<RpcResponseProto>> {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(IncomingRpcHandlerAdapter.class);

    private Logger LOG = DEFAULT_LOGGER;

    private Map<String, ServerHandler> handlers;

    public IncomingRpcHandlerAdapter(List<ServerHandler> handlers) {
        this.handlers = handlers.stream().collect(Collectors.toMap(ServerHandler::getId, Function.identity()));
    }

    @Override
    public void accept(RpcRequestProto request, StreamObserver<RpcResponseProto> responseStream) {
        if (handlers.containsKey(request.getModuleId())) {
            ServerHandler handler = handlers.get(request.getModuleId());
            handler.handle(request).whenComplete((response, error) -> {
                if (error != null) {
                    LOG.error("Exception on RPC: module-id={}", request.getModuleId(), error);
                    responseStream.onError(error);
                    return;
                }
                responseStream.onNext(response);
                responseStream.onCompleted();
            });
        }
    }
}
