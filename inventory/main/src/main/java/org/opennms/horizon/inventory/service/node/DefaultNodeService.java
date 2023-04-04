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

package org.opennms.horizon.inventory.service.node;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.opennms.horizon.inventory.discovery.IcmpActiveDiscoveryDTO;
import org.opennms.horizon.inventory.dto.DefaultNodeCreateDTO;
import org.opennms.horizon.inventory.dto.TagCreateListDTO;
import org.opennms.horizon.inventory.dto.TagEntityIdDTO;
import org.opennms.horizon.inventory.mapper.node.DefaultNodeMapper;
import org.opennms.horizon.inventory.model.MonitoringLocation;
import org.opennms.horizon.inventory.model.node.DefaultNode;
import org.opennms.horizon.inventory.repository.node.DefaultNodeRepository;
import org.opennms.horizon.inventory.service.IpInterfaceService;
import org.opennms.horizon.inventory.service.MonitoringLocationService;
import org.opennms.horizon.inventory.service.TagService;
import org.opennms.horizon.inventory.service.taskset.ScannerTaskSetService;
import org.opennms.horizon.snmp.api.SnmpConfiguration;
import org.opennms.node.scan.contract.NodeInfoResult;
import org.opennms.taskset.contract.ScanType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Service
@RequiredArgsConstructor
public class DefaultNodeService {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultNodeService.class);
    private final ThreadFactory threadFactory = new ThreadFactoryBuilder()
        .setNameFormat("delete-node-task-publish-%d")
        .build();
    private final ExecutorService executorService = Executors.newFixedThreadPool(10, threadFactory);
    private final DefaultNodeRepository nodeRepository;
    private final IpInterfaceService ipInterfaceService;
    private final ScannerTaskSetService scannerTaskSetService;
    private final MonitoringLocationService monitoringLocationService;
    private final TagService tagService;
    private final DefaultNodeMapper mapper;

    @Transactional
    public DefaultNode createNode(DefaultNodeCreateDTO request, ScanType scanType, String tenantId) {
        MonitoringLocation monitoringLocation =
            monitoringLocationService.saveMonitoringLocation(tenantId, request.getLocation());

        DefaultNode node = saveNode(request, monitoringLocation, scanType, tenantId);

        if (request.hasManagementIp()) {
            ipInterfaceService.saveIpInterface(tenantId, node, request.getManagementIp());
        }

        tagService.addTags(tenantId, TagCreateListDTO.newBuilder()
            .addEntityIds(TagEntityIdDTO.newBuilder()
                .setNodeId(node.getId()))
            .addAllTags(request.getTagsList())
            .build());

        return node;
    }

    @Transactional(readOnly = true)
    public Optional<DefaultNode> getNode(String tenantId, String location, InetAddress primaryIpAddress) {

        var list = nodeRepository.findByTenantId(tenantId);
        if (list.isEmpty()) {
            return Optional.empty();
        }
        var listOfNodesOnLocation = list.stream()
            .filter(node -> node.getMonitoringLocation()
                .getLocation().equals(location)).toList();
        if (listOfNodesOnLocation.isEmpty()) {
            return Optional.empty();
        }
        return listOfNodesOnLocation.stream().filter(node ->
            node.getIpInterfaces().stream().anyMatch(ipInterface -> ipInterface.getSnmpPrimary() &&
                ipInterface.getIpAddress().equals(primaryIpAddress))).findFirst();

    }

    private DefaultNode saveNode(DefaultNodeCreateDTO request, MonitoringLocation monitoringLocation,
                                 ScanType scanType, String tenantId) {

        DefaultNode node = new DefaultNode();

        node.setTenantId(tenantId);
        node.setNodeLabel(request.getLabel());
        node.setScanType(scanType);
        if (request.hasMonitoredState()) {
            node.setMonitoredState(request.getMonitoredState());
        }
        node.setCreateTime(LocalDateTime.now());
        node.setMonitoringLocation(monitoringLocation);
        node.setMonitoringLocationId(monitoringLocation.getId());

        return nodeRepository.save(node);
    }


    public void updateNodeInfo(DefaultNode node, NodeInfoResult nodeInfo) {
        mapper.updateFromNodeInfo(nodeInfo, node);
        if (StringUtils.isNotEmpty(nodeInfo.getSystemName())) {
            node.setNodeLabel(nodeInfo.getSystemName());
        }
        nodeRepository.save(node);
    }

    public void sendNewNodeTaskSetAsync(DefaultNode node, String location, IcmpActiveDiscoveryDTO icmpDiscoveryDTO) {
        executorService.execute(() -> sendTaskSetsToMinion(node, location, icmpDiscoveryDTO));
    }

    private void sendTaskSetsToMinion(DefaultNode node, String location, IcmpActiveDiscoveryDTO icmpDiscoveryDTO) {

        List<SnmpConfiguration> snmpConfigs = new ArrayList<>();
        try {
            var snmpConf = icmpDiscoveryDTO.getSnmpConf();
            snmpConf.getReadCommunityList().forEach(readCommunity -> {
                var builder = SnmpConfiguration.newBuilder()
                    .setReadCommunity(readCommunity);
                snmpConfigs.add(builder.build());
            });
            snmpConf.getPortsList().forEach(port -> {
                var builder = SnmpConfiguration.newBuilder()
                    .setPort(port);
                snmpConfigs.add(builder.build());
            });
            scannerTaskSetService.sendNodeScannerTask(mapper.modelToDto(node), location, snmpConfigs);
        } catch (Exception e) {
            LOG.error("Error while sending nodescan task for node with label {}", node.getNodeLabel());
        }
    }
}
