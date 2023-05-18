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
import org.opennms.azure.contract.AzureScanRequest;
import org.opennms.horizon.azure.api.AzureScanItem;
import org.opennms.horizon.azure.api.AzureScanNetworkInterfaceItem;
import org.opennms.horizon.azure.api.AzureScanResponse;
import org.opennms.horizon.minion.plugin.api.ScanResultsResponse;
import org.opennms.horizon.minion.plugin.api.ScanResultsResponseImpl;
import org.opennms.horizon.minion.plugin.api.Scanner;
import org.opennms.horizon.shared.azure.http.AzureHttpClient;
import org.opennms.horizon.shared.azure.http.AzureHttpException;
import org.opennms.horizon.shared.azure.http.dto.login.AzureOAuthToken;
import org.opennms.horizon.shared.azure.http.dto.networkinterface.AzureNetworkInterface;
import org.opennms.horizon.shared.azure.http.dto.networkinterface.AzureNetworkInterfaces;
import org.opennms.horizon.shared.azure.http.dto.networkinterface.IpConfiguration;
import org.opennms.horizon.shared.azure.http.dto.networkinterface.IpConfigurationProps;
import org.opennms.horizon.shared.azure.http.dto.networkinterface.NetworkInterfaceProps;
import org.opennms.horizon.shared.azure.http.dto.networkinterface.PublicIPAddress;
import org.opennms.horizon.shared.azure.http.dto.networkinterface.VirtualMachine;
import org.opennms.horizon.shared.azure.http.dto.publicipaddresses.AzurePublicIPAddress;
import org.opennms.horizon.shared.azure.http.dto.publicipaddresses.AzurePublicIpAddresses;
import org.opennms.horizon.shared.azure.http.dto.publicipaddresses.PublicIpAddressProps;
import org.opennms.horizon.shared.azure.http.dto.resourcegroup.AzureResourceGroups;
import org.opennms.horizon.shared.azure.http.dto.resourcegroup.AzureValue;
import org.opennms.horizon.shared.azure.http.dto.resources.AzureResources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class AzureScanner implements Scanner {
    private static final Logger log = LoggerFactory.getLogger(AzureScanner.class);
    private static final String MICROSOFT_COMPUTE_VIRTUAL_MACHINES = "Microsoft.Compute/virtualMachines";

    private final AzureHttpClient client;

    public AzureScanner(AzureHttpClient client) {
        this.client = client;
    }

    @Override
    public CompletableFuture<ScanResultsResponse> scan(Any config) {
        CompletableFuture<ScanResultsResponse> future = new CompletableFuture<>();

        try {
            if (!config.is(AzureScanRequest.class)) {
                throw new IllegalArgumentException("configuration must be an AzureScanRequest; type-url=" + config.getTypeUrl());
            }

            AzureScanRequest request = config.unpack(AzureScanRequest.class);

            AzureOAuthToken token = client.login(request.getDirectoryId(),
                request.getClientId(), request.getClientSecret(), request.getTimeoutMs(), request.getRetries());

            List<AzureScanItem> scannedItems = new LinkedList<>();

            AzureResourceGroups resourceGroups = client
                .getResourceGroups(token, request.getSubscriptionId(), request.getTimeoutMs(), request.getRetries());

            for (AzureValue resourceGroupValue : resourceGroups.getValue()) {
                String resourceGroup = resourceGroupValue.getName();
                scannedItems.addAll(scanForResourceGroup(request, token, resourceGroup));
            }

            future.complete(
                ScanResultsResponseImpl.builder()
                    .results(AzureScanResponse.newBuilder()
                        .addAllResults(scannedItems).build())
                    .build()
            );

        } catch (Exception e) {
            log.error("Failed to scan azure resources", e);
            future.complete(
                ScanResultsResponseImpl.builder()
                    .reason("Failed to scan for azure resources: " + e.getMessage())
                    .build()
            );
        }
        return future;
    }

    private List<AzureScanItem> scanForResourceGroup(AzureScanRequest request, AzureOAuthToken token, String resourceGroup) throws AzureHttpException {
        AzureResources resources = client.getResources(token, request.getSubscriptionId(),
            resourceGroup, request.getTimeoutMs(), request.getRetries());

        AzureNetworkInterfaces networkInterfaces = client.getNetworkInterfaces(token, request.getSubscriptionId(),
            resourceGroup, request.getTimeoutMs(), request.getRetries());

        AzurePublicIpAddresses publicIpAddresses = client.getPublicIpAddresses(token, request.getSubscriptionId(),
            resourceGroup, request.getTimeoutMs(), request.getRetries());

        return resources.getValue().stream()
            .filter(azureValue -> azureValue.getType()
                .equalsIgnoreCase(MICROSOFT_COMPUTE_VIRTUAL_MACHINES))
            .map(resource -> AzureScanItem.newBuilder()
                .setId(resource.getId())
                .setName(resource.getName())
                .setResourceGroup(resourceGroup)
                .setActiveDiscoveryId(request.getActiveDiscoveryId())
                .build())
            .map(scanItem -> scanNetworkInterfaces(scanItem, networkInterfaces, publicIpAddresses))
            .toList();
    }

    private AzureScanItem scanNetworkInterfaces(AzureScanItem scanItem,
                                                AzureNetworkInterfaces networkInterfaces,
                                                AzurePublicIpAddresses publicIpAddresses) {

        List<AzureNetworkInterface> interfaceList = findNetworkInterfacesForVmId(networkInterfaces, scanItem.getId());

        List<AzureScanNetworkInterfaceItem> scannedNetworkInterfaces = new ArrayList<>();

        for (AzureNetworkInterface networkInterface : interfaceList) {

            NetworkInterfaceProps networkInterfaceProps = networkInterface.getProperties();

            //note: maybe even consider getting just primary ip configuration for now - needs tested for more than 1 ip configuration
            for (IpConfiguration ipConfiguration : networkInterfaceProps.getIpConfigurations()) {
                IpConfigurationProps ipConfigurationProps = ipConfiguration.getProperties();

                scannedNetworkInterfaces.add(AzureScanNetworkInterfaceItem.newBuilder()
                    .setId(networkInterface.getId())
                    .setName(networkInterface.getName())
                    .setIpAddress(ipConfigurationProps.getPrivateIPAddress())
                    .setIsPublic(false)
                    .build());

                PublicIPAddress publicIPAddress = ipConfigurationProps.getPublicIPAddress();
                if (publicIPAddress != null) {
                    String publicIpId = publicIPAddress.getId();

                    Optional<AzurePublicIPAddress> publicAddressOpt = findPublicIpAddressForId(publicIpAddresses, publicIpId);
                    if (publicAddressOpt.isPresent()) {
                        AzurePublicIPAddress azurePublicIPAddress = publicAddressOpt.get();
                        PublicIpAddressProps properties = azurePublicIPAddress.getProperties();

                        scannedNetworkInterfaces.add(AzureScanNetworkInterfaceItem.newBuilder()
                            .setId(publicIpId)
                            .setName(azurePublicIPAddress.getName())
                            .setIpAddress(properties.getIpAddress())
                            .setIsPublic(true)
                            .build());
                    }
                }
            }
        }
        if (!scannedNetworkInterfaces.isEmpty()) {
            scanItem = scanItem.toBuilder()
                .addAllNetworkInterfaceItems(scannedNetworkInterfaces).build();
        }
        return scanItem;
    }

    private List<AzureNetworkInterface> findNetworkInterfacesForVmId(AzureNetworkInterfaces networkInterfaces, String vmId) {
        List<AzureNetworkInterface> networkInterfacesList = new ArrayList<>();
        for (AzureNetworkInterface networkInterface : networkInterfaces.getValue()) {
            NetworkInterfaceProps properties = networkInterface.getProperties();
            VirtualMachine virtualMachine = properties.getVirtualMachine();
            if (Objects.nonNull(virtualMachine) && vmId.equals(virtualMachine.getId())) {
                networkInterfacesList.add(networkInterface);
            }
        }
        return networkInterfacesList;
    }

    private Optional<AzurePublicIPAddress> findPublicIpAddressForId(AzurePublicIpAddresses azurePublicIpAddresses, String id) {
        for (AzurePublicIPAddress publicIpAddress : azurePublicIpAddresses.getValue()) {
            if (publicIpAddress.getId().equals(id)) {
                return Optional.of(publicIpAddress);
            }
        }
        return Optional.empty();
    }
}
