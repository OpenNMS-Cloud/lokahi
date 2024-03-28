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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.opennms.azure.contract.AzureCollectorRequest;
import org.opennms.azure.contract.AzureCollectorResourcesRequest;
import org.opennms.horizon.azure.api.AzureScanItem;
import org.opennms.horizon.inventory.model.IpInterface;
import org.opennms.horizon.inventory.model.SnmpInterface;
import org.opennms.horizon.inventory.model.discovery.active.AzureActiveDiscovery;
import org.opennms.horizon.inventory.repository.IpInterfaceRepository;
import org.opennms.horizon.inventory.repository.NodeRepository;
import org.opennms.horizon.inventory.repository.SnmpInterfaceRepository;
import org.opennms.horizon.inventory.snmp.SnmpCollectorConfig;
import org.opennms.horizon.shared.azure.http.AzureHttpClient;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.opennms.horizon.snmp.api.SnmpConfiguration;
import org.opennms.snmp.contract.SnmpCollectorPart;
import org.opennms.snmp.contract.SnmpCollectorRequest;
import org.opennms.snmp.contract.SnmpInterfaceElement;
import org.opennms.snmp.contract.SnmpInterfaceElement.Builder;
import org.opennms.taskset.contract.MonitorType;
import org.opennms.taskset.contract.TaskDefinition;
import org.opennms.taskset.contract.TaskType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CollectorTaskSetService {

    private final NodeRepository nodeRepository;
    private final IpInterfaceRepository ipInterfaceRepository;
    private final SnmpInterfaceRepository snmpInterfaceRepository;
    private final SnmpCollectorConfig snmpCollectorConfig;

    public TaskDefinition getCollectorTask(
            MonitorType monitorType, IpInterface ipInterface, long nodeId, SnmpConfiguration snmpConfiguration) {
        if (MonitorType.SNMP.equals(monitorType)) {
            return addSnmpCollectorTask(ipInterface, nodeId, snmpConfiguration);
        }
        return null;
    }

    public TaskDefinition addSnmpCollectorTask(
            IpInterface ipInterface, long nodeId, SnmpConfiguration snmpConfiguration) {
        String monitorTypeValue = MonitorType.SNMP.name();
        final var node = this.nodeRepository.getReferenceById(nodeId);
        String ipAddress = InetAddressUtils.toIpAddrString(ipInterface.getIpAddress());

        List<SnmpInterface> snmpInterfaces = getSnmpInterfaces(nodeId);
        List<IpInterface> ipInterfaces = getIpInterfaces(nodeId);

        Map<Integer, IpInterface> ifIndexMap = new HashMap<>();
        for (IpInterface anInterface : ipInterfaces) {
            ifIndexMap.put(ipInterface.getIfIndex(), anInterface);
        }

        // TODO LOK-2402: Remove the simple interface type and replace it with data from the collection parts
        // below?
        List<SnmpInterfaceElement> snmpInterfaceElements = new ArrayList<>();
        for (SnmpInterface snmpInterface : snmpInterfaces) {
            IpInterface ipInterfaceDTO = ifIndexMap.get(snmpInterface.getIfIndex());
            String ifName = snmpInterface.getIfName();
            if (ifName != null) {
                Builder elementBuilder = SnmpInterfaceElement.newBuilder()
                        .setIfIndex(snmpInterface.getIfIndex())
                        .setIfName(ifName);
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

        // Find all parts to collect for the systemObjectID we know about this node
        this.snmpCollectorConfig.findMatchingParts(node.getObjectId()).forEach(part -> {
            final var partBuilder = SnmpCollectorPart.newBuilder();

            part.asScalar().ifPresent(scalarPart -> {
                final var scalarBuilder = SnmpCollectorPart.Scalar.newBuilder();

                scalarPart.getElements().stream()
                        .map(element -> SnmpCollectorPart.Element.newBuilder()
                                .setOid(element.getOid())
                                .setAlias(element.getAlias())
                                .setType(element.getType()))
                        .forEach(scalarBuilder::addElement);

                partBuilder.setScalar(scalarBuilder);
            });

            part.asTable().ifPresent(tablePart -> {
                final var tableBuilder = SnmpCollectorPart.Table.newBuilder().setInstance(tablePart.getInstance());

                if (tablePart.getPersistFilterExpr() != null) {
                    tableBuilder.setPersistFilterExpr(tablePart.getPersistFilterExpr());
                }

                tablePart.getColumns().stream()
                        .map(column -> SnmpCollectorPart.Element.newBuilder()
                                .setOid(column.getOid())
                                .setAlias(column.getAlias())
                                .setType(column.getType()))
                        .forEach(tableBuilder::addElement);

                partBuilder.setTable(tableBuilder);
            });

            requestBuilder.addPart(partBuilder);
        });

        Any configuration = Any.pack(requestBuilder.build());

        String taskId = identityForIpTask(nodeId, ipAddress, name);
        TaskDefinition.Builder builder = TaskDefinition.newBuilder()
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
            if (!interfaceItem.hasPublicIpAddress()) {
                continue;
            }
            publicIpNames.add(interfaceItem.getPublicIpAddress().getName());
            targetInterfaceNames.add(interfaceItem.getInterfaceName());
        }

        Any configuration = Any.pack(AzureCollectorRequest.newBuilder()
                .setResource(scanItem.getName())
                .setResourceGroup(scanItem.getResourceGroup())
                .setClientId(discovery.getClientId())
                .setClientSecret(discovery.getClientSecret())
                .setSubscriptionId(discovery.getSubscriptionId())
                .setDirectoryId(discovery.getDirectoryId())
                .setTimeoutMs(TaskUtils.AZURE_DEFAULT_TIMEOUT_MS)
                .setRetries(TaskUtils.AZURE_DEFAULT_RETRIES)
                .addAllCollectorResources(targetInterfaceNames.stream()
                        .map(name -> AzureCollectorResourcesRequest.newBuilder()
                                .setType(AzureHttpClient.ResourcesType.NETWORK_INTERFACES.getMetricName())
                                .setResource(name)
                                .build())
                        .toList())
                .addAllCollectorResources(publicIpNames.stream()
                        .map(name -> AzureCollectorResourcesRequest.newBuilder()
                                .setType(AzureHttpClient.ResourcesType.PUBLIC_IP_ADDRESSES.getMetricName())
                                .setResource(name)
                                .build())
                        .toList())
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
        return snmpInterfaceRepository.findByNodeId(nodeId);
    }

    List<IpInterface> getIpInterfaces(long nodeId) {
        return ipInterfaceRepository.findByNodeId(nodeId);
    }
}
