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
import org.opennms.azure.contract.AzureScanRequest;
import org.opennms.horizon.inventory.model.AzureCredential;
import org.opennms.taskset.contract.TaskDefinition;
import org.opennms.taskset.contract.TaskType;
import org.opennms.taskset.service.api.TaskSetPublisher;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static org.opennms.horizon.inventory.service.taskset.TaskUtils.identityForAzureTask;

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
}
