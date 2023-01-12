package org.opennms.horizon.inventory.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.horizon.grpc.heartbeat.contract.HeartbeatMessage;
import org.opennms.horizon.inventory.dto.MonitoringSystemDTO;
import org.opennms.horizon.inventory.mapper.MonitoringSystemMapper;
import org.opennms.horizon.inventory.model.MonitoringLocation;
import org.opennms.horizon.inventory.model.MonitoringSystem;
import org.opennms.horizon.inventory.model.MonitoringSystemBean;
import org.opennms.horizon.inventory.repository.MonitoringLocationRepository;
import org.opennms.horizon.inventory.repository.MonitoringSystemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MonitoringSystemService {
    private final MonitoringSystemRepository modelRepo;
    private final MonitoringLocationRepository locationRepository;
    private final MonitoringSystemMapper mapper;
    private final ConfigUpdateService configUpdateService;

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

    public Optional<Long> addMonitoringSystemFromHeartbeat(HeartbeatMessage message, String tenantId) {
        Identity identity = message.getIdentity();
        Optional<MonitoringSystem> msOp = modelRepo.findBySystemIdAndTenantId(identity.getSystemId(), tenantId);
        if(msOp.isEmpty()) {
            Optional<MonitoringLocation> locationOp = locationRepository.findByLocationAndTenantId(identity.getLocation(), tenantId);
            MonitoringLocation location = new MonitoringLocation();
            if(locationOp.isPresent()) {
                location = locationOp.get();
            } else {
                location.setLocation(identity.getLocation());
                location.setTenantId(tenantId);
                var newLocation = locationRepository.save(location);
                // Send config updates asynchronously to Minion
                configUpdateService.sendConfigUpdate(newLocation.getTenantId(), newLocation.getLocation());

            }
            MonitoringSystem monitoringSystem = new MonitoringSystem();
            monitoringSystem.setSystemId(identity.getSystemId());
            monitoringSystem.setMonitoringLocation(location);
            monitoringSystem.setTenantId(tenantId);
            monitoringSystem.setLastCheckedIn(LocalDateTime.now());
            monitoringSystem.setLabel(identity.getSystemId().toUpperCase());
            monitoringSystem.setMonitoringLocationId(location.getId());
            modelRepo.save(monitoringSystem);
            return Optional.of(monitoringSystem.getId()); //only return new ID
        } else {
            MonitoringSystem monitoringSystem = msOp.get();
            monitoringSystem.setLastCheckedIn(LocalDateTime.now());
            modelRepo.save(monitoringSystem);
            return Optional.empty();
        }

    }

    //For inventory internal use only
    @Transactional
    public List<MonitoringSystemBean> listAllForMonitoring() {
        List<MonitoringSystemBean> list = new ArrayList<>();
        modelRepo.findAll().forEach(s->list.add(new MonitoringSystemBean(s.getSystemId(), s.getTenantId(), s.getMonitoringLocation().getLocation())));
        return list;
    }

    @Transactional
    public Optional<MonitoringSystemBean> getByIdForMonitoring(long id) {
        return modelRepo.findById(id).map(s->new MonitoringSystemBean(s.getSystemId(), s.getTenantId(), s.getMonitoringLocation().getLocation()));
    }

    public void deleteMonitoringSystem(long id) {
        modelRepo.deleteById(id);
    }
}
