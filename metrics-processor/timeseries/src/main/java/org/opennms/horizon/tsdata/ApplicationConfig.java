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

package org.opennms.horizon.tsdata;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.opennms.horizon.flows.FlowIngesterClient;
import org.opennms.horizon.timeseries.cortex.CortexTSS;
import org.opennms.horizon.timeseries.cortex.CortexTSSConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {
    @Value("${cortex.write.url}")
    private String cortexWriteURL;

    @Value("${cortex.maxconcurrenthttpconnections:100}")
    private int maxConcurrentHttpConnections;

    @Value("${cortex.cortexwritetimeoutinms:1000}")
    private long cortexWriteTimeoutInMs;

    @Value("${cortex.readtimeoutinms:1000}")
    private long readTimeoutInMs;

    @Value("${cortex.bulkheadmaxwaitdurationinms:9223372036854775807}")
    private long bulkheadMaxWaitDurationInMs;

    @Value("${cortex.organizationid}")
    private String organizationId;

    @Bean
    public CortexTSSConfig cortexTSSConfig() {
        return new CortexTSSConfig(cortexWriteURL, maxConcurrentHttpConnections, cortexWriteTimeoutInMs, readTimeoutInMs, bulkheadMaxWaitDurationInMs, organizationId);
    }

    @Bean
    public CortexTSS createCortex() {
        return new CortexTSS(cortexTSSConfig());
    }

    @Value("${grpc.server.deadline:60000}")
    private long deadline;

    @Value("${grpc.url.flow.ingester}")
    private String flowIngesterGrpcAddress;

    @Bean(name = "flowIngester")
    public ManagedChannel createFlowIngesterChannel() {
        return ManagedChannelBuilder.forTarget(flowIngesterGrpcAddress)
            .keepAliveWithoutCalls(true)
            .usePlaintext().build();
    }

    @Bean(destroyMethod = "shutdown", initMethod = "initialStubs")
    public FlowIngesterClient createFlowClient(@Qualifier("flowIngester") ManagedChannel channel) {
        return new FlowIngesterClient(channel, deadline);
    }

}

