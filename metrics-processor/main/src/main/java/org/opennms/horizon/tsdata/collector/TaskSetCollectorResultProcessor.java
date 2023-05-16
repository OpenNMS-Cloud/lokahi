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

package org.opennms.horizon.tsdata.collector;

import org.opennms.taskset.contract.CollectorResponse;
import org.opennms.taskset.contract.MonitorType;
import org.opennms.taskset.contract.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class TaskSetCollectorResultProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(TaskSetCollectorResultProcessor.class);

    private final TaskSetCollectorSnmpResponseProcessor taskSetCollectorSnmpResponseProcessor;
    private final TaskSetCollectorAzureResponseProcessor taskSetCollectorAzureResponseProcessor;

    @Autowired
    public TaskSetCollectorResultProcessor(TaskSetCollectorSnmpResponseProcessor taskSetCollectorSnmpResponseProcessor,
        TaskSetCollectorAzureResponseProcessor taskSetCollectorAzureResponseProcessor) {
        this.taskSetCollectorSnmpResponseProcessor = taskSetCollectorSnmpResponseProcessor;
        this.taskSetCollectorAzureResponseProcessor = taskSetCollectorAzureResponseProcessor;
    }

    public void processCollectorResponse(String tenantId, TaskResult taskResult, CollectorResponse collectorResponse) throws IOException {
        LOG.info("Have collector response, tenant-id: {}; task-id={};", tenantId, taskResult.getId());

        String[] labelValues =
            {
                collectorResponse.getIpAddress(),
                taskResult.getLocation(),
                taskResult.getSystemId(),
                collectorResponse.getMonitorType().name(),
                String.valueOf(collectorResponse.getNodeId())
            };

        if (collectorResponse.hasResult()) {
            MonitorType monitorType = collectorResponse.getMonitorType();
            if (monitorType.equals(MonitorType.SNMP)) {
                taskSetCollectorSnmpResponseProcessor.processSnmpCollectorResponse(tenantId, taskResult);
            } else if (monitorType.equals(MonitorType.AZURE)) {
                taskSetCollectorAzureResponseProcessor.processAzureCollectorResponse(tenantId, collectorResponse, labelValues);
            } else {
                LOG.warn("Unrecognized monitor type");
            }
        } else {
            LOG.warn("No result in response");
        }
    }
}
