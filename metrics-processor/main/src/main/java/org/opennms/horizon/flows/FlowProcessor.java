/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.opennms.horizon.flows;

import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.flows.document.TenantLocationSpecificFlowDocumentLog;
import org.opennms.horizon.flows.processing.Pipeline;
import org.opennms.horizon.tenantmetrics.TenantMetricsTracker;
import org.springframework.context.annotation.PropertySource;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@PropertySource("classpath:application.yml")
public class FlowProcessor {
    private final Pipeline pipeline;
    private final TenantMetricsTracker metricsTracker;

    public FlowProcessor(final Pipeline pipeline, final TenantMetricsTracker metricsTracker) {
        this.pipeline = Objects.requireNonNull(pipeline);
        this.metricsTracker = metricsTracker;
    }

    @KafkaListener(topics = "${kafka.flow-topics}", concurrency = "1")
    public void consume(@Payload byte[] data) {
        try {
            var flowDocumentLog = TenantLocationSpecificFlowDocumentLog.parseFrom(data);
            String tenantId = flowDocumentLog.getTenantId();
            CompletableFuture.supplyAsync(() -> {
                try {
                    log.trace("Processing flow: tenant-id={}; flow={}", tenantId, flowDocumentLog);
                    pipeline.process(flowDocumentLog);
                    metricsTracker.addTenantFlowCompletedCount(tenantId, flowDocumentLog.getMessageCount());
                } catch (Exception exc) {
                    log.warn("Error processing flow: tenant-id={}; error: {}", tenantId, exc.getMessage(), exc);
                } finally {
                    // record as close as possible to the addTenantFlowCompletedCount (i.e.: after processing)
                    // so both hopefully end up together in results so we can compute a reasonable success rate
                    metricsTracker.addTenantFlowReceviedCount(tenantId, flowDocumentLog.getMessageCount());
                }
                return null;
            });
        } catch (InvalidProtocolBufferException e) {
            log.error("Invalid data from kafka", e);
        }
    }
}
