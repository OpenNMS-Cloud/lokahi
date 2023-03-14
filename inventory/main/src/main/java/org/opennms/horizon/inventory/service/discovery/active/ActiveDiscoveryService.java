package org.opennms.horizon.inventory.service.discovery.active;

import lombok.RequiredArgsConstructor;
import org.opennms.horizon.inventory.dto.ActiveDiscoveryDTO;
import org.opennms.horizon.inventory.mapper.ActiveDiscoveryMapper;
import org.opennms.horizon.inventory.model.discovery.active.ActiveDiscovery;
import org.opennms.horizon.inventory.repository.discovery.active.ActiveDiscoveryRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ActiveDiscoveryService {
    private final ActiveDiscoveryRepository repository;
    private final ActiveDiscoveryMapper mapper;

    @Transactional(readOnly = true)
    public List<ActiveDiscoveryDTO> getActiveDiscoveries(String tenantId) {
        List<ActiveDiscovery> discoveries = repository.findByTenantIdOrderById(tenantId);
        return mapper.modelToDto(discoveries);
    }
}
