package org.opennms.horizon.inventory.service;

import lombok.RequiredArgsConstructor;
import org.opennms.horizon.inventory.dto.MonitoredServiceDTO;
import org.opennms.horizon.inventory.mapper.MonitoredServiceMapper;
import org.opennms.horizon.inventory.model.IpInterface;
import org.opennms.horizon.inventory.model.MonitoredService;
import org.opennms.horizon.inventory.model.MonitoredServiceType;
import org.opennms.horizon.inventory.repository.MonitoredServiceRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MonitoredServiceService {
    private final MonitoredServiceRepository modelRepo;

    private final MonitoredServiceMapper mapper;

    public void createSingle(MonitoredServiceDTO newMonitoredService,
                             MonitoredServiceType monitoredServiceType,
                             IpInterface ipInterface) {

        String tenantId = newMonitoredService.getTenantId();

        Optional<MonitoredService> monitoredServiceOpt = modelRepo
            .findByTenantIdTypeAndIpInterface(tenantId, monitoredServiceType, ipInterface);

        if (monitoredServiceOpt.isEmpty()) {

            MonitoredService monitoredService = mapper.dtoToModel(newMonitoredService);
            monitoredService.setIpInterface(ipInterface);
            monitoredService.setMonitoredServiceType(monitoredServiceType);

            modelRepo.save(monitoredService);
        }
    }

    public List<MonitoredServiceDTO> findByTenantId(String tenantId) {
        List<MonitoredService> all = modelRepo.findByTenantId(tenantId);
        return all
            .stream()
            .map(mapper::modelToDTO)
            .collect(Collectors.toList());
    }
}
