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

package org.opennms.horizon.inventory.component;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.events.proto.Event;
import org.opennms.horizon.inventory.dto.MonitoredState;
import org.opennms.horizon.inventory.dto.NodeCreateDTO;
import org.opennms.horizon.inventory.dto.TagCreateDTO;
import org.opennms.horizon.inventory.exception.EntityExistException;
import org.opennms.horizon.inventory.exception.InventoryRuntimeException;
import org.opennms.horizon.inventory.exception.LocationNotFoundException;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.model.Tag;
import org.opennms.horizon.inventory.model.discovery.PassiveDiscovery;
import org.opennms.horizon.inventory.repository.discovery.PassiveDiscoveryRepository;
import org.opennms.horizon.inventory.service.NodeService;
import org.opennms.horizon.inventory.service.discovery.PassiveDiscoveryService;
import org.opennms.horizon.shared.events.EventConstants;
import org.opennms.taskset.contract.ScanType;
import org.springframework.context.annotation.PropertySource;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
@Component
@PropertySource("classpath:application.yml")
public class InternalEventConsumer {
    private final NodeService nodeService;
    private final PassiveDiscoveryService passiveDiscoveryService;
    private final PassiveDiscoveryRepository passiveDiscoveryRepository;

    @KafkaListener(topics = "${kafka.topics.internal-events}", concurrency = "1")
    @Transactional
    public void receiveNewSuspectEvent(@Payload byte[] data) {
        try {
            var event = Event.parseFrom(data);
            if(event.getUei().equals(EventConstants.NEW_SUSPECT_INTERFACE_EVENT_UEI)) {
                if (Strings.isNullOrEmpty(event.getTenantId())) {
                    throw new InventoryRuntimeException("Missing tenant id on event: " + event);
                }
                var tenantId = event.getTenantId();
                var locationId = event.getLocationId();
                log.debug("Create new node from event with tenantId={}; locationId={}; interface={}", event.getIpAddress(), locationId, tenantId);

                NodeCreateDTO.Builder nodeCreateBuilder = NodeCreateDTO.newBuilder()
                    .setLocationId(locationId)
                    .setManagementIp(event.getIpAddress())
                    .setLabel("trap-" + event.getIpAddress())
                    .setMonitoredState(MonitoredState.DETECTED);

                Optional<PassiveDiscovery> discoveryOpt = passiveDiscoveryRepository.findByTenantIdAndLocationId(tenantId, Long.valueOf(locationId));

                if (discoveryOpt.isPresent()){
                    PassiveDiscovery discovery = discoveryOpt.get();

                    List<TagCreateDTO> tagCreateDtoList = discovery.getTags().stream().map((Function<Tag, TagCreateDTO>) tag ->
                            TagCreateDTO.newBuilder().setName(tag.getName()).build()).toList();

                    nodeCreateBuilder.addAllTags(tagCreateDtoList);
                }

                Node node = nodeService.createNode(nodeCreateBuilder.build(), ScanType.NODE_SCAN, tenantId);
                passiveDiscoveryService.sendNodeScan(node);
            }
        } catch (InvalidProtocolBufferException e) {
            log.error("Error while parsing Event. Payload: {}", Arrays.toString(data), e);
        } catch (EntityExistException e) {
            log.error("Duplicated device error.", e);
        } catch (LocationNotFoundException e) {
            log.error("Location not found.", e);
        }
    }
}
