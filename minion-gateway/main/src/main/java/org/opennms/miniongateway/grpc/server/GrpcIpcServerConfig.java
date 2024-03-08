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

import java.util.Arrays;
import java.util.Properties;
import org.opennms.horizon.shared.grpc.common.GrpcIpcServer;
import org.opennms.horizon.shared.grpc.common.GrpcIpcServerBuilder;
import org.opennms.horizon.shared.grpc.common.GrpcIpcUtils;
import org.opennms.horizon.shared.grpc.common.LocationServerInterceptor;
import org.opennms.horizon.shared.grpc.common.TenantIDGrpcServerInterceptor;
import org.opennms.horizon.shared.grpc.interceptor.LoggingInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcIpcServerConfig {

    public static final String GRPC_MAX_INBOUND_SIZE = "max.message.size";
    public static final long DEFAULT_MAX_MESSAGE_SIZE = 100 * (1024 * 1024);
    public static final int DEFAULT_EXTERNAL_GRPC_PORT = 8990;
    public static final int DEFAULT_INTERNAL_GRPC_PORT = 8991;

    @Value("${" + GRPC_MAX_INBOUND_SIZE + ":" + DEFAULT_MAX_MESSAGE_SIZE + "}")
    private long maxMessageSize;

    @Value("${grpc.port:" + DEFAULT_EXTERNAL_GRPC_PORT + "}")
    private int externalGrpcPort;

    @Value("${grpc.internal.port:" + DEFAULT_INTERNAL_GRPC_PORT + "}")
    private int internalGrpcPort;

    // ========================================
    // BEAN REGISTRATION
    // ----------------------------------------

    @Bean
    public TenantIDGrpcServerInterceptor prepareTenantIDGrpcInterceptor() {
        return new TenantIDGrpcServerInterceptor();
    }

    @Bean
    public LocationServerInterceptor prepareLocationInterceptor() {
        return new LocationServerInterceptor();
    }

    /**
     * External GRPC service for handling
     *
     * @return
     */
    @Bean(name = "externalGrpcIpcServer", destroyMethod = "stopServer")
    public GrpcIpcServer prepareExternalGrpcIpcServer(
            @Autowired TenantIDGrpcServerInterceptor tenantIDGrpcServerInterceptor,
            @Autowired LocationServerInterceptor locationServerInterceptor) {
        Properties properties = new Properties();
        properties.setProperty(GrpcIpcUtils.GRPC_MAX_INBOUND_SIZE, Long.toString(maxMessageSize));

        return new GrpcIpcServerBuilder(
                properties,
                externalGrpcPort,
                "PT10S",
                Arrays.asList(new LoggingInterceptor(), tenantIDGrpcServerInterceptor, locationServerInterceptor));
    }

    @Bean(name = "internalGrpcIpcServer", destroyMethod = "stopServer")
    public GrpcIpcServer prepareInternalGrpcIpcServerTenantIdInterceptor() {
        Properties properties = new Properties();
        properties.setProperty(GrpcIpcUtils.GRPC_MAX_INBOUND_SIZE, Long.toString(maxMessageSize));

        return new GrpcIpcServerBuilder(properties, internalGrpcPort, "PT10S", Arrays.asList(new LoggingInterceptor()));
    }
}
