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
import org.opennms.horizon.inventory.dto.AzureCredentialCreateDTO;
import org.opennms.horizon.inventory.dto.AzureCredentialDTO;
import org.opennms.horizon.inventory.exception.InventoryRuntimeException;
import org.opennms.horizon.inventory.mapper.AzureCredentialMapper;
import org.opennms.horizon.inventory.model.AzureCredential;
import org.opennms.horizon.inventory.repository.AzureCredentialRepository;
import org.opennms.horizon.inventory.service.taskset.ScannerTaskSetService;
import org.opennms.horizon.inventory.service.taskset.TaskUtils;
import org.opennms.horizon.shared.azure.http.AzureHttpClient;
import org.opennms.horizon.shared.azure.http.AzureHttpException;
import org.opennms.horizon.shared.azure.http.dto.login.AzureOAuthToken;
import org.opennms.horizon.shared.azure.http.dto.resourcegroup.AzureResourceGroups;
import org.opennms.horizon.shared.azure.http.dto.resourcegroup.AzureValue;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AzureCredentialService {
    private final AzureHttpClient client;
    private final AzureCredentialMapper mapper;
    private final AzureCredentialRepository repository;
    private final ScannerTaskSetService scannerTaskSetService;

    public AzureCredentialDTO createCredentials(String tenantId, AzureCredentialCreateDTO request) {
        List<String> resourceGroups = getResourceGroups(request);

        AzureCredential credential = mapper.dtoToModel(request);
        credential.setResourceGroup(String.join(",", resourceGroups)); //todo: add 1-to-many relationship
        credential.setTenantId(tenantId);
        credential.setCreateTime(LocalDateTime.now());
        credential = repository.save(credential);

        scannerTaskSetService.sendAzureScannerTask(credential);

        return mapper.modelToDto(credential);
    }

    private List<String> getResourceGroups(AzureCredentialCreateDTO request) {
        AzureOAuthToken token = login(request);

        try {
            AzureResourceGroups resourceGroups = client.getResourceGroups(token,
                request.getSubscriptionId(), TaskUtils.Azure.DEFAULT_TIMEOUT);

            return resourceGroups.getValue().stream()
                .map(AzureValue::getName)
                .collect(Collectors.toList());

        } catch (AzureHttpException e) {
            throw new InventoryRuntimeException("Failed to get resource groups for subscription", e);
        }
    }

    private AzureOAuthToken login(AzureCredentialCreateDTO request) {
        try {
            return client.login(request.getDirectoryId(),
                request.getClientId(), request.getClientSecret(), TaskUtils.Azure.DEFAULT_TIMEOUT);
        } catch (AzureHttpException e) {
            throw new InventoryRuntimeException("Failed to login with azure credentials", e);
        }
    }
}
