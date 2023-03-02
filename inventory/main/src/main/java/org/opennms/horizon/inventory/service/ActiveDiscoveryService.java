/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2023 The OpenNMS Group, Inc.
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
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.inventory.discovery.ActiveDiscoveryDTO;
import org.opennms.horizon.inventory.discovery.ActiveDiscoveryRequest;
import org.opennms.horizon.inventory.mapper.ActiveDiscoveryMapper;
import org.opennms.horizon.inventory.repository.ActiveDiscoveryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ActiveDiscoveryService {

    private final ActiveDiscoveryRepository repository;

    private final ActiveDiscoveryMapper mapper;

    public ActiveDiscoveryDTO createConfig(ActiveDiscoveryRequest request, String tenantId) {
        var activeDiscoveryConfig = mapper.mapRequest(request);
        activeDiscoveryConfig.setTenantId(tenantId);
        activeDiscoveryConfig = repository.save(activeDiscoveryConfig);
        return mapper.modelToDto(activeDiscoveryConfig);
    }

    public List<ActiveDiscoveryDTO> listDiscoveryConfigs(String tenantId) {
        var entities = repository.findByTenantId(tenantId);
        return entities.stream().map(mapper::modelToDto).toList();
    }

    public Optional<ActiveDiscoveryDTO> getDiscoveryConfigByName(String name, String tenantId) {
        var optional = repository.findByNameAndTenantId(name, tenantId);
        return optional.map(mapper::modelToDto);
    }
}
