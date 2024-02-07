/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022-2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.horizon.inventory.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.opennms.horizon.inventory.dto.MonitoringLocationDTO;
import org.opennms.horizon.inventory.exception.InventoryRuntimeException;
import org.opennms.horizon.inventory.exception.LocationNotFoundException;
import org.opennms.horizon.inventory.mapper.MonitoringLocationMapper;
import org.opennms.horizon.inventory.model.MonitoringLocation;
import org.opennms.horizon.inventory.repository.MonitoringLocationRepository;
import org.opennms.horizon.inventory.repository.MonitoringSystemRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MonitoringLocationService {
    private final MonitoringLocationRepository modelRepo;
    private final MonitoringSystemRepository monitoringSystemRepository;

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

    public Optional<MonitoringLocationDTO> findByLocationIdAndTenantId(long locationId, String tenantId) {
        return modelRepo.findByIdAndTenantId(locationId, tenantId).map(mapper::modelToDTO);
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

    public MonitoringLocationDTO upsert(MonitoringLocationDTO dto) throws LocationNotFoundException {
        if (dto.hasField(MonitoringLocationDTO.getDescriptor().findFieldByNumber(MonitoringLocationDTO.ID_FIELD_NUMBER))
            && modelRepo.findByIdAndTenantId(dto.getId(), dto.getTenantId()).isEmpty()) {
            throw new LocationNotFoundException("Location not found with ID " + dto.getId());
        }
        if (dto.hasField(MonitoringLocationDTO.getDescriptor().findFieldByNumber(MonitoringLocationDTO.LOCATION_FIELD_NUMBER))) {
            if (StringUtils.isBlank(dto.getLocation())) {
                throw new InventoryRuntimeException("Location is Blank");
            }
            var location = modelRepo.findByLocationAndTenantId(dto.getLocation().trim(), dto.getTenantId());
            if (location.isPresent() && location.get().getId() != dto.getId()) {
                throw new InventoryRuntimeException("Duplicate Location found with name " + dto.getLocation());
            }
        }

        MonitoringLocation model = mapper.dtoToModel(dto);
        return mapper.modelToDTO(modelRepo.save(model));
    }

    public void delete(Long id, String tenantId) throws LocationNotFoundException {
        MonitoringLocation monitoringLocation = modelRepo.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new LocationNotFoundException("Location not found with ID " + id));
        modelRepo.delete(monitoringLocation);
        var systems = monitoringSystemRepository.findByMonitoringLocationIdAndTenantId(id, tenantId);
        if (!systems.isEmpty()) {
            monitoringSystemRepository.deleteAll(systems);
        }
    }
}
