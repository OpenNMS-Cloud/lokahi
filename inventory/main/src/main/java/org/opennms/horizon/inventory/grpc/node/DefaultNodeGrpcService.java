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

package org.opennms.horizon.inventory.grpc.node;

import com.google.common.net.InetAddresses;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.Context;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.opennms.horizon.inventory.dto.DefaultNodeCreateDTO;
import org.opennms.horizon.inventory.dto.DefaultNodeDTO;
import org.opennms.horizon.inventory.dto.DefaultNodeServiceGrpc;
import org.opennms.horizon.inventory.dto.IpInterfaceDTO;
import org.opennms.horizon.inventory.grpc.TenantLookup;
import org.opennms.horizon.inventory.mapper.node.DefaultNodeMapper;
import org.opennms.horizon.inventory.model.node.DefaultNode;
import org.opennms.horizon.inventory.service.IpInterfaceService;
import org.opennms.horizon.inventory.service.node.DefaultNodeService;
import org.opennms.horizon.inventory.service.taskset.ScannerTaskSetService;
import org.opennms.taskset.contract.ScanType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Component
@RequiredArgsConstructor
public class DefaultNodeGrpcService extends DefaultNodeServiceGrpc.DefaultNodeServiceImplBase {
    private static final String IP_ADDRESS_ALREADY_EXISTS_FOR_LOCATION_MSG = "IP Address already exists for location";
    private final DefaultNodeService service;
    private final IpInterfaceService ipInterfaceService;
    private final DefaultNodeMapper defaultNodeMapper;
    private final TenantLookup tenantLookup;
    private final ScannerTaskSetService scannerService;
    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(DefaultNodeGrpcService.class);

    @Setter
    private Logger LOG = DEFAULT_LOGGER;

    private final ThreadFactory threadFactory = new ThreadFactoryBuilder()
        .setNameFormat("send-taskset-for-node-%d")
        .build();

    // Add setter for unit testing
    @Setter
    private ExecutorService executorService = Executors.newFixedThreadPool(10, threadFactory);

    @Override
    public void createNode(DefaultNodeCreateDTO request, StreamObserver<DefaultNodeDTO> responseObserver) {
        String tenantId = tenantLookup.lookupTenantId(Context.current()).orElseThrow();
        boolean valid = validateInput(request, tenantId, responseObserver);

        if (valid) {
            DefaultNode node = service.createNode(request, ScanType.NODE_SCAN, tenantId);
            responseObserver.onNext(defaultNodeMapper.modelToDto(node));
            responseObserver.onCompleted();

            // Asynchronously send task sets to Minion
            executorService.execute(() -> sendNodeScanTaskToMinion(node));
        }
    }

    private boolean validateInput(DefaultNodeCreateDTO request, String tenantId, StreamObserver<DefaultNodeDTO> responseObserver) {
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
                        .setMessage(IP_ADDRESS_ALREADY_EXISTS_FOR_LOCATION_MSG)
                        .build();
                    responseObserver.onError(StatusProto.toStatusRuntimeException(status));
                }
            }
        }

        return valid;
    }

    private void sendNodeScanTaskToMinion(DefaultNode node) {
        try {
            scannerService.sendNodeScannerTask(List.of(node),
                node.getMonitoringLocation().getLocation(), node.getTenantId());
        } catch (Exception e) {
            LOG.error("Error while sending detector task for node with label {}", node.getNodeLabel(), e);
        }
    }
}
