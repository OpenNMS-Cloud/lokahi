/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
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

import org.opennms.horizon.server.service.gateway.NotificationGateway;
import org.opennms.horizon.server.service.gateway.PlatformGateway;
import org.opennms.horizon.server.service.grpc.InventoryClient;
import org.opennms.horizon.server.utils.ServerHeaderUtil;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class ConfigurationUtil {
    @Value("${horizon-stream.core.url}")
    private String platformUrl;
    @Value("${horizon-stream.notifications.url}")
    private String notificationsUrl;
    @Value("${grpc.url.inventory}")
    private String inventoryGrpcAddress;

    @Bean
    public ServerHeaderUtil createHeaderUtil() {
        return new ServerHeaderUtil();
    }

    @Bean
    public PlatformGateway createGateway(ServerHeaderUtil util) {
        return new PlatformGateway(platformUrl, util);
    }

    @Bean
    public NotificationGateway createNotificationGateway(ServerHeaderUtil util) {
        return new NotificationGateway(notificationsUrl, util);
    }

    @Bean(name = "inventory")
    public ManagedChannel createInventoryChannel() {
        return ManagedChannelBuilder.forTarget(inventoryGrpcAddress)
            .keepAliveWithoutCalls(true)
            .usePlaintext().build();
    }

    @Bean(destroyMethod = "shutdown", initMethod = "initialStubs")
    public InventoryClient createInventoryClient(@Qualifier("inventory") ManagedChannel channel) {
        return new InventoryClient(channel);
    }
}
