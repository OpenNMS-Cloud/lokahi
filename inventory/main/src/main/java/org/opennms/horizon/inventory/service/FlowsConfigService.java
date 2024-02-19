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
package org.opennms.horizon.inventory.service;

import com.google.common.io.Resources;
import com.google.protobuf.Any;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.inventory.dto.MonitoringLocationDTO;
import org.opennms.horizon.inventory.service.taskset.TaskUtils;
import org.opennms.horizon.inventory.service.taskset.publisher.TaskSetPublisher;
import org.opennms.horizon.shared.protobuf.util.ProtobufUtil;
import org.opennms.sink.flows.contract.FlowsConfig;
import org.opennms.taskset.contract.TaskDefinition;
import org.opennms.taskset.contract.TaskType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FlowsConfigService {
    private static final Logger LOG = LoggerFactory.getLogger(FlowsConfigService.class);
    public static final String FLOWS_CONFIG = "flows-config";
    private final MonitoringLocationService monitoringLocationService;
    private final TaskSetPublisher taskSetPublisher;

    public void sendFlowConfigToMinionAfterStartup() {
        List<MonitoringLocationDTO> allLocations = monitoringLocationService.findAll();

        FlowsConfig flowsConfig = readFlowsConfig();

        if (flowsConfig != null) {
            for (MonitoringLocationDTO dto : allLocations) {
                try {
                    publishFlowsConfig(dto.getTenantId(), dto.getId(), flowsConfig);
                } catch (Exception exc) {
                    LOG.error(
                            "Failed to send flow config: tenant={}; location={}",
                            dto.getTenantId(),
                            dto.getLocation(),
                            exc);
                }
            }
        }
    }

    public void sendFlowsConfigToMinion(String tenantId, Long locationId) {
        FlowsConfig flowsConfig = readFlowsConfig();
        if (flowsConfig != null) {
            publishFlowsConfig(tenantId, locationId, flowsConfig);
        }
    }

    private void publishFlowsConfig(String tenantId, Long locationId, FlowsConfig flowsConfig) {
        TaskDefinition taskDefinition = TaskDefinition.newBuilder()
                .setId(TaskUtils.identityForConfig(FLOWS_CONFIG, locationId))
                .setPluginName("flows.parsers.config")
                .setType(TaskType.LISTENER)
                .setConfiguration(Any.pack(flowsConfig))
                .build();

        var taskList = new ArrayList<TaskDefinition>();
        taskList.add(taskDefinition);
        taskSetPublisher.publishNewTasks(tenantId, locationId, taskList);
    }

    private FlowsConfig readFlowsConfig() {
        try {
            URL url = this.getClass().getResource("/flows-config.json");
            return ProtobufUtil.fromJson(Resources.toString(url, StandardCharsets.UTF_8), FlowsConfig.class);
        } catch (IOException ex) {
            LOG.error("Failed to read flows config", ex);
            return null;
        }
    }
}
