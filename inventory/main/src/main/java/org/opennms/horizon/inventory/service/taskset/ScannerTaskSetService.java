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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.protobuf.Any;
import lombok.RequiredArgsConstructor;
import org.apache.commons.net.util.SubnetUtils;
import org.opennms.azure.contract.AzureScanRequest;
import org.opennms.horizon.inventory.dto.IpInterfaceDTO;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.inventory.mapper.NodeMapper;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.model.discovery.active.AzureActiveDiscovery;
import org.opennms.horizon.inventory.service.SnmpConfigService;
import org.opennms.horizon.inventory.service.taskset.publisher.TaskSetPublisher;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.opennms.horizon.snmp.api.SnmpConfiguration;
import org.opennms.icmp.contract.IpRange;
import org.opennms.icmp.contract.PingSweepRequest;
import org.opennms.inventory.types.ServiceType;
import org.opennms.node.scan.contract.DetectRequest;
import org.opennms.node.scan.contract.NodeScanRequest;
import org.opennms.taskset.contract.TaskDefinition;
import org.opennms.taskset.contract.TaskType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static org.opennms.horizon.inventory.service.taskset.TaskUtils.DEFAULT_SCHEDULE_FOR_SCAN;
import static org.opennms.horizon.inventory.service.taskset.TaskUtils.identityForAzureTask;
import static org.opennms.horizon.inventory.service.taskset.TaskUtils.identityForDiscoveryTask;
import static org.opennms.horizon.inventory.service.taskset.TaskUtils.identityForNodeScan;



@Component
@RequiredArgsConstructor
public class ScannerTaskSetService {
    private static final Logger LOG = LoggerFactory.getLogger(ScannerTaskSetService.class);
    private final ThreadFactory threadFactory = new ThreadFactoryBuilder()
        .setNameFormat("send-taskset-for-scan-%d")
        .build();
    private final ExecutorService executorService = Executors.newFixedThreadPool(10, threadFactory);

    public static final String DISCOVERY_TASK_PLUGIN_NAME = "Discovery-Ping";
    private final TaskSetPublisher taskSetPublisher;
    private final NodeMapper nodeMapper;
    private final SnmpConfigService snmpConfigService;

    public void sendAzureScannerTaskAsync(AzureActiveDiscovery discovery) {
        executorService.execute(() -> sendAzureScannerTask(discovery));
    }

    public void sendNodeScannerTask(List<NodeDTO> nodes, Long locationId, String tenantId) {
        List<TaskDefinition> tasks = nodes.stream().map(node -> createNodeScanTask(node, locationId, new ArrayList<>()))
            .flatMap(Optional::stream).toList();
        if (!tasks.isEmpty()) {
            taskSetPublisher.publishNewTasks(tenantId, locationId, tasks);
        }
    }

    public void sendNodeScannerTask(Node node, Long locationId, List<SnmpConfiguration> snmpConfigs) {
        NodeDTO nodeDTO = nodeMapper.modelToDTO(node);
        sendNodeScannerTask(nodeDTO, locationId, snmpConfigs);
    }

    public void sendNodeScannerTask(NodeDTO node, Long locationId, List<SnmpConfiguration> snmpConfigs) {
        var taskDef = createNodeScanTask(node, locationId, snmpConfigs);
        taskDef.ifPresent(taskDefinition -> taskSetPublisher.publishNewTasks(node.getTenantId(), locationId, List.of(taskDefinition)));
    }

    public Optional<TaskDefinition> getNodeScanTasks(Node node) {
        var nodeDto = nodeMapper.modelToDTO(node);
        return createNodeScanTask(nodeDto, node.getMonitoringLocation().getId(), new ArrayList<>());
    }

    public void sendDiscoveryScannerTask(List<String> ipAddresses, Long locationId, String tenantId, long activeDiscoveryId) {
        executorService.execute(() -> createAndPublishTasks(ipAddresses, locationId, tenantId, activeDiscoveryId));
    }

    private void createAndPublishTasks(List<String> ipAddresses, Long locationId, String tenantId, long activeDiscoveryId) {
        Optional<TaskDefinition> tasks = createDiscoveryTask(ipAddresses, locationId, activeDiscoveryId);
        tasks.ifPresent(taskDefinition -> taskSetPublisher.publishNewTasks(tenantId, locationId, List.of(taskDefinition)));
    }

    public void removeDiscoveryScanTask(Long locationId, long activeDiscoveryId, String tenantId) {
        String taskId = identityForDiscoveryTask(locationId, activeDiscoveryId);
        var taskDef = TaskDefinition.newBuilder()
            .setType(TaskType.SCANNER)
            .setPluginName(DISCOVERY_TASK_PLUGIN_NAME)
            .setId(taskId).build();

        taskSetPublisher.publishTaskDeletion(tenantId, locationId, List.of(taskDef));
    }

    Optional<TaskDefinition> createDiscoveryTask(List<String> ipAddresses, Long locationId, long activeDiscoveryId) {

        var ipRanges = new ArrayList<IpRange>();
        ipAddresses.forEach(ipAddressDTO -> {

            ipAddressDTO = ipAddressDTO.trim();
            if (ipAddressDTO.contains("-")) {
                var range = ipAddressDTO.split("-", 2);
                try {
                    var ipRangeBuilder = IpRange.newBuilder();
                    var begin = InetAddress.getByName(range[0].trim());
                    var end = InetAddress.getByName(range[1].trim());
                    ipRangeBuilder.setBegin(InetAddressUtils.str(begin));
                    ipRangeBuilder.setEnd(InetAddressUtils.str(end));
                    ipRanges.add(ipRangeBuilder.build());
                } catch (Exception e) {
                    LOG.error("Not able to parse IP range from {}", ipAddressDTO);
                }
            } else if (ipAddressDTO.contains("/")) {
                try {
                    var utils = new SubnetUtils(ipAddressDTO);
                    utils.setInclusiveHostCount(true);
                    var info = utils.getInfo();
                    var ipRangeBuilder = IpRange.newBuilder();
                    ipRangeBuilder.setBegin(info.getLowAddress());
                    ipRangeBuilder.setEnd(info.getHighAddress());
                    ipRanges.add(ipRangeBuilder.build());
                } catch (Exception e) {
                    LOG.error("Exception while parsing cidr notation {}", ipAddressDTO, e);
                }

            } else {
                // Assume it's only one IpAddress
                try {
                    var ipRangeBuilder = IpRange.newBuilder();
                    var begin = InetAddress.getByName(ipAddressDTO);
                    ipRangeBuilder.setBegin(InetAddressUtils.str(begin));
                    ipRangeBuilder.setEnd(InetAddressUtils.str(begin));
                    ipRanges.add(ipRangeBuilder.build());
                } catch (UnknownHostException e) {
                    LOG.error("Not able to parse IPAddress from {}", ipAddressDTO);
                }
            }
        });

        if (ipRanges.isEmpty()) {
            throw new IllegalArgumentException("No valid Ip ranges specified");
        }
        Any configuration = Any.pack(PingSweepRequest.newBuilder()
                .addAllIpRange(ipRanges)
                .setRetries(PingConstants.DEFAULT_RETRIES)
                .setTimeout(PingConstants.DEFAULT_TIMEOUT)
                .setPacketsPerSecond(PingConstants.DEFAULT_PACKETS_PER_SECOND)
                .setPacketSize(PingConstants.DEFAULT_PACKET_SIZE)
                .setActiveDiscoveryId(activeDiscoveryId)
                .build());

        String taskId = identityForDiscoveryTask(locationId, activeDiscoveryId);
        return Optional.of(TaskDefinition.newBuilder()
            .setType(TaskType.SCANNER)
            .setPluginName(DISCOVERY_TASK_PLUGIN_NAME)
            .setId(taskId)
            .setConfiguration(configuration)
            .setSchedule(DEFAULT_SCHEDULE_FOR_SCAN)
            .build());
    }

    private void sendAzureScannerTask(AzureActiveDiscovery discovery) {
        String tenantId = discovery.getTenantId();
        Long locationId = discovery.getLocationId();

        TaskDefinition task = addAzureScannerTask(discovery);

        taskSetPublisher.publishNewTasks(tenantId, locationId, List.of(task));
    }

    private TaskDefinition addAzureScannerTask(AzureActiveDiscovery discovery) {
        Any configuration =
            Any.pack(AzureScanRequest.newBuilder()
                .setActiveDiscoveryId(discovery.getId())
                .setClientId(discovery.getClientId())
                .setClientSecret(discovery.getClientSecret())
                .setSubscriptionId(discovery.getSubscriptionId())
                .setDirectoryId(discovery.getDirectoryId())
                .setTimeoutMs(TaskUtils.AZURE_DEFAULT_TIMEOUT_MS)
                .setRetries(TaskUtils.AZURE_DEFAULT_RETRIES)
                .build());

        String taskId = identityForAzureTask("azure-scanner", String.valueOf(discovery.getId()));
        return TaskDefinition.newBuilder()
            .setType(TaskType.SCANNER)
            .setPluginName("AZUREScanner")
            .setId(taskId)
            .setConfiguration(configuration)
            .setSchedule(DEFAULT_SCHEDULE_FOR_SCAN)
            .build();
    }

    private Optional<TaskDefinition> createNodeScanTask(NodeDTO node, Long locationId, List<SnmpConfiguration> snmpConfigs) {
        Optional<IpInterfaceDTO> ipInterface = node.getIpInterfacesList().stream()
            .filter(IpInterfaceDTO::getSnmpPrimary).findFirst()
            .or(() -> node.getIpInterfacesList().stream().findAny());
        if (ipInterface.isPresent()) {
            var snmpConfig = snmpConfigService.getSnmpConfig(node.getTenantId(),
                locationId, InetAddressUtils.getInetAddress(ipInterface.get().getIpAddress()));
            snmpConfig.ifPresent(snmpConfigs::add);
        }
        return ipInterface.map(ip -> {
            String taskId = identityForNodeScan(node.getId());

            Any taskConfig = Any.pack(NodeScanRequest.newBuilder()
                .setNodeId(node.getId())
                .setPrimaryIp(ip.getIpAddress())
                .addDetector(DetectRequest.newBuilder().setService(ServiceType.SNMP).build())
                .addDetector(DetectRequest.newBuilder().setService(ServiceType.ICMP).build())
                .addAllSnmpConfigs(snmpConfigs).build());

            return TaskDefinition.newBuilder()
                .setType(TaskType.SCANNER)
                .setPluginName("NodeScanner")
                .setId(taskId)
                .setNodeId(node.getId())
                .setConfiguration(taskConfig)
                .setSchedule(DEFAULT_SCHEDULE_FOR_SCAN)
                .build();
        }).or(Optional::empty);
    }
}
