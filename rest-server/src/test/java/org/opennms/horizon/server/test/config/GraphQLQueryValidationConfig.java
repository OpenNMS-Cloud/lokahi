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
package org.opennms.horizon.server.test.config;

import lombok.extern.slf4j.Slf4j;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opennms.horizon.server.service.GrpcAlertService;
import org.opennms.horizon.server.service.GrpcEventService;
import org.opennms.horizon.server.service.GrpcLocationService;
import org.opennms.horizon.server.service.GrpcMinionCertificateManager;
import org.opennms.horizon.server.service.GrpcMinionService;
import org.opennms.horizon.server.service.GrpcNodeService;
import org.opennms.horizon.server.service.GrpcTagService;
import org.opennms.horizon.server.service.NotificationService;
import org.opennms.horizon.server.service.discovery.GrpcActiveDiscoveryService;
import org.opennms.horizon.server.service.discovery.GrpcAzureActiveDiscoveryService;
import org.opennms.horizon.server.service.discovery.GrpcIcmpActiveDiscoveryService;
import org.opennms.horizon.server.service.discovery.GrpcPassiveDiscoveryService;
import org.opennms.horizon.server.service.flows.GrpcFlowService;
import org.opennms.horizon.server.service.metrics.TSDBMetricsService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@TestConfiguration
@Slf4j
public class GraphQLQueryValidationConfig {

    @Bean
    @Primary
    public GrpcAlertService grpcAlertService() {
        return Mockito.mock(GrpcAlertService.class, new GraphQLAnswer());
    }

    @Bean
    @Primary
    public GrpcEventService grpcEventService() {
        return Mockito.mock(GrpcEventService.class, new GraphQLAnswer());
    }

    @Bean
    @Primary
    public GrpcLocationService grpcLocationService() {
        return Mockito.mock(GrpcLocationService.class, new GraphQLAnswer());
    }

    @Bean
    @Primary
    public GrpcMinionCertificateManager grpcMinionCertificateManager() {
        return Mockito.mock(GrpcMinionCertificateManager.class, new GraphQLAnswer());
    }

    @Bean
    @Primary
    public GrpcMinionService grpcMinionService() {
        return Mockito.mock(GrpcMinionService.class, new GraphQLAnswer());
    }

    @Bean
    @Primary
    public GrpcNodeService grpcNodeService() {
        return Mockito.mock(GrpcNodeService.class, new GraphQLAnswer());
    }

    @Bean
    @Primary
    public GrpcTagService grpcTagService() {
        return Mockito.mock(GrpcTagService.class, new GraphQLAnswer());
    }

    @Bean
    @Primary
    public NotificationService notificationService() {
        return Mockito.mock(NotificationService.class, new GraphQLAnswer());
    }

    @Bean
    @Primary
    public GrpcActiveDiscoveryService grpcActiveDiscoveryService() {
        return Mockito.mock(GrpcActiveDiscoveryService.class, new GraphQLAnswer());
    }

    @Bean
    @Primary
    public GrpcAzureActiveDiscoveryService grpcAzureActiveDiscoveryService() {
        return Mockito.mock(GrpcAzureActiveDiscoveryService.class, new GraphQLAnswer());
    }

    @Bean
    @Primary
    public GrpcIcmpActiveDiscoveryService grpcIcmpActiveDiscoveryService() {
        return Mockito.mock(GrpcIcmpActiveDiscoveryService.class, new GraphQLAnswer());
    }

    @Bean
    @Primary
    public GrpcPassiveDiscoveryService grpcPassiveDiscoveryService() {
        return Mockito.mock(GrpcPassiveDiscoveryService.class, new GraphQLAnswer());
    }

    @Bean
    @Primary
    public GrpcFlowService grpcFlowService() {
        return Mockito.mock(GrpcFlowService.class, new GraphQLAnswer());
    }

    @Bean
    @Primary
    public TSDBMetricsService TSDBMetricsService() {
        return Mockito.mock(TSDBMetricsService.class, new GraphQLAnswer());
    }

    static class GraphQLAnswer implements Answer<Object> {

        @Override
        @SuppressWarnings("ReactiveStreamsUnusedPublisher")
        public Object answer(InvocationOnMock invocation) throws Throwable {
            Class<?> returnType = invocation.getMethod().getReturnType();
            if (returnType.isAssignableFrom(Mono.class)) {
                return Mono.empty();
            } else if (returnType.isAssignableFrom(Flux.class)) {
                return Flux.empty();
            }
            throw new RuntimeException("Unhandled class: " + returnType);
        }
    }
}
