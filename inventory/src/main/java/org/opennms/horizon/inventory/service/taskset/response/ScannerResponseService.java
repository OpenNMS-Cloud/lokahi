package org.opennms.horizon.inventory.service.taskset.response;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.azure.api.AzureScanItem;
import org.opennms.horizon.azure.api.AzureScanResponse;
import org.opennms.horizon.inventory.dto.NodeCreateDTO;
import org.opennms.horizon.inventory.model.AzureCredential;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.repository.AzureCredentialRepository;
import org.opennms.horizon.inventory.repository.NodeRepository;
import org.opennms.horizon.inventory.service.NodeService;
import org.opennms.horizon.inventory.service.taskset.CollectorTaskSetService;
import org.opennms.horizon.inventory.service.taskset.MonitorTaskSetService;
import org.opennms.taskset.contract.ScanType;
import org.opennms.taskset.contract.ScannerResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScannerResponseService {
    private final AzureCredentialRepository azureCredentialRepository;
    private final NodeRepository nodeRepository;
    private final NodeService nodeService;
    private final MonitorTaskSetService monitorTaskSetService;
    private final CollectorTaskSetService collectorTaskSetService;

    public void accept(String tenantId, String location, ScannerResponse response) throws InvalidProtocolBufferException {
        Any result = response.getResult();

        switch (getType(response)) {

            // other scan types

            case AZURE: {
                AzureScanResponse azureResponse = result.unpack(AzureScanResponse.class);
                List<AzureScanItem> resultsList = azureResponse.getResultsList();

                for (int index = 0; index < resultsList.size(); index++) {
                    AzureScanItem item = resultsList.get(index);

                    // HACK: for now, creating a dummy ip address in order for status to display on ui
                    // could maybe get ip interfaces from VM to save instead but private IPs may not be unique enough if no public IP attached ?
                    // Postgres requires a valid INET field
                    String ipAddress = String.format("127.0.0.%d", index + 1);

                    processAzureScanItem(tenantId, location, ipAddress, item);
                }
                break;
            }
            case UNRECOGNIZED: {
                log.warn("Unrecognized scan type");
            }
        }
    }

    private ScanType getType(ScannerResponse response) {
        Any result = response.getResult();
        if (result.is(AzureScanResponse.class)) {
            return ScanType.AZURE;
        }
        return ScanType.UNRECOGNIZED;
    }

    private void processAzureScanItem(String tenantId, String location, String ipAddress, AzureScanItem item) {
        Optional<AzureCredential> azureCredentialOpt = azureCredentialRepository.findById(item.getCredentialId());
        if (azureCredentialOpt.isEmpty()) {
            log.warn("No Azure Credential found for id: {}", item.getCredentialId());
            return;
        }

        AzureCredential credential = azureCredentialOpt.get();

        String nodeLabel = String.format("%s (%s)", item.getName(), item.getResourceGroup());
        Optional<Node> nodeOpt = nodeRepository.findByTenantLocationAndNodeLabel(tenantId, location, nodeLabel);

        if (nodeOpt.isEmpty()) {

            //note: may need to relate AzureCredential with Node for recovery of monitoring
            NodeCreateDTO createDTO = NodeCreateDTO.newBuilder()
                .setLocation(location)
                .setManagementIp(ipAddress)
                .setLabel(nodeLabel)
                .build();
            Node node = nodeService.createNode(createDTO, tenantId);

            monitorTaskSetService.sendAzureMonitorTasks(credential, item, ipAddress, node.getId());
            collectorTaskSetService.sendAzureCollectorTasks(credential, item, ipAddress, node.getId());

        } else {
            log.warn("Node already exists for tenant: {}, location: {}, label: {}", tenantId, location, nodeLabel);
        }
    }
}
