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
