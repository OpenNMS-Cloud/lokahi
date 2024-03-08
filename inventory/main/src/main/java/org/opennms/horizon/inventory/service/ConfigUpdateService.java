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

import static org.opennms.horizon.inventory.service.FlowsConfigService.FLOWS_CONFIG;
import static org.opennms.horizon.inventory.service.TrapConfigService.TRAPS_CONFIG;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.inventory.service.taskset.TaskUtils;
import org.opennms.horizon.inventory.service.taskset.publisher.TaskSetPublisher;
import org.opennms.taskset.contract.TaskDefinition;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigUpdateService {

    private final ThreadFactory threadFactory = new ThreadFactoryBuilder()
            .setNameFormat("new-location-run-config-update-%d")
            .build();

    @Setter // Testability
    private ExecutorService executorService = Executors.newFixedThreadPool(10, threadFactory);

    private final TrapConfigService trapConfigService;
    private final FlowsConfigService flowsConfigService;
    private final TaskSetPublisher taskSetPublisher;

    private void sendConfigUpdatesToMinion(String tenantId, Long locationId) {
        try {
            trapConfigService.sendTrapConfigToMinion(tenantId, locationId);
        } catch (Exception e) {
            log.error("Exception while sending traps to Minion", e);
        }
        try {
            flowsConfigService.sendFlowsConfigToMinion(tenantId, locationId);
        } catch (Exception e) {
            log.error("Exception while sending flows to Minion", e);
        }
    }

    public void sendConfigUpdate(String tenantId, Long locationId) {
        executorService.execute(() -> sendConfigUpdatesToMinion(tenantId, locationId));
    }

    public void removeConfigsFromTaskSet(String tenantId, Long locationId) {

        executorService.execute(() -> {
            TaskDefinition trapsConfig = TaskDefinition.newBuilder()
                    .setId(TaskUtils.identityForConfig(TRAPS_CONFIG, locationId))
                    .build();
            TaskDefinition flowsConfig = TaskDefinition.newBuilder()
                    .setId(TaskUtils.identityForConfig(FLOWS_CONFIG, locationId))
                    .build();
            var tasks = new ArrayList<TaskDefinition>();
            tasks.add(trapsConfig);
            tasks.add(flowsConfig);
            taskSetPublisher.publishTaskDeletion(tenantId, locationId, tasks);
        });
    }
}
