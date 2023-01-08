package org.opennms.horizon.inventory.service.taskset;

import com.google.protobuf.Any;
import lombok.RequiredArgsConstructor;
import org.opennms.azure.contract.AzureScanRequest;
import org.opennms.horizon.inventory.model.AzureCredential;
import org.opennms.taskset.contract.TaskDefinition;
import org.opennms.taskset.contract.TaskType;
import org.opennms.taskset.service.api.TaskSetPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.opennms.horizon.inventory.service.taskset.TaskUtils.identityForAzureTask;

@Component
@RequiredArgsConstructor
public class ScannerTaskSetService {
    private final TaskSetPublisher taskSetPublisher;

    public void sendAzureScannerTask(AzureCredential credential) {
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
                .setTimeout(TaskUtils.Azure.DEFAULT_TIMEOUT)
                .setRetries(TaskUtils.Azure.DEFAULT_RETRIES)
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
