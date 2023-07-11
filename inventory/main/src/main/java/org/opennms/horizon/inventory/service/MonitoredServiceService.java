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
import org.opennms.horizon.inventory.dto.MonitoredServiceDTO;
import org.opennms.horizon.inventory.mapper.MonitoredServiceMapper;
import org.opennms.horizon.inventory.model.IpInterface;
import org.opennms.horizon.inventory.model.MonitoredService;
import org.opennms.horizon.inventory.model.MonitoredServiceType;
import org.opennms.horizon.inventory.repository.MonitoredServiceRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MonitoredServiceService {
    private final MonitoredServiceRepository modelRepo;

    private final MonitoredServiceMapper mapper;

    public MonitoredService createSingle(MonitoredServiceDTO newMonitoredService,
                             MonitoredServiceType monitoredServiceType,
                             IpInterface ipInterface) {

        String tenantId = newMonitoredService.getTenantId();

        Optional<MonitoredService> monitoredServiceOpt = modelRepo
            .findByTenantIdTypeAndIpInterface(tenantId, monitoredServiceType, ipInterface);

        if (monitoredServiceOpt.isEmpty()) {

            MonitoredService monitoredService = mapper.dtoToModel(newMonitoredService);
            monitoredService.setIpInterface(ipInterface);
            monitoredService.setMonitoredServiceType(monitoredServiceType);

            modelRepo.save(monitoredService);
            return monitoredService;
        }
        return monitoredServiceOpt.get();
    }

    public List<MonitoredServiceDTO> findByTenantId(String tenantId) {
        List<MonitoredService> all = modelRepo.findByTenantId(tenantId);
        return all
            .stream()
            .map(mapper::modelToDTO)
            .collect(Collectors.toList());
    }
}
