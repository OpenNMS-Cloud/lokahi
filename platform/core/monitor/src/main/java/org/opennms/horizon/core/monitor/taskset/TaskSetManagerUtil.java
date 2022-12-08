/*
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
 */

package org.opennms.horizon.core.monitor.taskset;

import com.google.protobuf.Any;
import org.opennms.horizon.taskset.manager.TaskSetManager;
import org.opennms.icmp.contract.IcmpMonitorRequest;
import org.opennms.snmp.contract.SnmpDetectorRequest;
import org.opennms.snmp.contract.SnmpMonitorRequest;
import org.opennms.taskset.contract.TaskDefinition;
import org.opennms.taskset.contract.TaskType;

import java.net.InetAddress;

public class TaskSetManagerUtil {

    private MonitorTaskSetIdentityUtil monitorTaskSetIdentityUtil = new MonitorTaskSetIdentityUtil();

    private TaskSetManager taskSetManager;

    public TaskSetManagerUtil(TaskSetManager taskSetManager) {
        this.taskSetManager = taskSetManager;
    }

    public void addEchoTask(String tenantId, String location, InetAddress inetAddress, String name, TaskType taskType, String pluginName, String schedule, IcmpMonitorRequest echoRequest) {
        String taskId = monitorTaskSetIdentityUtil.identityForIpTask(inetAddress.getHostAddress(), name);

        TaskDefinition.Builder builder =
            TaskDefinition.newBuilder()
                .setType(taskType)
                .setPluginName(pluginName)
                .setSchedule(schedule)
                .setId(taskId)
                .setConfiguration(Any.pack(echoRequest))
                ;

        TaskDefinition taskDefinition = builder.build();

        taskSetManager.addTaskSet(tenantId, location, taskDefinition);
    }

    public void addSnmpTask(String tenantId, String location, InetAddress inetAddress, String name, TaskType taskType, String pluginName, String schedule, SnmpMonitorRequest snmpMonitorRequest) {
        String taskId = monitorTaskSetIdentityUtil.identityForIpTask(inetAddress.getHostAddress(), name);

        TaskDefinition.Builder builder =
            TaskDefinition.newBuilder()
                .setType(taskType)
                .setPluginName(pluginName)
                .setSchedule(schedule)
                .setId(taskId)
                .setConfiguration(Any.pack(snmpMonitorRequest))
                ;

        TaskDefinition taskDefinition = builder.build();

        taskSetManager.addTaskSet(tenantId, location, taskDefinition);
    }

    public void addSnmpTask(String tenantId, String location, InetAddress inetAddress, String name, TaskType taskType, String pluginName, SnmpDetectorRequest snmpDetectorRequest) {
        String taskId = monitorTaskSetIdentityUtil.identityForIpTask(inetAddress.getHostAddress(), name);

        TaskDefinition.Builder builder =
            TaskDefinition.newBuilder()
                .setType(taskType)
                .setPluginName(pluginName)
                .setId(taskId)
                .setConfiguration(Any.pack(snmpDetectorRequest))
            ;

        TaskDefinition taskDefinition = builder.build();

        taskSetManager.addTaskSet(tenantId, location, taskDefinition);
    }
}
