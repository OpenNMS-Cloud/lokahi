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
import org.opennms.horizon.inventory.model.IpInterface;
import org.opennms.horizon.inventory.model.MonitoredServiceType;
import org.opennms.horizon.inventory.repository.IpInterfaceRepository;
import org.opennms.horizon.inventory.service.MonitoredServiceService;
import org.opennms.horizon.inventory.service.MonitoredServiceTypeService;
import org.opennms.horizon.inventory.service.taskset.TaskSetHandler;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.opennms.taskset.contract.DetectorResponse;
import org.opennms.taskset.contract.MonitorType;
import org.opennms.taskset.contract.TaskContext;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DetectorResponseService {
    private final IpInterfaceRepository ipInterfaceRepository;
    private final MonitoredServiceTypeService monitoredServiceTypeService;
    private final MonitoredServiceService monitoredServiceService;
    private final TaskSetHandler taskSetHandler;

    public void accept(String tenantId, String location, DetectorResponse response, TaskContext taskContext) {
        log.info("Received Detector Response = {} for tenant = {} and location = {}", response, tenantId, location);

        InetAddress ipAddress = InetAddressUtils.getInetAddress(response.getIpAddress());
        Optional<IpInterface> ipInterfaceOpt = ipInterfaceRepository
            .findByIpAddressAndLocationAndTenantId(ipAddress, location, tenantId);

        if (ipInterfaceOpt.isPresent() && taskContext.hasNodeId()) {
            IpInterface ipInterface = ipInterfaceOpt.get();

            if (response.getDetected()) {
                createMonitoredService(response, ipInterface);

                MonitorType monitorType = response.getMonitorType();
                long nodeId = taskContext.getNodeId();

                taskSetHandler.sendMonitorTask(location, monitorType, ipInterface, nodeId);
                taskSetHandler.sendCollectorTask(location, monitorType, ipInterface, nodeId);

            } else {
                log.info("{} not detected on ip address = {}", response.getMonitorType(), ipAddress.getAddress());
            }
        } else {
            log.warn("Failed to find IP Interface during detection for ip = {}", ipAddress.getHostAddress());
        }
    }

    private void createMonitoredService(DetectorResponse response, IpInterface ipInterface) {
        String tenantId = ipInterface.getTenantId();

        MonitoredServiceType monitoredServiceType =
            monitoredServiceTypeService.createSingle(MonitoredServiceTypeDTO.newBuilder()
                .setServiceName(response.getMonitorType().name())
                .setTenantId(tenantId)
                .build());

        MonitoredServiceDTO newMonitoredService = MonitoredServiceDTO.newBuilder()
            .setTenantId(tenantId)
            .build();

        monitoredServiceService.createSingle(newMonitoredService, monitoredServiceType, ipInterface);
    }
}
