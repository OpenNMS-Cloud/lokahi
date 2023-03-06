/*******************************************************************************
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
 *******************************************************************************/

package org.opennms.horizon.flows.grpc.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;

@TestConfiguration
@ImportAutoConfiguration
public class IngestorApplicationConfigTest {

    public static final String SERVER_NAME = InProcessServerBuilder.generateName();

    @Value("${grpc.server.deadline:60000}")
    private long deadline;

    @Value("${grpc.flow-ingestor.retry.maxAttempts}")
    private int maxNumberOfAttempts;

    @Value("${grpc.flow-ingestor.retry.maxDelay}")
    private int backOffPeriod;

    @Bean
    public GrpcIngesterMockServer grpcIngesterMockServer() {
        return new GrpcIngesterMockServer();
    }

    @Bean(name = "ingestorChannel")
    public ManagedChannel createIngestorChannel() {
        return InProcessChannelBuilder.forName(SERVER_NAME).directExecutor().build();
    }

    @Bean(destroyMethod = "shutdown", initMethod = "initStubs")
    public IngestorClient createIngestorClient(@Qualifier("ingestorChannel") ManagedChannel channel, RetryTemplate retryTemplate) {
        return new IngestorClient(channel, deadline, retryTemplate);
    }

    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        fixedBackOffPolicy.setBackOffPeriod(backOffPeriod);
        retryTemplate.setBackOffPolicy(fixedBackOffPolicy);
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(maxNumberOfAttempts);
        retryTemplate.setRetryPolicy(retryPolicy);
        return retryTemplate;
    }

}
