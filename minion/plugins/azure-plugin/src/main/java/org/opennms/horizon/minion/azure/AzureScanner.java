/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.opennms.horizon.minion.azure;

import com.google.common.base.Strings;
import com.google.protobuf.Any;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.opennms.azure.contract.AzureScanRequest;
import org.opennms.horizon.azure.api.AzureScanItem;
import org.opennms.horizon.azure.api.AzureScanNetworkInterfaceItem;
import org.opennms.horizon.azure.api.AzureScanResponse;
import org.opennms.horizon.minion.plugin.api.ScanResultsResponse;
import org.opennms.horizon.minion.plugin.api.ScanResultsResponseImpl;
import org.opennms.horizon.minion.plugin.api.Scanner;
import org.opennms.horizon.shared.azure.http.AzureHttpClient;
import org.opennms.horizon.shared.azure.http.AzureHttpException;
import org.opennms.horizon.shared.azure.http.dto.instanceview.AzureInstanceView;
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
import org.opennms.horizon.shared.azure.http.dto.resources.AzureResources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
                throw new IllegalArgumentException(
                        "configuration must be an AzureScanRequest; type-url=" + config.getTypeUrl());
            }

            AzureScanRequest request = config.unpack(AzureScanRequest.class);

            AzureOAuthToken token = client.login(
                    request.getDirectoryId(),
                    request.getClientId(),
                    request.getClientSecret(),
                    request.getTimeoutMs(),
                    request.getRetries());

            List<AzureScanItem> scannedItems = new LinkedList<>();

            AzureResourceGroups resourceGroups = client.getResourceGroups(
                    token, request.getSubscriptionId(), request.getTimeoutMs(), request.getRetries());

            for (var resourceGroupValue : resourceGroups.getValue()) {
                String resourceGroup = resourceGroupValue.getName();
                if (!Strings.isNullOrEmpty(resourceGroup)) {
                    scannedItems.addAll(scanForResourceGroup(request, token, resourceGroup));
                }
            }

            future.complete(ScanResultsResponseImpl.builder()
                    .results(AzureScanResponse.newBuilder()
                            .addAllResults(scannedItems)
                            .build())
                    .build());

        } catch (Exception e) {
            log.error("Failed to scan azure resources", e);
            future.complete(ScanResultsResponseImpl.builder()
                    .reason("Failed to scan for azure resources: " + e.getMessage())
                    .build());
        }
        return future;
    }

    private List<AzureScanItem> scanForResourceGroup(
            AzureScanRequest request, AzureOAuthToken token, String resourceGroup) throws AzureHttpException {
        AzureResources resources = client.getResources(
                token, request.getSubscriptionId(), resourceGroup, request.getTimeoutMs(), request.getRetries());

        // currently we only care VM
        var filteredResources = resources.getValue().stream()
                .filter(azureValue -> azureValue.getType().equalsIgnoreCase(MICROSOFT_COMPUTE_VIRTUAL_MACHINES))
                .toList();

        if (filteredResources.isEmpty()) {
            return new ArrayList<>();
        }

        AzureNetworkInterfaces networkInterfaces = client.getNetworkInterfaces(
                token, request.getSubscriptionId(), resourceGroup, request.getTimeoutMs(), request.getRetries());

        AzurePublicIpAddresses publicIpAddresses = client.getPublicIpAddresses(
                token, request.getSubscriptionId(), resourceGroup, request.getTimeoutMs(), request.getRetries());

        return filteredResources.stream()
                .map(resource -> {
                    AzureInstanceView azureInstanceView = null;
                    try {
                        azureInstanceView = client.getInstanceView(
                                token,
                                request.getSubscriptionId(),
                                resourceGroup,
                                resource.getName(),
                                request.getTimeoutMs(),
                                request.getRetries());

                    } catch (AzureHttpException ex) {
                        log.warn("Fail to get InstanceView error: {}", ex.getMessage());
                    }

                    var scanItem = AzureScanItem.newBuilder()
                            .setId(resource.getId())
                            .setName(resource.getName())
                            .setLocation(resource.getLocation())
                            .setResourceGroup(resourceGroup)
                            .setActiveDiscoveryId(request.getActiveDiscoveryId());
                    if (azureInstanceView != null) {
                        if (azureInstanceView.getOsName() != null) {
                            scanItem.setOsName(azureInstanceView.getOsName());
                        }
                        if (azureInstanceView.getOsVersion() != null) {
                            scanItem.setOsVersion(azureInstanceView.getOsVersion());
                        }
                    }

                    return scanItem.build();
                })
                .map(scanItem -> scanNetworkInterfaces(scanItem, networkInterfaces, publicIpAddresses))
                .toList();
    }

    private AzureScanItem scanNetworkInterfaces(
            AzureScanItem scanItem,
            AzureNetworkInterfaces networkInterfaces,
            AzurePublicIpAddresses publicIpAddresses) {

        List<AzureNetworkInterface> interfaceList = findNetworkInterfacesForVmId(networkInterfaces, scanItem.getId());

        List<AzureScanNetworkInterfaceItem> scannedNetworkInterfaces = new ArrayList<>();

        for (AzureNetworkInterface networkInterface : interfaceList) {
            NetworkInterfaceProps networkInterfaceProps = networkInterface.getProperties();
            if (networkInterfaceProps == null) {
                log.warn("SKIP AzureNetworkInterfaces: {} that don't have Properties", networkInterface.getName());
                continue;
            }

            for (IpConfiguration ipConfiguration : networkInterfaceProps.getIpConfigurations()) {
                final var scannedNetworkInterface = AzureScanNetworkInterfaceItem.newBuilder();
                IpConfigurationProps ipConfigurationProps = ipConfiguration.getProperties();

                if (ipConfigurationProps == null || Strings.isNullOrEmpty(ipConfigurationProps.getPrivateIPAddress())) {
                    log.warn("SKIP ipConfig that don't have IP address. {}", ipConfiguration);
                    continue;
                }
                scannedNetworkInterface
                        .setInterfaceName(networkInterface.getName())
                        .setId(ipConfiguration.getId())
                        .setName(ipConfiguration.getName())
                        .setIpAddress(ipConfigurationProps.getPrivateIPAddress())
                        .setIsPrimary(ipConfiguration.getProperties().isPrimary())
                        .setLocation(networkInterface.getLocation());

                PublicIPAddress publicIPAddress = ipConfigurationProps.getPublicIPAddress();
                if (publicIPAddress != null) {
                    this.handlePublicIpAddress(
                            networkInterface, publicIPAddress, publicIpAddresses, scannedNetworkInterface);
                }
                scannedNetworkInterfaces.add(scannedNetworkInterface.build());
            }
        }
        if (!scannedNetworkInterfaces.isEmpty()) {
            scanItem = scanItem.toBuilder()
                    .addAllNetworkInterfaceItems(scannedNetworkInterfaces)
                    .build();
        }
        return scanItem;
    }

    private void handlePublicIpAddress(
            final AzureNetworkInterface networkInterface,
            final PublicIPAddress publicIPAddress,
            final AzurePublicIpAddresses publicIpAddresses,
            final AzureScanNetworkInterfaceItem.Builder scannedNetworkInterface) {
        String publicIpId = publicIPAddress.getId();

        Optional<AzurePublicIPAddress> publicAddressOpt = findPublicIpAddressForId(publicIpAddresses, publicIpId);
        if (publicAddressOpt.isPresent()) {
            AzurePublicIPAddress azurePublicIPAddress = publicAddressOpt.get();
            PublicIpAddressProps properties = azurePublicIPAddress.getProperties();

            if (Strings.isNullOrEmpty(properties.getIpAddress())
                    || Strings.isNullOrEmpty(azurePublicIPAddress.getName())) {
                log.warn("SKIP azurePublicIPAddress that don't have name / IP address. {}", azurePublicIPAddress);
                return;
            }
            scannedNetworkInterface
                    .setPublicIpAddress(AzureScanNetworkInterfaceItem.newBuilder()
                            .setInterfaceName(networkInterface.getName())
                            .setId(publicIpId)
                            .setIpAddress(properties.getIpAddress())
                            .setName(azurePublicIPAddress.getName()))
                    .setLocation(networkInterface.getLocation());
        }
    }

    private List<AzureNetworkInterface> findNetworkInterfacesForVmId(
            AzureNetworkInterfaces networkInterfaces, String vmId) {
        List<AzureNetworkInterface> networkInterfacesList = new ArrayList<>();
        for (AzureNetworkInterface networkInterface : networkInterfaces.getValue()) {
            NetworkInterfaceProps properties = networkInterface.getProperties();
            if (properties == null) {
                log.warn("SKIP AzureNetworkInterfaces: {} that don't have Properties", networkInterface.getName());
                continue;
            }
            VirtualMachine virtualMachine = properties.getVirtualMachine();
            if (Objects.nonNull(virtualMachine) && vmId.equalsIgnoreCase(virtualMachine.getId())) {
                networkInterfacesList.add(networkInterface);
            }
        }
        return networkInterfacesList;
    }

    private Optional<AzurePublicIPAddress> findPublicIpAddressForId(
            AzurePublicIpAddresses azurePublicIpAddresses, String id) {
        for (AzurePublicIPAddress publicIpAddress : azurePublicIpAddresses.getValue()) {
            if (publicIpAddress.getId().equals(id)) {
                return Optional.of(publicIpAddress);
            }
        }
        return Optional.empty();
    }
}
