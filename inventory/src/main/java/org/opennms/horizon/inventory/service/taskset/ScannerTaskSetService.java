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

import static org.opennms.horizon.inventory.service.taskset.TaskUtils.identityForAzureTask;
import static org.opennms.horizon.inventory.service.taskset.TaskUtils.identityForDiscoveryTask;
import static org.opennms.horizon.inventory.service.taskset.TaskUtils.identityForNodeScan;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.opennms.azure.contract.AzureScanRequest;
import org.opennms.horizon.inventory.dto.IpInterfaceDTO;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.inventory.mapper.NodeMapper;
import org.opennms.horizon.inventory.model.AzureCredential;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.taskset.api.TaskSetPublisher;
import org.opennms.icmp.contract.IcmpScannerRequest;
import org.opennms.node.scan.contract.NodeScanRequest;
import org.opennms.taskset.contract.TaskDefinition;
import org.opennms.taskset.contract.TaskType;
import org.springframework.stereotype.Component;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.protobuf.Any;

import lombok.RequiredArgsConstructor;



@Component
@RequiredArgsConstructor
public class ScannerTaskSetService {
    private final ThreadFactory threadFactory = new ThreadFactoryBuilder()
        .setNameFormat("send-taskset-for-scan-%d")
        .build();
    private final ExecutorService executorService = Executors.newFixedThreadPool(10, threadFactory);

    private final TaskSetPublisher taskSetPublisher;
    private final NodeMapper nodeMapper;

    public void sendAzureScannerTaskAsync(AzureCredential credential) {
        executorService.execute(() -> sendAzureScannerTask(credential));
    }

    public void sendNodeScannerTask(List<NodeDTO> nodes, String location, String tenantId) {

            List<TaskDefinition> tasks = nodes.stream().map(this::createNodeScanTask)
                .flatMap(Optional::stream).toList();
            if(!tasks.isEmpty()) {
                taskSetPublisher.publishNewTasks(tenantId, location, tasks);
            }
    }

    public Optional<TaskDefinition> getNodeScanTasks(Node node) {
        var nodeDto = nodeMapper.modelToDTO(node);
        return createNodeScanTask(nodeDto);
    }

    public void sendDiscoveryScannerTask(List<String> ipAddresses, String location, String tenantId, String requisitionName) {
        Optional<TaskDefinition> tasks = createDiscoveryTask(ipAddresses, location, requisitionName);
        tasks.ifPresent(taskDefinition -> taskSetPublisher.publishNewTasks(tenantId, location, List.of(taskDefinition)));
    }

    private Optional<TaskDefinition> createDiscoveryTask(List<String> ipAddresses, String location, String requisitionName) {
        Any configuration =
            Any.pack(IcmpScannerRequest.newBuilder()
                .addAllHost(ipAddresses)
                .setRetries(1)
                .setTimeout(2000)
                .setPacketsPerSeconds(1)
                .build());

        String taskId = identityForDiscoveryTask(ipAddresses.get(0), location, requisitionName);
        return Optional.of(TaskDefinition.newBuilder()
            .setType(TaskType.SCANNER)
            .setPluginName("Discovery")
            .setId(taskId)
            .setConfiguration(configuration)
            .setSchedule(TaskUtils.DEFAULT_SCHEDULE)
            .build());
    }

    private void sendAzureScannerTask(AzureCredential credential) {
        String tenantId = credential.getTenantId();
        String location = credential.getMonitoringLocation().getLocation();

        TaskDefinition task = addAzureScannerTask(credential);

        taskSetPublisher.publishNewTasks(tenantId, location, List.of(task));
    }

    private TaskDefinition addAzureScannerTask(AzureCredential credential) {
        Any configuration =
            Any.pack(AzureScanRequest.newBuilder()
                .setCredentialId(credential.getId())
                .setClientId(credential.getClientId())
                .setClientSecret(credential.getClientSecret())
                .setSubscriptionId(credential.getSubscriptionId())
                .setDirectoryId(credential.getDirectoryId())
                .setTimeoutMs(TaskUtils.AZURE_DEFAULT_TIMEOUT_MS)
                .setRetries(TaskUtils.AZURE_DEFAULT_RETRIES)
                .build());

        String taskId = identityForAzureTask("azure-scanner", String.valueOf(credential.getId()));
        return TaskDefinition.newBuilder()
            .setType(TaskType.SCANNER)
            .setPluginName("AZUREScanner")
            .setId(taskId)
            .setConfiguration(configuration)
            .setSchedule(TaskUtils.DEFAULT_SCHEDULE)
            .build();
    }


    private Optional<TaskDefinition> createNodeScanTask(NodeDTO node) {
        Optional<IpInterfaceDTO> ipInterface = node.getIpInterfacesList().stream()
            .filter(IpInterfaceDTO::getSnmpPrimary).findFirst()
            .or(() -> node.getIpInterfacesList().stream().findAny());
        return ipInterface.map(ip -> {
            String taskId = identityForNodeScan(node.getId());
            Any taskConfig = Any.pack(NodeScanRequest.newBuilder()
                .setNodeId(node.getId())
                .setPrimaryIp(ip.getIpAddress()).build());

            return TaskDefinition.newBuilder()
                .setType(TaskType.SCANNER)
                .setPluginName("NodeScanner")
                .setId(taskId)
                .setNodeId(node.getId())
                .setConfiguration(taskConfig)
                .setSchedule(TaskUtils.DEFAULT_SCHEDULE)
                .build();
        }).or(Optional::empty);
    }
}
