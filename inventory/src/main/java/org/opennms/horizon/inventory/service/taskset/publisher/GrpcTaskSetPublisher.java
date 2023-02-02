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

package org.opennms.horizon.inventory.service.taskset.publisher;

import io.grpc.Context;
import io.grpc.ManagedChannel;
import org.opennms.horizon.inventory.grpc.TenantIdClientInterceptor;
import org.opennms.horizon.inventory.grpc.TenantLookup;
import org.opennms.horizon.inventory.taskset.api.TaskSetPublisher;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.opennms.taskset.contract.TaskDefinition;
import org.opennms.taskset.contract.TaskSet;
import org.opennms.taskset.service.contract.PublishTaskSetRequest;
import org.opennms.taskset.service.contract.PublishTaskSetResponse;
import org.opennms.taskset.service.contract.TaskSetServiceGrpc;
import org.opennms.taskset.service.contract.TaskSetServiceGrpc.TaskSetServiceBlockingStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class GrpcTaskSetPublisher implements TaskSetPublisher {

    public static final String TASK_SET_PUBLISH_BEAN_NAME = "taskSetServiceBlockingStub";
    private static final Logger LOG = LoggerFactory.getLogger(GrpcTaskSetPublisher.class);
    private final ManagedChannel channel;
    private final TenantLookup tenantLookup;
    private final long deadline;
    private final Map<String, Map<String, TaskSet>> taskSetsByTenantLocation = new HashMap<>();

    private TaskSetServiceBlockingStub taskSetServiceStub;

    public GrpcTaskSetPublisher(ManagedChannel channel, TenantLookup tenantLookup, long deadline) {
        this.channel = channel;
        this.tenantLookup = tenantLookup;
        this.deadline = deadline;
    }

    private void init() {
        taskSetServiceStub = TaskSetServiceGrpc.newBlockingStub(channel)
            .withInterceptors(new TenantIdClientInterceptor(tenantLookup));
    }

    private void publishTaskSet(String tenantId, String location, TaskSet taskSet) {
        try {
            PublishTaskSetRequest request =
                PublishTaskSetRequest.newBuilder()
                    .setLocation(location)
                    .setTaskSet(taskSet)
                    .build();

            PublishTaskSetResponse response = Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, tenantId).call(() ->
                taskSetServiceStub.withDeadlineAfter(deadline, TimeUnit.MILLISECONDS).publishTaskSet(request)
            );

            LOG.info("Publish task set complete: location={}, response={}", location, response);
        } catch (Exception e) {
            LOG.error("Error publishing taskset", e);
            throw new RuntimeException("failed to publish taskset", e);
        }
    }

    @Override
    public synchronized void publishNewTasks(String tenantId, String location, List<TaskDefinition> taskList) {
        var taskSet = buildNewTaskSets(tenantId, location, taskList);
        publishTaskSet(tenantId, location, taskSet);
    }

    @Override
    public synchronized void publishTaskDeletion(String tenantId, String location, List<TaskDefinition> taskList) {
        var optionalTaskSet = buildTaskSetForRemoval(tenantId, location, taskList);
        optionalTaskSet.ifPresent(taskSet -> publishTaskSet(tenantId, location, taskSet));
    }


    TaskSet buildNewTaskSets(String tenantId, String location, List<TaskDefinition> taskList) {
        Map<String, TaskSet> taskSetsByLocation = taskSetsByTenantLocation.computeIfAbsent(tenantId, (unusedTenantId) -> new HashMap<>());
        TaskSet existingTaskSet = taskSetsByLocation.get(location);
        TaskSet taskSet;

        if (existingTaskSet != null) {
            List<TaskDefinition> existingTasks = new ArrayList<>(existingTaskSet.getTaskDefinitionList());
            taskList.forEach(task -> existingTasks.removeIf(existingTask -> task.getId().equals(existingTask.getId())));
            taskSet = TaskSet.newBuilder().addAllTaskDefinition(existingTasks)
                .addAllTaskDefinition(taskList).build();
        } else {
            taskSet = TaskSet.newBuilder().addAllTaskDefinition(taskList).build();
        }

        taskSetsByLocation.put(location, taskSet);
        return taskSet;
    }

    Optional<TaskSet> buildTaskSetForRemoval(String tenantId, String location, List<TaskDefinition> taskList) {
        Map<String, TaskSet> taskSetsByLocation = taskSetsByTenantLocation.computeIfAbsent(tenantId, (unusedTenantId) -> new HashMap<>());
        TaskSet existingTaskSet = taskSetsByLocation.get(location); 

        if (existingTaskSet != null) {
            List<TaskDefinition> existingTasks = new ArrayList<>(existingTaskSet.getTaskDefinitionList());
            taskList.forEach(task -> existingTasks.removeIf(existingTask -> task.getId().equals(existingTask.getId())));
            TaskSet updatedTaskSet = TaskSet.newBuilder()
                .addAllTaskDefinition(existingTasks).build();
            taskSetsByLocation.put(location, updatedTaskSet);
            return Optional.of(updatedTaskSet);
        }
        return Optional.empty();
    }
}
