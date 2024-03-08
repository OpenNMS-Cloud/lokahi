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
package org.opennms.horizon.inventory.service.discovery;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.opennms.horizon.inventory.dto.MonitoredState;
import org.opennms.horizon.inventory.dto.PassiveDiscoveryDTO;
import org.opennms.horizon.inventory.dto.PassiveDiscoveryToggleDTO;
import org.opennms.horizon.inventory.dto.PassiveDiscoveryUpsertDTO;
import org.opennms.horizon.inventory.dto.TagCreateListDTO;
import org.opennms.horizon.inventory.dto.TagEntityIdDTO;
import org.opennms.horizon.inventory.exception.InventoryRuntimeException;
import org.opennms.horizon.inventory.exception.LocationNotFoundException;
import org.opennms.horizon.inventory.mapper.discovery.PassiveDiscoveryMapper;
import org.opennms.horizon.inventory.model.MonitoringLocation;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.model.discovery.PassiveDiscovery;
import org.opennms.horizon.inventory.repository.NodeRepository;
import org.opennms.horizon.inventory.repository.discovery.PassiveDiscoveryRepository;
import org.opennms.horizon.inventory.service.Constants;
import org.opennms.horizon.inventory.service.MonitoringLocationService;
import org.opennms.horizon.inventory.service.TagService;
import org.opennms.horizon.inventory.service.taskset.ScannerTaskSetService;
import org.opennms.horizon.snmp.api.SnmpConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Component
@RequiredArgsConstructor
public class PassiveDiscoveryService {
    private static final Logger log = LoggerFactory.getLogger(PassiveDiscoveryService.class);
    private final PassiveDiscoveryMapper mapper;
    private final PassiveDiscoveryRepository repository;
    private final TagService tagService;
    private final NodeRepository nodeRepository;
    private final ScannerTaskSetService scannerTaskSetService;
    private final MonitoringLocationService monitoringLocationService;

    @Transactional
    public PassiveDiscoveryDTO createDiscovery(String tenantId, PassiveDiscoveryUpsertDTO request) {
        validateDiscovery(tenantId, request);
        validateSnmpPorts(request);
        validateCommunityStrings(request);

        PassiveDiscovery discovery = mapper.dtoToModel(request);
        discovery.setTenantId(tenantId);
        discovery.setToggle(true);
        discovery.setCreateTime(LocalDateTime.now());
        discovery = repository.save(discovery);

        tagService.addTags(
                tenantId,
                TagCreateListDTO.newBuilder()
                        .addEntityIds(TagEntityIdDTO.newBuilder().setPassiveDiscoveryId(discovery.getId()))
                        .addAllTags(request.getTagsList())
                        .build());

        sendNodeScan(discovery);

        return mapper.modelToDto(discovery);
    }

    @Transactional
    public PassiveDiscoveryDTO updateDiscovery(String tenantId, PassiveDiscoveryUpsertDTO request) {
        long id = request.getId();
        Optional<PassiveDiscovery> discoveryOpt = repository.findByTenantIdAndId(tenantId, id);
        if (discoveryOpt.isEmpty()) {
            throw new InventoryRuntimeException("Passive discovery not found for id: " + id);
        }

        validateDiscovery(tenantId, request);

        validateSnmpPorts(request);
        validateCommunityStrings(request);

        PassiveDiscovery discovery = discoveryOpt.get();
        mapper.updateFromDto(request, discovery);
        discovery = repository.save(discovery);

        tagService.updateTags(
                tenantId,
                TagCreateListDTO.newBuilder()
                        .addEntityIds(TagEntityIdDTO.newBuilder().setPassiveDiscoveryId(discovery.getId()))
                        .addAllTags(request.getTagsList())
                        .build());

        sendNodeScan(discovery);

        return mapper.modelToDto(discovery);
    }

    public List<PassiveDiscoveryDTO> getPassiveDiscoveries(String tenantId) {
        List<PassiveDiscovery> discoveries = repository.findByTenantId(tenantId);
        return discoveries.stream().map(mapper::modelToDto).toList();
    }

    public PassiveDiscoveryDTO getPassiveDiscovery(long locationId, String tenantId) {
        return repository
                .findByTenantIdAndLocationId(tenantId, locationId)
                .map(mapper::modelToDto)
                .orElse(null);
    }

    public PassiveDiscoveryDTO toggleDiscovery(String tenantId, PassiveDiscoveryToggleDTO request) {
        Optional<PassiveDiscovery> discoveryOpt = repository.findByTenantIdAndId(tenantId, request.getId());
        if (discoveryOpt.isPresent()) {
            PassiveDiscovery discovery = discoveryOpt.get();
            discovery.setToggle(request.getToggle());
            discovery = repository.save(discovery);

            if (discovery.isToggle()) {
                sendNodeScan(discovery);
            }

            return mapper.modelToDto(discovery);
        }
        throw new InventoryRuntimeException("Passive discovery not found, cannot update toggle");
    }

    private void validateDiscovery(String tenantId, PassiveDiscoveryUpsertDTO dto) {
        if (StringUtils.isBlank(dto.getName())) {
            throw new InventoryRuntimeException("Discovery name is blank");
        }
        if (repository.findByTenantIdAndName(tenantId, dto.getName().trim()).stream()
                .anyMatch(d -> d.getId() != dto.getId())) {
            throw new InventoryRuntimeException("Duplicate passive discovery with name " + dto.getName());
        }
        var location =
                monitoringLocationService.findByLocationIdAndTenantId(Long.parseLong(dto.getLocationId()), tenantId);
        if (location.isEmpty()) {
            throw new LocationNotFoundException("Location not found with location " + dto.getLocationId());
        }
        Optional<PassiveDiscovery> discoveryOpt =
                repository.findByTenantIdAndLocationId(tenantId, Long.valueOf(dto.getLocationId()));
        if (discoveryOpt.isPresent()) {
            PassiveDiscovery discovery = discoveryOpt.get();

            if (discovery.getId() != dto.getId()) {
                throw new InventoryRuntimeException("Already a passive discovery with location " + dto.getLocationId());
            }
        }
    }

    public void validateSnmpPorts(PassiveDiscoveryUpsertDTO dto) {
        List<Integer> snmpPorts = dto.getPortsList();
        for (Integer port : snmpPorts) {
            if (port < Constants.SNMP_PORT_MIN || port > Constants.SNMP_PORT_MAX) {
                String message = String.format(
                        "SNMP port is not in range [%d,%d] with value: %d",
                        Constants.SNMP_PORT_MIN, Constants.SNMP_PORT_MAX, port);
                throw new InventoryRuntimeException(message);
            }
        }
    }

    public void validateCommunityStrings(PassiveDiscoveryUpsertDTO passiveDiscovery) throws InventoryRuntimeException {
        String snmpCommunities = "";
        for (String snmpCommunity : passiveDiscovery.getCommunitiesList()) {
            snmpCommunities += snmpCommunity.replace(",", "") + " ";
        }
        if (snmpCommunities.length() > 128) {
            throw new InventoryRuntimeException("Snmp communities string is too long");
        }
        for (byte b : snmpCommunities.getBytes()) {
            char c = (char) b;
            if (c > 127) {
                throw new InventoryRuntimeException("All characters must be 7bit ascii");
            }
        }
        if (snmpCommunities.length() > 128) {
            throw new InventoryRuntimeException("Snmp communities string is too long");
        }
    }

    private void sendNodeScan(PassiveDiscovery discovery) {
        if (discovery.isToggle()) {
            String tenantId = discovery.getTenantId();
            Long locationId = discovery.getLocationId();

            List<Node> detectedNodes = nodeRepository.findByTenantIdAndLocationId(tenantId, locationId);

            if (!CollectionUtils.isEmpty(detectedNodes)) {
                for (Node node : detectedNodes) {
                    sendTaskSetsToMinion(node, mapper.modelToDto(discovery));
                }
            }
        } else {
            log.info("Passive discovery is toggled off");
        }
    }

    public void sendNodeScan(Node node, PassiveDiscoveryDTO passiveDiscovery) {
        if (node.getMonitoredState() != MonitoredState.DETECTED) {
            log.info(
                    "Node is not in monitored state DETECTED, so not sending node scan for node {}",
                    node.getNodeLabel());
            return;
        }
        MonitoringLocation monitoringLocation = node.getMonitoringLocation();
        String location = monitoringLocation.getLocation();

        if (passiveDiscovery != null) {
            if (passiveDiscovery.getToggle()) {
                sendTaskSetsToMinion(node, passiveDiscovery);
            } else {
                log.info("Passive discovery is toggled off for location {}", location);
            }
        } else {
            log.info("No Passive discovery found for location {}", location);
        }
    }

    private void sendTaskSetsToMinion(Node node, PassiveDiscoveryDTO discovery) {
        List<SnmpConfiguration> snmpConfigs = new ArrayList<>();

        discovery.getCommunitiesList().forEach(readCommunity -> {
            var builder = SnmpConfiguration.newBuilder().setReadCommunity(readCommunity);
            snmpConfigs.add(builder.build());
        });
        discovery.getPortsList().forEach(port -> {
            var builder = SnmpConfiguration.newBuilder().setPort(port);
            snmpConfigs.add(builder.build());
        });
        scannerTaskSetService.sendNodeScannerTask(node, Long.parseLong(discovery.getLocationId()), snmpConfigs);
    }

    @Transactional
    public void deleteDiscovery(String tenantId, long id) {
        Optional<PassiveDiscovery> passiveDiscoveryOpt = repository.findByTenantIdAndId(tenantId, id);
        if (passiveDiscoveryOpt.isPresent()) {
            PassiveDiscovery discovery = passiveDiscoveryOpt.get();
            repository.delete(discovery);
        } else {
            throw new InventoryRuntimeException("Discovery not found.");
        }
    }
}
