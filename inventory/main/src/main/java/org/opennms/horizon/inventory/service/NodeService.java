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

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.opennms.horizon.inventory.component.TagPublisher;
import org.opennms.horizon.inventory.discovery.IcmpActiveDiscoveryDTO;
import org.opennms.horizon.inventory.dto.MonitoredState;
import org.opennms.horizon.inventory.dto.NodeCreateDTO;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.inventory.dto.TagCreateListDTO;
import org.opennms.horizon.inventory.dto.TagEntityIdDTO;
import org.opennms.horizon.inventory.exception.EntityExistException;
import org.opennms.horizon.inventory.exception.LocationNotFoundException;
import org.opennms.horizon.inventory.mapper.IpInterfaceMapper;
import org.opennms.horizon.inventory.mapper.NodeMapper;
import org.opennms.horizon.inventory.mapper.SnmpInterfaceMapper;
import org.opennms.horizon.inventory.model.IpInterface;
import org.opennms.horizon.inventory.model.MonitoringLocation;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.model.Tag;
import org.opennms.horizon.inventory.repository.IpInterfaceRepository;
import org.opennms.horizon.inventory.repository.MonitoringLocationRepository;
import org.opennms.horizon.inventory.repository.NodeRepository;
import org.opennms.horizon.inventory.repository.TagRepository;
import org.opennms.horizon.inventory.service.taskset.CollectorTaskSetService;
import org.opennms.horizon.inventory.service.taskset.MonitorTaskSetService;
import org.opennms.horizon.inventory.service.taskset.ScannerTaskSetService;
import org.opennms.horizon.inventory.service.taskset.publisher.TaskSetPublisher;
import org.opennms.horizon.shared.common.tag.proto.Operation;
import org.opennms.horizon.shared.common.tag.proto.TagOperationProto;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.opennms.horizon.snmp.api.SnmpConfiguration;
import org.opennms.node.scan.contract.NodeInfoResult;
import org.opennms.taskset.contract.MonitorType;
import org.opennms.taskset.contract.ScanType;
import org.opennms.taskset.contract.TaskDefinition;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class NodeService {

    private final ThreadFactory threadFactory = new ThreadFactoryBuilder()
        .setNameFormat("delete-node-task-publish-%d")
        .build();
    private final ExecutorService executorService = Executors.newFixedThreadPool(10, threadFactory);
    private final NodeRepository nodeRepository;
    private final MonitoringLocationRepository monitoringLocationRepository;
    private final IpInterfaceRepository ipInterfaceRepository;
    private final ConfigUpdateService configUpdateService;
    private final CollectorTaskSetService collectorTaskSetService;
    private final MonitorTaskSetService monitorTaskSetService;
    private final ScannerTaskSetService scannerTaskSetService;
    private final TaskSetPublisher taskSetPublisher;
    private final TagService tagService;
    private final NodeMapper mapper;
    private final SnmpInterfaceMapper snmpInterfaceMapper;
    private final IpInterfaceMapper ipInterfaceMapper;
    private final TagPublisher tagPublisher;
    private final TagRepository tagRepository;

    @Transactional(readOnly = true)
    public List<NodeDTO> findByTenantId(String tenantId) {
        List<Node> all = nodeRepository.findByTenantId(tenantId);
        return all
            .stream()
            .map(mapper::modelToDTO)
            .toList();
    }

    @Transactional(readOnly = true)
    public Optional<NodeDTO> getByIdAndTenantId(long id, String tenantId) {
        return nodeRepository.findByIdAndTenantId(id, tenantId).map(mapper::modelToDTO);
    }

    @Transactional(readOnly = true)
    public List<NodeDTO> findByMonitoredState(String tenantId, MonitoredState monitoredState) {
        return nodeRepository.findByTenantIdAndMonitoredStateEquals(tenantId, monitoredState)
            .stream().map(mapper::modelToDTO).toList();
    }

    @Transactional(readOnly = true)
    public Optional<Node> getNode(String tenantId, String location, InetAddress primaryIpAddress) {
        var list = nodeRepository.findByTenantId(tenantId);
        if (list.isEmpty()) {
            return Optional.empty();
        }
        var listOfNodesOnLocation = list.stream().filter(node -> node.getMonitoringLocation().getLocation().equals(location)).toList();
        if (listOfNodesOnLocation.isEmpty()) {
            return Optional.empty();
        }
        return listOfNodesOnLocation.stream().filter(node ->
            node.getIpInterfaces().stream().anyMatch(ipInterface -> ipInterface.getSnmpPrimary() &&
                ipInterface.getIpAddress().equals(primaryIpAddress))).findFirst();

    }

    private void saveIpInterfaces(NodeCreateDTO request, Node node, String tenantId) {
        if (request.hasManagementIp()) {
            IpInterface ipInterface = new IpInterface();
            ipInterface.setNode(node);
            ipInterface.setTenantId(tenantId);
            ipInterface.setIpAddress(InetAddressUtils.getInetAddress(request.getManagementIp()));
            ipInterface.setSnmpPrimary(true);
            ipInterfaceRepository.save(ipInterface);
            node.setIpInterfaces(List.of(ipInterface));
        }
    }

    private MonitoringLocation findMonitoringLocation(NodeCreateDTO request, String tenantId) throws LocationNotFoundException {
        Optional<MonitoringLocation> found = monitoringLocationRepository.findByIdAndTenantId(Long.parseLong(request.getLocationId()), tenantId);

        return found.orElseThrow(() -> new LocationNotFoundException("Location not found " + request.getLocationId()));
    }

    private Node saveNode(NodeCreateDTO request, MonitoringLocation monitoringLocation,
                          ScanType scanType, String tenantId) {

        Node node = new Node();

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

    @Transactional
    public Node createNode(NodeCreateDTO request, ScanType scanType, String tenantId) throws EntityExistException, LocationNotFoundException {
        if (request.hasManagementIp()) { //Do we really want to create a node without managed IP?
            Optional<IpInterface> ipInterfaceOpt = ipInterfaceRepository
                .findByIpLocationIdTenantAndScanType(InetAddressUtils.getInetAddress(request.getManagementIp()), Long.parseLong(request.getLocationId()), tenantId, scanType);
            if (ipInterfaceOpt.isPresent()) {
                IpInterface ipInterface = ipInterfaceOpt.get();
                log.error("IP address {} already exists in the system and belong to device {}", request.getManagementIp(), ipInterface.getNode().getNodeLabel());
                throw new EntityExistException("IP address " + request.getManagementIp() + " already exists in the system and belong to device " + ipInterface.getNode().getNodeLabel());
            }
        }
        MonitoringLocation monitoringLocation = findMonitoringLocation(request, tenantId);
        Node node = saveNode(request, monitoringLocation, scanType, tenantId);
        saveIpInterfaces(request, node, tenantId);

        tagService.addTags(tenantId, TagCreateListDTO.newBuilder()
            .addEntityIds(TagEntityIdDTO.newBuilder()
                .setNodeId(node.getId()))
            .addAllTags(request.getTagsList())
            .build());
        return node;
    }

    @Transactional
    public Map<Long, List<NodeDTO>> listNodeByIds(List<Long> ids, String tenantId) {
        List<Node> nodeList = nodeRepository.findByIdInAndTenantId(ids, tenantId);
        if (nodeList.isEmpty()) {
            return new HashMap<>();
        }
        return nodeList.stream().collect(Collectors.groupingBy(node -> node.getMonitoringLocation().getId(),
            Collectors.mapping(mapper::modelToDTO, Collectors.toList())));
    }

    @Transactional
    public void deleteNode(long id) {
        Optional<Node> optionalNode = nodeRepository.findById(id);
        if (optionalNode.isEmpty()) {
            log.warn("Node with ID {} doesn't exist", id);
            throw new IllegalArgumentException("Node with ID : " + id + "doesn't exist");
        } else {
            var node = optionalNode.get();
            var tenantId = node.getTenantId();
            var location = node.getMonitoringLocationId();
            var tasks = getTasksForNode(node);
            removeAssociatedTags(node);
            nodeRepository.deleteById(id);
            executorService.execute(() -> taskSetPublisher.publishTaskDeletion(tenantId, location, tasks));
        }
    }

    public List<TaskDefinition> getTasksForNode(Node node) {
        var tasks = new ArrayList<TaskDefinition>();
        scannerTaskSetService.getNodeScanTasks(node).ifPresent(tasks::add);
        node.getIpInterfaces().forEach(ipInterface -> {
            ipInterface.getMonitoredServices().forEach((ms) -> {
                String serviceName = ms.getMonitoredServiceType().getServiceName();
                var monitorType = MonitorType.valueOf(serviceName);
                var monitorTask = monitorTaskSetService.getMonitorTask(monitorType, ipInterface, node.getId(), null);
                Optional.ofNullable(monitorTask).ifPresent(tasks::add);
                var collectorTask = collectorTaskSetService.getCollectorTask(monitorType, ipInterface, node.getId(), null);
                Optional.ofNullable(collectorTask).ifPresent(tasks::add);
            });
        });
        return tasks;
    }

    public void updateNodeMonitoredState(Node node) {
        final var monitored = tagRepository.findByTenantIdAndNodeId(node.getTenantId(), node.getId()).stream()
            .anyMatch(tag -> !tag.getMonitorPolicyIds().isEmpty());

        final var monitoredState = monitored
            ? MonitoredState.MONITORED
            : node.getMonitoredState() == MonitoredState.DETECTED
                ? MonitoredState.DETECTED
                : MonitoredState.UNMONITORED;

        if (node.getMonitoredState() != monitoredState) {
            node.setMonitoredState(monitoredState);
            this.nodeRepository.save(node);
        }
    }

    public void updateNodeInfo(Node node, NodeInfoResult nodeInfo) {
        mapper.updateFromNodeInfo(nodeInfo, node);

        if (StringUtils.isNotEmpty(nodeInfo.getSystemName())) {
            // HS-1364: update the node label if the incoming System Name is set, and the existing node value is not
            if (StringUtils.isEmpty(node.getNodeLabel())) {
                node.setNodeLabel(nodeInfo.getSystemName());
            } else {
                log.debug("Node already has a nodeLabel - keeping the existing value: node-label={}; system-name={}", node.getNodeLabel(), nodeInfo.getSystemName());
            }
        }

        this.updateNodeMonitoredState(node);

        nodeRepository.save(node);
    }

    private void removeAssociatedTags(Node node) {
        List<TagOperationProto> tagOpList = new ArrayList<>();
        for (Tag tag : node.getTags()) {
            tag.getNodes().remove(node);
            tagOpList.add(TagOperationProto.newBuilder()
                .setTagName(tag.getName())
                .setTenantId(tag.getTenantId())
                .setOperation(Operation.REMOVE_TAG)
                .addNodeId(node.getId())
                .build());
        }
        tagPublisher.publishTagUpdate(tagOpList);
        this.updateNodeMonitoredState(node);
    }

    public void sendNewNodeTaskSetAsync(Node node, Long locationId, IcmpActiveDiscoveryDTO icmpDiscoveryDTO) {
        executorService.execute(() -> sendTaskSetsToMinion(node, locationId, icmpDiscoveryDTO));
    }

    private void sendTaskSetsToMinion(Node node, Long locationId, IcmpActiveDiscoveryDTO icmpDiscoveryDTO) {

        List<SnmpConfiguration> snmpConfigs = new ArrayList<>();
        try {
            var snmpConf = icmpDiscoveryDTO.getSnmpConfig();
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
            scannerTaskSetService.sendNodeScannerTask(mapper.modelToDTO(node), locationId, snmpConfigs);
        } catch (Exception e) {
            log.error("Error while sending nodescan task for node with label {}", node.getNodeLabel());
        }
    }

    @Transactional(readOnly = true)
    public List<NodeDTO> listNodesByNodeLabelSearch(String tenantId, String nodeLabelSearchTerm) {
        return nodeRepository.findByTenantIdAndNodeLabelLike(tenantId, nodeLabelSearchTerm).stream()
            .map(mapper::modelToDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<NodeDTO> listNodesByTags(String tenantId, List<String> tags) {
        return nodeRepository.findByTenantIdAndTagNamesIn(tenantId, tags).stream()
            .map(mapper::modelToDTO).toList();
    }

}
