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

package org.opennms.horizon.minion.azure;

import com.google.protobuf.Any;
import org.opennms.azure.contract.AzureScanItem;
import org.opennms.azure.contract.AzureScanRequest;
import org.opennms.horizon.minion.plugin.api.AzureScannerResponse;
import org.opennms.horizon.minion.plugin.api.AzureScannerResponseImpl;
import org.opennms.horizon.minion.plugin.api.Scanner;
import org.opennms.horizon.shared.azure.http.AzureHttpClient;
import org.opennms.horizon.shared.azure.http.dto.login.AzureOAuthToken;
import org.opennms.horizon.shared.azure.http.dto.resourcegroup.AzureResourceGroups;
import org.opennms.horizon.shared.azure.http.dto.resourcegroup.AzureValue;
import org.opennms.horizon.shared.azure.http.dto.resources.AzureResources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class AzureScanner implements Scanner {
    private static final Logger log = LoggerFactory.getLogger(AzureScanner.class);
    private static final String MICROSOFT_COMPUTE_VIRTUAL_MACHINES = "Microsoft.Compute/virtualMachines";

    private final AzureHttpClient client;

    public AzureScanner(AzureHttpClient client) {
        this.client = client;
    }

    @Override
    public CompletableFuture<AzureScannerResponse> scan(Any config) {
        CompletableFuture<AzureScannerResponse> future = new CompletableFuture<>();

        try {
            if (!config.is(AzureScanRequest.class)) {
                throw new IllegalArgumentException("configuration must be an AzureScanRequest; type-url=" + config.getTypeUrl());
            }

            AzureScanRequest request = config.unpack(AzureScanRequest.class);

            AzureOAuthToken token = client.login(request.getDirectoryId(),
                request.getClientId(), request.getClientSecret(), request.getTimeout(), request.getRetries());

            List<AzureScanItem> scannedItems = new LinkedList<>();

            AzureResourceGroups resourceGroups = client
                .getResourceGroups(token, request.getSubscriptionId(), request.getTimeout(), request.getRetries());

            for (AzureValue resourceGroupValue : resourceGroups.getValue()) {
                String resourceGroup = resourceGroupValue.getName();

                AzureResources resources = client.getResources(token, request.getSubscriptionId(),
                    resourceGroup, request.getTimeout(), request.getRetries());

                scannedItems.addAll(resources.getValue().stream()
                    .filter(azureValue -> azureValue.getType()
                        .equalsIgnoreCase(MICROSOFT_COMPUTE_VIRTUAL_MACHINES))
                    .map(azureValue -> AzureScanItem.newBuilder()
                        .setId(azureValue.getId())
                        .setName(azureValue.getName())
                        .setResourceGroup(resourceGroup)
                        .setCredentialId(request.getCredentialId())
                        .build())
                    .collect(Collectors.toList()));
            }

            future.complete(
                AzureScannerResponseImpl.builder()
                    .results(scannedItems)
                    .build()
            );

        } catch (Exception e) {
            log.error("Failed to scan azure resources", e);
            future.complete(
                AzureScannerResponseImpl.builder()
                    .reason("Failed to scan for azure resources: " + e.getMessage())
                    .build()
            );
        }
        return future;
    }
}
