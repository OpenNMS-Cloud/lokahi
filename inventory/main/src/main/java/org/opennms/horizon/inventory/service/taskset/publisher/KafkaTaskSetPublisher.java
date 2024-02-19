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
package org.opennms.horizon.inventory.service.taskset.publisher;

import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.taskset.contract.TaskDefinition;
import org.opennms.taskset.service.contract.AddSingleTaskOp;
import org.opennms.taskset.service.contract.RemoveSingleTaskOp;
import org.opennms.taskset.service.contract.UpdateSingleTaskOp;
import org.opennms.taskset.service.contract.UpdateTasksRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
@PropertySource("classpath:application.yml")
@Primary
public class KafkaTaskSetPublisher implements TaskSetPublisher {

    private static final String DEFAULT_TASK_SET_PUBLISH_TOPIC = "task-set-publisher";
    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    @Value("${kafka.topics.task-set-publisher:" + DEFAULT_TASK_SET_PUBLISH_TOPIC + "}")
    private String kafkaTopic;

    @Override
    public void publishNewTasks(String tenantId, Long locationId, List<TaskDefinition> taskList) {
        log.info("Publishing task updates for tenantId={}; locationId={}; taskDef={}", tenantId, locationId, taskList);
        publishTaskSetUpdate(
                (updateBuilder) ->
                        taskList.forEach((taskDefinition) -> addAdditionOpToTaskUpdate(updateBuilder, taskDefinition)),
                tenantId,
                locationId);
    }

    @Override
    public void publishTaskDeletion(String tenantId, Long locationId, List<TaskDefinition> taskList) {
        log.info(
                "Publishing task removal for location for tenantId={}; locationId={}; taskDef={}",
                tenantId,
                locationId,
                taskList);
        publishTaskSetUpdate(
                (updateBuilder) -> taskList.forEach(
                        (taskDefinition) -> addRemovalOpToUpdate(updateBuilder, taskDefinition.getId())),
                tenantId,
                locationId);
    }

    private void addAdditionOpToTaskUpdate(UpdateTasksRequest.Builder updateBuilder, TaskDefinition task) {
        AddSingleTaskOp addOp =
                AddSingleTaskOp.newBuilder().setTaskDefinition(task).build();

        UpdateSingleTaskOp updateOp =
                UpdateSingleTaskOp.newBuilder().setAddTask(addOp).build();

        updateBuilder.addUpdate(updateOp);
    }

    private void addRemovalOpToUpdate(UpdateTasksRequest.Builder updateBuilder, String taskId) {
        RemoveSingleTaskOp removeOp =
                RemoveSingleTaskOp.newBuilder().setTaskId(taskId).build();

        UpdateSingleTaskOp updateOp =
                UpdateSingleTaskOp.newBuilder().setRemoveTask(removeOp).build();

        updateBuilder.addUpdate(updateOp);
    }

    private void publishTaskSetUpdate(
            Consumer<UpdateTasksRequest.Builder> populateUpdateRequestOp, String tenantId, Long locationId) {
        UpdateTasksRequest.Builder request =
                UpdateTasksRequest.newBuilder().setTenantId(tenantId).setLocationId(String.valueOf(locationId));

        populateUpdateRequestOp.accept(request);

        kafkaTemplate.send(
                kafkaTopic, tenantId + ":" + locationId, request.build().toByteArray());
    }
}
