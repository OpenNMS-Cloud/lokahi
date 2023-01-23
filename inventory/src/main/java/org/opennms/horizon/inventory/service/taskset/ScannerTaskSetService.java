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

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;

import org.opennms.azure.contract.AzureScanRequest;
import org.opennms.horizon.inventory.dto.IpInterfaceDTO;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.inventory.model.AzureCredential;
import org.opennms.node.scan.contract.NodeScanRequest;
import org.opennms.taskset.contract.TaskDefinition;
import org.opennms.taskset.contract.TaskType;
import org.opennms.taskset.service.api.TaskSetPublisher;
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

    public void sendAzureScannerTaskAsync(AzureCredential credential) {
        executorService.execute(() -> sendAzureScannerTask(credential));
    }

    public void sendNodeScannerTask(List<NodeDTO> nodes, String location, String tenantId) {
            List<TaskDefinition> tasks = nodes.stream().map(this::createNodeScanTask).collect(Collectors.toList());
            taskSetPublisher.publishNewTasks(tenantId, location, tasks);
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

        String taskId = identityForAzureTask("azure-scanner");
        return TaskDefinition.newBuilder()
            .setType(TaskType.SCANNER)
            .setPluginName("AZUREScanner")
            .setId(taskId)
            .setConfiguration(configuration)
            .setSchedule(TaskUtils.DEFAULT_SCHEDULE)
            .build();
    }

    private TaskDefinition createNodeScanTask(NodeDTO node) {
        List<String> ipAddresses = node.getIpInterfacesList().stream().map(IpInterfaceDTO::getIpAddress).collect(Collectors.toList());
        String taskId = "node-scan=" + node.getNodeLabel() + "-" + node.getId() ;
        Any taskConfig = Any.pack(NodeScanRequest.newBuilder()
            .setNodeId(node.getId())
            .addAllIpAddresses(ipAddresses).build());

        return TaskDefinition.newBuilder()
            .setType(TaskType.SCANNER)
            .setPluginName("NodeScanner")
            .setId(taskId)
            .setNodeId(node.getId())
            .setConfiguration(taskConfig)
            .setSchedule(TaskUtils.DEFAULT_SCHEDULE)
            .build();
    }
}
