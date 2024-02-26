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
    public TaskSetResultProcessor(
            TaskSetMonitorResultProcessor taskSetMonitorResultProcessor,
            TaskSetCollectorResultProcessor taskSetCollectorResultProcessor) {
        this.taskSetMonitorResultProcessor = taskSetMonitorResultProcessor;
        this.taskSetCollectorResultProcessor = taskSetCollectorResultProcessor;
    }

    public void processTaskResult(String tenantId, String locationId, TaskResult taskResult) {
        try {
            LOG.info("Processing task set result {}", taskResult);
            if (taskResult.hasMonitorResponse()
                    && taskResult.getMonitorResponse().getResponseTimeMs() > 0) {
                taskSetMonitorResultProcessor.processMonitorResponse(
                        tenantId, locationId, taskResult, taskResult.getMonitorResponse());
            } else if (taskResult.hasCollectorResponse()
                    && taskResult.getCollectorResponse().getStatus()) {
                taskSetCollectorResultProcessor.processCollectorResponse(
                        tenantId, locationId, taskResult, taskResult.getCollectorResponse());
            }
        } catch (Exception exc) {
            // TODO: throttle
            LOG.warn("Error processing task result", exc);
        }
    }
}
