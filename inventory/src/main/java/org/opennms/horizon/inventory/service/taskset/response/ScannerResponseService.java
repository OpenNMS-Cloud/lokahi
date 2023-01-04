package org.opennms.horizon.inventory.service.taskset.response;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.azure.contract.AzureScanItem;
import org.opennms.horizon.inventory.dto.NodeCreateDTO;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.repository.IpInterfaceRepository;
import org.opennms.horizon.inventory.repository.MonitoringLocationRepository;
import org.opennms.horizon.inventory.repository.NodeRepository;
import org.opennms.horizon.inventory.service.NodeService;
import org.opennms.taskset.contract.ScannerResponse;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScannerResponseService {
    private final IpInterfaceRepository ipInterfaceRepository;
    private final NodeRepository nodeRepository;
    private final NodeService nodeService;
    private final MonitoringLocationRepository monitoringLocationRepository;

    public void accept(String tenantId, String location, ScannerResponse response) {
        for (AzureScanItem item : response.getResultsList()) {
            process(tenantId, location, item);
        }
    }

    private void process(String tenantId, String location, AzureScanItem item) {

        //adding resource group here, we also need to save with node in order to do next requests
        String nodeLabel = String.format("%s (%s)", item.getName(), item.getResourceGroup());
        Optional<Node> nodeOpt = nodeRepository.findByTenantLocationAndNodeLabel(tenantId, location, nodeLabel);

        if (nodeOpt.isPresent()) {
            log.warn("Node already exists for tenant: {}, location: {}, label: {}", tenantId, location, nodeLabel);
        } else {

            NodeCreateDTO createDTO = NodeCreateDTO.newBuilder()
                .setLocation(location)
                .setManagementIp("127.0.0.1") // dummy interface needed for the UI to display the node
                .setLabel(nodeLabel)
                .build();
            nodeService.createNode(createDTO, tenantId);
        }
    }
}
