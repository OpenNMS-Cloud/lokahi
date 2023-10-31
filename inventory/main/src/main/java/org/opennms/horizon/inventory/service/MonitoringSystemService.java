package org.opennms.horizon.inventory.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.horizon.grpc.heartbeat.contract.TenantLocationSpecificHeartbeatMessage;
import org.opennms.horizon.inventory.dto.MonitoringSystemDTO;
import org.opennms.horizon.inventory.exception.InventoryRuntimeException;
import org.opennms.horizon.inventory.exception.LocationNotFoundException;
import org.opennms.horizon.inventory.mapper.MonitoringSystemMapper;
import org.opennms.horizon.inventory.model.MonitoringSystem;
import org.opennms.horizon.inventory.repository.MonitoringLocationRepository;
import org.opennms.horizon.inventory.repository.MonitoringSystemRepository;
import org.springframework.stereotype.Service;

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

    public List<MonitoringSystemDTO> findByMonitoringLocationIdAndTenantId(long locationId, String tenantId) {
        if (locationRepository.findByIdAndTenantId(locationId, tenantId).isEmpty()) {
            throw new LocationNotFoundException("Location not found for id: " + locationId);
        }
        List<MonitoringSystem> all = systemRepository.findByMonitoringLocationIdAndTenantId(locationId, tenantId);
        return all
            .stream()
            .map(mapper::modelToDTO)
            .toList();
    }

    public Optional<MonitoringSystemDTO> findBySystemId(String systemId, String tenantId) {
        return systemRepository.findBySystemIdAndTenantId(systemId, tenantId).map(mapper::modelToDTO);
    }

    public Optional<MonitoringSystemDTO> findById(long id, String tenantId) {
        return systemRepository.findByIdAndTenantId(id, tenantId).map(mapper::modelToDTO);
    }

    public Optional<MonitoringSystemDTO> findByLocationAndSystemId(String location, String systemId, String tenantId) {
        return systemRepository.findByMonitoringLocationAndSystemIdAndTenantId(location, systemId, tenantId).map(mapper::modelToDTO);
    }

    public void addMonitoringSystemFromHeartbeat(TenantLocationSpecificHeartbeatMessage message)  {
        Identity identity = message.getIdentity();
        MonitoringSystem monitoringSystem;
        var optionalSystem =
            systemRepository.findByMonitoringLocationIdAndSystemIdAndTenantId(Long.parseLong(message.getLocationId()),
                identity.getSystemId(), message.getTenantId());
        if (optionalSystem.isEmpty()) {
            var monitoringLocation =
                locationRepository.findByIdAndTenantId(Long.parseLong(message.getLocationId()), message.getTenantId()).orElseThrow();
            monitoringSystem = new MonitoringSystem();
            monitoringSystem.setSystemId(identity.getSystemId());
            monitoringSystem.setTenantId(message.getTenantId());
            monitoringSystem.setLastCheckedIn(LocalDateTime.now());
            monitoringSystem.setLabel(identity.getSystemId().toUpperCase());
            monitoringSystem.setMonitoringLocation(monitoringLocation);
            systemRepository.save(monitoringSystem);
            // Asynchronously send config updates to Minion
            configUpdateService.sendConfigUpdate(message.getTenantId(), monitoringLocation.getId());
        } else {
            monitoringSystem = optionalSystem.get();
            monitoringSystem.setLastCheckedIn(LocalDateTime.now());
            systemRepository.save(monitoringSystem);
        }

    }

    public void deleteMonitoringSystem(long id) {

        var optionalMS = systemRepository.findById(id);
        if (optionalMS.isPresent()) {
            var monitoringSystem = optionalMS.get();
            var locationId = monitoringSystem.getMonitoringLocationId();
            var tenantId = monitoringSystem.getTenantId();
            systemRepository.deleteById(id);
        }
    }
}
