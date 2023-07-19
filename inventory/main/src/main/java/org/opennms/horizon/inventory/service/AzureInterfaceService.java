package org.opennms.horizon.inventory.service;

import lombok.RequiredArgsConstructor;
import org.opennms.horizon.azure.api.AzureScanNetworkInterfaceItem;
import org.opennms.horizon.inventory.dto.SnmpInterfaceDTO;
import org.opennms.horizon.inventory.mapper.AzureInterfaceMapper;
import org.opennms.horizon.inventory.mapper.SnmpInterfaceMapper;
import org.opennms.horizon.inventory.model.AzureInterface;
import org.opennms.horizon.inventory.model.IpInterface;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.model.SnmpInterface;
import org.opennms.horizon.inventory.repository.AzureInterfaceRepository;
import org.opennms.horizon.inventory.repository.SnmpInterfaceRepository;
import org.opennms.node.scan.contract.SnmpInterfaceResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AzureInterfaceService {
    private final AzureInterfaceRepository modelRepo;
    private final AzureInterfaceMapper mapper;

    public Optional<AzureInterface> findByIpInterfaceIdAndTenantId(long ipInterfaceId, String tenantId) {
        return modelRepo.findByIpInterfaceIdAndTenantId(ipInterfaceId, tenantId);
    }

    public AzureInterface createOrUpdateFromScanResult(String tenantId, IpInterface ipInterface,
                                                       AzureScanNetworkInterfaceItem azureScanNetworkInterfaceItem) {
        Objects.requireNonNull(azureScanNetworkInterfaceItem);
        Objects.requireNonNull(ipInterface);
        return modelRepo.findByIpInterfaceIdAndTenantId(ipInterface.getId(), tenantId)
            .map(azure -> {
                mapper.updateFromScanResult(azure, azureScanNetworkInterfaceItem);
                return modelRepo.save(azure);
            }).orElseGet(() -> {
                var azure = mapper.scanResultToModel(azureScanNetworkInterfaceItem);
                azure.setTenantId(tenantId);
                azure.setIpInterface(ipInterface);
                return modelRepo.save(azure);
            });
    }
}
