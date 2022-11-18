/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2021 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2021 The OpenNMS Group, Inc.
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

package org.opennms.miniongatewaygrpcproxy.grpc;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

@Component
public class GrpcIpcServerBuilder {

    public static final int DEFAULT_MAX_MESSAGE_SIZE = 1_0485_760;

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(GrpcIpcServerBuilder.class);

    private Logger LOG = DEFAULT_LOGGER;

    @Value("${grpc.listen.port:8990}")
    private int port;

    @Value("${grpc.inbound.max-message-size:" + DEFAULT_MAX_MESSAGE_SIZE + "}")
    private int maxMessageSize;

    @Value("${grpc.inbound.tls-enabled:false}")
    private boolean tlsEnabled;

    @Autowired
    private List<BindableService> bindableServices;

    @Autowired
    private GrpcHeaderCaptureInterceptor grpcHeaderCaptureInterceptor;

    private Server server;

//========================================
// Lifecycle
//----------------------------------------

    @PostConstruct
    public void start() {
        try {
            initializeServer();
        } catch (IOException ioExc) {
            LOG.error("Failed to start GRPC server on port " + port, ioExc);
            throw new RuntimeException("Failed to start GRPC server on port " + port, ioExc);
        }
    }

//========================================
// Internals
//----------------------------------------

    private void initializeServer() throws IOException {
        NettyServerBuilder serverBuilder =
            NettyServerBuilder.forAddress(new InetSocketAddress(this.port))
                .intercept(grpcHeaderCaptureInterceptor)
                .maxInboundMessageSize(maxMessageSize)
            ;

        if (tlsEnabled) {
            throw new RuntimeException("TLS currently NOT supported");
            // SslContextBuilder sslContextBuilder = GrpcIpcUtils.getSslContextBuilder(properties);
            // if (sslContextBuilder != null) {
            //     try {
            //         serverBuilder.sslContext(sslContextBuilder.build());
            //         LOG.info("TLS enabled for Grpc IPC Server");
            //     } catch (SSLException e) {
            //         LOG.error("Couldn't initialize ssl context from {}", properties, e);
            //     }
            // }
        }

        int count = 0;
        for (BindableService oneService : bindableServices) {
            serverBuilder.addService(oneService);
            count++;
        }

        LOG.info("Starting GRPC Service: service-definition-count={}", count);

        server = serverBuilder.build();
        server.start();
    }
}
