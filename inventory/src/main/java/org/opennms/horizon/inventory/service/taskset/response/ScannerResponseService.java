package org.opennms.horizon.inventory.service.taskset.response;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.azure.contract.AzureScanItem;
import org.opennms.horizon.inventory.dto.NodeCreateDTO;
import org.opennms.horizon.inventory.model.AzureCredential;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.repository.AzureCredentialRepository;
import org.opennms.horizon.inventory.repository.NodeRepository;
import org.opennms.horizon.inventory.service.NodeService;
import org.opennms.horizon.inventory.service.taskset.MonitorTaskSetService;
import org.opennms.taskset.contract.ScannerResponse;
import org.opennms.taskset.contract.TaskDefinition;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScannerResponseService {
    private final NodeRepository nodeRepository;
    private final NodeService nodeService;
    private final MonitorTaskSetService monitorTaskSetService;
    private final AzureCredentialRepository azureCredentialRepository;

    public void accept(String tenantId, String location, ScannerResponse response) {
        List<TaskDefinition> tasks = new ArrayList<>();

        for (int index = 0; index < response.getResultsList().size(); index++) {

            // HACK: creating a dummy semi-unique ip address in order for status to display on ui
            String ipAddress = String.format("127.0.0.%d", index + 1);

            AzureScanItem item = response.getResultsList().get(index);
            Optional<TaskDefinition> taskOpt = process(tenantId, location, ipAddress, item);
            taskOpt.ifPresent(tasks::add);
        }

        monitorTaskSetService.sendMonitorTasks(tenantId, location, tasks);
    }

    private Optional<TaskDefinition> process(String tenantId, String location, String ipAddress, AzureScanItem item) {

        Optional<AzureCredential> azureCredentialOpt = azureCredentialRepository.findById(item.getCredentialId());
        if (azureCredentialOpt.isEmpty()) {
            log.warn("No Azure Credential found for id: {}", item.getCredentialId());
            return Optional.empty();
        }

        AzureCredential credential = azureCredentialOpt.get();

        //adding resource group here, we also need to save with node in order to do next requests
        String nodeLabel = String.format("%s (%s)", item.getName(), item.getResourceGroup());
        Optional<Node> nodeOpt = nodeRepository.findByTenantLocationAndNodeLabel(tenantId, location, nodeLabel);

        if (nodeOpt.isEmpty()) {
            NodeCreateDTO createDTO = NodeCreateDTO.newBuilder()
                .setLocation(location)
                .setManagementIp(ipAddress)
                .setLabel(nodeLabel)
                .build();
            Node node = nodeService.createNode(createDTO, tenantId);

            TaskDefinition monitorTask = monitorTaskSetService.addAzureMonitorTask(credential, item,ipAddress, node.getId());
            return Optional.of(monitorTask);
        }

        log.warn("Node already exists for tenant: {}, location: {}, label: {}", tenantId, location, nodeLabel);
        return Optional.empty();
    }
}
