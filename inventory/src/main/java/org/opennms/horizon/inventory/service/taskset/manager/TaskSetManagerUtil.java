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

import com.google.protobuf.Any;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.opennms.horizon.inventory.service.taskset.identity.TaskSetIdentityUtil;
import org.opennms.taskset.contract.TaskDefinition;
import org.opennms.taskset.contract.TaskType;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class TaskSetManagerUtil {
    private final TaskSetManager taskSetManager;
    private final TaskSetIdentityUtil taskSetIdentityUtil;

    public void addTask(String location, String ipAddress, String name, TaskType taskType,
                        String pluginName, String schedule, long nodeId, Any configuration) {

        String taskId = taskSetIdentityUtil.identityForIpTask(ipAddress, name);
        addTaskToTaskSet(location, taskType, pluginName, schedule, configuration, taskId, nodeId);
    }

    public void addTask(String location, String ipAddress, String name, TaskType taskType,
                        String pluginName, Any configuration, long nodeId) {

        String taskId = taskSetIdentityUtil.identityForIpTask(ipAddress, name);
        addTaskToTaskSet(location, taskType, pluginName, null, configuration, taskId, nodeId);
    }

    private void addTaskToTaskSet(String location, TaskType taskType, String pluginName, String schedule,
                                  Any configuration, String taskId, long nodeId) {


        TaskDefinition.Builder builder =
            TaskDefinition.newBuilder()
                .setType(taskType)
                .setPluginName(pluginName)
                .setNodeId(nodeId)
                .setId(taskId);

        if (StringUtils.isNotBlank(schedule)) {
            builder.setSchedule(schedule);
        }

        if (!Objects.isNull(configuration)) {
            builder.setConfiguration(configuration);
        }

        TaskDefinition taskDefinition = builder.build();

        taskSetManager.addTaskSet(location, taskDefinition);
    }
}
