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
import org.opennms.horizon.inventory.dto.TagCreateListDTO;
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
import org.opennms.horizon.shared.azure.http.dto.error.AzureErrorDescription;
import org.opennms.horizon.shared.azure.http.dto.login.AzureOAuthToken;
import org.opennms.horizon.shared.azure.http.dto.subscription.AzureSubscription;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AzureCredentialService {
    private static final String SUB_ENABLED_STATE = "Enabled";

    private final AzureHttpClient client;
    private final AzureCredentialMapper mapper;
    private final AzureCredentialRepository repository;
    private final MonitoringLocationRepository locationRepository;
    private final ConfigUpdateService configUpdateService;
    private final ScannerTaskSetService scannerTaskSetService;
    private final TagService tagService;

    public AzureCredentialDTO createCredentials(String tenantId, AzureCredentialCreateDTO request) {
        validateCredentials(request);

        MonitoringLocation monitoringLocation = getMonitoringLocation(tenantId, request);

        AzureCredential credential = mapper.dtoToModel(request);
        credential.setTenantId(tenantId);
        credential.setCreateTime(LocalDateTime.now());
        credential.setMonitoringLocation(monitoringLocation);
        credential = repository.save(credential);

        tagService.addTags(tenantId, TagCreateListDTO.newBuilder()
            .setAzureCredentialId(credential.getId())
            .addAllTags(request.getTagsList())
            .build());

        // Asynchronously send task sets to Minion
        scannerTaskSetService.sendAzureScannerTaskAsync(credential);

        return mapper.modelToDto(credential);
    }

    private void validateCredentials(AzureCredentialCreateDTO request) {
        AzureOAuthToken token;
        try {
            token = client.login(request.getDirectoryId(), request.getClientId(),
                request.getClientSecret(), TaskUtils.AZURE_DEFAULT_TIMEOUT_MS, TaskUtils.AZURE_DEFAULT_RETRIES);
        } catch (AzureHttpException e) {
            if (e.hasDescription()) {
                AzureErrorDescription description = e.getDescription();
                throw new InventoryRuntimeException(description.toString(), e);
            }
            throw new InventoryRuntimeException("Failed to login with azure credentials", e);
        } catch (Exception e) {
            throw new InventoryRuntimeException("Failed to login with azure credentials", e);
        }
        AzureSubscription subscription;
        try {
            subscription = client.getSubscription(token, request.getSubscriptionId(),
                TaskUtils.AZURE_DEFAULT_TIMEOUT_MS, TaskUtils.AZURE_DEFAULT_RETRIES);
        } catch (AzureHttpException e) {
            if (e.hasDescription()) {
                AzureErrorDescription description = e.getDescription();
                throw new InventoryRuntimeException(description.toString(), e);
            }
            String message = String.format("Failed to get azure subscription %s", request.getSubscriptionId());
            throw new InventoryRuntimeException(message, e);
        } catch (Exception e) {
            String message = String.format("Failed to get azure subscription %s", request.getSubscriptionId());
            throw new InventoryRuntimeException(message, e);
        }
        if (!subscription.getState().equalsIgnoreCase(SUB_ENABLED_STATE)) {
            String message = String.format("Subscription %s is not enabled", request.getSubscriptionId());
            throw new InventoryRuntimeException(message);
        }
    }

    private MonitoringLocation getMonitoringLocation(String tenantId, AzureCredentialCreateDTO request) {
        String location = StringUtils.isEmpty(request.getLocation())
            ? GrpcConstants.DEFAULT_LOCATION : request.getLocation();

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
