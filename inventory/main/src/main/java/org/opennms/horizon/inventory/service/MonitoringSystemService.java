package org.opennms.horizon.inventory.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.horizon.grpc.heartbeat.contract.HeartbeatMessage;
import org.opennms.horizon.grpc.heartbeat.contract.TenantLocationSpecificHeartbeatMessage;
import org.opennms.horizon.inventory.dto.MonitoringSystemDTO;
import org.opennms.horizon.inventory.mapper.MonitoringSystemMapper;
import org.opennms.horizon.inventory.model.MonitoringLocation;
import org.opennms.horizon.inventory.model.MonitoringSystem;
import org.opennms.horizon.inventory.repository.MonitoringLocationRepository;
import org.opennms.horizon.inventory.repository.MonitoringSystemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
            .collect(Collectors.toList());
    }

    public Optional<MonitoringSystemDTO> findBySystemId(String systemId, String tenantId) {
        return systemRepository.findBySystemIdAndTenantId(systemId, tenantId).map(mapper::modelToDTO);
    }

    public void addMonitoringSystemFromHeartbeat(TenantLocationSpecificHeartbeatMessage message) {
        String tenantId = message.getTenantId();
        String location = message.getLocation();
        Identity identity = message.getIdentity();

        Optional<MonitoringSystem> msOp = systemRepository.findBySystemIdAndTenantId(identity.getSystemId(), tenantId);
        if(msOp.isEmpty()) {
            Optional<MonitoringLocation> locationOp = locationRepository.findByLocationAndTenantId(location, tenantId);
            MonitoringLocation monitoringLocatgion = new MonitoringLocation();
            if(locationOp.isPresent()) {
                monitoringLocatgion = locationOp.get();
            } else {
                monitoringLocatgion.setLocation(location);
                monitoringLocatgion.setTenantId(tenantId);
                var newLocation = locationRepository.save(monitoringLocatgion);
                // Send config updates asynchronously to Minion
                configUpdateService.sendConfigUpdate(newLocation.getTenantId(), newLocation.getLocation());

            }

            MonitoringSystem monitoringSystem = new MonitoringSystem();
            monitoringSystem.setSystemId(identity.getSystemId());
            monitoringSystem.setMonitoringLocation(monitoringLocatgion);
            monitoringSystem.setTenantId(tenantId);
            monitoringSystem.setLastCheckedIn(LocalDateTime.now());
            monitoringSystem.setLabel(identity.getSystemId().toUpperCase());
            monitoringSystem.setMonitoringLocationId(monitoringLocatgion.getId());
            systemRepository.save(monitoringSystem);
        } else {
            MonitoringSystem monitoringSystem = msOp.get();
            monitoringSystem.setLastCheckedIn(LocalDateTime.now());
            systemRepository.save(monitoringSystem);
        }
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
            if (retrieved.size() == 0) {
                locationRepository.delete(monitoringSystem.getMonitoringLocation());
                configUpdateService.removeConfigsFromTaskSet(tenantId, location);
            }
        }
    }
}
