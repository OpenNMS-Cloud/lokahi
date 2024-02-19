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
package org.opennms.horizon.tsdata.collector;

import java.io.IOException;
import org.opennms.taskset.contract.CollectorResponse;
import org.opennms.taskset.contract.MonitorType;
import org.opennms.taskset.contract.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TaskSetCollectorResultProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(TaskSetCollectorResultProcessor.class);

    private final TaskSetCollectorSnmpResponseProcessor taskSetCollectorSnmpResponseProcessor;
    private final TaskSetCollectorAzureResponseProcessor taskSetCollectorAzureResponseProcessor;

    @Autowired
    public TaskSetCollectorResultProcessor(
            TaskSetCollectorSnmpResponseProcessor taskSetCollectorSnmpResponseProcessor,
            TaskSetCollectorAzureResponseProcessor taskSetCollectorAzureResponseProcessor) {
        this.taskSetCollectorSnmpResponseProcessor = taskSetCollectorSnmpResponseProcessor;
        this.taskSetCollectorAzureResponseProcessor = taskSetCollectorAzureResponseProcessor;
    }

    public void processCollectorResponse(
            String tenantId, String location, TaskResult taskResult, CollectorResponse collectorResponse)
            throws IOException {
        LOG.info(
                "Have collector response: tenant-id={}; location={}; system-id={}; task-id={}",
                tenantId,
                location,
                taskResult.getIdentity().getSystemId(),
                taskResult.getId());

        String[] labelValues = {
            collectorResponse.getIpAddress(),
            location,
            taskResult.getIdentity().getSystemId(),
            collectorResponse.getMonitorType().name(),
            String.valueOf(collectorResponse.getNodeId())
        };

        if (collectorResponse.hasResult()) {
            MonitorType monitorType = collectorResponse.getMonitorType();
            if (monitorType.equals(MonitorType.SNMP)) {
                taskSetCollectorSnmpResponseProcessor.processSnmpCollectorResponse(tenantId, location, taskResult);
            } else if (monitorType.equals(MonitorType.AZURE)) {
                taskSetCollectorAzureResponseProcessor.processAzureCollectorResponse(
                        tenantId, location, collectorResponse, labelValues);
            } else {
                LOG.warn("Unrecognized monitor type");
            }
        } else {
            LOG.warn("No result in response");
        }
    }
}
