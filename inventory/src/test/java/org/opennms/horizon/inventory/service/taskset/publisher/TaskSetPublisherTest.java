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

package org.opennms.horizon.inventory.service.taskset.publisher;

import io.grpc.ManagedChannel;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.opennms.taskset.contract.TaskDefinition;
import org.opennms.taskset.contract.TaskSet;
import org.opennms.taskset.contract.TaskType;

import java.util.ArrayList;
import java.util.Optional;

public class TaskSetPublisherTest {


    @Test
    public void testTaskSets() {

        var channel = Mockito.mock(ManagedChannel.class);
        var grpcTaskSetPublisher = new GrpcTaskSetPublisher(channel,
            (ctx) -> Optional.ofNullable(GrpcConstants.TENANT_ID_CONTEXT_KEY.get()), 5000);
        String tenantId = "tenant1";
        String location = "location";
        var fullTaskList = new ArrayList<TaskDefinition>();
        for (int i = 0; i < 10; i++) {
            fullTaskList.add(TaskDefinition.newBuilder()
                .setId(Integer.toString(i))
                .setPluginName(i + ":detector")
                .setType(TaskType.DETECTOR).build());
        }
        // Build new tasks
        TaskSet taskSet = grpcTaskSetPublisher.buildNewTaskSets(tenantId, location, fullTaskList);
        Assertions.assertThat(taskSet).isEqualTo(TaskSet.newBuilder().addAllTaskDefinition(fullTaskList).build());

        // Add a new task.
        var newTaskList = new ArrayList<TaskDefinition>();
        String taskId = "101";
        var taskDefinition = TaskDefinition.newBuilder()
            .setId(taskId)
            .setPluginName(taskId + ":detector")
            .setType(TaskType.DETECTOR).build();
        newTaskList.add(taskDefinition);
        fullTaskList.add(taskDefinition);
        taskSet = grpcTaskSetPublisher.buildNewTaskSets(tenantId, location, newTaskList);
        Assertions.assertThat(taskSet).isEqualTo(TaskSet.newBuilder().addAllTaskDefinition(fullTaskList).build());

        // Add new task for a new tenant, taskset should not match complete full task set.
        taskSet = grpcTaskSetPublisher.buildNewTaskSets("tenant-new", location, newTaskList);
        Assertions.assertThat(taskSet).isNotEqualTo(TaskSet.newBuilder().addAllTaskDefinition(fullTaskList).build());
        Assertions.assertThat(taskSet).isEqualTo(TaskSet.newBuilder().addAllTaskDefinition(newTaskList).build());

        // Add new task for a new location, taskset should not match complete full task set.
        taskSet = grpcTaskSetPublisher.buildNewTaskSets(tenantId, "location-new", newTaskList);
        Assertions.assertThat(taskSet).isNotEqualTo(TaskSet.newBuilder().addAllTaskDefinition(fullTaskList).build());
        Assertions.assertThat(taskSet).isEqualTo(TaskSet.newBuilder().addAllTaskDefinition(newTaskList).build());

        // Add existing task with matching id should replace existing task.
        taskDefinition = TaskDefinition.newBuilder()
            .setId(taskId)
            .setPluginName(taskId + ":detector")
            .setType(TaskType.DETECTOR).build();
        newTaskList = new ArrayList<>();
        newTaskList.add(taskDefinition);
        taskSet = grpcTaskSetPublisher.buildNewTaskSets(tenantId, location, newTaskList);
        Assertions.assertThat(taskSet).isEqualTo(TaskSet.newBuilder().addAllTaskDefinition(fullTaskList).build());

        //Remove from existing taskset
        var optionalTaskSet = grpcTaskSetPublisher.buildTaskSetForRemoval(tenantId, location, newTaskList);
        fullTaskList.remove(taskDefinition);
        Assertions.assertThat(optionalTaskSet.get()).isEqualTo(TaskSet.newBuilder().addAllTaskDefinition(fullTaskList).build());
    }
}
