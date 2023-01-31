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

package org.opennms.horizon.inventory.grpc;

import com.google.common.base.Strings;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.inventory.dto.IpInterfaceDTO;
import org.opennms.horizon.inventory.dto.NodeCreateDTO;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.inventory.dto.NodeIdList;
import org.opennms.horizon.inventory.dto.NodeIdQuery;
import org.opennms.horizon.inventory.dto.NodeList;
import org.opennms.horizon.inventory.dto.NodeServiceGrpc;
import org.opennms.horizon.inventory.mapper.NodeMapper;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.service.IpInterfaceService;
import org.opennms.horizon.inventory.service.NodeService;
import org.opennms.horizon.inventory.service.taskset.DetectorTaskSetService;
import org.opennms.horizon.inventory.service.taskset.ScannerTaskSetService;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.opennms.taskset.contract.ScanType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Slf4j
@Component
@RequiredArgsConstructor
public class NodeGrpcService extends NodeServiceGrpc.NodeServiceImplBase {
    private final NodeService nodeService;
    private final IpInterfaceService ipInterfaceService;
    private final NodeMapper nodeMapper;
    private final TenantLookup tenantLookup;
    private final DetectorTaskSetService taskSetService;
    private final ScannerTaskSetService scannerService;
    private final ThreadFactory threadFactory = new ThreadFactoryBuilder()
        .setNameFormat("send-taskset-for-node-%d")
        .build();
    private final ExecutorService executorService = Executors.newFixedThreadPool(10, threadFactory);

    @Override
    public void createNode(NodeCreateDTO request, StreamObserver<NodeDTO> responseObserver) {
        Optional<String> tenantId = tenantLookup.lookupTenantId(Context.current());
        boolean valid = tenantId.map(id -> validateInput(request, id, responseObserver)).orElseThrow();

        if (valid) {
            Node node = nodeService.createNode(request, ScanType.NODE_SCAN, tenantId.orElseThrow());
            responseObserver.onNext(nodeMapper.modelToDTO(node));
            responseObserver.onCompleted();

            // Asynchronously send task sets to Minion
            executorService.execute(() -> sendTaskSetsToMinion(node, tenantId.orElseThrow()));
        }
    }

    @Override
    public void listNodes(Empty request, StreamObserver<NodeList> responseObserver) {
        List<NodeDTO> list = tenantLookup.lookupTenantId(Context.current())
            .map(nodeService::findByTenantId).orElseThrow();
        responseObserver.onNext(NodeList.newBuilder().addAllNodes(list).build());
        responseObserver.onCompleted();
    }

    @Override
    public void getNodeById(Int64Value request, StreamObserver<NodeDTO> responseObserver) {
        Optional<NodeDTO> node = tenantLookup.lookupTenantId(Context.current())
            .map(tenantId -> nodeService.getByIdAndTenantId(request.getValue(), tenantId))
            .orElseThrow();
        node.ifPresentOrElse(nodeDTO -> {
            responseObserver.onNext(nodeDTO);
            responseObserver.onCompleted();
        }, () -> responseObserver.onError(StatusProto.toStatusRuntimeException(createStatusNotExits(request.getValue()))));
    }

    @Override
    public void getNodeIdFromQuery(NodeIdQuery request,
                                   StreamObserver<Int64Value> responseObserver) {

        Optional<String> tenantIdOptional = tenantLookup.lookupTenantId(Context.current());

        if (tenantIdOptional.isEmpty()) {
            Status status = Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage("Tenant Id can't be empty")
                .build();
            responseObserver.onError(StatusProto.toStatusRuntimeException(status));
            return;
        }

        String tenantId = tenantIdOptional.get();
        String location = request.getLocation();
        String ipAddress = request.getIpAddress();
        if (Strings.isNullOrEmpty(location) || Strings.isNullOrEmpty(ipAddress)) {
            Status status = Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage("Invalid Request Query, location/ipAddress can't be empty")
                .build();
            responseObserver.onError(StatusProto.toStatusRuntimeException(status));
            return;
        }

        Optional<IpInterfaceDTO> optional = ipInterfaceService.findByIpAddressAndLocationAndTenantId(ipAddress, location, tenantId);

        if (optional.isEmpty()) {
            Status status = Status.newBuilder()
                .setCode(Code.NOT_FOUND_VALUE)
                .setMessage("Didn't find a valid node id with the given query")
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
    public void deleteNode(Int64Value request, StreamObserver<BoolValue> responseObserver) {
        Optional<NodeDTO> node = tenantLookup.lookupTenantId(Context.current())
            .map(tenantId -> nodeService.getByIdAndTenantId(request.getValue(), tenantId))
            .orElseThrow();
        node.ifPresentOrElse(nodeDTO -> {
            try {
                nodeService.deleteNode(nodeDTO.getId());
                responseObserver.onNext(BoolValue.newBuilder().setValue(true).build());
                responseObserver.onCompleted();
            } catch (Exception e) {
                log.error("Error while deleting node with ID {}", request.getValue(), e);
                Status status = Status.newBuilder()
                    .setCode(Code.INTERNAL_VALUE)
                    .setMessage("Error while deleting node with ID " + request.getValue()).build();
                responseObserver.onError(StatusProto.toStatusRuntimeException(status));
            }}, () -> responseObserver.onError(StatusProto.toStatusRuntimeException(createStatusNotExits(request.getValue())))
        );
    }

    @Override
    public void startNodeScanByIds(NodeIdList request, StreamObserver<BoolValue> responseObserver) {
        tenantLookup.lookupTenantId(Context.current()).ifPresentOrElse(tenantId -> {
            Map<String, List<NodeDTO>> nodes = nodeService.listNodeByIds(request.getIdsList(), tenantId);
            if(nodes != null && !nodes.isEmpty()) {
                executorService.execute(() -> sendTaskSetsToMinion(nodes, tenantId));
                responseObserver.onNext(BoolValue.of(true));
                responseObserver.onCompleted();
            } else {
                Status status = Status.newBuilder()
                    .setCode(Code.NOT_FOUND_VALUE)
                    .setMessage("No nodes exist with ids " + request.getIdsList()).build();
                responseObserver.onError(StatusProto.toStatusRuntimeException(status));
            }
        }, () -> responseObserver.onError(StatusProto.toStatusRuntimeException(createTenantIdMissingStatus())));
    }

    private Status createTenantIdMissingStatus() {
        return Status.newBuilder().setCode(Code.INVALID_ARGUMENT_VALUE).setMessage("Tenant ID is missing").build();
    }

    private Status createStatusNotExits(long id) {
        return Status.newBuilder()
            .setCode(Code.NOT_FOUND_VALUE)
            .setMessage(String.format("Node with id: %s doesn't exist.", id)).build();

    }

    private boolean validateInput(NodeCreateDTO request, String tenantId, StreamObserver<NodeDTO> responseObserver) {
        boolean valid = true;

        if (request.hasManagementIp()) {
            if (!InetAddresses.isInetAddress(request.getManagementIp())) {
                valid = false;
                Status status = Status.newBuilder()
                    .setCode(Code.INVALID_ARGUMENT_VALUE)
                    .setMessage("Bad management_ip: " + request.getManagementIp())
                    .build();
                responseObserver.onError(StatusProto.toStatusRuntimeException(status));
            } else {
                Optional<IpInterfaceDTO> optionalIpInterface = ipInterfaceService.findByIpAddressAndLocationAndTenantId(request.getManagementIp(), request.getLocation(), tenantId);
                if (optionalIpInterface.isPresent()) {
                    valid = false;
                    Status status = Status.newBuilder()
                        .setCode(Code.ALREADY_EXISTS_VALUE)
                        .setMessage("Ip address already exists for location")
                        .build();
                    responseObserver.onError(StatusProto.toStatusRuntimeException(status));
                }
            }
        }

        return valid;
    }

    private void sendTaskSetsToMinion(Node node, String tenantId) {
        Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, tenantId).run(()->
        {
            try {
                taskSetService.sendDetectorTasks(node);
                scannerService.sendNodeScannerTask(List.of(nodeMapper.modelToDTO(node)),
                    node.getMonitoringLocation().getLocation(), node.getTenantId());
            } catch (Exception e) {
                log.error("Error while sending detector task for node with label {}", node.getNodeLabel());
            }
        });
    }

    private void sendTaskSetsToMinion(Map<String, List<NodeDTO>> locationNodes, String tenantId) {
        Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, tenantId).run(()->
        {
            for (String location : locationNodes.keySet()) {
                scannerService.sendNodeScannerTask(locationNodes.get(location), location, tenantId);
            }
        });
    }
}
