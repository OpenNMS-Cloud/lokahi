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

package org.opennms.horizon.tsdata;

import org.opennms.horizon.tsdata.collector.TaskSetCollectorResultProcessor;
import org.opennms.horizon.tsdata.monitor.TaskSetMonitorResultProcessor;
import org.opennms.taskset.contract.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TaskSetResultProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(TaskSetResultProcessor.class);


    private final TaskSetMonitorResultProcessor taskSetMonitorResultProcessor;

    private final TaskSetCollectorResultProcessor taskSetCollectorResultProcessor;

    @Autowired
    public TaskSetResultProcessor(TaskSetMonitorResultProcessor taskSetMonitorResultProcessor,
        TaskSetCollectorResultProcessor taskSetCollectorResultProcessor) {
        this.taskSetMonitorResultProcessor = taskSetMonitorResultProcessor;
        this.taskSetCollectorResultProcessor = taskSetCollectorResultProcessor;
    }

    public void processTaskResult(String tenantId, String location, TaskResult taskResult) {
        try {
            LOG.info("Processing task set result {}", taskResult);
            if (taskResult.hasMonitorResponse()) {
                LOG.info("Have monitor response, tenant-id: {}; task-id={};", tenantId, taskResult.getId());
                taskSetMonitorResultProcessor.processMonitorResponse(tenantId, location, taskResult, taskResult.getMonitorResponse());
            } else if (taskResult.hasCollectorResponse()) {
                taskSetCollectorResultProcessor.processCollectorResponse(tenantId, location, taskResult, taskResult.getCollectorResponse());
            }
        } catch (Exception exc) {
            // TODO: throttle
            LOG.warn("Error processing task result", exc);
        }
    }


}
