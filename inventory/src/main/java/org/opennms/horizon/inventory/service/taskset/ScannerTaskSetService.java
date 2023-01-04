package org.opennms.horizon.inventory.service.taskset;

import com.google.protobuf.Any;
import lombok.RequiredArgsConstructor;
import org.opennms.azure.contract.AzureScanRequest;
import org.opennms.horizon.inventory.model.AzureCredential;
import org.opennms.taskset.contract.TaskDefinition;
import org.opennms.taskset.contract.TaskType;
import org.opennms.taskset.service.api.TaskSetPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

import static org.opennms.horizon.inventory.service.taskset.TaskUtils.identityForAzureTask;

@Component
@RequiredArgsConstructor
public class ScannerTaskSetService {
    private static final Logger log = LoggerFactory.getLogger(ScannerTaskSetService.class);
    private final TaskSetPublisher taskSetPublisher;

    public void sendAzureScannerTask(AzureCredential credential) {
        String tenantId = credential.getTenantId();

        var task = addAzureScannerTask(credential);
        taskSetPublisher.publishNewTasks(tenantId, "Default", Arrays.asList(task));
    }

    private TaskDefinition addAzureScannerTask(AzureCredential credential) {

        Any configuration =
            Any.pack(AzureScanRequest.newBuilder()
                .setCredentialId(credential.getId())
                .setClientId(credential.getClientId())
                .setClientSecret(credential.getClientSecret())
                .setSubscriptionId(credential.getSubscriptionId())
                .setDirectoryId(credential.getDirectoryId())
                .setResourceGroup(credential.getResourceGroup())
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
