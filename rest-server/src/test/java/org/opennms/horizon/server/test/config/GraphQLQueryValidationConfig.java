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
import org.opennms.horizon.server.service.AlertGraphQLService;
import org.opennms.horizon.server.service.EventGraphQLService;
import org.opennms.horizon.server.service.LocationGraphQLService;
import org.opennms.horizon.server.service.CertificateManagerGraphQLService;
import org.opennms.horizon.server.service.MinionGraphQLService;
import org.opennms.horizon.server.service.NodeGraphQLService;
import org.opennms.horizon.server.service.TagGraphQLService;
import org.opennms.horizon.server.service.NotificationGraphQLService;
import org.opennms.horizon.server.service.discovery.ActiveDiscoveryGraphQLService;
import org.opennms.horizon.server.service.discovery.AzureActiveDiscoveryGraphQLService;
import org.opennms.horizon.server.service.discovery.IcmpActiveDiscoveryGraphQLService;
import org.opennms.horizon.server.service.discovery.PassiveDiscoveryGraphQLService;
import org.opennms.horizon.server.service.flows.FlowGraphQLService;
import org.opennms.horizon.server.service.metrics.TSDBMetricsGraphQLService;
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
    public AlertGraphQLService grpcAlertService() {
        return Mockito.mock(AlertGraphQLService.class, new GraphQLAnswer());
    }

    @Bean
    @Primary
    public EventGraphQLService grpcEventService() {
        return Mockito.mock(EventGraphQLService.class, new GraphQLAnswer());
    }

    @Bean
    @Primary
    public LocationGraphQLService grpcLocationService() {
        return Mockito.mock(LocationGraphQLService.class, new GraphQLAnswer());
    }

    @Bean
    @Primary
    public CertificateManagerGraphQLService grpcMinionCertificateManager() {
        return Mockito.mock(CertificateManagerGraphQLService.class, new GraphQLAnswer());
    }

    @Bean
    @Primary
    public MinionGraphQLService grpcMinionService() {
        return Mockito.mock(MinionGraphQLService.class, new GraphQLAnswer());
    }

    @Bean
    @Primary
    public NodeGraphQLService grpcNodeService() {
        return Mockito.mock(NodeGraphQLService.class, new GraphQLAnswer());
    }

    @Bean
    @Primary
    public TagGraphQLService grpcTagService() {
        return Mockito.mock(TagGraphQLService.class, new GraphQLAnswer());
    }

    @Bean
    @Primary
    public NotificationGraphQLService notificationService() {
        return Mockito.mock(NotificationGraphQLService.class, new GraphQLAnswer());
    }

    @Bean
    @Primary
    public ActiveDiscoveryGraphQLService grpcActiveDiscoveryService() {
        return Mockito.mock(ActiveDiscoveryGraphQLService.class, new GraphQLAnswer());
    }

    @Bean
    @Primary
    public AzureActiveDiscoveryGraphQLService grpcAzureActiveDiscoveryService() {
        return Mockito.mock(AzureActiveDiscoveryGraphQLService.class, new GraphQLAnswer());
    }

    @Bean
    @Primary
    public IcmpActiveDiscoveryGraphQLService grpcIcmpActiveDiscoveryService() {
        return Mockito.mock(IcmpActiveDiscoveryGraphQLService.class, new GraphQLAnswer());
    }

    @Bean
    @Primary
    public PassiveDiscoveryGraphQLService grpcPassiveDiscoveryService() {
        return Mockito.mock(PassiveDiscoveryGraphQLService.class, new GraphQLAnswer());
    }

    @Bean
    @Primary
    public FlowGraphQLService grpcFlowService() {
        return Mockito.mock(FlowGraphQLService.class, new GraphQLAnswer());
    }

    @Bean
    @Primary
    public TSDBMetricsGraphQLService TSDBMetricsService() {
        return Mockito.mock(TSDBMetricsGraphQLService.class, new GraphQLAnswer());
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
