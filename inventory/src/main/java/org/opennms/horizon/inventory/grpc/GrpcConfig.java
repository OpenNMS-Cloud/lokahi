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

package org.opennms.horizon.inventory.grpc;

import java.util.Collections;

import org.opennms.horizon.inventory.repository.MonitoringLocationRepository;
import org.opennms.horizon.inventory.service.MonitoringGrpcService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcConfig {
    private static final long DEFAULT_MAX_MESSAGE_SIZE = 100 * ( 1024 * 1024 );
    private static final int DEFAULT_GRPC_PORT = 8990;

    @Value("${grpc.server.port:" + DEFAULT_GRPC_PORT +"}")
    private int port;
    @Value("${grpc.server.max.message.size: " + DEFAULT_MAX_MESSAGE_SIZE +"}")
    private int maxMessageSize;
    private final MonitoringLocationRepository locationRepo;

    public GrpcConfig(MonitoringLocationRepository locationRepo) {
        this.locationRepo = locationRepo;
    }

    @Bean
    public MonitoringGrpcService createService() {
        return new MonitoringGrpcService(locationRepo);
    }

    @Bean(destroyMethod = "stopServer")
    public GrpcServerManager startServer(MonitoringGrpcService service) {
        GrpcServerManager manager = new GrpcServerManager(port, maxMessageSize);
        //for next step with more than one services
        manager.startServer(Collections.singletonList(service));
        return manager;
    }
}
