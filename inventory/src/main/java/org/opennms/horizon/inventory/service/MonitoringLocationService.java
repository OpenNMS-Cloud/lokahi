package org.opennms.horizon.inventory.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.opennms.horizon.inventory.dto.MonitoringLocationDTO;
import org.opennms.horizon.inventory.mapper.MonitoringLocationMapper;
import org.opennms.horizon.inventory.model.MonitoringLocation;
import org.opennms.horizon.inventory.repository.MonitoringLocationRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MonitoringLocationService {
    private final MonitoringLocationRepository modelRepo;

    private final MonitoringLocationMapper mapper;
    private final JdbcTemplate jdbcTemplate;

    public List<MonitoringLocationDTO> findByTenantId(String tenantId) {
        List<MonitoringLocation> all = modelRepo.findByTenantId(tenantId);
        return all
            .stream()
            .map(mapper::modelToDTO)
            .collect(Collectors.toList());
    }

    public Optional<MonitoringLocationDTO> findByLocationAndTenantId(String location, String tenantId) {
        return modelRepo.findByLocationAndTenantId(location, tenantId).map(mapper::modelToDTO);
    }

    public Optional<MonitoringLocationDTO> getByIdAndTenantId(long id, String tenantId) {
        return modelRepo.findByIdAndTenantId(id, tenantId).map(mapper::modelToDTO);
    }

    public List<MonitoringLocationDTO> findByLocationIds(List<Long> ids) {
        return modelRepo.findByIdIn(ids).stream().map(mapper::modelToDTO).collect(Collectors.toList());
    }

    public List<MonitoringLocationDTO> findAll() {
        List<MonitoringLocationDTO> all = jdbcTemplate.query(
            "Select id, tenant_id, location from monitoring_location",
            (rs, rowNum) ->
                MonitoringLocationDTO.newBuilder()
                    .setId(rs.getLong("id"))
                    .setLocation(rs.getString("location"))
                    .setTenantId("tenant_id")
                    .build()
        );
        return all;
    }

    public List<MonitoringLocationDTO> searchLocations(String location) {
        return modelRepo.findByLocationContainingIgnoreCase(location)
            .stream().map(mapper::modelToDTO).toList();
    }
}
