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
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.inventory.dto.ConfigKey;
import org.opennms.horizon.inventory.dto.ConfigurationDTO;
import org.opennms.horizon.inventory.mapper.ConfigurationMapper;
import org.opennms.horizon.inventory.model.Configuration;
import org.opennms.horizon.inventory.repository.ConfigurationRepository;

@RequiredArgsConstructor
public abstract class ConfigurationService {
    private final ConfigurationRepository modelRepo;

    private final ConfigurationMapper mapper;

    public Configuration createSingle(ConfigurationDTO newConfigurationDTO) {

        Optional<Configuration> configuration =
                modelRepo.getByTenantIdAndKey(newConfigurationDTO.getTenantId(), newConfigurationDTO.getKey());

        return configuration.orElseGet(() -> modelRepo.save(mapper.dtoToModel(newConfigurationDTO)));
    }

    public Configuration createOrUpdate(ConfigurationDTO configDTO) {
        return modelRepo
                .getByTenantIdAndKey(configDTO.getTenantId(), configDTO.getKey())
                .map(config -> {
                    mapper.updateFromDTO(configDTO, config);
                    modelRepo.save(config);
                    return config;
                })
                .orElseGet(() -> modelRepo.save(mapper.dtoToModel(configDTO)));
    }

    public List<ConfigurationDTO> findByTenantId(String tenantId) {
        List<Configuration> all = modelRepo.findByTenantId(tenantId);
        return all.stream().map(mapper::modelToDTO).collect(Collectors.toList());
    }

    public List<ConfigurationDTO> findAll() {
        List<Configuration> all = modelRepo.findAll();
        return all.stream().map(mapper::modelToDTO).collect(Collectors.toList());
    }

    public List<ConfigurationDTO> findByLocation(String tenantId, String location) {
        List<Configuration> all = modelRepo.findByTenantIdAndLocation(tenantId, location);
        return all.stream().map(mapper::modelToDTO).collect(Collectors.toList());
    }

    public Optional<ConfigurationDTO> findByKey(String tenantId, ConfigKey key) {
        Optional<Configuration> configuration = modelRepo.getByTenantIdAndKey(tenantId, key);
        return configuration.map(mapper::modelToDTO);
    }
}
