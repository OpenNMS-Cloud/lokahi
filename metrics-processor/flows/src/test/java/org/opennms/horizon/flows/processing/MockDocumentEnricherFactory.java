/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2017-2023 The OpenNMS Group, Inc.
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

package org.opennms.horizon.flows.processing;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.Lists;
import org.opennms.horizon.flows.classification.ClassificationEngine;
import org.opennms.horizon.flows.classification.FilterService;
import org.opennms.horizon.flows.classification.internal.DefaultClassificationEngine;
import org.opennms.horizon.flows.classification.persistence.api.RuleBuilder;
import org.opennms.horizon.flows.grpc.client.InventoryClient;
import org.opennms.horizon.inventory.dto.NodeDTO;

import javax.script.ScriptEngineManager;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockDocumentEnricherFactory {

    private InventoryClient client;
    private final DocumentEnricherImpl enricher;
    private final ClassificationEngine classificationEngine;

    private final AtomicInteger nodeDaoGetCounter = new AtomicInteger(0);

    public MockDocumentEnricherFactory(Map<Long, NodeDTO> nodeIdToDto) throws InterruptedException {
        this(0, nodeIdToDto);
    }

    public MockDocumentEnricherFactory(final long clockSkewCorrectionThreshold, Map<Long, NodeDTO> nodeIdToDto) throws InterruptedException {
        client = createInventoryClient(nodeIdToDto);

        classificationEngine = new DefaultClassificationEngine(() -> Lists.newArrayList(
            new RuleBuilder().withName("http").withDstPort("80").withProtocol("tcp,udp").build(),
            new RuleBuilder().withName("https").withDstPort("443").withProtocol("tcp,udp").build(),
            new RuleBuilder().withName("http").withSrcPort("80").withProtocol("tcp,udp").build(),
            new RuleBuilder().withName("https").withSrcPort("443").withProtocol("tcp,udp").build()
        ), FilterService.NOOP);
        enricher = new DocumentEnricherImpl(
            new MetricRegistry(),
            client,
            classificationEngine,
            clockSkewCorrectionThreshold);
    }

    public DocumentEnricherImpl getEnricher() {
        return enricher;
    }

    public AtomicInteger getNodeDaoGetCounter() {
        return nodeDaoGetCounter;
    }

    public ClassificationEngine getClassificationEngine() {
        return classificationEngine;
    }

    private InventoryClient createInventoryClient(Map<Long, NodeDTO> nodeIdToDto) {
        // Spy on MockNodeDao to count access to get(int)
        var client = mock(InventoryClient.class);
        when(client.getNodeById(anyLong(), anyString())).thenAnswer(i -> {
            nodeDaoGetCounter.incrementAndGet();
            return nodeIdToDto.get(i.getArgument(0));
        });
        return client;
    }
}
