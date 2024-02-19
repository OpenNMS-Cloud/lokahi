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
package org.opennms.miniongateway.rpcrequest.service;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import javax.annotation.PostConstruct;
import lombok.Setter;
import org.opennms.cloud.grpc.minion_gateway.GatewayRpcRequestProto;
import org.opennms.cloud.grpc.minion_gateway.GatewayRpcResponseProto;
import org.opennms.cloud.grpc.minion_gateway.RpcRequestServiceGrpc;
import org.opennms.horizon.shared.grpc.common.GrpcIpcServer;
import org.opennms.miniongateway.rpcrequest.RpcRequestRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class RpcRequestGrpcService extends RpcRequestServiceGrpc.RpcRequestServiceImplBase {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(RpcRequestGrpcService.class);

    private Logger log = DEFAULT_LOGGER;

    @Autowired
    @Setter
    private RpcRequestRouter rpcRequestRouter;

    @Autowired
    @Qualifier("internalGrpcIpcServer")
    @Setter
    private GrpcIpcServer grpcIpcServer;

    // ========================================
    // Lifecycle
    // ----------------------------------------

    @PostConstruct
    public void start() throws IOException {
        // TODO: use explicit tenant-id handling
        grpcIpcServer.startServer(this);
        log.info("Initiated RPC-Request GRPC Service");
    }

    // ========================================
    // Service API
    // ----------------------------------------

    @Override
    public void request(GatewayRpcRequestProto request, StreamObserver<GatewayRpcResponseProto> responseObserver) {
        CompletableFuture<GatewayRpcResponseProto> future = rpcRequestRouter.routeRequest(request);

        future.whenComplete((response, exception) -> handleCompletedRequest(response, exception, responseObserver));
    }

    // ========================================
    // Internals
    // ----------------------------------------

    private void handleCompletedRequest(
            GatewayRpcResponseProto response,
            Throwable exception,
            StreamObserver<GatewayRpcResponseProto> responseObserver) {
        if (exception != null) {
            Status status = Status.UNAVAILABLE.withDescription(exception.getMessage());
            responseObserver.onError(status.asRuntimeException());
        } else {
            responseObserver.onNext(response);
        }

        responseObserver.onCompleted();
    }
}
