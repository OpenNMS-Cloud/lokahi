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
package org.opennms.horizon.inventory.component;

import com.google.common.base.Strings;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.events.proto.Event;
import org.opennms.horizon.events.proto.EventLog;
import org.opennms.horizon.inventory.dto.ListTagsByEntityIdParamsDTO;
import org.opennms.horizon.inventory.dto.MonitoredState;
import org.opennms.horizon.inventory.dto.NodeCreateDTO;
import org.opennms.horizon.inventory.dto.TagCreateDTO;
import org.opennms.horizon.inventory.dto.TagEntityIdDTO;
import org.opennms.horizon.inventory.exception.EntityExistException;
import org.opennms.horizon.inventory.exception.InventoryRuntimeException;
import org.opennms.horizon.inventory.exception.LocationNotFoundException;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.service.NodeService;
import org.opennms.horizon.inventory.service.TagService;
import org.opennms.horizon.inventory.service.discovery.PassiveDiscoveryService;
import org.opennms.horizon.shared.events.EventConstants;
import org.opennms.taskset.contract.ScanType;
import org.springframework.context.annotation.PropertySource;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Slf4j
@Component
@PropertySource("classpath:application.yml")
public class InternalEventConsumer {
    private final NodeService nodeService;
    private final PassiveDiscoveryService passiveDiscoveryService;
    private final TagService tagService;

    @KafkaListener(topics = "${kafka.topics.internal-events}", concurrency = "${kafka.concurrency.internal-events}")
    public void consumeInternalEvents(@Payload byte[] data) {
        try {
            var eventLog = EventLog.parseFrom(data);
            eventLog.getEventsList().forEach(this::handleNewSuspectEvent);
        } catch (InvalidProtocolBufferException e) {
            log.error("Error while parsing Event. Payload: {}", Arrays.toString(data), e);
        }
    }

    private void handleNewSuspectEvent(Event event) {
        try {
            if (event.getUei().equals(EventConstants.NEW_SUSPECT_INTERFACE_EVENT_UEI)) {
                if (Strings.isNullOrEmpty(event.getTenantId())) {
                    throw new InventoryRuntimeException("Missing tenant id on event: " + event);
                }
                var tenantId = event.getTenantId();
                var locationId = event.getLocationId();
                var optionalNode = nodeService.getNode(event.getIpAddress(), Long.parseLong(locationId), tenantId);
                if (optionalNode.isPresent()) {
                    log.warn(
                            "Node already exists with the Ip Address {} at location {} for tenant {}",
                            event.getIpAddress(),
                            event.getLocationId(),
                            event.getTenantId());
                    return;
                }
                NodeCreateDTO.Builder nodeCreateBuilder = NodeCreateDTO.newBuilder()
                        .setLocationId(locationId)
                        .setManagementIp(event.getIpAddress())
                        .setLabel(event.getIpAddress())
                        .setMonitoredState(MonitoredState.DETECTED);
                var passiveDiscovery =
                        passiveDiscoveryService.getPassiveDiscovery(Long.parseLong(locationId), tenantId);
                if (passiveDiscovery != null) {
                    nodeCreateBuilder.addDiscoveryIds(passiveDiscovery.getId());
                    var list = ListTagsByEntityIdParamsDTO.newBuilder()
                            .setEntityId(TagEntityIdDTO.newBuilder()
                                    .setPassiveDiscoveryId(passiveDiscovery.getId())
                                    .build())
                            .build();
                    var tagList = tagService.getTagsByEntityId(tenantId, list);
                    List<TagCreateDTO> tags = tagList.stream()
                            .map(tag -> TagCreateDTO.newBuilder()
                                    .setName(tag.getName())
                                    .build())
                            .toList();
                    nodeCreateBuilder.addAllTags(tags);
                }
                log.debug(
                        "Create new node from event with tenantId={}; locationId={}; interface={}",
                        event.getIpAddress(),
                        locationId,
                        tenantId);
                Node node = nodeService.createNode(nodeCreateBuilder.build(), ScanType.DISCOVERY_SCAN, tenantId);
                nodeService.updateNodeMonitoredState(node.getId(), node.getTenantId());
                passiveDiscoveryService.sendNodeScan(node, passiveDiscovery);
            }
        } catch (EntityExistException e) {
            log.error("Duplicated device error.", e);
        } catch (LocationNotFoundException e) {
            log.error("Location not found.", e);
        }
    }
}
