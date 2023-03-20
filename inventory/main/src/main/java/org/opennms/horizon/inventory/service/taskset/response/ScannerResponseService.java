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

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.azure.api.AzureScanItem;
import org.opennms.horizon.azure.api.AzureScanResponse;
import org.opennms.horizon.inventory.dto.NodeCreateDTO;
import org.opennms.horizon.inventory.dto.TagCreateDTO;
import org.opennms.horizon.inventory.dto.TagCreateListDTO;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.model.SnmpInterface;
import org.opennms.horizon.inventory.model.discovery.PassiveDiscovery;
import org.opennms.horizon.inventory.model.discovery.active.AzureActiveDiscovery;
import org.opennms.horizon.inventory.repository.discovery.PassiveDiscoveryRepository;
import org.opennms.horizon.inventory.repository.discovery.active.AzureActiveDiscoveryRepository;
import org.opennms.horizon.inventory.repository.NodeRepository;
import org.opennms.horizon.inventory.service.IpInterfaceService;
import org.opennms.horizon.inventory.service.NodeService;
import org.opennms.horizon.inventory.service.SnmpInterfaceService;
import org.opennms.horizon.inventory.service.TagService;
import org.opennms.horizon.inventory.service.taskset.DetectorTaskSetService;
import org.opennms.horizon.inventory.service.taskset.TaskSetHandler;
import org.opennms.node.scan.contract.IpInterfaceResult;
import org.opennms.node.scan.contract.NodeScanResult;
import org.opennms.node.scan.contract.SnmpInterfaceResult;
import org.opennms.taskset.contract.DiscoveryScanResult;
import org.opennms.taskset.contract.PingResponse;
import org.opennms.taskset.contract.ScanType;
import org.opennms.taskset.contract.ScannerResponse;
import org.opennms.taskset.contract.TaskContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


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
    private final TagService tagService;
    private final PassiveDiscoveryRepository passiveDiscoveryRepository;
    private final DetectorTaskSetService detectorTaskSetService;

    public void accept(String tenantId, String location, ScannerResponse response, TaskContext taskContext) throws InvalidProtocolBufferException {
        Any result = response.getResult();

        switch (getType(response)) {
            case AZURE_SCAN -> {
                AzureScanResponse azureResponse = result.unpack(AzureScanResponse.class);
                List<AzureScanItem> resultsList = azureResponse.getResultsList();

                for (int index = 0; index < resultsList.size(); index++) {
                    AzureScanItem item = resultsList.get(index);

                    // HACK: for now, creating a dummy ip address in order for status to display on ui
                    // could maybe get ip interfaces from VM to save instead but private IPs may not be unique enough if no public IP attached ?
                    // Postgres requires a valid INET field
                    String ipAddress = String.format("127.0.0.%d", index + 1);

                    processAzureScanItem(tenantId, location, ipAddress, item);
                }
            }
            //TODO process the node scan results
            case NODE_SCAN -> {
                NodeScanResult nodeScanResult = result.unpack(NodeScanResult.class);
                log.debug("received node scan result: {}", nodeScanResult);
                processNodeScanResponse(tenantId, nodeScanResult);
            }
            case DISCOVERY_SCAN -> {
                DiscoveryScanResult discoveryScanResult = result.unpack(DiscoveryScanResult.class);
                log.debug("received discovery result: {}", discoveryScanResult);
                processDiscoveryScanResponse(tenantId, location, discoveryScanResult);
            }
            case UNRECOGNIZED -> log.warn("Unrecognized scan type");

        }
    }

    private ScanType getType(ScannerResponse response) {
        Any result = response.getResult();
        if (result.is(AzureScanResponse.class)) {
            return ScanType.AZURE_SCAN;
        } else if(result.is(NodeScanResult.class)) {
            return ScanType.NODE_SCAN;
        } else if(result.is(DiscoveryScanResult.class)) {
            return ScanType.DISCOVERY_SCAN;
        }
        return ScanType.UNRECOGNIZED;
    }

    private void processDiscoveryScanResponse(String tenantId, String location, DiscoveryScanResult discoveryScanResult) {
        for (PingResponse pingResponse : discoveryScanResult.getPingResponseList()) {
            NodeCreateDTO createDTO = NodeCreateDTO.newBuilder()
                .setLocation(location)
                .setManagementIp(pingResponse.getIpAddress())
                .setLabel(pingResponse.getIpAddress())
                .build();
            Node node = nodeService.createNode(createDTO, ScanType.DISCOVERY_SCAN, tenantId);
            nodeService.sendNewNodeTaskSetAsync(node, discoveryScanResult.getActiveDiscoveryId());
        }
    }

    private void processAzureScanItem(String tenantId, String location, String ipAddress, AzureScanItem item) {
        Optional<AzureActiveDiscovery> discoveryOpt = azureActiveDiscoveryRepository.findByTenantIdAndId(tenantId, item.getActiveDiscoveryId());
        if (discoveryOpt.isEmpty()) {
            log.warn("No Azure Active Discovery found for id: {}", item.getActiveDiscoveryId());
            return;
        }

        AzureActiveDiscovery discovery = discoveryOpt.get();

        String nodeLabel = String.format("%s (%s)", item.getName(), item.getResourceGroup());
        Optional<Node> nodeOpt = nodeRepository.findByTenantLocationAndNodeLabel(tenantId, location, nodeLabel);

        Node node;
        if (nodeOpt.isPresent()) {
            node = nodeOpt.get();
            log.warn("Node already exists for tenant: {}, location: {}, label: {}", tenantId, location, nodeLabel);
        } else {
            NodeCreateDTO createDTO = NodeCreateDTO.newBuilder()
                .setLocation(location)
                .setManagementIp(ipAddress)
                .setLabel(nodeLabel)
                .build();
            node = nodeService.createNode(createDTO, ScanType.AZURE_SCAN, tenantId);

            taskSetHandler.sendAzureMonitorTasks(discovery, item, ipAddress, node.getId());
            taskSetHandler.sendAzureCollectorTasks(discovery, item, ipAddress, node.getId());
        }
        List<TagCreateDTO> tags = discovery.getTags().stream()
            .map(tag -> TagCreateDTO.newBuilder().setName(tag.getName()).build())
            .toList();
        tagService.addTags(tenantId, TagCreateListDTO.newBuilder()
            .setNodeId(node.getId()).addAllTags(tags).build());
    }

    private void processNodeScanResponse(String tenantId, NodeScanResult result ) {
        Optional<Node> nodeOpt = nodeRepository.findByIdAndTenantId(result.getNodeId(), tenantId);
        if (nodeOpt.isPresent()) {
            Node node = nodeOpt.get();
            Map<Integer, SnmpInterface> ifIndexSNMPMap = new HashMap<>();
            nodeService.updateNodeInfo(node, result.getNodeInfo());

            for (SnmpInterfaceResult snmpIfResult : result.getSnmpInterfacesList()) {
                SnmpInterface snmpInterface = snmpInterfaceService.createOrUpdateFromScanResult(tenantId, node, snmpIfResult);
                ifIndexSNMPMap.put(snmpInterface.getIfIndex(), snmpInterface);
            }
            for (IpInterfaceResult ipIfResult : result.getIpInterfacesList()) {
                ipInterfaceService.creatUpdateFromScanResult(tenantId, node, ipIfResult, ifIndexSNMPMap);
            }

            if (result.hasPassiveDiscoveryId()) {
                Optional<PassiveDiscovery> passiveDiscoveryOpt = passiveDiscoveryRepository
                    .findByTenantIdAndId(tenantId, result.getPassiveDiscoveryId());

                if (passiveDiscoveryOpt.isPresent()) {

                    PassiveDiscovery discovery = passiveDiscoveryOpt.get();
                    //todo: use snmp agent config for detection, monitor and collection
                }
            }

            //todo: start detection, monitor and collection here

        } else {
            log.error("Error while process node scan results, node with id {} doesn't exist", result.getNodeId());
        }
    }
}
