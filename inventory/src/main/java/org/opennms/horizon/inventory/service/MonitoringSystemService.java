package org.opennms.horizon.inventory.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.horizon.grpc.heartbeat.contract.HeartbeatMessage;
import org.opennms.horizon.inventory.dto.MonitoringSystemDTO;
import org.opennms.horizon.inventory.mapper.MonitoringSystemMapper;
import org.opennms.horizon.inventory.model.MonitoringLocation;
import org.opennms.horizon.inventory.model.MonitoringSystem;
import org.opennms.horizon.inventory.repository.MonitoringLocationRepository;
import org.opennms.horizon.inventory.repository.MonitoringSystemRepository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MonitoringSystemService {
    private final MonitoringSystemRepository modelRepo;
    private final MonitoringLocationRepository locationRepository;
    private final MonitoringSystemMapper mapper;

    public List<MonitoringSystemDTO> findByTenantId(String tenantId) {
        List<MonitoringSystem> all = modelRepo.findByTenantId(tenantId);
        return all
            .stream()
            .map(mapper::modelToDTO)
            .collect(Collectors.toList());
    }

    public Optional<MonitoringSystemDTO> findBySystemId(String systemId, String tenantId) {
        return modelRepo.findBySystemIdAndTenantId(systemId, tenantId).map(mapper::modelToDTO);
    }

    public void addMonitoringSystemFromHeartbeat(HeartbeatMessage message, String tenantId) {
        Identity identity = message.getIdentity();
        Optional<MonitoringSystem> msOp = modelRepo.findBySystemId(identity.getSystemId());
        if(msOp.isEmpty()) {
            Optional<MonitoringLocation> locationOp = locationRepository.findByLocation(identity.getLocation());
            MonitoringLocation location = new MonitoringLocation();
            if(locationOp.isPresent()) {
                location = locationOp.get();
            } else {
                location.setLocation(identity.getLocation());
                location.setTenantId(tenantId);
                locationRepository.save(location);
            }
            MonitoringSystem monitoringSystem = new MonitoringSystem();
            monitoringSystem.setSystemId(identity.getSystemId());
            monitoringSystem.setMonitoringLocation(location);
            monitoringSystem.setTenantId(location.getTenantId());
            monitoringSystem.setLastCheckedIn(LocalDateTime.now());
            monitoringSystem.setLabel(identity.getSystemId().toUpperCase());
            monitoringSystem.setMonitoringLocationId(location.getId());
            modelRepo.save(monitoringSystem);
        } else {
            MonitoringSystem monitoringSystem = msOp.get();
            monitoringSystem.setLastCheckedIn(LocalDateTime.now());
            modelRepo.save(monitoringSystem);
        }
    }
}
