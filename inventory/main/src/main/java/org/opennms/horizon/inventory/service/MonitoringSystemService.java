package org.opennms.horizon.inventory.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.horizon.grpc.heartbeat.contract.TenantLocationSpecificHeartbeatMessage;
import org.opennms.horizon.inventory.dto.MonitoringSystemDTO;
import org.opennms.horizon.inventory.exception.LocationNotFoundException;
import org.opennms.horizon.inventory.mapper.MonitoringSystemMapper;
import org.opennms.horizon.inventory.model.MonitoringLocation;
import org.opennms.horizon.inventory.model.MonitoringSystem;
import org.opennms.horizon.inventory.repository.MonitoringLocationRepository;
import org.opennms.horizon.inventory.repository.MonitoringSystemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MonitoringSystemService {
    private final MonitoringSystemRepository systemRepository;
    private final MonitoringLocationRepository locationRepository;
    private final MonitoringSystemMapper mapper;
    private final ConfigUpdateService configUpdateService;

    public List<MonitoringSystemDTO> findByTenantId(String tenantId) {
        List<MonitoringSystem> all = systemRepository.findByTenantId(tenantId);
        return all
            .stream()
            .map(mapper::modelToDTO)
            .toList();
    }

    public Optional<MonitoringSystemDTO> findBySystemId(String systemId, String tenantId) {
        return systemRepository.findBySystemIdAndTenantId(systemId, tenantId).map(mapper::modelToDTO);
    }

    public void addMonitoringSystemFromHeartbeat(TenantLocationSpecificHeartbeatMessage message) throws LocationNotFoundException {
        Identity identity = message.getIdentity();
        MonitoringSystem monitoringSystem;
        Optional<MonitoringSystem> msOp = systemRepository.findBySystemIdAndTenantId(identity.getSystemId(), message.getTenantId());
        if (msOp.isEmpty()) {
            MonitoringLocation location = locationRepository.findByLocationAndTenantId(message.getLocation(), message.getTenantId())
                .orElseThrow(() -> new LocationNotFoundException("Location not found " + message.getLocation()));
            monitoringSystem = new MonitoringSystem();
            monitoringSystem.setSystemId(identity.getSystemId());
            monitoringSystem.setMonitoringLocation(location);
            monitoringSystem.setTenantId(message.getTenantId());
            monitoringSystem.setLastCheckedIn(LocalDateTime.now());
            monitoringSystem.setLabel(identity.getSystemId().toUpperCase());
            monitoringSystem.setMonitoringLocationId(location.getId());
            systemRepository.save(monitoringSystem);
        } else {
            monitoringSystem = msOp.get();
            monitoringSystem.setLastCheckedIn(LocalDateTime.now());
            systemRepository.save(monitoringSystem);
        }

        // Asynchronously send config updates to Minion
        configUpdateService.sendConfigUpdate(message.getTenantId(), message.getLocation());
    }

    @Transactional
    public void deleteMonitoringSystem(long id) {

        var optionalMS = systemRepository.findById(id);
        if (optionalMS.isPresent()) {
            var monitoringSystem = optionalMS.get();
            var location = monitoringSystem.getMonitoringLocation().getLocation();
            var tenantId = monitoringSystem.getTenantId();
            systemRepository.deleteById(id);
            var retrieved = systemRepository.findByMonitoringLocationIdAndTenantId(optionalMS.get().getMonitoringLocationId(), tenantId);
            if (retrieved.isEmpty()) {
                locationRepository.delete(monitoringSystem.getMonitoringLocation());
                configUpdateService.removeConfigsFromTaskSet(tenantId, location);
            }
        }
    }
}
