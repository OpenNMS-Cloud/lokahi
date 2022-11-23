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

package org.opennms.horizon.inventory.service.taskset.manager;

import org.opennms.taskset.contract.TaskDefinition;
import org.opennms.taskset.contract.TaskSet;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DefaultTaskSetManager implements TaskSetManager {

    private final Map<String, TaskSet> taskSetsByLocation = new HashMap<>();

    @Override
    public synchronized void addTaskSet(String location, TaskDefinition newTaskDefinition) {
        TaskSet existingTaskSet = taskSetsByLocation.get(location);
        TaskSet taskSet;
        if (existingTaskSet != null) {
            List<TaskDefinition> existingTaskDefinitions = new ArrayList<>(existingTaskSet.getTaskDefinitionList());
            existingTaskDefinitions.removeIf(task -> task.getId().equals(newTaskDefinition.getId()));
            taskSet = TaskSet.newBuilder().addAllTaskDefinition(existingTaskDefinitions)
                .addTaskDefinition(newTaskDefinition).build();
        } else {
            taskSet = TaskSet.newBuilder().addTaskDefinition(newTaskDefinition).build();
        }
        taskSetsByLocation.put(location, taskSet);
    }

    @Override
    public synchronized TaskSet getTaskSet(String location) {
        return taskSetsByLocation.get(location);
    }
}
