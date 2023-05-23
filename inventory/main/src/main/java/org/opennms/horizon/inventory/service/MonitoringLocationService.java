package org.opennms.horizon.inventory.service;

import lombok.RequiredArgsConstructor;
import org.opennms.horizon.inventory.dto.MonitoringLocationDTO;
import org.opennms.horizon.inventory.mapper.MonitoringLocationMapper;
import org.opennms.horizon.inventory.model.MonitoringLocation;
import org.opennms.horizon.inventory.repository.MonitoringLocationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MonitoringLocationService {
    private final MonitoringLocationRepository modelRepo;

    private final MonitoringLocationMapper mapper;

    public List<MonitoringLocationDTO> findByTenantId(String tenantId) {
        List<MonitoringLocation> all = modelRepo.findByTenantId(tenantId);
        return all
            .stream()
            .map(mapper::modelToDTO)
            .toList();
    }

    public Optional<MonitoringLocationDTO> findByLocationAndTenantId(String location, String tenantId) {
        return modelRepo.findByLocationAndTenantId(location, tenantId).map(mapper::modelToDTO);
    }

    public Optional<MonitoringLocationDTO> getByIdAndTenantId(long id, String tenantId) {
        return modelRepo.findByIdAndTenantId(id, tenantId).map(mapper::modelToDTO);
    }

    public List<MonitoringLocationDTO> findByLocationIds(List<Long> ids) {
        return modelRepo.findByIdIn(ids).stream().map(mapper::modelToDTO).toList();
    }

    public List<MonitoringLocationDTO> findAll() {
        List<MonitoringLocation> all = modelRepo.findAll();
        return all
            .stream()
            .map(mapper::modelToDTO)
            .toList();
    }

    public List<MonitoringLocationDTO> searchLocationsByTenantId(String location, String tenantId) {
        return modelRepo.findByLocationContainingIgnoreCaseAndTenantId(location, tenantId)
            .stream().map(mapper::modelToDTO).toList();
    }

    public MonitoringLocationDTO upsert(MonitoringLocationDTO dto) {
        MonitoringLocation model = mapper.dtoToModel(dto);
        return mapper.modelToDTO(modelRepo.save(model));
    }

    public void delete(Long id, String tenantId) {
        modelRepo.findByIdAndTenantId(id, tenantId).ifPresent(modelRepo::delete);
    }
}
