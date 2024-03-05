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
package org.opennms.horizon.inventory.service;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.opennms.horizon.inventory.component.TagPublisher;
import org.opennms.horizon.inventory.discovery.IcmpActiveDiscoveryDTO;
import org.opennms.horizon.inventory.dto.IpInterfaceDTO;
import org.opennms.horizon.inventory.dto.MonitoredState;
import org.opennms.horizon.inventory.dto.NodeCreateDTO;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.inventory.dto.NodeUpdateDTO;
import org.opennms.horizon.inventory.dto.TagCreateListDTO;
import org.opennms.horizon.inventory.dto.TagEntityIdDTO;
import org.opennms.horizon.inventory.exception.EntityExistException;
import org.opennms.horizon.inventory.exception.InventoryRuntimeException;
import org.opennms.horizon.inventory.exception.LocationNotFoundException;
import org.opennms.horizon.inventory.mapper.IpInterfaceMapper;
import org.opennms.horizon.inventory.mapper.NodeMapper;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class NodeService {
    private static final String DEFAULT_TAG = "default";
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
    private final TagPublisher tagPublisher;
    private final TagRepository tagRepository;
    private final IpInterfaceMapper ipInterfaceMapper;

    @Transactional(readOnly = true)
    public List<NodeDTO> findByTenantId(String tenantId) {
        List<Node> all = nodeRepository.findByTenantId(tenantId);
        return all.stream().map(mapper::modelToDTO).toList();
    }

    @Transactional(readOnly = true)
    public Optional<NodeDTO> getByIdAndTenantId(long id, String tenantId) {
        return nodeRepository.findByIdAndTenantId(id, tenantId).map(mapper::modelToDTO);
    }

    @Transactional(readOnly = true)
    public List<NodeDTO> findByMonitoredState(String tenantId, MonitoredState monitoredState) {
        return nodeRepository.findByTenantIdAndMonitoredStateEquals(tenantId, monitoredState).stream()
                .map(mapper::modelToDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<Node> getNode(String tenantId, String location, InetAddress primaryIpAddress) {
        var list = nodeRepository.findByTenantId(tenantId);
        if (list.isEmpty()) {
            return Optional.empty();
        }
        var listOfNodesOnLocation = list.stream()
                .filter(node -> node.getMonitoringLocation().getLocation().equals(location))
                .toList();
        if (listOfNodesOnLocation.isEmpty()) {
            return Optional.empty();
        }
        return listOfNodesOnLocation.stream()
                .filter(node -> node.getIpInterfaces().stream()
                        .anyMatch(ipInterface -> ipInterface.getSnmpPrimary()
                                && ipInterface.getIpAddress().equals(primaryIpAddress)))
                .findFirst();
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

    private MonitoringLocation findMonitoringLocation(NodeCreateDTO request, String tenantId)
            throws LocationNotFoundException {
        Optional<MonitoringLocation> found =
                monitoringLocationRepository.findByIdAndTenantId(Long.parseLong(request.getLocationId()), tenantId);

        return found.orElseThrow(
                () -> new LocationNotFoundException("Location not found with ID " + request.getLocationId()));
    }

    private Node saveNode(
            NodeCreateDTO request, MonitoringLocation monitoringLocation, ScanType scanType, String tenantId) {

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
    public Node createNode(NodeCreateDTO request, ScanType scanType, String tenantId)
            throws EntityExistException, LocationNotFoundException {
        if (request.hasManagementIp()) { // Do we really want to create a node without managed IP?
            Optional<IpInterface> ipInterfaceOpt = ipInterfaceRepository.findByIpLocationIdTenantAndScanType(
                    InetAddressUtils.getInetAddress(request.getManagementIp()),
                    Long.parseLong(request.getLocationId()),
                    tenantId,
                    scanType);
            if (ipInterfaceOpt.isPresent()) {
                IpInterface ipInterface = ipInterfaceOpt.get();
                log.error(
                        "IP address {} already exists in the system and belong to device {}",
                        request.getManagementIp(),
                        ipInterface.getNode().getNodeLabel());
                throw new EntityExistException("IP address " + request.getManagementIp()
                        + " already exists in the system and belong to device "
                        + ipInterface.getNode().getNodeLabel());
            }
        }
        MonitoringLocation monitoringLocation = findMonitoringLocation(request, tenantId);
        Node node = saveNode(request, monitoringLocation, scanType, tenantId);
        saveIpInterfaces(request, node, tenantId);
        tagService.addTags(
                tenantId,
                TagCreateListDTO.newBuilder()
                        .addEntityIds(TagEntityIdDTO.newBuilder().setNodeId(node.getId()))
                        .addAllTags(request.getTagsList())
                        .build());
        return node;
    }

    @Transactional
    public Long updateNode(NodeUpdateDTO request, String tenantId) {
        var node = nodeRepository
                .findByIdAndTenantId(request.getId(), tenantId)
                .orElseThrow(() -> new InventoryRuntimeException("Node with ID " + request.getId() + " not found"));

        if (request.hasNodeAlias()) {
            String alias = request.getNodeAlias().trim();
            if (!StringUtils.isBlank(alias)
                    && nodeRepository.findByNodeAliasAndTenantId(alias, tenantId).stream()
                            .anyMatch(n -> request.getId() != n.getId())) {
                throw new InventoryRuntimeException("Duplicate node alias with name " + alias);
            }
            node.setNodeAlias(request.getNodeAlias());
        }
        return nodeRepository.save(node).getId();
    }

    @Transactional(readOnly = true)
    public Map<Long, List<NodeDTO>> listNodeByIds(List<Long> ids, String tenantId) {
        List<Node> nodeList = nodeRepository.findByIdInAndTenantId(ids, tenantId);
        if (nodeList.isEmpty()) {
            return new HashMap<>();
        }
        return nodeList.stream()
                .collect(Collectors.groupingBy(
                        node -> node.getMonitoringLocation().getId(),
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
                var monitorTask =
                        monitorTaskSetService.getMonitorTask(monitorType, ipInterface, node.getId(), ms.getId(), null);
                Optional.ofNullable(monitorTask).ifPresent(tasks::add);
                var collectorTask =
                        collectorTaskSetService.getCollectorTask(monitorType, ipInterface, node.getId(), null);
                Optional.ofNullable(collectorTask).ifPresent(tasks::add);
            });
        });
        return tasks;
    }

    public void updateNodeMonitoredState(long nodeId, String tenantId) {

        // See HS-1812, Always match "default" tag.
        final var monitored = tagRepository.findByTenantIdAndNodeId(tenantId, nodeId).stream()
                .anyMatch(tag -> !tag.getMonitorPolicyIds().isEmpty() || DEFAULT_TAG.equals(tag.getName()));

        var optional = nodeRepository.findByIdAndTenantId(nodeId, tenantId);
        if (optional.isEmpty()) {
            throw new InventoryRuntimeException("Node not found for id: " + nodeId);
        }
        var node = optional.get();
        final var monitoredState = monitored
                ? MonitoredState.MONITORED
                : (node.getMonitoredState() == MonitoredState.DETECTED
                        ? MonitoredState.DETECTED
                        : MonitoredState.UNMONITORED);

        if (node.getMonitoredState() != monitoredState) {
            node.setMonitoredState(monitoredState);
            this.nodeRepository.save(node);
        }
    }

    @Transactional(readOnly = true)
    public Optional<NodeDTO> getNode(String ipAddress, long locationId, String tenantId) {
        var optionalIpAddress = ipInterfaceRepository.findByIpLocationIdTenantAndScanType(
                InetAddressUtils.getInetAddress(ipAddress), locationId, tenantId, ScanType.DISCOVERY_SCAN);
        return optionalIpAddress.map(IpInterface::getNode).map(mapper::modelToDTO);
    }

    @Transactional(readOnly = true)
    public Long getNodeCount(String tenantId) {
        return nodeRepository.countDistinctNodes(tenantId);
    }

    public void updateNodeInfo(Node node, NodeInfoResult nodeInfo) {
        mapper.updateFromNodeInfo(nodeInfo, node);

        if (StringUtils.isNotEmpty(nodeInfo.getSystemName())) {
            Boolean validIpAddress = isValidInetAddress(node.getNodeLabel());
            if (validIpAddress) {
                // Overwrite node label with System name if label is an IP Address.
                node.setNodeLabel(nodeInfo.getSystemName());
            } else {
                log.debug(
                        "Node already has a nodeLabel - keeping the existing value: node-label={}; system-name={}",
                        node.getNodeLabel(),
                        nodeInfo.getSystemName());
            }
        }

        nodeRepository.save(node);
    }

    private Boolean isValidInetAddress(String ipAddress) {
        try {
            InetAddressUtils.addr(ipAddress);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
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
    }

    public void sendNewNodeTaskSetAsync(NodeDTO nodeDTO, Long locationId, IcmpActiveDiscoveryDTO icmpDiscoveryDTO) {
        executorService.execute(() -> sendTaskSetsToMinion(nodeDTO, locationId, icmpDiscoveryDTO));
    }

    public void sendNewNodeTaskSetAsync(Node node, Long locationId, IcmpActiveDiscoveryDTO icmpDiscoveryDTO) {
        var nodeDTO = mapper.modelToDTO(node);
        executorService.execute(() -> sendTaskSetsToMinion(nodeDTO, locationId, icmpDiscoveryDTO));
    }

    private void sendTaskSetsToMinion(NodeDTO nodeDTO, Long locationId, IcmpActiveDiscoveryDTO icmpDiscoveryDTO) {

        List<SnmpConfiguration> snmpConfigs = new ArrayList<>();
        try {
            var snmpConf = icmpDiscoveryDTO.getSnmpConfig();
            snmpConf.getReadCommunityList().forEach(readCommunity -> {
                var builder = SnmpConfiguration.newBuilder().setReadCommunity(readCommunity);
                snmpConfigs.add(builder.build());
            });
            snmpConf.getPortsList().forEach(port -> {
                var builder = SnmpConfiguration.newBuilder().setPort(port);
                snmpConfigs.add(builder.build());
            });
            scannerTaskSetService.sendNodeScannerTask(nodeDTO, locationId, snmpConfigs);
        } catch (Exception e) {
            log.error("Error while sending nodescan task for node with label {}", nodeDTO.getNodeLabel(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<NodeDTO> listNodesByNodeLabelSearch(String tenantId, String nodeLabelSearchTerm) {
        return nodeRepository.findByTenantIdAndNodeLabelOrAliasLike(tenantId, nodeLabelSearchTerm).stream()
                .map(mapper::modelToDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NodeDTO> listNodesByTags(String tenantId, List<String> tags) {
        return nodeRepository.findByTenantIdAndTagNamesIn(tenantId, tags).stream()
                .map(mapper::modelToDTO)
                .toList();
    }

    public List<IpInterfaceDTO> searchIpInterfacesByNodeAndSearchTerm(
            String tenantId, Long nodeId, String searchIpInterfaceTerm) {

        return ipInterfaceRepository
                .findAllByTenantIdAndNodeIdAndSearchTerm(tenantId, nodeId, searchIpInterfaceTerm)
                .stream()
                .map(ipInterfaceMapper::modelToDTO)
                .collect(Collectors.toList());
    }
}
