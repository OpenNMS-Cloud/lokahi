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

package org.opennms.horizon.inventory.service.discovery.active;

import com.google.protobuf.Int64Value;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.inventory.discovery.IcmpActiveDiscoveryCreateDTO;
import org.opennms.horizon.inventory.discovery.IcmpActiveDiscoveryDTO;
import org.opennms.horizon.inventory.dto.TagCreateListDTO;
import org.opennms.horizon.inventory.dto.TagEntityIdDTO;
import org.opennms.horizon.inventory.dto.TagRemoveListDTO;
import org.opennms.horizon.inventory.mapper.discovery.IcmpActiveDiscoveryMapper;
import org.opennms.horizon.inventory.model.Tag;
import org.opennms.horizon.inventory.model.discovery.active.IcmpActiveDiscovery;
import org.opennms.horizon.inventory.repository.discovery.active.IcmpActiveDiscoveryRepository;
import org.opennms.horizon.inventory.service.TagService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class IcmpActiveDiscoveryService {

    private final IcmpActiveDiscoveryRepository repository;
    private final IcmpActiveDiscoveryMapper mapper;
    private final TagService tagService;

    @Transactional
    public IcmpActiveDiscoveryDTO createActiveDiscovery(IcmpActiveDiscoveryCreateDTO request, String tenantId) {

        IcmpActiveDiscovery discovery = mapper.dtoToModel(request);
        discovery.setTenantId(tenantId);
        discovery.setCreateTime(LocalDateTime.now());
        discovery = repository.save(discovery);
        tagService.addTags(tenantId, TagCreateListDTO.newBuilder()
            .addEntityIds(TagEntityIdDTO.newBuilder()
                .setActiveDiscoveryId(discovery.getId()))
            .addAllTags(request.getTagsList())
            .build());

        return mapper.modelToDto(discovery);
    }

    @Transactional
    public IcmpActiveDiscoveryDTO upsertActiveDiscovery(IcmpActiveDiscoveryCreateDTO request, String tenantId) {
        if (request.hasId()) {
            var optionalDiscovery = repository.findByIdAndTenantId(request.getId(), tenantId);
            if (optionalDiscovery.isEmpty()) {
                throw new IllegalArgumentException("Discovery with Id" + request.getId() + " doesn't exist");
            }
            var discovery = optionalDiscovery.get();
            mapper.updateFromDto(request, discovery);
            discovery.setCreateTime(LocalDateTime.now());
            repository.save(discovery);
            tagService.updateTags(tenantId, TagCreateListDTO.newBuilder()
                .addEntityIds(TagEntityIdDTO.newBuilder()
                    .setActiveDiscoveryId(discovery.getId()))
                .addAllTags(request.getTagsList())
                .build());
            return mapper.modelToDto(discovery);
        } else {
            return createActiveDiscovery(request, tenantId);
        }
    }

    @Transactional
    public Boolean deleteActiveDiscovery(long id, String tenantId) {
        try {
            var discovery = repository.findByIdAndTenantId(id, tenantId);
            if (discovery.isEmpty()) {
                return false;
            }
            var icmpActiveDiscovery = discovery.get();
            var tags = icmpActiveDiscovery.getTags();
            tagService.removeTags(tenantId, TagRemoveListDTO.newBuilder()
                    .addEntityIds(TagEntityIdDTO.newBuilder()
                        .setActiveDiscoveryId(icmpActiveDiscovery.getId())
                        .build())
                    .addAllTagIds(tags.stream().map(Tag::getId).map(Int64Value::of).toList())
                .build());
            repository.deleteById(icmpActiveDiscovery.getId());
            return true;
        } catch (Exception e) {
            log.error("Exception while deleting active discovery with id {}", id, e);
            return false;
        }
    }

    @Transactional(readOnly = true)
    public List<IcmpActiveDiscoveryDTO> getActiveDiscoveries(String tenantId) {
        var entities = repository.findByTenantId(tenantId);
        return entities.stream().map(mapper::modelToDto).toList();
    }

    @Transactional(readOnly = true)
    public Optional<IcmpActiveDiscoveryDTO> getDiscoveryById(long id, String tenantId) {
        var optional = repository.findByIdAndTenantId(id, tenantId);
        return optional.map(mapper::modelToDto);
    }
}
