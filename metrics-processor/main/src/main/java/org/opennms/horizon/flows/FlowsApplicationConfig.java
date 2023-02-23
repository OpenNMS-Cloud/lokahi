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
import org.opennms.horizon.flows.classification.ClassificationEngine;
import org.opennms.horizon.flows.classification.ClassificationRuleProvider;
import org.opennms.horizon.flows.classification.FilterService;
import org.opennms.horizon.flows.classification.csv.CsvImporter;
import org.opennms.horizon.flows.classification.internal.DefaultClassificationEngine;
import org.opennms.horizon.flows.grpc.client.InventoryClient;
import org.opennms.horizon.flows.integration.FlowRepository;
import org.opennms.horizon.flows.integration.FlowRepositoryImpl;
import org.opennms.horizon.flows.processing.DocumentEnricherImpl;
import org.opennms.horizon.flows.processing.Pipeline;
import org.opennms.horizon.flows.processing.PipelineImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.HashMap;

@Configuration
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

    @Value("${grpc.url.inventory}")
    private String inventoryGrpcAddress;
    @Value("${grpc.server.deadline:60000}")
    private long deadline;

    @Bean
    public ManagedChannel createInventoryChannel() {
        return ManagedChannelBuilder.forTarget(inventoryGrpcAddress)
            .keepAliveWithoutCalls(true)
            .usePlaintext().build();
    }

    @Bean(destroyMethod = "shutdown", initMethod = "initialStubs")
    public InventoryClient createInventoryClient(ManagedChannel channel) {
        return new InventoryClient(channel, deadline);
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
    public FlowRepository createFlowRepositoryImpl() {
        return new FlowRepositoryImpl();
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
    public DocumentEnricherImpl createDocumentEnricher(final MetricRegistry metricRegistry,
                                                       final InventoryClient inventoryClient,
                                                       final ClassificationEngine classificationEngine) {
        return new DocumentEnricherImpl(metricRegistry, inventoryClient, classificationEngine,
            clockSkewCorrectionThreshold);
    }
}
