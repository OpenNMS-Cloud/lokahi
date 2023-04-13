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

package org.opennms.horizon.flows;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.flows.document.TenantLocationSpecificFlowDocumentLog;
import org.opennms.horizon.flows.processing.Pipeline;
import org.opennms.horizon.tenantmetrics.TenantMetricsTracker;
import org.springframework.context.annotation.PropertySource;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@PropertySource("classpath:application.yml")
public class FlowProcessor {
    private final Pipeline pipeline;
    private final TenantMetricsTracker metricsTracker;

    public FlowProcessor(final Pipeline pipeline, final TenantMetricsTracker metricsTracker){
        this.pipeline = Objects.requireNonNull(pipeline);
        this.metricsTracker = metricsTracker;
    }

    @KafkaListener(topics = "${kafka.flow-topics}", concurrency = "1")
    public void consume(@Payload byte[] data) {
        try {
            var flowDocumentLog = TenantLocationSpecificFlowDocumentLog.parseFrom(data);
            CompletableFuture.supplyAsync(() -> {
                try {
                    String tenantId = flowDocumentLog.getTenantId();

                    log.trace("Processing flow: tenant-id={}; flow={}", tenantId, flowDocumentLog);

                    pipeline.process(flowDocumentLog.getMessageList(), tenantId);
                    metricsTracker.addTenantFlowSampleCount(tenantId, flowDocumentLog.getMessageCount());
                } catch (Exception exc) {
                    log.warn("Error processing flow: {} error: {}", flowDocumentLog, exc);
                }
                return null;
            });
        } catch (InvalidProtocolBufferException e) {
            log.error("Invalid data from kafka", e);
        }
    }
}
