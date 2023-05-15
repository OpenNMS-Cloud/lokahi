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
import org.opennms.azure.contract.AzureMonitorRequest;
import org.opennms.horizon.azure.api.AzureScanItem;
import org.opennms.horizon.inventory.model.IpInterface;
import org.opennms.horizon.inventory.model.discovery.active.AzureActiveDiscovery;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.opennms.horizon.snmp.api.SnmpConfiguration;
import org.opennms.icmp.contract.IcmpMonitorRequest;
import org.opennms.snmp.contract.SnmpMonitorRequest;
import org.opennms.taskset.contract.MonitorType;
import org.opennms.taskset.contract.TaskDefinition;
import org.opennms.taskset.contract.TaskType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static org.opennms.horizon.inventory.service.taskset.TaskUtils.identityForAzureTask;
import static org.opennms.horizon.inventory.service.taskset.TaskUtils.identityForIpTask;

@Component
public class MonitorTaskSetService {

    private static final Logger log = LoggerFactory.getLogger(MonitorTaskSetService.class);

    public TaskDefinition getMonitorTask(MonitorType monitorType, IpInterface ipInterface, long nodeId, SnmpConfiguration snmpConfiguration) {

        String monitorTypeValue = monitorType.getValueDescriptor().getName();
        String ipAddress = InetAddressUtils.toIpAddrString(ipInterface.getIpAddress());

        String name = String.format("%s-monitor", monitorTypeValue.toLowerCase());
        String pluginName = String.format("%sMonitor", monitorTypeValue);
        TaskDefinition taskDefinition = null;
        Any configuration = null;

        switch (monitorType) {
            case ICMP -> configuration =
                Any.pack(IcmpMonitorRequest.newBuilder()
                    .setHost(ipAddress)
                    .setTimeout(TaskUtils.ICMP_DEFAULT_TIMEOUT_MS)
                    .setDscp(TaskUtils.ICMP_DEFAULT_DSCP)
                    .setAllowFragmentation(TaskUtils.ICMP_DEFAULT_ALLOW_FRAGMENTATION)
                    .setPacketSize(TaskUtils.ICMP_DEFAULT_PACKET_SIZE)
                    .setRetries(TaskUtils.ICMP_DEFAULT_RETRIES)
                    .build());
            case SNMP -> {
                var requestBuilder = SnmpMonitorRequest.newBuilder()
                    .setHost(ipAddress);
                if (snmpConfiguration != null) {
                    requestBuilder.setAgentConfig(snmpConfiguration);
                }
                configuration = Any.pack(requestBuilder.build());
            }
            case UNRECOGNIZED -> log.warn("Unrecognized monitor type");
            case UNKNOWN -> log.warn("Unknown monitor type");
        }

        if (configuration != null) {
            String taskId = identityForIpTask(nodeId, ipAddress, name);
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

    public TaskDefinition addAzureMonitorTask(AzureActiveDiscovery discovery, AzureScanItem scanItem, long nodeId) {

        Any configuration =
            Any.pack(AzureMonitorRequest.newBuilder()
                .setResource(scanItem.getName())
                .setResourceGroup(scanItem.getResourceGroup())
                .setClientId(discovery.getClientId())
                .setClientSecret(discovery.getClientSecret())
                .setSubscriptionId(discovery.getSubscriptionId())
                .setDirectoryId(discovery.getDirectoryId())
                .setTimeoutMs(TaskUtils.AZURE_DEFAULT_TIMEOUT_MS)
                .setRetries(TaskUtils.AZURE_DEFAULT_RETRIES)
                .build());

        String name = String.join("-", "azure", "monitor", scanItem.getId());
        String id = String.join("-", String.valueOf(discovery.getId()), String.valueOf(nodeId));
        String taskId = identityForAzureTask(name, id);
        return TaskDefinition.newBuilder()
            .setType(TaskType.MONITOR)
            .setPluginName("AZUREMonitor")
            .setNodeId(nodeId)
            .setId(taskId)
            .setConfiguration(configuration)
            .setSchedule(TaskUtils.AZURE_MONITOR_SCHEDULE)
            .build();
    }


}
