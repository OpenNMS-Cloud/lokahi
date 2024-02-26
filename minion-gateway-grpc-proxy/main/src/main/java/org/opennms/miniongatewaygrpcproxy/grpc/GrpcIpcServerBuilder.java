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
package org.opennms.miniongatewaygrpcproxy.grpc;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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

    // ========================================
    // Lifecycle
    // ----------------------------------------

    @PostConstruct
    public void start() {
        try {
            initializeServer();
        } catch (IOException ioExc) {
            LOG.error("Failed to start GRPC server on port " + port, ioExc);
            throw new RuntimeException("Failed to start GRPC server on port " + port, ioExc);
        }
    }

    // ========================================
    // Internals
    // ----------------------------------------

    private void initializeServer() throws IOException {
        NettyServerBuilder serverBuilder = NettyServerBuilder.forAddress(new InetSocketAddress(this.port))
                .intercept(grpcHeaderCaptureInterceptor)
                .maxInboundMessageSize(maxMessageSize);

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
