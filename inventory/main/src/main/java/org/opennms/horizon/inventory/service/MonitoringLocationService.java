/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.opennms.horizon.inventory.service;

import java.util.List;
import java.util.Optional;
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

@Service
@RequiredArgsConstructor
public class MonitoringLocationService {
    private final MonitoringLocationRepository modelRepo;
    private final MonitoringSystemRepository monitoringSystemRepository;

    private final MonitoringLocationMapper mapper;

    public List<MonitoringLocationDTO> findByTenantId(String tenantId) {
        List<MonitoringLocation> all = modelRepo.findByTenantId(tenantId);
        return all.stream().map(mapper::modelToDTO).toList();
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
        return all.stream().map(mapper::modelToDTO).toList();
    }

    public List<MonitoringLocationDTO> searchLocationsByTenantId(String location, String tenantId) {
        return modelRepo.findByLocationContainingIgnoreCaseAndTenantId(location, tenantId).stream()
                .map(mapper::modelToDTO)
                .toList();
    }

    public MonitoringLocationDTO upsert(MonitoringLocationDTO dto) throws LocationNotFoundException {
        if (dto.hasField(MonitoringLocationDTO.getDescriptor().findFieldByNumber(MonitoringLocationDTO.ID_FIELD_NUMBER))
                && modelRepo.findByIdAndTenantId(dto.getId(), dto.getTenantId()).isEmpty()) {
            throw new LocationNotFoundException("Location not found with ID " + dto.getId());
        }
        if (dto.hasField(
                MonitoringLocationDTO.getDescriptor().findFieldByNumber(MonitoringLocationDTO.LOCATION_FIELD_NUMBER))) {
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
        MonitoringLocation monitoringLocation = modelRepo
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new LocationNotFoundException("Location not found with ID " + id));
        modelRepo.delete(monitoringLocation);
        var systems = monitoringSystemRepository.findByMonitoringLocationIdAndTenantId(id, tenantId);
        if (!systems.isEmpty()) {
            monitoringSystemRepository.deleteAll(systems);
        }
    }
}
