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
package org.opennms.horizon.inventory.service.taskset.response;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.opennms.horizon.azure.api.AzureScanItem;
import org.opennms.horizon.azure.api.AzureScanNetworkInterfaceItem;
import org.opennms.horizon.azure.api.AzureScanResponse;
import org.opennms.horizon.inventory.dto.ListTagsByEntityIdParamsDTO;
import org.opennms.horizon.inventory.dto.MonitoredServiceDTO;
import org.opennms.horizon.inventory.dto.MonitoredServiceTypeDTO;
import org.opennms.horizon.inventory.dto.NodeCreateDTO;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.inventory.dto.TagCreateDTO;
import org.opennms.horizon.inventory.dto.TagCreateListDTO;
import org.opennms.horizon.inventory.dto.TagEntityIdDTO;
import org.opennms.horizon.inventory.exception.EntityExistException;
import org.opennms.horizon.inventory.exception.LocationNotFoundException;
import org.opennms.horizon.inventory.model.IpInterface;
import org.opennms.horizon.inventory.model.MonitoredService;
import org.opennms.horizon.inventory.model.MonitoredServiceType;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.model.SnmpInterface;
import org.opennms.horizon.inventory.model.discovery.active.AzureActiveDiscovery;
import org.opennms.horizon.inventory.repository.IpInterfaceRepository;
import org.opennms.horizon.inventory.repository.NodeRepository;
import org.opennms.horizon.inventory.repository.discovery.active.AzureActiveDiscoveryRepository;
import org.opennms.horizon.inventory.service.AzureInterfaceService;
import org.opennms.horizon.inventory.service.IpInterfaceService;
import org.opennms.horizon.inventory.service.MonitoredServiceService;
import org.opennms.horizon.inventory.service.MonitoredServiceTypeService;
import org.opennms.horizon.inventory.service.NodeService;
import org.opennms.horizon.inventory.service.SnmpConfigService;
import org.opennms.horizon.inventory.service.SnmpInterfaceService;
import org.opennms.horizon.inventory.service.TagService;
import org.opennms.horizon.inventory.service.discovery.active.IcmpActiveDiscoveryService;
import org.opennms.horizon.inventory.service.taskset.TaskSetHandler;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.opennms.node.scan.contract.IpInterfaceResult;
import org.opennms.node.scan.contract.NodeInfoResult;
import org.opennms.node.scan.contract.NodeScanResult;
import org.opennms.node.scan.contract.ServiceResult;
import org.opennms.node.scan.contract.SnmpInterfaceResult;
import org.opennms.taskset.contract.DiscoveryScanResult;
import org.opennms.taskset.contract.MonitorType;
import org.opennms.taskset.contract.PingResponse;
import org.opennms.taskset.contract.ScanType;
import org.opennms.taskset.contract.ScannerResponse;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScannerResponseService {
    private final AzureActiveDiscoveryRepository azureActiveDiscoveryRepository;
    private final NodeRepository nodeRepository;
    private final NodeService nodeService;
    private final TaskSetHandler taskSetHandler;
    private final IpInterfaceService ipInterfaceService;
    private final SnmpInterfaceService snmpInterfaceService;
    private final AzureInterfaceService azureInterfaceService;
    private final TagService tagService;
    private final SnmpConfigService snmpConfigService;
    private final IcmpActiveDiscoveryService icmpActiveDiscoveryService;
    private final IpInterfaceRepository ipInterfaceRepository;
    private final MonitoredServiceTypeService monitoredServiceTypeService;
    private final MonitoredServiceService monitoredServiceService;

    public void accept(String tenantId, Long locationId, ScannerResponse response)
            throws InvalidProtocolBufferException {
        Any result = response.getResult();

        switch (getType(response)) {
            case AZURE_SCAN -> {
                AzureScanResponse azureResponse = result.unpack(AzureScanResponse.class);
                log.debug("received azure scan result: {}", azureResponse);
                processAzureScanResponse(tenantId, locationId, azureResponse);
            }
            case NODE_SCAN -> {
                NodeScanResult nodeScanResult = result.unpack(NodeScanResult.class);
                log.debug("received node scan result: {}", nodeScanResult);
                processNodeScanResponse(tenantId, nodeScanResult, locationId);
            }
            case DISCOVERY_SCAN -> {
                DiscoveryScanResult discoveryScanResult = result.unpack(DiscoveryScanResult.class);
                log.debug("received discovery result: {}", discoveryScanResult);
                processDiscoveryScanResponse(tenantId, locationId, discoveryScanResult);
            }
            case UNRECOGNIZED -> log.warn("Unrecognized scan type");
        }
    }

    private ScanType getType(ScannerResponse response) {
        Any result = response.getResult();
        if (result.is(AzureScanResponse.class)) {
            return ScanType.AZURE_SCAN;
        } else if (result.is(NodeScanResult.class)) {
            return ScanType.NODE_SCAN;
        } else if (result.is(DiscoveryScanResult.class)) {
            return ScanType.DISCOVERY_SCAN;
        }
        return ScanType.UNRECOGNIZED;
    }

    public void processDiscoveryScanResponse(
            String tenantId, Long locationId, DiscoveryScanResult discoveryScanResult) {
        for (PingResponse pingResponse : discoveryScanResult.getPingResponseList()) {
            var discoveryOptional =
                    icmpActiveDiscoveryService.getDiscoveryById(discoveryScanResult.getActiveDiscoveryId(), tenantId);
            if (discoveryOptional.isPresent()) {
                var icmpDiscovery = discoveryOptional.get();
                NodeCreateDTO createDTO = NodeCreateDTO.newBuilder()
                        .setLocationId(String.valueOf(locationId))
                        .setManagementIp(pingResponse.getIpAddress())
                        .setLabel(pingResponse.getIpAddress())
                        .addAllTags(getTagCreateDTO(icmpDiscovery.getId(), tenantId))
                        .addDiscoveryIds(icmpDiscovery.getId())
                        .build();
                try {
                    var optionalNode = nodeService.getNode(pingResponse.getIpAddress(), locationId, tenantId);
                    if (optionalNode.isPresent()) {
                        var nodeDTO = optionalNode.get();
                        tagService.addTags(
                                tenantId,
                                TagCreateListDTO.newBuilder()
                                        .addEntityIds(
                                                TagEntityIdDTO.newBuilder().setNodeId(nodeDTO.getId()))
                                        .addAllTags(createDTO.getTagsList())
                                        .build());

                        updateNodeWithDiscoveryIds(nodeDTO, icmpDiscovery.getId());
                        nodeService.updateNodeMonitoredState(nodeDTO.getId(), nodeDTO.getTenantId());
                        nodeService.sendNewNodeTaskSetAsync(nodeDTO, locationId, icmpDiscovery);
                    } else {
                        var node = nodeService.createNode(createDTO, ScanType.DISCOVERY_SCAN, tenantId);
                        nodeService.updateNodeMonitoredState(node.getId(), node.getTenantId());
                        nodeService.sendNewNodeTaskSetAsync(node, locationId, icmpDiscovery);
                    }
                } catch (Exception e) {
                    log.error(
                            "Exception while adding new device for tenantId={}; locationId={} with IP {}",
                            tenantId,
                            locationId,
                            pingResponse.getIpAddress(),
                            e);
                }
            }
        }
    }

    private void updateNodeWithDiscoveryIds(NodeDTO nodeDTO, Long discoveryId) {
        List<Long> discoveryIdsList = new ArrayList<>(nodeDTO.getDiscoveryIdsList());

        if (!discoveryIdsList.contains(discoveryId)) {
            discoveryIdsList.add(discoveryId);
            nodeService.updateNodeDiscoveryIds(nodeDTO.getId(), nodeDTO.getTenantId(), discoveryIdsList);
        }
    }

    private void processAzureScanResponse(String tenantId, Long locationId, AzureScanResponse azureResponse) {
        for (AzureScanItem azureScanItem : azureResponse.getResultsList()) {

            Optional<AzureActiveDiscovery> discoveryOpt =
                    azureActiveDiscoveryRepository.findByTenantIdAndId(tenantId, azureScanItem.getActiveDiscoveryId());
            if (discoveryOpt.isEmpty()) {
                log.warn("No Azure Active Discovery found for id: {}", azureScanItem.getActiveDiscoveryId());
                continue;
            }
            AzureActiveDiscovery discovery = discoveryOpt.get();

            String nodeLabel = String.format("%s (%s)", azureScanItem.getName(), azureScanItem.getResourceGroup());

            Optional<Node> nodeOpt = nodeRepository.findByTenantLocationIdAndNodeLabel(tenantId, locationId, nodeLabel);

            try {
                Node node;
                if (nodeOpt.isPresent()) {
                    node = nodeOpt.get();
                    // todo: perform update if AzureScanner is on a schedule or gets called again
                    log.warn(
                            "Node already exists for tenantId={}; locationId={}; label={}",
                            tenantId,
                            locationId,
                            nodeLabel);
                } else {
                    NodeCreateDTO createDTO = NodeCreateDTO.newBuilder()
                            .setLocationId(String.valueOf(locationId))
                            .setLabel(nodeLabel)
                            .addAllTags(getTagCreateDTO(discovery.getId(), tenantId))
                            .build();

                    node = nodeService.createNode(createDTO, ScanType.AZURE_SCAN, tenantId);
                    long nodeId = node.getId();

                    var nodeInfoResult = NodeInfoResult.newBuilder()
                            .setSystemLocation(azureScanItem.getLocation())
                            .setSystemName(azureScanItem.getName())
                            .setSystemDescr(getSystemDescr(azureScanItem))
                            .build();
                    nodeService.updateNodeInfo(node, nodeInfoResult);

                    for (AzureScanNetworkInterfaceItem networkInterfaceItem :
                            azureScanItem.getNetworkInterfaceItemsList()) {
                        var azureInterface = azureInterfaceService.createOrUpdateFromScanResult(
                                tenantId, node, networkInterfaceItem);
                        ipInterfaceService.createFromAzureScanResult(
                                tenantId, node, azureInterface, networkInterfaceItem);
                    }

                    taskSetHandler.sendAzureMonitorTasks(discovery, azureScanItem, nodeId);
                    taskSetHandler.sendAzureCollectorTasks(discovery, azureScanItem, nodeId);
                }
                nodeService.updateNodeMonitoredState(node.getId(), node.getTenantId());
            } catch (EntityExistException e) {
                log.error("Error while adding new Azure node for tenantId={}; locationId={}", tenantId, locationId);
            } catch (LocationNotFoundException e) {
                log.error(
                        "Location not found while adding new Azure device for tenantId={}; locationId={}",
                        tenantId,
                        locationId);
            }
        }
    }

    private String getSystemDescr(AzureScanItem azureScanItem) {
        if (Strings.isNotEmpty(azureScanItem.getOsName()) && Strings.isNotEmpty(azureScanItem.getOsVersion())) {
            return String.format("%s (%s)", azureScanItem.getOsName(), azureScanItem.getOsVersion());
        } else if (Strings.isNotEmpty(azureScanItem.getOsName())) {
            return azureScanItem.getOsName();
        } else {
            return azureScanItem.getOsVersion();
        }
    }

    private void processNodeScanResponse(String tenantId, NodeScanResult result, Long locationId) {
        var snmpConfiguration = result.getSnmpConfig();
        // Save SNMP Config for all the interfaces in the node.
        result.getIpInterfacesList()
                .forEach(ipInterfaceResult -> snmpConfigService.saveOrUpdateSnmpConfig(
                        tenantId, locationId, ipInterfaceResult.getIpAddress(), snmpConfiguration));

        Optional<Node> nodeOpt = nodeRepository.findByIdAndTenantId(result.getNodeId(), tenantId);
        if (nodeOpt.isPresent()) {
            Node node = nodeOpt.get();
            Map<Integer, SnmpInterface> ifIndexSNMPMap = new HashMap<>();
            nodeService.updateNodeInfo(node, result.getNodeInfo());

            IpInterface ipInterface = ipInterfaceService.getPrimaryInterfaceForNode(node.getId());
            snmpConfigService.saveOrUpdateSnmpConfig(
                    tenantId,
                    locationId,
                    InetAddressUtils.toIpAddrString(ipInterface.getIpAddress()),
                    snmpConfiguration);

            for (SnmpInterfaceResult snmpIfResult : result.getSnmpInterfacesList()) {
                SnmpInterface snmpInterface =
                        snmpInterfaceService.createOrUpdateFromScanResult(tenantId, node, snmpIfResult);
                ifIndexSNMPMap.put(snmpInterface.getIfIndex(), snmpInterface);
            }
            for (IpInterfaceResult ipIfResult : result.getIpInterfacesList()) {
                ipInterfaceService.createOrUpdateFromScanResult(tenantId, node, ipIfResult, ifIndexSNMPMap);
            }
            result.getDetectorResultList()
                    .forEach(detectorResult ->
                            processDetectorResults(tenantId, locationId, node.getId(), detectorResult));
        } else {
            log.error(
                    "Error while process node scan results, tenantId={}; locationId={}; node with id {} doesn't exist",
                    tenantId,
                    locationId,
                    result.getNodeId());
        }
    }

    private void processDetectorResults(String tenantId, Long locationId, long nodeId, ServiceResult serviceResult) {

        log.info("Received Detector tenantId={}; locationId={}; response={}", serviceResult, tenantId, locationId);

        InetAddress ipAddress = InetAddressUtils.getInetAddress(serviceResult.getIpAddress());
        Optional<IpInterface> ipInterfaceOpt =
                ipInterfaceService.findByIpAddressAndLocationIdAndTenantIdModel(ipAddress, locationId, tenantId);

        if (ipInterfaceOpt.isPresent()) {
            IpInterface ipInterface = ipInterfaceOpt.get();

            if (serviceResult.getStatus()) {
                var monitoredService = createMonitoredService(serviceResult, ipInterface);
                // TODO: Combine Monitor type and Service type
                MonitorType monitorType =
                        MonitorType.valueOf(serviceResult.getService().name());

                taskSetHandler.sendMonitorTask(locationId, monitorType, ipInterface, nodeId, monitoredService.getId());
                taskSetHandler.sendCollectorTask(locationId, monitorType, ipInterface, nodeId);

            } else {
                log.info(
                        "{} not detected on tenantId={}; locationId={}; ip={}",
                        serviceResult.getService().name(),
                        tenantId,
                        locationId,
                        ipAddress.getAddress());
            }
        } else {
            log.warn(
                    "Failed to find IP Interface during detection for tenantId={}; locationId={}; ip={}",
                    tenantId,
                    locationId,
                    ipAddress.getHostAddress());
        }
    }

    private MonitoredService createMonitoredService(ServiceResult serviceResult, IpInterface ipInterface) {
        String tenantId = ipInterface.getTenantId();

        MonitoredServiceType monitoredServiceType =
                monitoredServiceTypeService.createSingle(MonitoredServiceTypeDTO.newBuilder()
                        // TODO: Combine Monitor type and Service type
                        .setServiceName(serviceResult.getService().name())
                        .setTenantId(tenantId)
                        .build());

        MonitoredServiceDTO newMonitoredService =
                MonitoredServiceDTO.newBuilder().setTenantId(tenantId).build();

        return monitoredServiceService.createSingle(newMonitoredService, monitoredServiceType, ipInterface);
    }

    private List<TagCreateDTO> getTagCreateDTO(long discoveryId, String tenantId) {
        var tagsList = tagService.getTagsByEntityId(
                tenantId,
                ListTagsByEntityIdParamsDTO.newBuilder()
                        .setEntityId(TagEntityIdDTO.newBuilder()
                                .setActiveDiscoveryId(discoveryId)
                                .build())
                        .build());
        return tagsList.stream()
                .map(tag -> TagCreateDTO.newBuilder().setName(tag.getName()).build())
                .toList();
    }
}
