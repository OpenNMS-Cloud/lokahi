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

package org.opennms.horizon.inventory.component;

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

    @KafkaListener(topics = "${kafka.topics.task-set-results}", concurrency = "1")
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
                    scannerResponseService.accept(tenantId, Long.valueOf(locationId), response);
                } else if(taskResult.hasMonitorResponse()) {
                    var monitorResponse = taskResult.getMonitorResponse();
                    monitorResponseService.updateMonitoredState(tenantId, monitorResponse);
                }

            }
        } catch (Exception e) {
            log.error("Error while processing kafka message for TaskResults: ", e);
        }
    }
}
