/*
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
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
