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

package org.opennms.horizon.inventory.service.node;

import lombok.RequiredArgsConstructor;
import org.opennms.horizon.inventory.dto.AzureNodeCreateDTO;
import org.opennms.horizon.inventory.dto.TagCreateListDTO;
import org.opennms.horizon.inventory.dto.TagEntityIdDTO;
import org.opennms.horizon.inventory.model.MonitoringLocation;
import org.opennms.horizon.inventory.model.node.AzureNode;
import org.opennms.horizon.inventory.repository.node.AzureNodeRepository;
import org.opennms.horizon.inventory.service.IpInterfaceService;
import org.opennms.horizon.inventory.service.MonitoringLocationService;
import org.opennms.horizon.inventory.service.TagService;
import org.opennms.taskset.contract.ScanType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class AzureNodeService {
    private final AzureNodeRepository azureNodeRepository;
    private final IpInterfaceService ipInterfaceService;
    private final MonitoringLocationService monitoringLocationService;
    private final TagService tagService;

    @Transactional
    public AzureNode createNode(String tenantId, AzureNodeCreateDTO request) {
        MonitoringLocation monitoringLocation =
            monitoringLocationService.saveMonitoringLocation(tenantId, request.getLocation());

        AzureNode node = saveNode(request, monitoringLocation, tenantId);

        if (request.hasManagementIp()) {
            ipInterfaceService.saveIpInterface(tenantId, node, request.getManagementIp());
        }

        tagService.addTags(tenantId, TagCreateListDTO.newBuilder()
            .addEntityIds(TagEntityIdDTO.newBuilder()
                .setNodeId(node.getId()))
            .addAllTags(request.getTagsList())
            .build());

        return node;
    }

    private AzureNode saveNode(AzureNodeCreateDTO request, MonitoringLocation monitoringLocation, String tenantId) {
        AzureNode node = new AzureNode();

        node.setTenantId(tenantId);
        node.setNodeLabel(request.getLabel());
        node.setScanType(ScanType.AZURE_SCAN);
        if (request.hasMonitoredState()) {
            node.setMonitoredState(request.getMonitoredState());
        }
        node.setCreateTime(LocalDateTime.now());
        node.setMonitoringLocation(monitoringLocation);
        node.setMonitoringLocationId(monitoringLocation.getId());

        return azureNodeRepository.save(node);
    }
}
