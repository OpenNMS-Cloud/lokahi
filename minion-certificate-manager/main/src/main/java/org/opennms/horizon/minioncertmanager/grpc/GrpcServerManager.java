/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.horizon.minioncertmanager.grpc;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerInterceptor;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;

@RequiredArgsConstructor
public class GrpcServerManager {

    private static final Logger LOG = LoggerFactory.getLogger(GrpcServerManager.class);

    private Server grpcServer;
    private final int port;
    private final ServerInterceptor interceptor;

    public synchronized void startServer(BindableService ... services) {
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
        if(grpcServer != null && !grpcServer.isShutdown()) {
            grpcServer.shutdown();
            grpcServer.awaitTermination();
        }
    }
}
