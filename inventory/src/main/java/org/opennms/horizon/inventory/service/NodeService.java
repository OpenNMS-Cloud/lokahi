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
import org.opennms.horizon.inventory.dto.NodeCreateDTO;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.inventory.mapper.NodeMapper;
import org.opennms.horizon.inventory.model.IpInterface;
import org.opennms.horizon.inventory.model.MonitoringLocation;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.model.Tag;
import org.opennms.horizon.inventory.repository.IpInterfaceRepository;
import org.opennms.horizon.inventory.repository.MonitoringLocationRepository;
import org.opennms.horizon.inventory.repository.NodeRepository;
import org.opennms.horizon.inventory.service.taskset.CollectorTaskSetService;
import org.opennms.horizon.inventory.service.taskset.DetectorTaskSetService;
import org.opennms.horizon.inventory.service.taskset.MonitorTaskSetService;
import org.opennms.horizon.inventory.service.taskset.ScannerTaskSetService;
import org.opennms.horizon.inventory.taskset.api.TaskSetPublisher;
import org.opennms.horizon.inventory.repository.TagRepository;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.opennms.taskset.contract.MonitorType;
import org.opennms.taskset.contract.TaskDefinition;
import org.opennms.node.scan.contract.NodeInfoResult;
import org.opennms.taskset.contract.ScanType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final TagRepository tagRepository;
    private final ConfigUpdateService configUpdateService;
    private final DetectorTaskSetService detectorTaskSetService;
    private final CollectorTaskSetService collectorTaskSetService;
    private final MonitorTaskSetService monitorTaskSetService;
    private final ScannerTaskSetService scannerTaskSetService;
    private final TaskSetPublisher taskSetPublisher;


    private final NodeMapper mapper;

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

    private MonitoringLocation saveMonitoringLocation(NodeCreateDTO request, String tenantId) {
        String location = StringUtils.isEmpty(request.getLocation()) ? GrpcConstants.DEFAULT_LOCATION : request.getLocation();
        Optional<MonitoringLocation> found =
            monitoringLocationRepository.findByLocationAndTenantId(location, tenantId);

        if (found.isPresent()) {
            return found.get();
        } else {
            MonitoringLocation newLocation = new MonitoringLocation();

            newLocation.setTenantId(tenantId);
            newLocation.setLocation(location);

            MonitoringLocation saved = monitoringLocationRepository.save(newLocation);
            // Asynchronously send config updates to Minion
            configUpdateService.sendConfigUpdate(tenantId, saved.getLocation());
            return saved;
        }
    }

    private Node saveNode(NodeCreateDTO request, MonitoringLocation monitoringLocation,
                          ScanType scanType, String tenantId) {

        Node node = new Node();

        node.setTenantId(tenantId);
        node.setNodeLabel(request.getLabel());
        node.setScanType(scanType);
        node.setCreateTime(LocalDateTime.now());
        node.setMonitoringLocation(monitoringLocation);
        node.setMonitoringLocationId(monitoringLocation.getId());

        return nodeRepository.save(node);
    }

    @Transactional
    public Node createNode(NodeCreateDTO request, ScanType scanType, String tenantId) {
        MonitoringLocation monitoringLocation = saveMonitoringLocation(request, tenantId);
        Node node = saveNode(request, monitoringLocation, scanType, tenantId);
        saveIpInterfaces(request, node, tenantId);

        return node;
    }

    @Transactional
    public Map<String, Map<String, List<NodeDTO>>> listAllNodeForMonitoring() {
        Map<String, Map<String, List<NodeDTO>>> nodesByTenantLocation = new HashMap<>();
        nodeRepository.findAll().forEach(node -> {
            Map<String, List<NodeDTO>> nodeByLocation = nodesByTenantLocation.computeIfAbsent(node.getTenantId(), (tenantId) -> new HashMap<>());
            List<NodeDTO> nodeList = nodeByLocation.computeIfAbsent(node.getMonitoringLocation().getLocation(), location -> new ArrayList<>());
            nodeList.add(mapper.modelToDTO(node));
        });
        return nodesByTenantLocation;
    }

    @Transactional
    public Map<String, List<NodeDTO>> listNodeByIds(List<Long> ids, String tenantId) {
        List<Node> nodeList = nodeRepository.findByIdInAndTenantId(ids, tenantId);
        if(nodeList.isEmpty()) {
            return new HashMap<>();
        }
        return nodeList.stream().collect(Collectors.groupingBy(node -> node.getMonitoringLocation().getLocation(),
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
            var location = node.getMonitoringLocation().getLocation();
            var tasks = getTasksForNode(node);
            removeAssociatedTags(node);
            nodeRepository.deleteById(id);
            executorService.execute(() -> taskSetPublisher.publishTaskDeletion(tenantId, location, tasks));
        }
    }

    public List<TaskDefinition> getTasksForNode(Node node) {
        var tasks = new ArrayList<TaskDefinition>();
        var detectorTasks = detectorTaskSetService.getDetectorTasks(node);
        scannerTaskSetService.getNodeScanTasks(node).ifPresent(tasks::add);
        tasks.addAll(detectorTasks);
        node.getIpInterfaces().forEach(ipInterface -> {
            ipInterface.getMonitoredServices().forEach((ms) -> {
                String serviceName = ms.getMonitoredServiceType().getServiceName();
                var monitorType = MonitorType.valueOf(serviceName);
                var monitorTask = monitorTaskSetService.getMonitorTask(monitorType, ipInterface, node.getId());
                Optional.ofNullable(monitorTask).ifPresent(tasks::add);
                var collectorTask = collectorTaskSetService.getCollectorTask(monitorType, ipInterface, node.getId());
                Optional.ofNullable(collectorTask).ifPresent(tasks::add);
            });
        });
        return tasks;
    }

    public void updateNodeInfo(Node node, NodeInfoResult nodeInfo) {
        mapper.updateFromNodeInfo(nodeInfo, node);
        nodeRepository.save(node);
    }

    private void removeAssociatedTags(Node node) {
        for (Tag tag : node.getTags()) {
            tag.getNodes().remove(node);
        }
    }
}
