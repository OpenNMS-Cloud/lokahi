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

package org.opennms.horizon.inventory.service.taskset.response;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.inventory.dto.MonitoredServiceDTO;
import org.opennms.horizon.inventory.dto.MonitoredServiceTypeDTO;
import org.opennms.horizon.inventory.dto.MonitoredState;
import org.opennms.horizon.inventory.model.IpInterface;
import org.opennms.horizon.inventory.model.MonitoredServiceType;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.repository.IpInterfaceRepository;
import org.opennms.horizon.inventory.repository.NodeRepository;
import org.opennms.horizon.inventory.service.MonitoredServiceService;
import org.opennms.horizon.inventory.service.MonitoredServiceTypeService;
import org.opennms.horizon.inventory.service.taskset.TaskSetHandler;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.opennms.taskset.contract.DetectorResponse;
import org.opennms.taskset.contract.MonitorResponse;
import org.opennms.taskset.contract.MonitorType;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonitorResponseService {
    private final NodeRepository nodeRepository;

    public void accept(String tenantId, MonitorResponse response) {
        log.info("Received Monitor Response = {} for tenant = {}", response, tenantId);
        switch (response.getMonitorType()) {
            case UNRECOGNIZED, UNKNOWN, ECHO -> {
                return;
            }
        }
        setMonitoredStateMonitored(tenantId, response.getNodeId());
    }

    private void setMonitoredStateMonitored(String tenantId, long nodeId) {
        Optional<Node> nodeOpt = nodeRepository.findByIdAndTenantId(nodeId, tenantId);
        if (nodeOpt.isPresent()) {
            Node node = nodeOpt.get();
            if (MonitoredState.MONITORED != node.getMonitoredState()) {
                node.setMonitoredState(MonitoredState.MONITORED);
                nodeRepository.save(node);
            }
        } else {
            log.info("No node found with ID: {}", nodeId);
        }
    }
}
