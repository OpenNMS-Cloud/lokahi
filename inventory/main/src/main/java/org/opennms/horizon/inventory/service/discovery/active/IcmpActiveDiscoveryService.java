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
package org.opennms.horizon.inventory.service.discovery.active;

import com.google.protobuf.Int64Value;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
import org.opennms.horizon.inventory.repository.discovery.active.ActiveDiscoveryRepository;
import org.opennms.horizon.inventory.repository.discovery.active.IcmpActiveDiscoveryRepository;
import org.opennms.horizon.inventory.service.MonitoringLocationService;
import org.opennms.horizon.inventory.service.TagService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class IcmpActiveDiscoveryService implements ActiveDiscoveryValidationService {

    private final IcmpActiveDiscoveryRepository repository;
    private final ActiveDiscoveryRepository activeDiscoveryRepository;
    private final MonitoringLocationService monitoringLocationService;
    private final IcmpActiveDiscoveryMapper mapper;
    private final TagService tagService;

    @Transactional
    public IcmpActiveDiscoveryDTO createActiveDiscovery(IcmpActiveDiscoveryCreateDTO request, String tenantId) {
        validateActiveDiscoveryName(request.getName(), tenantId);
        validateLocation(request.getLocationId(), tenantId);

        IcmpActiveDiscovery discovery = mapper.dtoToModel(request);
        discovery.setTenantId(tenantId);
        discovery.setCreateTime(LocalDateTime.now());
        discovery = repository.save(discovery);
        tagService.addTags(
                tenantId,
                TagCreateListDTO.newBuilder()
                        .addEntityIds(TagEntityIdDTO.newBuilder().setActiveDiscoveryId(discovery.getId()))
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
            validateActiveDiscoveryName(request.getName(), discovery.getId(), tenantId);

            if (!String.valueOf(discovery.getLocationId()).equals(request.getLocationId())) {
                validateLocation(request.getLocationId(), tenantId);
            }
            mapper.updateFromDto(request, discovery);
            discovery.setCreateTime(LocalDateTime.now());
            repository.save(discovery);
            tagService.updateTags(
                    tenantId,
                    TagCreateListDTO.newBuilder()
                            .addEntityIds(TagEntityIdDTO.newBuilder().setActiveDiscoveryId(discovery.getId()))
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
            tagService.removeTags(
                    tenantId,
                    TagRemoveListDTO.newBuilder()
                            .addEntityIds(TagEntityIdDTO.newBuilder()
                                    .setActiveDiscoveryId(icmpActiveDiscovery.getId())
                                    .build())
                            .addAllTagIds(tags.stream()
                                    .map(Tag::getId)
                                    .map(Int64Value::of)
                                    .toList())
                            .build());
            repository.deleteById(icmpActiveDiscovery.getId());
            return true;
        } catch (Exception e) {
            log.error("Exception while deleting active discovery with id {}", id, e);
            return false;
        }
    }

    public List<IcmpActiveDiscoveryDTO> getActiveDiscoveries(String tenantId) {
        var entities = repository.findByTenantId(tenantId);
        return entities.stream().map(mapper::modelToDto).toList();
    }

    public Optional<IcmpActiveDiscoveryDTO> getDiscoveryById(long id, String tenantId) {
        var optional = repository.findByIdAndTenantId(id, tenantId);
        return optional.map(mapper::modelToDto);
    }

    @Override
    public ActiveDiscoveryRepository getActiveDiscoveryRepository() {
        return activeDiscoveryRepository;
    }

    @Override
    public MonitoringLocationService getMonitoringLocationService() {
        return monitoringLocationService;
    }
}
