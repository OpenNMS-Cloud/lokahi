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
import org.opennms.horizon.flows.cache.CacheConfig;
import org.opennms.horizon.flows.classification.ClassificationEngine;
import org.opennms.horizon.flows.classification.internal.DefaultClassificationEngine;
import org.opennms.horizon.flows.dao.InterfaceToNodeCache;
import org.opennms.horizon.flows.grpc.client.InventoryClient;
import org.opennms.horizon.flows.processing.DocumentEnricherImpl;
import org.opennms.horizon.flows.processing.DocumentMangler;
import org.opennms.horizon.flows.processing.InterfaceToNodeCacheDaoImpl;
import org.opennms.horizon.flows.processing.Pipeline;
import org.opennms.horizon.flows.processing.PipelineImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.script.ScriptEngineManager;

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

    @Bean
    public CacheConfig createCacheConfig(final MetricRegistry metricRegistry) {
        var config = new CacheConfig("nodes");
        config.setMetricRegistry(metricRegistry);
        config.setEnabled(nodeCacheEnabled);
        config.setMaximumSize(nodeCacheMaximumSize);
        config.setExpireAfterWrite(nodeCacheExpireAfterWrite);
        config.setRecordStats(nodeCacheRecordStats);
        return config;
    }

    @Bean
    public DocumentMangler createDocumentMangler(){
        return new DocumentMangler(new ScriptEngineManager());
    }

    @Bean
    public InterfaceToNodeCache createInterfaceToNodeCache(InventoryClient client){
        return new InterfaceToNodeCacheDaoImpl(client);
    }

    @Bean
    public Pipeline createPipeLine(DocumentEnricherImpl documentEnricher) {
        return new PipelineImpl(documentEnricher);
    }
//
//    @Bean
//    public ClassificationEngine createClassificationEngine(){
//        return new DefaultClassificationEngine();
//    }
    @Bean
    public DocumentEnricherImpl createDocumentEnricher(final MetricRegistry metricRegistry,
                                                       final InventoryClient inventoryClient,
                                                       final InterfaceToNodeCache interfaceToNodeCache,
                                                       final ClassificationEngine classificationEngine,
                                                       final CacheConfig cacheConfig,
                                                       final DocumentMangler mangler){
        return new DocumentEnricherImpl(metricRegistry, inventoryClient, interfaceToNodeCache, classificationEngine,
            cacheConfig, clockSkewCorrectionThreshold, mangler);
    }
}

