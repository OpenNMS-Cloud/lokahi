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
import org.opennms.azure.contract.AzureCollectorResourcesRequest;
import org.opennms.horizon.azure.api.AzureScanItem;
import org.opennms.horizon.inventory.model.IpInterface;
import org.opennms.horizon.inventory.model.SnmpInterface;
import org.opennms.horizon.inventory.model.discovery.active.AzureActiveDiscovery;
import org.opennms.horizon.inventory.repository.NodeRepository;
import org.opennms.horizon.shared.azure.http.AzureHttpClient;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.opennms.horizon.snmp.api.SnmpConfiguration;
import org.opennms.snmp.contract.SnmpCollectorRequest;
import org.opennms.snmp.contract.SnmpInterfaceElement;
import org.opennms.snmp.contract.SnmpInterfaceElement.Builder;
import org.opennms.taskset.contract.MonitorType;
import org.opennms.taskset.contract.TaskDefinition;
import org.opennms.taskset.contract.TaskType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.opennms.horizon.inventory.service.taskset.TaskUtils.identityForAzureTask;
import static org.opennms.horizon.inventory.service.taskset.TaskUtils.identityForIpTask;

@Component
@RequiredArgsConstructor
public class CollectorTaskSetService {

    private final NodeRepository nodeRepository;

    public TaskDefinition getCollectorTask(MonitorType monitorType, IpInterface ipInterface, long nodeId,
                                           SnmpConfiguration snmpConfiguration) {
        if (MonitorType.SNMP.equals(monitorType)) {
            return addSnmpCollectorTask(ipInterface, nodeId, snmpConfiguration);
        }
        return null;
    }

    public TaskDefinition addSnmpCollectorTask(IpInterface ipInterface, long nodeId,
                                           SnmpConfiguration snmpConfiguration) {
        String monitorTypeValue = MonitorType.SNMP.name();
        String ipAddress = InetAddressUtils.toIpAddrString(ipInterface.getIpAddress());

        List<SnmpInterface> snmpInterfaces = getSnmpInterfaces(nodeId);
        List<IpInterface> ipInterfaces = getIpInterfaces(nodeId);

        Map<Integer, IpInterface> ifIndexMap = new HashMap<>();
        for (IpInterface anInterface : ipInterfaces) {
            ifIndexMap.put(ipInterface.getIfIndex(), anInterface);
        }
        List<SnmpInterfaceElement> snmpInterfaceElements = new ArrayList<>();
        for (SnmpInterface snmpInterface : snmpInterfaces) {
            IpInterface ipInterfaceDTO = ifIndexMap.get(snmpInterface.getIfIndex());
            String ifName = snmpInterface.getIfName();
            if (ifName != null) {
                Builder elementBuilder = SnmpInterfaceElement.newBuilder().setIfIndex(snmpInterface.getIfIndex()).setIfName(ifName);
                if (ipInterfaceDTO != null) {
                    elementBuilder.setIpAddress(InetAddressUtils.toIpAddrString(ipInterfaceDTO.getIpAddress()));
                }
                snmpInterfaceElements.add(elementBuilder.build());
            }
        }

        String name = String.format("%s-collector", monitorTypeValue.toLowerCase());
        String pluginName = String.format("%sCollector", monitorTypeValue);

        SnmpCollectorRequest.Builder requestBuilder = SnmpCollectorRequest.newBuilder()
            .setHost(ipAddress)
            .setNodeId(nodeId)
            .addAllSnmpInterface(snmpInterfaceElements);
        if (snmpConfiguration != null) {
            requestBuilder.setAgentConfig(snmpConfiguration);
        }
        Any configuration = Any.pack(requestBuilder.build());

        String taskId = identityForIpTask(nodeId, ipAddress, name);
        TaskDefinition.Builder builder =
            TaskDefinition.newBuilder()
                .setType(TaskType.COLLECTOR)
                .setPluginName(pluginName)
                .setNodeId(nodeId)
                .setId(taskId)
                .setConfiguration(configuration)
                .setSchedule(TaskUtils.DEFAULT_SCHEDULE);
        return builder.build();
    }


    public TaskDefinition addAzureCollectorTask(AzureActiveDiscovery discovery, AzureScanItem scanItem, long nodeId) {
        // uniq interface names
        Set<String> targetInterfaceNames = new HashSet<>();
        Set<String> publicIpNames = new HashSet<>();
        for (final var interfaceItem : scanItem.getNetworkInterfaceItemsList()) {
            // Azure only provide traffic data for IP with public IP
            if(!interfaceItem.hasPublicIpAddress()){
                continue;
            }
            publicIpNames.add(interfaceItem.getPublicIpAddress().getName());
            targetInterfaceNames.add(interfaceItem.getInterfaceName());
        }

        Any configuration =
            Any.pack(AzureCollectorRequest.newBuilder()
                .setResource(scanItem.getName())
                .setResourceGroup(scanItem.getResourceGroup())
                .setClientId(discovery.getClientId())
                .setClientSecret(discovery.getClientSecret())
                .setSubscriptionId(discovery.getSubscriptionId())
                .setDirectoryId(discovery.getDirectoryId())
                .setTimeoutMs(TaskUtils.AZURE_DEFAULT_TIMEOUT_MS)
                .setRetries(TaskUtils.AZURE_DEFAULT_RETRIES)
                .addAllCollectorResources(targetInterfaceNames.stream().map(name ->
                    AzureCollectorResourcesRequest.newBuilder()
                        .setType(AzureHttpClient.ResourcesType.NETWORK_INTERFACES.getMetricName())
                        .setResource(name)
                        .build()).toList())
                .addAllCollectorResources(publicIpNames.stream().map(name ->
                    AzureCollectorResourcesRequest.newBuilder()
                        .setType(AzureHttpClient.ResourcesType.PUBLIC_IP_ADDRESSES.getMetricName())
                        .setResource(name)
                        .build()).toList())
                .build());

        String name = String.join("-", "azure", "collector", scanItem.getId());
        String id = String.join("-", String.valueOf(discovery.getId()), String.valueOf(nodeId));
        String taskId = identityForAzureTask(name, id);
        return TaskDefinition.newBuilder()
            .setType(TaskType.COLLECTOR)
            .setPluginName("AZURECollector")
            .setNodeId(nodeId)
            .setId(taskId)
            .setConfiguration(configuration)
            .setSchedule(TaskUtils.AZURE_COLLECTOR_SCHEDULE)
            .build();
    }

    List<SnmpInterface> getSnmpInterfaces(long nodeId) {
        var optional = nodeRepository.findById(nodeId);
        if (optional.isEmpty()) {
            return new ArrayList<>();
        } else {
            return optional.get().getSnmpInterfaces();
        }
    }

    List<IpInterface> getIpInterfaces(long nodeId) {
        var optional = nodeRepository.findById(nodeId);
        if (optional.isEmpty()) {
            return new ArrayList<>();
        } else {
            return optional.get().getIpInterfaces();
        }
    }
}
