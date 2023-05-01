/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022-2023 The OpenNMS Group, Inc.
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
 *******************************************************************************/

package org.opennms.horizon.server.config;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.server.mapper.alert.MonitorPolicyMapper;
import org.opennms.horizon.server.service.flows.FlowClient;
import org.opennms.horizon.server.service.grpc.AlertsClient;
import org.opennms.horizon.server.service.grpc.EventsClient;
import org.opennms.horizon.server.service.grpc.InventoryClient;
import org.opennms.horizon.server.service.grpc.MinionCertificateManagerClient;
import org.opennms.horizon.server.service.grpc.NotificationClient;
import org.opennms.horizon.server.utils.JWTValidator;
import org.opennms.horizon.server.utils.ServerHeaderUtil;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ConfigurationUtil {
    @Value("${grpc.url.inventory}")
    private String inventoryGrpcAddress;

    @Value("${grpc.url.events}")
    private String eventsGrpcAddress;

    @Value("${grpc.server.deadline:60000}")
    private long deadline;

    @Value("${grpc.url.notification}")
    private String notificationGrpcAddress;

    @Value("${grpc.url.alerts}")
    private String alertsGrpcAddress;

    private final MonitorPolicyMapper policyMapper;

    @Value("${grpc.url.flows}")
    private String flowQuerierGrpcAddress;

    @Value("${grpc.url.minion-certificate-manager}")
    private String minionCertificateManagerGrpcAddress;

    @Bean
    public ServerHeaderUtil createHeaderUtil(JWTValidator validator) {
        return new ServerHeaderUtil(validator);
    }

    @Bean(name = "inventory")
    public ManagedChannel createInventoryChannel() {
        return ManagedChannelBuilder.forTarget(inventoryGrpcAddress)
            .keepAliveWithoutCalls(true)
            .usePlaintext().build();
    }

    @Bean(name = "events")
    public ManagedChannel createEventsChannel() {
        return ManagedChannelBuilder.forTarget(eventsGrpcAddress)
            .keepAliveWithoutCalls(true)
            .usePlaintext().build();
    }

    @Bean(name = "notification")
    public ManagedChannel createNotificationChannel() {
        return ManagedChannelBuilder.forTarget(notificationGrpcAddress)
            .keepAliveWithoutCalls(true)
            .usePlaintext().build();
    }

    @Bean(name = "alerts")
    public ManagedChannel createAlertsChannel() {
        return ManagedChannelBuilder.forTarget(alertsGrpcAddress)
            .keepAliveWithoutCalls(true)
            .usePlaintext().build();
    }

    @Bean(name = "flowQuerier")
    public ManagedChannel createFlowQuerierChannel() {
        return ManagedChannelBuilder.forTarget(flowQuerierGrpcAddress)
            .keepAliveWithoutCalls(true)
            .build();
    }

    @Bean(name = "minionCertificateManager")
    public ManagedChannel minionCertificateManagerChannel() {
        return ManagedChannelBuilder.forTarget(minionCertificateManagerGrpcAddress)
            .keepAliveWithoutCalls(true)
            .usePlaintext().build();
    }

    @Bean(destroyMethod = "shutdown", initMethod = "initialStubs")
    public InventoryClient createInventoryClient(@Qualifier("inventory") ManagedChannel channel) {
        return new InventoryClient(channel, deadline);
    }

    @Bean(destroyMethod = "shutdown", initMethod = "initialStubs")
    public EventsClient createEventsClient(@Qualifier("events") ManagedChannel channel) {
        return new EventsClient(channel, deadline);
    }

    @Bean(destroyMethod = "shutdown", initMethod = "initialStubs")
    public NotificationClient createNotificationClient(@Qualifier("notification") ManagedChannel channel) {
        return new NotificationClient(channel);
    }

    @Bean(destroyMethod = "shutdown", initMethod = "initialStubs")
    public AlertsClient createAlertsClient(@Qualifier("alerts") ManagedChannel channel) {
        return new AlertsClient(channel, deadline, policyMapper);
    }

    @Bean(destroyMethod = "shutdown", initMethod = "initialStubs")
    public FlowClient createFlowClient(@Qualifier("flowQuerier") ManagedChannel channel, InventoryClient inventoryClient) {
        return new FlowClient(inventoryClient, channel, deadline);
    }

    @Bean(destroyMethod = "shutdown", initMethod = "initialStubs")
    public MinionCertificateManagerClient createMinionCertificateManagerClient(@Qualifier("minionCertificateManager") ManagedChannel channel) {
        return new MinionCertificateManagerClient(channel, deadline);
    }
}
