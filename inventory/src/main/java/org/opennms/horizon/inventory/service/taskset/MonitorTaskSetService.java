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

package org.opennms.horizon.inventory.service.taskset;

import com.google.protobuf.Any;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.inventory.model.IpInterface;
import org.opennms.icmp.contract.IcmpMonitorRequest;
import org.opennms.snmp.contract.SnmpMonitorRequest;
import org.opennms.taskset.contract.MonitorType;
import org.opennms.taskset.contract.TaskDefinition;
import org.opennms.taskset.contract.TaskType;
import org.opennms.taskset.service.api.TaskSetPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

import static org.opennms.horizon.inventory.service.taskset.TaskUtils.identityForIpTask;

@Component
@RequiredArgsConstructor
public class MonitorTaskSetService {

    private static final Logger log = LoggerFactory.getLogger(MonitorTaskSetService.class);

    private final TaskSetPublisher taskSetPublisher;

    public void sendMonitorTask(String location, MonitorType monitorType, IpInterface ipInterface, long nodeId) {
        String tenantId = ipInterface.getTenantId();

        var task = addMonitorTask(monitorType, ipInterface, nodeId);
        if (task != null) {
            taskSetPublisher.publishNewTasks(tenantId, location, Arrays.asList(task));
        }
    }

    private TaskDefinition addMonitorTask(MonitorType monitorType, IpInterface ipInterface, long nodeId) {

        String monitorTypeValue = monitorType.getValueDescriptor().getName();
        String ipAddress = ipInterface.getIpAddress().getAddress();

        String name = String.format("%s-monitor", monitorTypeValue.toLowerCase());
        String pluginName = String.format("%sMonitor", monitorTypeValue);
        TaskDefinition taskDefinition = null;
        Any configuration = null;

        switch (monitorType) {
            case ICMP: {
                configuration =
                    Any.pack(IcmpMonitorRequest.newBuilder()
                        .setHost(ipAddress)
                        .setTimeout(TaskUtils.Icmp.DEFAULT_TIMEOUT)
                        .setDscp(TaskUtils.Icmp.DEFAULT_DSCP)
                        .setAllowFragmentation(TaskUtils.Icmp.DEFAULT_ALLOW_FRAGMENTATION)
                        .setPacketSize(TaskUtils.Icmp.DEFAULT_PACKET_SIZE)
                        .setRetries(TaskUtils.Icmp.DEFAULT_RETRIES)
                        .build());

                break;
            }
            case SNMP: {
                configuration =
                    Any.pack(SnmpMonitorRequest.newBuilder()
                        .setHost(ipAddress)
                        .setTimeout(TaskUtils.Snmp.DEFAULT_TIMEOUT)
                        .setRetries(TaskUtils.Snmp.DEFAULT_RETRIES)
                        .build());
                break;
            }
            case UNRECOGNIZED: {
                log.warn("Unrecognized monitor type");
                break;
            }
            case UNKNOWN: {
                log.warn("Unknown monitor type");
                break;
            }
        }

        if (configuration != null) {
            String taskId = identityForIpTask(ipAddress, name);
            TaskDefinition.Builder builder =
                TaskDefinition.newBuilder()
                    .setType(TaskType.MONITOR)
                    .setPluginName(pluginName)
                    .setNodeId(nodeId)
                    .setId(taskId)
                    .setConfiguration(configuration)
                    .setSchedule(TaskUtils.DEFAULT_SCHEDULE);
            taskDefinition = builder.build();
        }
        return taskDefinition;
    }

}
