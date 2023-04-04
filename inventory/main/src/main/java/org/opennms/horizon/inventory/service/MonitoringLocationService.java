package org.opennms.horizon.inventory.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.opennms.horizon.inventory.dto.MonitoringLocationDTO;
import org.opennms.horizon.inventory.mapper.MonitoringLocationMapper;
import org.opennms.horizon.inventory.model.MonitoringLocation;
import org.opennms.horizon.inventory.repository.MonitoringLocationRepository;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MonitoringLocationService {
    private final MonitoringLocationRepository repository;
    private final MonitoringLocationMapper mapper;
    private final ConfigUpdateService configUpdateService;

    public MonitoringLocation saveMonitoringLocation(String tenantId, String location) {
        location = StringUtils.isEmpty(location) ? GrpcConstants.DEFAULT_LOCATION : location;

        Optional<MonitoringLocation> locationOpt =
            repository.findByLocationAndTenantId(location, tenantId);

        if (locationOpt.isPresent()) {
            return locationOpt.get();
        } else {
            MonitoringLocation newLocation = new MonitoringLocation();

            newLocation.setTenantId(tenantId);
            newLocation.setLocation(location);

            MonitoringLocation saved = repository.save(newLocation);
            // Asynchronously send config updates to Minion
            configUpdateService.sendConfigUpdate(tenantId, saved.getLocation());
            return saved;
        }
    }

    public List<MonitoringLocationDTO> findByTenantId(String tenantId) {
        List<MonitoringLocation> all = repository.findByTenantId(tenantId);
        return all
            .stream()
            .map(mapper::modelToDTO)
            .toList();
    }

    public Optional<MonitoringLocationDTO> findByLocationAndTenantId(String location, String tenantId) {
        return repository.findByLocationAndTenantId(location, tenantId).map(mapper::modelToDTO);
    }

    public Optional<MonitoringLocationDTO> getByIdAndTenantId(long id, String tenantId) {
        return repository.findByIdAndTenantId(id, tenantId).map(mapper::modelToDTO);
    }

    public List<MonitoringLocationDTO> findByLocationIds(List<Long> ids) {
        return repository.findByIdIn(ids).stream().map(mapper::modelToDTO).toList();
    }

    public List<MonitoringLocationDTO> findAll() {
        List<MonitoringLocation> all = repository.findAll();
        return all
            .stream()
            .map(mapper::modelToDTO)
            .toList();
    }

    public List<MonitoringLocationDTO> searchLocationsByTenantId(String location, String tenantId) {
        return repository.findByLocationContainingIgnoreCaseAndTenantId(location, tenantId)
            .stream().map(mapper::modelToDTO).toList();
    }
}
