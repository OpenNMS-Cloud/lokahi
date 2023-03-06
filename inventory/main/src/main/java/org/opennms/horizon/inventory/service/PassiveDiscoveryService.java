/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
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
import org.opennms.horizon.inventory.dto.PassiveDiscoveryDTO;
import org.opennms.horizon.inventory.dto.PassiveDiscoveryToggleDTO;
import org.opennms.horizon.inventory.dto.PassiveDiscoveryUpsertDTO;
import org.opennms.horizon.inventory.dto.TagCreateListDTO;
import org.opennms.horizon.inventory.exception.InventoryRuntimeException;
import org.opennms.horizon.inventory.mapper.PassiveDiscoveryMapper;
import org.opennms.horizon.inventory.model.PassiveDiscovery;
import org.opennms.horizon.inventory.repository.PassiveDiscoveryRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PassiveDiscoveryService {
    private final PassiveDiscoveryMapper mapper;
    private final PassiveDiscoveryRepository repository;
    private final TagService tagService;

    @Transactional
    public PassiveDiscoveryDTO createDiscovery(String tenantId, PassiveDiscoveryUpsertDTO request) {
        validateSnmpPorts(request);

        PassiveDiscovery discovery = mapper.dtoToModel(request);
        discovery.setTenantId(tenantId);
        discovery.setToggle(true);
        discovery.setCreateTime(LocalDateTime.now());
        discovery = repository.save(discovery);

        tagService.addTags(tenantId, TagCreateListDTO.newBuilder()
            .setPassiveDiscoveryId(discovery.getId())
            .addAllTags(request.getTagsList())
            .build());

        return mapper.modelToDtoCustom(discovery);
    }

    @Transactional
    public PassiveDiscoveryDTO updateDiscovery(String tenantId, PassiveDiscoveryUpsertDTO request) {
        long id = request.getId();
        Optional<PassiveDiscovery> discoveryOpt = repository.findByTenantIdAndId(tenantId, id);
        if (discoveryOpt.isEmpty()) {
            throw new InventoryRuntimeException("Passive discovery not found for id: " + id);
        }

        validateSnmpPorts(request);

        PassiveDiscovery discovery = discoveryOpt.get();
        mapper.updateFromDto(request, discovery);
        discovery = repository.save(discovery);

        tagService.updateTags(tenantId, TagCreateListDTO.newBuilder()
            .setPassiveDiscoveryId(discovery.getId())
            .addAllTags(request.getTagsList())
            .build());

        return mapper.modelToDtoCustom(discovery);
    }

    @Transactional(readOnly = true)
    public List<PassiveDiscoveryDTO> getPassiveDiscoveries(String tenantId) {
        List<PassiveDiscovery> discoveries = repository.findByTenantId(tenantId);
        return discoveries.stream().map(mapper::modelToDtoCustom).toList();
    }

    @Transactional
    public PassiveDiscoveryDTO toggleDiscovery(String tenantId, PassiveDiscoveryToggleDTO request) {
        Optional<PassiveDiscovery> discoveryOpt = repository.findByTenantIdAndId(tenantId,request.getId());
        if (discoveryOpt.isPresent()) {
            PassiveDiscovery discovery = discoveryOpt.get();
            discovery.setToggle(request.getToggle());
            discovery = repository.save(discovery);
            return mapper.modelToDtoCustom(discovery);
        }
        throw new InventoryRuntimeException("Passive discovery not found, cannot update toggle");
    }

    private void validateSnmpPorts(PassiveDiscoveryUpsertDTO dto) {
        List<Integer> snmpPorts = dto.getPortsList();
        for (Integer port : snmpPorts) {
            if (port < Constants.SNMP_PORT_MIN || port > Constants.SNMP_PORT_MAX) {
                String message = String.format("SNMP port is not in range [%d,%d] with value: %d",
                    Constants.SNMP_PORT_MIN, Constants.SNMP_PORT_MAX, port);
                throw new InventoryRuntimeException(message);
            }
        }
    }
}
