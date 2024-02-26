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
package org.opennms.horizon.inventory.service.taskset;

import static org.opennms.horizon.inventory.service.taskset.TaskUtils.identityForAzureTask;
import static org.opennms.horizon.inventory.service.taskset.TaskUtils.identityForIpTask;

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

@Component
public class MonitorTaskSetService {

    private static final Logger log = LoggerFactory.getLogger(MonitorTaskSetService.class);

    public TaskDefinition getMonitorTask(
            MonitorType monitorType,
            IpInterface ipInterface,
            long nodeId,
            long monitoredServiceId,
            SnmpConfiguration snmpConfiguration) {

        String monitorTypeValue = monitorType.getValueDescriptor().getName();
        String ipAddress = InetAddressUtils.toIpAddrString(ipInterface.getIpAddress());

        String name = String.format("%s-monitor", monitorTypeValue.toLowerCase());
        String pluginName = String.format("%sMonitor", monitorTypeValue);
        TaskDefinition taskDefinition = null;
        Any configuration = null;

        switch (monitorType) {
            case ICMP -> configuration = Any.pack(IcmpMonitorRequest.newBuilder()
                    .setHost(ipAddress)
                    .setTimeout(TaskUtils.ICMP_DEFAULT_TIMEOUT_MS)
                    .setDscp(TaskUtils.ICMP_DEFAULT_DSCP)
                    .setAllowFragmentation(TaskUtils.ICMP_DEFAULT_ALLOW_FRAGMENTATION)
                    .setPacketSize(TaskUtils.ICMP_DEFAULT_PACKET_SIZE)
                    .setRetries(TaskUtils.ICMP_DEFAULT_RETRIES)
                    .build());
            case SNMP -> {
                var requestBuilder = SnmpMonitorRequest.newBuilder().setHost(ipAddress);
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
            TaskDefinition.Builder builder = TaskDefinition.newBuilder()
                    .setType(TaskType.MONITOR)
                    .setPluginName(pluginName)
                    .setNodeId(nodeId)
                    .setId(taskId)
                    .setMonitorServiceId(monitoredServiceId)
                    .setConfiguration(configuration)
                    .setSchedule(TaskUtils.DEFAULT_SCHEDULE);
            taskDefinition = builder.build();
        }
        return taskDefinition;
    }

    public TaskDefinition addAzureMonitorTask(AzureActiveDiscovery discovery, AzureScanItem scanItem, long nodeId) {

        Any configuration = Any.pack(AzureMonitorRequest.newBuilder()
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
