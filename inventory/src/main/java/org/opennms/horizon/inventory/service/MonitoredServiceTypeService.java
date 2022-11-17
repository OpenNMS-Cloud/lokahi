package org.opennms.horizon.inventory.service;

import lombok.RequiredArgsConstructor;
import org.opennms.horizon.inventory.dto.MonitoredServiceTypeDTO;
import org.opennms.horizon.inventory.mapper.MonitoredServiceTypeMapper;
import org.opennms.horizon.inventory.model.MonitoredServiceType;
import org.opennms.horizon.inventory.repository.MonitoredServiceTypeRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MonitoredServiceTypeService {
    private final MonitoredServiceTypeRepository modelRepo;

    private final MonitoredServiceTypeMapper mapper;

    public MonitoredServiceType createSingle(MonitoredServiceTypeDTO newMonitoredServiceType) {

        Optional<MonitoredServiceType> monitoredServiceTypeOpt = modelRepo.findByTenantIdAndServiceName(
            newMonitoredServiceType.getTenantId(), newMonitoredServiceType.getServiceName());

        if (monitoredServiceTypeOpt.isPresent()) {
            return monitoredServiceTypeOpt.get();
        }

        MonitoredServiceType monitoredServiceType = mapper.dtoToModel(newMonitoredServiceType);
        return modelRepo.save(monitoredServiceType);
    }

    public List<MonitoredServiceTypeDTO> findByTenantId(String tenantId) {
        List<MonitoredServiceType> all = modelRepo.findByTenantId(tenantId);
        return all
            .stream()
            .map(mapper::modelToDTO)
            .collect(Collectors.toList());
    }
}
