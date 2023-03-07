/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
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

import com.google.protobuf.Empty;
import com.google.protobuf.Int64Value;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.Context;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.inventory.discovery.ActiveDiscoveryDTO;
import org.opennms.horizon.inventory.discovery.ActiveDiscoveryList;
import org.opennms.horizon.inventory.discovery.ActiveDiscoveryOperationGrpc;
import org.opennms.horizon.inventory.discovery.ActiveDiscoveryRequest;
import org.opennms.horizon.inventory.service.ActiveDiscoveryService;
import org.opennms.horizon.inventory.service.taskset.ScannerTaskSetService;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActiveDiscoveryGrpcService extends ActiveDiscoveryOperationGrpc.ActiveDiscoveryOperationImplBase {

    private final TenantLookup tenantLookup;
    private final ActiveDiscoveryService configService;
    private final ScannerTaskSetService scannerTaskSetService;

    @Override
    public void createConfig(ActiveDiscoveryRequest request, StreamObserver<ActiveDiscoveryDTO> responseObserver) {
        tenantLookup.lookupTenantId(Context.current())
            .ifPresentOrElse(tenantId -> {
                try {
                    var activeDiscoveryConfig = configService.createConfig(request, tenantId);
                    responseObserver.onNext(activeDiscoveryConfig);
                    responseObserver.onCompleted();
                    scannerTaskSetService.sendDiscoveryScannerTask(request.getIpAddressesList(),
                        request.getLocation(), tenantId, activeDiscoveryConfig.getId());
                } catch (Exception e) {
                    responseObserver.onError(StatusProto.toStatusRuntimeException(createStatus(Code.INVALID_ARGUMENT_VALUE, "Invalid request " + request)));
                }
            }, () -> responseObserver.onError(StatusProto.toStatusRuntimeException(createMissingTenant())));
    }

    @Override
    public void listDiscoveryConfig(Empty request, StreamObserver<ActiveDiscoveryList> responseObserver) {
        tenantLookup.lookupTenantId(Context.current())
            .ifPresentOrElse(tenantId -> {
                List<ActiveDiscoveryDTO> list = configService.listDiscoveryConfigs(tenantId);
                responseObserver.onNext(ActiveDiscoveryList.newBuilder().addAllDiscoverConfigs(list).build());
                responseObserver.onCompleted();
            }, () -> responseObserver.onError(StatusProto.toStatusRuntimeException(createMissingTenant())));
    }

    @Override
    public void getDiscoveryConfigById(Int64Value request, StreamObserver<ActiveDiscoveryDTO> responseObserver) {
        tenantLookup.lookupTenantId(Context.current())
            .ifPresentOrElse(tenantId ->
                    configService.getDiscoveryConfigById(request.getValue(), tenantId)
                        .ifPresentOrElse(config -> {
                            responseObserver.onNext(config);
                            responseObserver.onCompleted();
                        }, () -> responseObserver.onError(StatusProto.toStatusRuntimeException(createStatus(Code.NOT_FOUND_VALUE,
                            "Can't find discovery config for name: " + request.getValue())))),
                () -> responseObserver.onError(StatusProto.toStatusRuntimeException(createMissingTenant())));
    }

    private Status createMissingTenant() {
        return Status.newBuilder().setCode(Code.INVALID_ARGUMENT_VALUE).setMessage("Missing tenantId").build();
    }

    private Status createStatus(int code, String message) {
        return Status.newBuilder().setCode(code).setMessage(message).build();
    }

}
