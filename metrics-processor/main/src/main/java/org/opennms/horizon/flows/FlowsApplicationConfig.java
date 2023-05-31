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

package org.opennms.horizon.flows;

import com.codahale.metrics.MetricRegistry;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import org.opennms.horizon.flows.classification.ClassificationEngine;
import org.opennms.horizon.flows.classification.ClassificationRuleProvider;
import org.opennms.horizon.flows.classification.FilterService;
import org.opennms.horizon.flows.classification.csv.CsvImporter;
import org.opennms.horizon.flows.classification.internal.DefaultClassificationEngine;
import org.opennms.horizon.flows.grpc.client.IngestorClient;
import org.opennms.horizon.flows.grpc.client.InventoryClient;
import org.opennms.horizon.flows.integration.FlowRepository;
import org.opennms.horizon.flows.integration.FlowRepositoryImpl;
import org.opennms.horizon.flows.processing.DocumentEnricherImpl;
import org.opennms.horizon.flows.processing.FlowDocumentClassificationRequestMapper;
import org.opennms.horizon.flows.processing.Pipeline;
import org.opennms.horizon.flows.processing.PipelineImpl;
import org.opennms.horizon.flows.processing.impl.FlowDocumentClassificationRequestMapperImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;

@Configuration
@Profile("!test")
public class FlowsApplicationConfig {

    @Value("${flows.clockSkewCorrectionThreshold:0}")
    private long clockSkewCorrectionThreshold;

    @Value("${flows.mangleScriptPath:}")
    private String mangleScriptPath;

    @Value("${flows.nodeCache.enabled:true}")
    private boolean nodeCacheEnabled;

    @Value("${flows.nodeCache.maximumSize:10000}")
    private long nodeCacheMaximumSize;

    @Value("${flows.nodeCache.expireAfterWrite:0}")
    private long nodeCacheExpireAfterWrite;

    @Value("${flows.nodeCache.recordStats:true}")
    private boolean nodeCacheRecordStats;

    @Value("${grpc.inventory.url}")
    private String inventoryGrpcAddress;

    @Value("${grpc.flow-ingestor.url}")
    private String ingestorGrpcAddress;

    @Value("${grpc.flow.tls.enabled:false}")
    private boolean flowTlsEnabled;

    @Value("${grpc.server.deadline:60000}")
    private long deadline;

    @Value("${grpc.flow-ingestor.retry.maxAttempts}")
    private int maxNumberOfAttempts;

    @Value("${grpc.flow-ingestor.retry.maxDelay}")
    private int backOffPeriod;

    @Bean(name = "inventoryChannel")
    public ManagedChannel createInventoryChannel() {
        return ManagedChannelBuilder.forTarget(inventoryGrpcAddress)
            .keepAliveWithoutCalls(true)
            .usePlaintext().build();
    }

    @Bean(name = "ingestorChannel")
    public ManagedChannel createIngestorChannel() throws SSLException {
        var builder = NettyChannelBuilder.forTarget(ingestorGrpcAddress)
            .keepAliveTime(10, TimeUnit.SECONDS)
            .keepAliveTimeout(15, TimeUnit.SECONDS);
        if (flowTlsEnabled) {
            builder.sslContext(GrpcSslContexts.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build());
        } else {
            builder.usePlaintext();
        }
        return builder.build();
    }

    @Bean(destroyMethod = "shutdown", initMethod = "initialStubs")
    public InventoryClient createInventoryClient(@Qualifier("inventoryChannel") ManagedChannel channel) {
        return new InventoryClient(channel, deadline);
    }

    @Bean(destroyMethod = "shutdown", initMethod = "initStubs")
    public IngestorClient createIngestorClient(@Qualifier("ingestorChannel") ManagedChannel channel, RetryTemplate retryTemplate) {
        return new IngestorClient(channel, deadline, retryTemplate);
    }

    @Bean
    public Pipeline createPipeLine(final MetricRegistry metricRegistry, final DocumentEnricherImpl documentEnricher,
                                   final FlowRepository flowRepository) {
        var pipeLine = new PipelineImpl(metricRegistry, documentEnricher);
        var properties = new HashMap<>();
        properties.put(PipelineImpl.REPOSITORY_ID, "DataPlatform");
        pipeLine.onBind(flowRepository, properties);
        return pipeLine;
    }

    @Bean
    public FlowRepository createFlowRepositoryImpl(final IngestorClient ingestorClient) {
        return new FlowRepositoryImpl(ingestorClient);
    }

    @Bean
    public ClassificationEngine createClassificationEngine() throws InterruptedException, IOException {
        final var rules = CsvImporter.parseCSV(
            FlowProcessor.class.getResourceAsStream("/pre-defined-rules.csv"),
            true);

        return new DefaultClassificationEngine(
            ClassificationRuleProvider.forList(rules),
            FilterService.NOOP);
    }

    @Bean
    public DocumentEnricherImpl createDocumentEnricher(InventoryClient inventoryClient,
                                                       ClassificationEngine classificationEngine,
                                                       FlowDocumentClassificationRequestMapper flowDocumentClassificationRequestMapper
    ) {
        return new DocumentEnricherImpl(inventoryClient, classificationEngine, flowDocumentClassificationRequestMapper, clockSkewCorrectionThreshold);
    }

    @Bean
    public FlowDocumentClassificationRequestMapper flowDocumentClassificationRequestMapper() {
        return new FlowDocumentClassificationRequestMapperImpl();
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
