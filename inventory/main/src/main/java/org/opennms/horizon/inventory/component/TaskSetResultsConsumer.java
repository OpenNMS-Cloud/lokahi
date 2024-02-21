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
package org.opennms.horizon.inventory.component;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.opennms.horizon.inventory.exception.InventoryRuntimeException;
import org.opennms.horizon.inventory.service.taskset.response.MonitorResponseService;
import org.opennms.horizon.inventory.service.taskset.response.ScannerResponseService;
import org.opennms.taskset.contract.ScannerResponse;
import org.opennms.taskset.contract.TaskResult;
import org.opennms.taskset.contract.TenantLocationSpecificTaskSetResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskSetResultsConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(TaskSetResultsConsumer.class);

    private final ScannerResponseService scannerResponseService;

    private final MonitorResponseService monitorResponseService;
    private final ThreadFactory threadFactory =
            new ThreadFactoryBuilder().setNameFormat("handle-taskset-%d").build();
    // This fixed thread pool allows us to process tasksets in parallel apart from Kafka concurrency which is
    // still tunable at the application level.
    private final ExecutorService executorService = Executors.newFixedThreadPool(100, threadFactory);

    @KafkaListener(topics = "${kafka.topics.task-set-results}", concurrency = "${kafka.concurrency.task-set-results}")
    public void receiveMessage(@Payload byte[] data) {
        LOG.debug("Have message from Task Set Results kafka topic");

        try {
            TenantLocationSpecificTaskSetResults message = TenantLocationSpecificTaskSetResults.parseFrom(data);

            String tenantId = message.getTenantId();
            String locationId = message.getLocationId();

            if (Strings.isEmpty(tenantId)) {
                throw new InventoryRuntimeException("Missing tenant id");
            }

            for (TaskResult taskResult : message.getResultsList()) {
                log.info("Received taskset results from minion with tenantId={}; locationId={}", tenantId, locationId);
                if (taskResult.hasScannerResponse()) {
                    ScannerResponse response = taskResult.getScannerResponse();
                    executorService.execute(() -> {
                        try {
                            scannerResponseService.accept(tenantId, Long.valueOf(locationId), response);
                        } catch (InvalidProtocolBufferException e) {
                            LOG.error("Exception while parsing response", e);
                        } catch (Exception e) {
                            LOG.error("Exception while consuming response", e);
                        }
                    });
                } else if (taskResult.hasMonitorResponse()) {
                    var monitorResponse = taskResult.getMonitorResponse();
                    monitorResponseService.updateMonitoredState(tenantId, locationId, monitorResponse);
                }
            }
        } catch (Exception e) {
            log.error("Error while processing kafka message for TaskResults: ", e);
        }
    }
}
