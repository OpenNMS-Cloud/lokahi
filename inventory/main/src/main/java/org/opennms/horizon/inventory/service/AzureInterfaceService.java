package org.opennms.horizon.inventory.service;

import lombok.RequiredArgsConstructor;
import org.opennms.horizon.azure.api.AzureScanNetworkInterfaceItem;
import org.opennms.horizon.inventory.dto.AzureInterfaceDTO;
import org.opennms.horizon.inventory.mapper.AzureInterfaceMapper;
import org.opennms.horizon.inventory.model.AzureInterface;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.repository.AzureInterfaceRepository;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AzureInterfaceService {
    private final AzureInterfaceRepository modelRepo;
    private final AzureInterfaceMapper mapper;

    public Optional<AzureInterfaceDTO> findByIdAndTenantId(long id, String tenantId) {
        return modelRepo.findByIdAndTenantId(id, tenantId).map(mapper::modelToDTO);
    }

    public AzureInterface createOrUpdateFromScanResult(String tenantId, Node node,
                                                       AzureScanNetworkInterfaceItem azureScanNetworkInterfaceItem) {
        Objects.requireNonNull(azureScanNetworkInterfaceItem);
        String publicIpId = azureScanNetworkInterfaceItem.hasPublicIpAddress() ?
            azureScanNetworkInterfaceItem.getPublicIpAddress().getName() : null;
        return modelRepo.findByTenantIdAndPublicIpId(tenantId, publicIpId)
            .map(azure -> {
                mapper.updateFromScanResult(azure, azureScanNetworkInterfaceItem);
                return modelRepo.save(azure);
            }).orElseGet(() -> {
                var azure = mapper.scanResultToModel(azureScanNetworkInterfaceItem);
                azure.setTenantId(tenantId);
                azure.setNode(node);
                return modelRepo.save(azure);
            });
    }
}
