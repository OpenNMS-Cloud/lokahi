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
package org.opennms.horizon.inventory.grpc;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.google.common.net.InetAddresses;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.protobuf.BoolValue;
import com.google.protobuf.Empty;
import com.google.protobuf.Int64Value;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.Context;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.opennms.horizon.inventory.dto.ActiveDiscoveryDTO;
import org.opennms.horizon.inventory.dto.ActiveDiscoveryList;
import org.opennms.horizon.inventory.dto.IpInterfaceDTO;
import org.opennms.horizon.inventory.dto.IpInterfaceList;
import org.opennms.horizon.inventory.dto.MonitoredStateQuery;
import org.opennms.horizon.inventory.dto.MonitoringPolicies;
import org.opennms.horizon.inventory.dto.NodeCreateDTO;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.inventory.dto.NodeIdList;
import org.opennms.horizon.inventory.dto.NodeIdQuery;
import org.opennms.horizon.inventory.dto.NodeLabelSearchQuery;
import org.opennms.horizon.inventory.dto.NodeList;
import org.opennms.horizon.inventory.dto.NodeServiceGrpc;
import org.opennms.horizon.inventory.dto.NodeUpdateDTO;
import org.opennms.horizon.inventory.dto.SearchBy;
import org.opennms.horizon.inventory.dto.SearchIpInterfaceQuery;
import org.opennms.horizon.inventory.dto.SnmpInterfaceDTO;
import org.opennms.horizon.inventory.dto.SnmpInterfacesList;
import org.opennms.horizon.inventory.dto.TagNameQuery;
import org.opennms.horizon.inventory.exception.EntityExistException;
import org.opennms.horizon.inventory.exception.InventoryRuntimeException;
import org.opennms.horizon.inventory.exception.LocationNotFoundException;
import org.opennms.horizon.inventory.mapper.NodeMapper;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.service.IpInterfaceService;
import org.opennms.horizon.inventory.service.MonitoringLocationService;
import org.opennms.horizon.inventory.service.NodeService;
import org.opennms.horizon.inventory.service.SnmpInterfaceService;
import org.opennms.horizon.inventory.service.TagService;
import org.opennms.horizon.inventory.service.taskset.ScannerTaskSetService;
import org.opennms.taskset.contract.ScanType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NodeGrpcService extends NodeServiceGrpc.NodeServiceImplBase {

    public static final String DIDNT_MATCH_NODE_ID_MSG = "Didn't find a valid node id with the given query";
    public static final String INVALID_REQUEST_LOCATION_AND_IP_NOT_EMPTY_MSG =
            "Invalid Request Query, location/ipAddress can't be empty or not found.";
    public static final String TENANT_ID_IS_MISSING_MSG = "Tenant ID is missing";
    public static final String IP_ADDRESS_ALREADY_EXISTS_FOR_LOCATION_MSG = "Ip address already exists for location";
    public static final String EMPTY_TENANT_ID_MSG = "Tenant Id can't be empty";

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(NodeGrpcService.class);

    @Setter
    private Logger LOG = DEFAULT_LOGGER;

    private final NodeService nodeService;
    private final IpInterfaceService ipInterfaceService;
    private final NodeMapper nodeMapper;
    private final TenantLookup tenantLookup;
    private final ScannerTaskSetService scannerService;
    private final MonitoringLocationService monitoringLocationService;
    private final SnmpInterfaceService snmpInterfaceService;
    private final TagService tagService;

    private final ThreadFactory threadFactory =
            new ThreadFactoryBuilder().setNameFormat("send-taskset-for-node-%d").build();
    // Add setter for unit testing
    @Setter
    private ExecutorService executorService = Executors.newFixedThreadPool(10, threadFactory);

    @Override
    public void createNode(NodeCreateDTO request, StreamObserver<NodeDTO> responseObserver) {
        String tenantId = tenantLookup.lookupTenantId(Context.current()).orElseThrow();
        boolean valid = validateInput(request, responseObserver);
        if (valid) {
            try {
                Node node = nodeService.createNode(request, ScanType.NODE_SCAN, tenantId);
                responseObserver.onNext(nodeMapper.modelToDTO(node));
                responseObserver.onCompleted();
                // Asynchronously send task sets to Minion
                executorService.execute(() -> sendNodeScanTaskToMinion(node));
            } catch (EntityExistException e) {
                Status status = Status.newBuilder()
                        .setCode(Code.ALREADY_EXISTS_VALUE)
                        .setMessage(IP_ADDRESS_ALREADY_EXISTS_FOR_LOCATION_MSG)
                        .build();
                responseObserver.onError(StatusProto.toStatusRuntimeException(status));
            } catch (LocationNotFoundException e) {
                Status status = Status.newBuilder()
                        .setCode(Code.NOT_FOUND_VALUE)
                        .setMessage(INVALID_REQUEST_LOCATION_AND_IP_NOT_EMPTY_MSG)
                        .build();
                responseObserver.onError(StatusProto.toStatusRuntimeException(status));
            }
        }
    }

    @Override
    public void updateNode(NodeUpdateDTO request, StreamObserver<Int64Value> responseObserver) {
        try {
            String tenantId = tenantLookup.lookupTenantId(Context.current()).orElseThrow();
            Long nodeId = nodeService.updateNode(request, tenantId);
            responseObserver.onNext(Int64Value.of(nodeId));
            responseObserver.onCompleted();
        } catch (Exception e) {
            Status status = Status.newBuilder()
                    .setCode(e instanceof InventoryRuntimeException ? Code.INVALID_ARGUMENT_VALUE : Code.INTERNAL_VALUE)
                    .setMessage(e.getMessage())
                    .build();
            responseObserver.onError(StatusProto.toStatusRuntimeException(status));
        }
    }

    @Override
    public void listNodes(Empty request, StreamObserver<NodeList> responseObserver) {
        List<NodeDTO> list = tenantLookup
                .lookupTenantId(Context.current())
                .map(nodeService::findByTenantId)
                .orElseThrow();
        responseObserver.onNext(NodeList.newBuilder().addAllNodes(list).build());
        responseObserver.onCompleted();
    }

    @Override
    public void listNodesByMonitoredState(MonitoredStateQuery request, StreamObserver<NodeList> responseObserver) {
        try {
            List<NodeDTO> list = tenantLookup
                    .lookupTenantId(Context.current())
                    .map((Function<String, List<NodeDTO>>)
                            tenantId -> nodeService.findByMonitoredState(tenantId, request.getMonitoredState()))
                    .orElseThrow();
            responseObserver.onNext(NodeList.newBuilder().addAllNodes(list).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void getNodeById(Int64Value request, StreamObserver<NodeDTO> responseObserver) {
        Optional<NodeDTO> node = tenantLookup
                .lookupTenantId(Context.current())
                .map(tenantId -> nodeService.getByIdAndTenantId(request.getValue(), tenantId))
                .orElseThrow();
        node.ifPresentOrElse(
                nodeDTO -> {
                    responseObserver.onNext(nodeDTO);
                    responseObserver.onCompleted();
                },
                () -> responseObserver.onError(
                        StatusProto.toStatusRuntimeException(createStatusNotExits(request.getValue()))));
    }

    @Override
    public void getNodeIdFromQuery(NodeIdQuery request, StreamObserver<Int64Value> responseObserver) {

        Optional<String> tenantIdOptional = tenantLookup.lookupTenantId(Context.current());

        if (tenantIdOptional.isEmpty()) {
            Status status = Status.newBuilder()
                    .setCode(Code.INVALID_ARGUMENT_VALUE)
                    .setMessage(EMPTY_TENANT_ID_MSG)
                    .build();
            responseObserver.onError(StatusProto.toStatusRuntimeException(status));
            return;
        }

        String tenantId = tenantIdOptional.get();
        String locationId = request.getLocationId();
        var location = monitoringLocationService.findByLocationIdAndTenantId(Long.parseLong(locationId), tenantId);
        if (location.isEmpty()) {
            Status status = Status.newBuilder()
                    .setCode(Code.NOT_FOUND_VALUE)
                    .setMessage(INVALID_REQUEST_LOCATION_AND_IP_NOT_EMPTY_MSG)
                    .build();
            responseObserver.onError(StatusProto.toStatusRuntimeException(status));
            return;
        }
        String ipAddress = request.getIpAddress();
        if (Strings.isNullOrEmpty(locationId) || Strings.isNullOrEmpty(ipAddress)) {
            Status status = Status.newBuilder()
                    .setCode(Code.INVALID_ARGUMENT_VALUE)
                    .setMessage(INVALID_REQUEST_LOCATION_AND_IP_NOT_EMPTY_MSG)
                    .build();
            responseObserver.onError(StatusProto.toStatusRuntimeException(status));
            return;
        }

        Optional<IpInterfaceDTO> optional =
                ipInterfaceService.findByIpAddressAndLocationIdAndTenantId(ipAddress, locationId, tenantId);

        if (optional.isEmpty()) {
            Status status = Status.newBuilder()
                    .setCode(Code.NOT_FOUND_VALUE)
                    .setMessage(DIDNT_MATCH_NODE_ID_MSG)
                    .build();
            responseObserver.onError(StatusProto.toStatusRuntimeException(status));
            return;
        }

        var ipInterface = optional.get();
        long nodeId = ipInterface.getNodeId();
        var nodeIdProto = Int64Value.newBuilder().setValue(nodeId).build();
        responseObserver.onNext(nodeIdProto);
        responseObserver.onCompleted();
    }

    @Override
    public void listNodesByNodeLabel(NodeLabelSearchQuery request, StreamObserver<NodeList> responseObserver) {
        Optional<String> tenantIdOptional = tenantLookup.lookupTenantId(Context.current());

        tenantIdOptional.ifPresentOrElse(
                tenantId -> {
                    try {
                        List<NodeDTO> nodes = nodeService.listNodesByNodeLabelSearch(tenantId, request.getSearchTerm());
                        responseObserver.onNext(
                                NodeList.newBuilder().addAllNodes(nodes).build());
                        responseObserver.onCompleted();
                    } catch (Exception e) {

                        Status status = Status.newBuilder()
                                .setCode(Code.INTERNAL_VALUE)
                                .setMessage(e.getMessage())
                                .build();
                        responseObserver.onError(StatusProto.toStatusRuntimeException(status));
                    }
                },
                () -> {
                    Status status = Status.newBuilder()
                            .setCode(Code.INVALID_ARGUMENT_VALUE)
                            .setMessage(EMPTY_TENANT_ID_MSG)
                            .build();
                    responseObserver.onError(StatusProto.toStatusRuntimeException(status));
                });
    }

    @Override
    public void listNodesByTags(TagNameQuery request, StreamObserver<NodeList> responseObserver) {
        Optional<String> tenantIdOptional = tenantLookup.lookupTenantId(Context.current());

        tenantIdOptional.ifPresentOrElse(
                tenantId -> {
                    try {
                        List<NodeDTO> nodes = nodeService.listNodesByTags(tenantId, request.getTagsList());
                        responseObserver.onNext(
                                NodeList.newBuilder().addAllNodes(nodes).build());
                        responseObserver.onCompleted();
                    } catch (Exception e) {

                        Status status = Status.newBuilder()
                                .setCode(Code.INTERNAL_VALUE)
                                .setMessage(e.getMessage())
                                .build();
                        responseObserver.onError(StatusProto.toStatusRuntimeException(status));
                    }
                },
                () -> {
                    Status status = Status.newBuilder()
                            .setCode(Code.INVALID_ARGUMENT_VALUE)
                            .setMessage(EMPTY_TENANT_ID_MSG)
                            .build();
                    responseObserver.onError(StatusProto.toStatusRuntimeException(status));
                });
    }

    @Override
    public void deleteNode(Int64Value request, StreamObserver<BoolValue> responseObserver) {
        // TBD888: why lookup the node, then delete it by ID?  How about just calling deleteNode() and skip the
        // getByIdAndTenantId?
        Optional<NodeDTO> node = tenantLookup
                .lookupTenantId(Context.current())
                .map(tenantId -> nodeService.getByIdAndTenantId(request.getValue(), tenantId))
                .orElseThrow();

        node.ifPresentOrElse(
                nodeDTO -> deleteNodeByDTO(nodeDTO, request, responseObserver),
                () -> responseObserver.onError(
                        StatusProto.toStatusRuntimeException(createStatusNotExits(request.getValue()))));
    }

    private void deleteNodeByDTO(NodeDTO nodeDTO, Int64Value request, StreamObserver<BoolValue> responseObserver) {
        try {
            nodeService.deleteNode(nodeDTO.getId());
            responseObserver.onNext(BoolValue.newBuilder().setValue(true).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOG.error("Error while deleting node with ID {}", request.getValue(), e);
            Status status = Status.newBuilder()
                    .setCode(Code.INTERNAL_VALUE)
                    .setMessage("Error while deleting node with ID " + request.getValue())
                    .build();
            responseObserver.onError(StatusProto.toStatusRuntimeException(status));
        }
    }

    @Override
    public void startNodeScanByIds(NodeIdList request, StreamObserver<BoolValue> responseObserver) {
        tenantLookup
                .lookupTenantId(Context.current())
                .ifPresentOrElse(
                        tenantId -> startNodeScanByIdsForTenant(tenantId, request, responseObserver),
                        () -> responseObserver.onError(
                                StatusProto.toStatusRuntimeException(createTenantIdMissingStatus())));
    }

    private void startNodeScanByIdsForTenant(
            String tenantId, NodeIdList request, StreamObserver<BoolValue> responseObserver) {
        Map<Long, List<NodeDTO>> nodes = nodeService.listNodeByIds(request.getIdsList(), tenantId);
        AtomicLong totalNodes = new AtomicLong();
        nodes.forEach((k, v) -> totalNodes.addAndGet(v.size()));
        if (request.getIdsCount() == totalNodes.get()) {
            executorService.execute(() -> sendScannerTasksToMinion(nodes, tenantId));
            responseObserver.onNext(BoolValue.of(true));
            responseObserver.onCompleted();
        } else {
            Set<Long> diff = Sets.difference(new HashSet<>(request.getIdsList()), nodes.keySet());
            Status status = Status.newBuilder()
                    .setCode(Code.NOT_FOUND_VALUE)
                    .setMessage(
                            "No nodes exist with ids " + diff.stream().sorted().toList())
                    .build();
            responseObserver.onError(StatusProto.toStatusRuntimeException(status));
        }
    }

    /**
     * Get IpInterface by locationId and ipAddress
     * @param request
     * @param responseObserver
     */
    @Override
    public void getIpInterfaceFromQuery(NodeIdQuery request, StreamObserver<IpInterfaceDTO> responseObserver) {
        tenantLookup
                .lookupTenantId(Context.current())
                .ifPresentOrElse(
                        tenantId -> {
                            var location = monitoringLocationService.findByLocationIdAndTenantId(
                                    Long.parseLong(request.getLocationId()), tenantId);
                            if (location.isEmpty()) {
                                Status status = Status.newBuilder()
                                        .setCode(Code.NOT_FOUND_VALUE)
                                        .setMessage(INVALID_REQUEST_LOCATION_AND_IP_NOT_EMPTY_MSG)
                                        .build();
                                responseObserver.onError(StatusProto.toStatusRuntimeException(status));
                                return;
                            }
                            ipInterfaceService
                                    .findByIpAddressAndLocationIdAndTenantId(
                                            request.getIpAddress(), request.getLocationId(), tenantId)
                                    .ifPresentOrElse(
                                            ipInterface -> {
                                                responseObserver.onNext(ipInterface);
                                                responseObserver.onCompleted();
                                            },
                                            () -> responseObserver.onError(
                                                    StatusProto.toStatusRuntimeException(Status.newBuilder()
                                                            .setCode(Code.NOT_FOUND_VALUE)
                                                            .setMessage(String.format(
                                                                    "IpInterface with IP: %s doesn't exist.",
                                                                    request.getIpAddress()))
                                                            .build())));
                        },
                        () -> responseObserver.onError(
                                StatusProto.toStatusRuntimeException(createTenantIdMissingStatus())));
    }

    @Override
    public void getIpInterfaceById(Int64Value request, StreamObserver<IpInterfaceDTO> responseObserver) {
        var ipInterface = tenantLookup
                .lookupTenantId(Context.current())
                .map(tenantId -> ipInterfaceService.getByIdAndTenantId(request.getValue(), tenantId))
                .orElseThrow();
        ipInterface.ifPresentOrElse(
                ipInterfaceDTO -> {
                    responseObserver.onNext(ipInterfaceDTO);
                    responseObserver.onCompleted();
                },
                () -> responseObserver.onError(
                        StatusProto.toStatusRuntimeException(createStatusNotExits(request.getValue()))));
    }

    @Override
    public void getNodeCount(Empty request, StreamObserver<Int64Value> responseObserver) {
        try {
            Optional<String> tenantIdOptional = tenantLookup.lookupTenantId(Context.current());
            if (tenantIdOptional.isEmpty()) {
                Status status = Status.newBuilder()
                        .setCode(Code.INVALID_ARGUMENT_VALUE)
                        .setMessage(EMPTY_TENANT_ID_MSG)
                        .build();
                responseObserver.onError(StatusProto.toStatusRuntimeException(status));
                return;
            }
            String tenantId = tenantIdOptional.get();
            long val = nodeService.getNodeCount(tenantId);
            responseObserver.onNext(Int64Value.of(val));
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOG.error("Error while getting node count", e);
            Status status = Status.newBuilder()
                    .setCode(Code.INTERNAL_VALUE)
                    .setMessage("Error while getting node count")
                    .build();
            responseObserver.onError(StatusProto.toStatusRuntimeException(status));
        }
    }

    @Override
    public void listSnmpInterfaces(SearchBy searchBy, StreamObserver<SnmpInterfacesList> responseObserver) {
        try {
            Optional<String> tenantIdOptional = tenantLookup.lookupTenantId(Context.current());
            if (tenantIdOptional.isPresent()) {
                List<SnmpInterfaceDTO> list = snmpInterfaceService.searchBy(searchBy, tenantIdOptional.get());

                responseObserver.onNext(SnmpInterfacesList.newBuilder()
                        .addAllSnmpInterfaces(list)
                        .build());
                responseObserver.onCompleted();
            }
        } catch (Exception e) {
            LOG.error("Error while getting snmpInterfaces", e);
            Status status = Status.newBuilder()
                    .setCode(Code.INTERNAL_VALUE)
                    .setMessage("Error while getting snmpInterfaces")
                    .build();
            responseObserver.onError(StatusProto.toStatusRuntimeException(status));
        }
    }

    private Status createTenantIdMissingStatus() {
        return Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(TENANT_ID_IS_MISSING_MSG)
                .build();
    }

    private Status createStatusNotExits(long id) {
        return Status.newBuilder()
                .setCode(Code.NOT_FOUND_VALUE)
                .setMessage(String.format("Node with id: %s doesn't exist.", id))
                .build();
    }

    private boolean validateInput(NodeCreateDTO request, StreamObserver<NodeDTO> responseObserver) {
        boolean valid = true;

        if (request.hasManagementIp() && (!InetAddresses.isInetAddress(request.getManagementIp()))) {
            valid = false;
            Status status = Status.newBuilder()
                    .setCode(Code.INVALID_ARGUMENT_VALUE)
                    .setMessage("Bad management_ip: " + request.getManagementIp())
                    .build();
            responseObserver.onError(StatusProto.toStatusRuntimeException(status));
        }

        return valid;
    }

    private void sendNodeScanTaskToMinion(Node node) {
        try {
            scannerService.sendNodeScannerTask(
                    List.of(nodeMapper.modelToDTO(node)), node.getMonitoringLocationId(), node.getTenantId());
        } catch (Exception e) {
            LOG.error("Error while sending detector task for node with label {}", node.getNodeLabel(), e);
        }
    }

    private void sendScannerTasksToMinion(Map<Long, List<NodeDTO>> locationNodes, String tenantId) {
        for (Map.Entry<Long, List<NodeDTO>> entry : locationNodes.entrySet()) {
            scannerService.sendNodeScannerTask(entry.getValue(), entry.getKey(), tenantId);
        }
    }

    @Override
    public void searchIpInterfaces(SearchIpInterfaceQuery request, StreamObserver<IpInterfaceList> responseObserver) {
        Optional<String> tenantIdOptional = tenantLookup.lookupTenantId(Context.current());

        tenantIdOptional.ifPresentOrElse(
                tenantId -> {
                    try {
                        List<IpInterfaceDTO> ipInterfaceList = nodeService.searchIpInterfacesByNodeAndSearchTerm(
                                tenantId, request.getNodeId(), request.getSearchTerm());
                        responseObserver.onNext(IpInterfaceList.newBuilder()
                                .addAllIpInterface(ipInterfaceList)
                                .build());
                        responseObserver.onCompleted();
                    } catch (Exception e) {
                        LOG.error("Error while searching IpInterfaces ", e);
                        Status status = Status.newBuilder()
                                .setCode(Code.INTERNAL_VALUE)
                                .setMessage(e.getMessage())
                                .build();
                        responseObserver.onError(StatusProto.toStatusRuntimeException(status));
                    }
                },
                () -> {
                    Status status = Status.newBuilder()
                            .setCode(Code.INVALID_ARGUMENT_VALUE)
                            .setMessage(EMPTY_TENANT_ID_MSG)
                            .build();
                    responseObserver.onError(StatusProto.toStatusRuntimeException(status));
                });
    }

    /**
     * * Get Discoveries by nodeId
     * @param request
     * @param responseObserver
     */
    @Override
    public void getDiscoveriesByNode(Int64Value request, StreamObserver<ActiveDiscoveryList> responseObserver) {

        Optional<NodeDTO> node = tenantLookup
                .lookupTenantId(Context.current())
                .map(tenantId -> nodeService.getByIdAndTenantId(request.getValue(), tenantId))
                .orElseThrow();
        node.ifPresentOrElse(
                nodeDTO -> {
                    try {
                        List<ActiveDiscoveryDTO> discoveries = nodeService.getActiveDiscoveriesByIdList(
                                nodeDTO.getTenantId(), nodeDTO.getDiscoveryIdsList());
                        responseObserver.onNext(ActiveDiscoveryList.newBuilder()
                                .addAllActiveDiscoveries(discoveries)
                                .build());
                        responseObserver.onCompleted();
                    } catch (Exception e) {
                        Status status = Status.newBuilder()
                                .setCode(Code.INTERNAL_VALUE)
                                .setMessage(e.getMessage())
                                .build();
                        responseObserver.onError(StatusProto.toStatusRuntimeException(status));
                    }
                },
                () -> {
                    Status status = Status.newBuilder()
                            .setCode(Code.NOT_FOUND_VALUE)
                            .setMessage("Node not found")
                            .build();
                    responseObserver.onError(StatusProto.toStatusRuntimeException(status));
                });
    }

    @Override
    public void getMonitoringPoliciesByNode(Int64Value request, StreamObserver<MonitoringPolicies> responseObserver) {
        Optional<String> tenantIdOptional = tenantLookup.lookupTenantId(Context.current());

        tenantIdOptional.ifPresentOrElse(
                tenantId -> {
                    try {
                        List<Integer> monitoredPolicies =
                                tagService.getMonitoringPoliciesByNodeId(tenantId, request.getValue());
                        responseObserver.onNext(MonitoringPolicies.newBuilder()
                                .addAllIds(monitoredPolicies.stream()
                                        .map(Long::valueOf)
                                        .collect(Collectors.toList()))
                                .build());
                        responseObserver.onCompleted();
                    } catch (Exception e) {

                        Status status = Status.newBuilder()
                                .setCode(Code.INTERNAL_VALUE)
                                .setMessage(e.getMessage())
                                .build();
                        responseObserver.onError(StatusProto.toStatusRuntimeException(status));
                    }
                },
                () -> {
                    Status status = Status.newBuilder()
                            .setCode(Code.INVALID_ARGUMENT_VALUE)
                            .setMessage(EMPTY_TENANT_ID_MSG)
                            .build();
                    responseObserver.onError(StatusProto.toStatusRuntimeException(status));
                });
    }
}
