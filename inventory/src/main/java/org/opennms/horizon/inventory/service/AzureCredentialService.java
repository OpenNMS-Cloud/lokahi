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
import org.apache.commons.lang3.StringUtils;
import org.opennms.horizon.inventory.dto.AzureCredentialCreateDTO;
import org.opennms.horizon.inventory.dto.AzureCredentialDTO;
import org.opennms.horizon.inventory.exception.InventoryRuntimeException;
import org.opennms.horizon.inventory.mapper.AzureCredentialMapper;
import org.opennms.horizon.inventory.model.AzureCredential;
import org.opennms.horizon.inventory.model.MonitoringLocation;
import org.opennms.horizon.inventory.repository.AzureCredentialRepository;
import org.opennms.horizon.inventory.repository.MonitoringLocationRepository;
import org.opennms.horizon.inventory.service.taskset.ScannerTaskSetService;
import org.opennms.horizon.inventory.service.taskset.TaskUtils;
import org.opennms.horizon.shared.azure.http.AzureHttpClient;
import org.opennms.horizon.shared.azure.http.AzureHttpException;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AzureCredentialService {
    private final AzureHttpClient client;
    private final AzureCredentialMapper mapper;
    private final AzureCredentialRepository repository;
    private final MonitoringLocationRepository locationRepository;
    private final ConfigUpdateService configUpdateService;
    private final ScannerTaskSetService scannerTaskSetService;

    public AzureCredentialDTO createCredentials(String tenantId, AzureCredentialCreateDTO request) {
        validateCredentials(request);

        MonitoringLocation monitoringLocation = getMonitoringLocation(tenantId, request);

        AzureCredential credential = mapper.dtoToModel(request);
        credential.setTenantId(tenantId);
        credential.setCreateTime(LocalDateTime.now());
        credential.setMonitoringLocation(monitoringLocation);
        credential = repository.save(credential);

        scannerTaskSetService.sendAzureScannerTask(credential);

        return mapper.modelToDto(credential);
    }

    private void validateCredentials(AzureCredentialCreateDTO request) {
        try {
            client.login(request.getDirectoryId(),
                request.getClientId(), request.getClientSecret(), TaskUtils.Azure.DEFAULT_TIMEOUT);
        } catch (AzureHttpException e) {
            throw new InventoryRuntimeException("Failed to login with azure credentials", e);
        }
    }

    private MonitoringLocation getMonitoringLocation(String tenantId, AzureCredentialCreateDTO request) {
        String location = StringUtils.isEmpty(request.getLocation())
            ? GrpcConstants.DEFAULT_LOCATION: request.getLocation();

        Optional<MonitoringLocation> locationOp = locationRepository
            .findByLocationAndTenantId(location, tenantId);

        if (locationOp.isPresent()) {
            return locationOp.get();
        }

        MonitoringLocation monitoringLocation = new MonitoringLocation();
        monitoringLocation.setLocation(location);
        monitoringLocation.setTenantId(tenantId);
        monitoringLocation = locationRepository.save(monitoringLocation);

        // Send config updates asynchronously to Minion
        configUpdateService.sendConfigUpdate(tenantId, location);

        return monitoringLocation;
    }
}
