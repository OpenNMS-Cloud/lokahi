/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022-2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
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
package org.opennms.horizon.inventory.service.discovery.active;

import lombok.RequiredArgsConstructor;
import org.opennms.horizon.inventory.dto.AzureActiveDiscoveryCreateDTO;
import org.opennms.horizon.inventory.dto.AzureActiveDiscoveryDTO;
import org.opennms.horizon.inventory.dto.TagCreateListDTO;
import org.opennms.horizon.inventory.dto.TagEntityIdDTO;
import org.opennms.horizon.inventory.exception.InventoryRuntimeException;
import org.opennms.horizon.inventory.exception.LocationNotFoundException;
import org.opennms.horizon.inventory.mapper.discovery.AzureActiveDiscoveryMapper;
import org.opennms.horizon.inventory.model.discovery.active.AzureActiveDiscovery;
import org.opennms.horizon.inventory.repository.discovery.active.ActiveDiscoveryRepository;
import org.opennms.horizon.inventory.repository.discovery.active.AzureActiveDiscoveryRepository;
import org.opennms.horizon.inventory.service.MonitoringLocationService;
import org.opennms.horizon.inventory.service.TagService;
import org.opennms.horizon.inventory.service.taskset.ScannerTaskSetService;
import org.opennms.horizon.inventory.service.taskset.TaskUtils;
import org.opennms.horizon.shared.azure.http.AzureHttpClient;
import org.opennms.horizon.shared.azure.http.AzureHttpException;
import org.opennms.horizon.shared.azure.http.dto.error.AzureHttpError;
import org.opennms.horizon.shared.azure.http.dto.login.AzureOAuthToken;
import org.opennms.horizon.shared.azure.http.dto.subscription.AzureSubscription;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AzureActiveDiscoveryService implements ActiveDiscoveryValidationService {
    private static final String SUB_ENABLED_STATE = "Enabled";

    private final AzureHttpClient client;
    private final AzureActiveDiscoveryMapper mapper;
    private final AzureActiveDiscoveryRepository repository;
    private final ActiveDiscoveryRepository activeDiscoveryRepository;
    private final ScannerTaskSetService scannerTaskSetService;
    private final MonitoringLocationService monitoringLocationService;
    private final TagService tagService;

    public AzureActiveDiscoveryDTO createActiveDiscovery(String tenantId, AzureActiveDiscoveryCreateDTO request) {
        validateDiscovery(tenantId, request);

        AzureActiveDiscovery discovery = mapper.dtoToModel(request);
        discovery.setTenantId(tenantId);
        discovery.setCreateTime(LocalDateTime.now());
        discovery = repository.save(discovery);

        tagService.addTags(tenantId, TagCreateListDTO.newBuilder()
            .addEntityIds(TagEntityIdDTO.newBuilder()
                .setActiveDiscoveryId(discovery.getId()))
            .addAllTags(request.getTagsList())
            .build());

        // Asynchronously send task sets to Minion
        scannerTaskSetService.sendAzureScannerTaskAsync(discovery);

        return mapper.modelToDto(discovery);
    }

    private void validateDiscovery(String tenantId, AzureActiveDiscoveryCreateDTO request) {
        validateAlreadyExists(tenantId, request);
        validateActiveDiscoveryName(request.getName(), tenantId);
        validateLocation(request.getLocationId(), tenantId);

        AzureOAuthToken token;
        try {
            token = client.login(request.getDirectoryId(), request.getClientId(),
                request.getClientSecret(), TaskUtils.AZURE_DEFAULT_TIMEOUT_MS, TaskUtils.AZURE_DEFAULT_RETRIES);
        } catch (AzureHttpException e) {
            if (e.hasHttpError()) {
                AzureHttpError httpError = e.getHttpError();
                throw new InventoryRuntimeException(httpError.toString(), e);
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
            if (e.hasHttpError()) {
                AzureHttpError httpError = e.getHttpError();
                throw new InventoryRuntimeException(httpError.toString(), e);
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

    private void validateAlreadyExists(String tenantId, AzureActiveDiscoveryCreateDTO request) {
        Optional<AzureActiveDiscovery> azureDiscoveryOpt = repository
            .findByTenantIdAndSubscriptionIdAndDirectoryIdAndClientId(tenantId,
                request.getSubscriptionId(), request.getDirectoryId(), request.getClientId());
        if (azureDiscoveryOpt.isPresent()) {
            throw new InventoryRuntimeException("Azure Discovery already exists with the provided subscription, directory and client ID");
        }
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
