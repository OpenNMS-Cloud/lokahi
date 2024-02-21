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
package org.opennms.horizon.shared.grpc.common;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLException;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrpcIpcServerBuilder implements GrpcIpcServer {

    private static final Logger LOG = LoggerFactory.getLogger(GrpcIpcServerBuilder.class);

    private Properties properties;
    private final List<ServerInterceptor> interceptors;
    private NettyServerBuilder serverBuilder;
    private Server server;
    private final int port;
    private final Duration delay;
    private boolean serverStartScheduled = false;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private Set<BindableService> services = new HashSet<>();

    public GrpcIpcServerBuilder(ConfigurationAdmin configAdmin, int port, String delay) {
        this(
                GrpcIpcUtils.getPropertiesFromConfig(configAdmin, GrpcIpcUtils.GRPC_SERVER_PID),
                port,
                delay,
                Collections.emptyList());
    }

    public GrpcIpcServerBuilder(Properties properties, int port, String delay) {
        this(properties, port, delay, Collections.emptyList());
    }

    public GrpcIpcServerBuilder(Properties properties, int port, String delay, List<ServerInterceptor> interceptors) {
        this.port = port;
        this.delay = Duration.parse(delay);
        this.properties = properties;
        this.interceptors = interceptors;
    }

    @Override
    public synchronized void startServer(BindableService bindableService) throws IOException {
        initializeServerFromConfig();

        if (!services.contains(bindableService)) {
            serverBuilder.addService(bindableService);
            services.add(bindableService);
        }
        if (!serverStartScheduled) {
            startServerWithDelay(delay.toMillis());
            serverStartScheduled = true;
        }
    }

    @Override
    public void startServerWithInterceptors(BindableService bindableService, ServerInterceptor... interceptors)
            throws IOException {
        initializeServerFromConfig();

        if (!services.contains(bindableService)) {
            serverBuilder.addService(bindableService);

            ServerServiceDefinition serverServiceDefinition =
                    ServerInterceptors.intercept(bindableService, interceptors);
            serverBuilder.addService(serverServiceDefinition);
            services.add(bindableService);
        }

        if (!serverStartScheduled) {
            startServerWithDelay(delay.toMillis());
            serverStartScheduled = true;
        }
    }

    @Override
    public synchronized void stopServer() {
        if (server != null && !server.isShutdown()) {
            server.shutdownNow();
        }
        services.clear();
        serverStartScheduled = false;
    }

    private void initializeServerFromConfig() {
        if (serverBuilder == null) {
            int maxInboundMessageSize = PropertiesUtils.getProperty(
                    properties, GrpcIpcUtils.GRPC_MAX_INBOUND_SIZE, GrpcIpcUtils.DEFAULT_MESSAGE_SIZE);
            boolean tlsEnabled = PropertiesUtils.getProperty(properties, GrpcIpcUtils.TLS_ENABLED, false);

            serverBuilder = NettyServerBuilder.forAddress(new InetSocketAddress(this.port))
                    .maxInboundMessageSize(maxInboundMessageSize);

            // Add the interceptors
            interceptors.forEach(serverBuilder::intercept);

            if (tlsEnabled) {
                SslContextBuilder sslContextBuilder = GrpcIpcUtils.getSslContextBuilder(properties);
                if (sslContextBuilder != null) {
                    try {
                        serverBuilder.sslContext(sslContextBuilder.build());
                        LOG.info("TLS enabled for Grpc IPC Server");
                    } catch (SSLException e) {
                        LOG.error("Couldn't initialize ssl context from {}", properties, e);
                    }
                }
            }
        }
    }

    @Override
    public Properties getProperties() {
        return properties;
    }

    private void startServerWithDelay(long delay) {
        executor.schedule(this::startServerNow, delay, TimeUnit.MILLISECONDS);
    }

    private void startServerNow() {
        server = serverBuilder.build();
        try {
            server.start();
            LOG.info("OpenNMS IPC gRPC server started with {} services", services.size());
        } catch (IOException e) {
            LOG.error("Exception while starting IPC Grpc Server", e);
        }
    }
}
