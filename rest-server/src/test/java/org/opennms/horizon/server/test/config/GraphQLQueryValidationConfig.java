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
import org.opennms.horizon.server.service.GraphQLAlertService;
import org.opennms.horizon.server.service.GraphQLEventService;
import org.opennms.horizon.server.service.GraphQLLocationService;
import org.opennms.horizon.server.service.GraphQLMinionCertificateManager;
import org.opennms.horizon.server.service.GraphQLMinionService;
import org.opennms.horizon.server.service.GraphQLNodeService;
import org.opennms.horizon.server.service.GraphQLNotificationService;
import org.opennms.horizon.server.service.GraphQLTagService;
import org.opennms.horizon.server.service.discovery.GraphQLActiveDiscoveryService;
import org.opennms.horizon.server.service.discovery.GraphQLAzureActiveDiscoveryService;
import org.opennms.horizon.server.service.discovery.GraphQLIcmpActiveDiscoveryService;
import org.opennms.horizon.server.service.discovery.GraphQLPassiveDiscoveryService;
import org.opennms.horizon.server.service.flows.GraphQLFlowService;
import org.opennms.horizon.server.service.metrics.GraphQLTSDBMetricsService;
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
    public GraphQLAlertService graphQLAlertService() {
        return Mockito.mock(GraphQLAlertService.class, new GraphQLAnswer());
    }

    @Bean
    @Primary
    public GraphQLEventService graphQLEventService() {
        return Mockito.mock(GraphQLEventService.class, new GraphQLAnswer());
    }

    @Bean
    @Primary
    public GraphQLLocationService graphQLLocationService() {
        return Mockito.mock(GraphQLLocationService.class, new GraphQLAnswer());
    }

    @Bean
    @Primary
    public GraphQLMinionCertificateManager graphQLMinionCertificateManager() {
        return Mockito.mock(GraphQLMinionCertificateManager.class, new GraphQLAnswer());
    }

    @Bean
    @Primary
    public GraphQLMinionService graphQLMinionService() {
        return Mockito.mock(GraphQLMinionService.class, new GraphQLAnswer());
    }

    @Bean
    @Primary
    public GraphQLNodeService graphQLNodeService() {
        return Mockito.mock(GraphQLNodeService.class, new GraphQLAnswer());
    }

    @Bean
    @Primary
    public GraphQLTagService graphQLTagService() {
        return Mockito.mock(GraphQLTagService.class, new GraphQLAnswer());
    }

    @Bean
    @Primary
    public GraphQLNotificationService graphQLNotificationService() {
        return Mockito.mock(GraphQLNotificationService.class, new GraphQLAnswer());
    }

    @Bean
    @Primary
    public GraphQLActiveDiscoveryService graphQLActiveDiscoveryService() {
        return Mockito.mock(GraphQLActiveDiscoveryService.class, new GraphQLAnswer());
    }

    @Bean
    @Primary
    public GraphQLAzureActiveDiscoveryService graphQLAzureActiveDiscoveryService() {
        return Mockito.mock(GraphQLAzureActiveDiscoveryService.class, new GraphQLAnswer());
    }

    @Bean
    @Primary
    public GraphQLIcmpActiveDiscoveryService graphQLIcmpActiveDiscoveryService() {
        return Mockito.mock(GraphQLIcmpActiveDiscoveryService.class, new GraphQLAnswer());
    }

    @Bean
    @Primary
    public GraphQLPassiveDiscoveryService graphQLPassiveDiscoveryService() {
        return Mockito.mock(GraphQLPassiveDiscoveryService.class, new GraphQLAnswer());
    }

    @Bean
    @Primary
    public GraphQLFlowService graphQLFlowService() {
        return Mockito.mock(GraphQLFlowService.class, new GraphQLAnswer());
    }

    @Bean
    @Primary
    public GraphQLTSDBMetricsService graphQLTSDBMetricsService() {
        return Mockito.mock(GraphQLTSDBMetricsService.class, new GraphQLAnswer());
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
