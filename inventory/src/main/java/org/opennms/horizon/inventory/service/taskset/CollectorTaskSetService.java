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
import org.opennms.azure.contract.AzureCollectorRequest;
import org.opennms.horizon.azure.api.AzureScanItem;
import org.opennms.horizon.inventory.model.AzureCredential;
import org.opennms.horizon.inventory.model.IpInterface;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.opennms.horizon.snmp.api.SnmpConfiguration;
import org.opennms.horizon.snmp.api.Version;
import org.opennms.snmp.contract.SnmpCollectorRequest;
import org.opennms.taskset.contract.MonitorType;
import org.opennms.taskset.contract.TaskDefinition;
import org.opennms.taskset.contract.TaskType;
import org.opennms.taskset.service.api.TaskSetPublisher;
import org.springframework.stereotype.Component;

import java.util.Arrays;

import static org.opennms.horizon.inventory.service.taskset.TaskUtils.identityForAzureTask;
import static org.opennms.horizon.inventory.service.taskset.TaskUtils.identityForIpTask;

@Component
@RequiredArgsConstructor
public class CollectorTaskSetService {
    private final TaskSetPublisher taskSetPublisher;

    public void sendCollectorTask(String location, MonitorType monitorType, IpInterface ipInterface, long nodeId) {
        String tenantId = ipInterface.getTenantId();
        var task = addCollectorTask(monitorType, ipInterface, nodeId);
        if (task != null) {
            taskSetPublisher.publishNewTasks(tenantId, location, Arrays.asList(task));
        }
    }

    public void sendAzureCollectorTasks(AzureCredential credential, AzureScanItem item, String ipAddress, long nodeId) {
        String tenantId = credential.getTenantId();
        String location = credential.getMonitoringLocation().getLocation();

        TaskDefinition task = addAzureCollectorTask(credential, item, ipAddress, nodeId);
        taskSetPublisher.publishNewTasks(tenantId, location, Arrays.asList(task));
    }

    private TaskDefinition addCollectorTask(MonitorType monitorType, IpInterface ipInterface, long nodeId) {
        String monitorTypeValue = monitorType.getValueDescriptor().getName();
        String ipAddress = InetAddressUtils.toIpAddrString(ipInterface.getIpAddress());

        String name = String.format("%s-collector", monitorTypeValue.toLowerCase());
        String pluginName = String.format("%sCollector", monitorTypeValue);
        TaskDefinition taskDefinition = null;

        switch (monitorType) {
            case SNMP: {
                Any configuration =
                    Any.pack(SnmpCollectorRequest.newBuilder()
                        .setHost(ipAddress)
                        .setAgentConfig(SnmpConfiguration.newBuilder()
                            .setAddress(ipAddress)
                            .setVersion(Version.v2)
                            .setTimeout(30000).build())
                        .setNodeId(nodeId)
                        .build());

                String taskId = identityForIpTask(ipAddress, name);
                TaskDefinition.Builder builder =
                    TaskDefinition.newBuilder()
                        .setType(TaskType.COLLECTOR)
                        .setPluginName(pluginName)
                        .setNodeId(nodeId)
                        .setId(taskId)
                        .setConfiguration(configuration)
                        .setSchedule(TaskUtils.DEFAULT_SCHEDULE);
                taskDefinition = builder.build();
                break;
            }
        }
        return taskDefinition;
    }

    private TaskDefinition addAzureCollectorTask(AzureCredential credential, AzureScanItem scanItem, String ipAddress, long nodeId) {
        Any configuration =
            Any.pack(AzureCollectorRequest.newBuilder()
                .setResource(scanItem.getName())
                .setResourceGroup(scanItem.getResourceGroup())
                .setHost(ipAddress) // dummy address to allow metrics to be added
                .setClientId(credential.getClientId())
                .setClientSecret(credential.getClientSecret())
                .setSubscriptionId(credential.getSubscriptionId())
                .setDirectoryId(credential.getDirectoryId())
                .setTimeoutMs(TaskUtils.AZURE_DEFAULT_TIMEOUT_MS)
                .setRetries(TaskUtils.AZURE_DEFAULT_RETRIES)
                .build());

        String name = String.join("-", "azure", "collector", scanItem.getId());
        String taskId = identityForAzureTask(name);
        return TaskDefinition.newBuilder()
            .setType(TaskType.COLLECTOR)
            .setPluginName("AZURECollector")
            .setNodeId(nodeId)
            .setId(taskId)
            .setConfiguration(configuration)
            .setSchedule(TaskUtils.AZURE_COLLECTOR_SCHEDULE)
            .build();
    }
}
