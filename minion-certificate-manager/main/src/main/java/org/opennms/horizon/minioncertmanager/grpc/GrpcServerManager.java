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
package org.opennms.horizon.minioncertmanager.grpc;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerInterceptor;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequiredArgsConstructor
public class GrpcServerManager {

    private static final Logger LOG = LoggerFactory.getLogger(GrpcServerManager.class);

    private Server grpcServer;
    private final int port;
    private final ServerInterceptor interceptor;

    public synchronized void startServer(BindableService... services) {
        NettyServerBuilder serverBuilder = NettyServerBuilder.forAddress(new InetSocketAddress(port))
                .intercept(interceptor)
                .addService(ProtoReflectionService.newInstance());
        Arrays.stream(services).forEach(serverBuilder::addService);
        grpcServer = serverBuilder.build();
        try {
            grpcServer.start();
            LOG.info("gRPC server started at port {}", port);
        } catch (IOException e) {
            LOG.error("Couldn't start gRPC server", e);
        }
    }

    public synchronized void stopServer() throws InterruptedException {
        if (grpcServer != null && !grpcServer.isShutdown()) {
            grpcServer.shutdown();
            grpcServer.awaitTermination();
        }
    }
}
