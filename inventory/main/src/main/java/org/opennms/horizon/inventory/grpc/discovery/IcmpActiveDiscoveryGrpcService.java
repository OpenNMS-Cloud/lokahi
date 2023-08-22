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

package org.opennms.horizon.inventory.grpc.discovery;

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
import org.opennms.horizon.inventory.discovery.IcmpActiveDiscoveryCreateDTO;
import org.opennms.horizon.inventory.discovery.IcmpActiveDiscoveryDTO;
import org.opennms.horizon.inventory.discovery.IcmpActiveDiscoveryList;
import org.opennms.horizon.inventory.discovery.IcmpActiveDiscoveryServiceGrpc;
import org.opennms.horizon.inventory.grpc.TenantLookup;
import org.opennms.horizon.inventory.service.discovery.active.IcmpActiveDiscoveryService;
import org.opennms.horizon.inventory.service.taskset.ScannerTaskSetService;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class IcmpActiveDiscoveryGrpcService extends IcmpActiveDiscoveryServiceGrpc.IcmpActiveDiscoveryServiceImplBase {
    private final TenantLookup tenantLookup;
    private final IcmpActiveDiscoveryService discoveryService;
    private final ScannerTaskSetService scannerTaskSetService;

    @Override
    public void createDiscovery(IcmpActiveDiscoveryCreateDTO request, StreamObserver<IcmpActiveDiscoveryDTO> responseObserver) {
        tenantLookup.lookupTenantId(Context.current())
            .ifPresentOrElse(tenantId -> {
                try {
                    var activeDiscoveryConfig = discoveryService.createActiveDiscovery(request, tenantId);
                    responseObserver.onNext(activeDiscoveryConfig);
                    responseObserver.onCompleted();
                    scannerTaskSetService.sendDiscoveryScannerTask(request.getIpAddressesList(), Long.valueOf(request.getLocationId()), tenantId, activeDiscoveryConfig.getId());
                } catch (Exception e) {
                    log.error("failed to create ICMP active discovery", e);
                    responseObserver.onError(StatusProto.toStatusRuntimeException(createStatus(Code.INVALID_ARGUMENT_VALUE, "Invalid request " + request)));
                }
            }, () -> responseObserver.onError(StatusProto.toStatusRuntimeException(createMissingTenant())));
    }

    @Override
    public void listDiscoveries(Empty request, StreamObserver<IcmpActiveDiscoveryList> responseObserver) {
        tenantLookup.lookupTenantId(Context.current())
            .ifPresentOrElse(tenantId -> {
                List<IcmpActiveDiscoveryDTO> list = discoveryService.getActiveDiscoveries(tenantId);
                responseObserver.onNext(IcmpActiveDiscoveryList.newBuilder().addAllDiscoveries(list).build());
                responseObserver.onCompleted();
            }, () -> responseObserver.onError(StatusProto.toStatusRuntimeException(createMissingTenant())));
    }

    @Override
    public void getDiscoveryById(Int64Value request, StreamObserver<IcmpActiveDiscoveryDTO> responseObserver) {
        tenantLookup.lookupTenantId(Context.current())
            .ifPresentOrElse(tenantId ->
                    discoveryService.getDiscoveryById(request.getValue(), tenantId)
                        .ifPresentOrElse(config -> {
                            responseObserver.onNext(config);
                            responseObserver.onCompleted();
                        }, () -> responseObserver.onError(StatusProto.toStatusRuntimeException(createStatus(Code.NOT_FOUND_VALUE,
                            "Can't find discovery config for name: " + request.getValue())))),
                () -> responseObserver.onError(StatusProto.toStatusRuntimeException(createMissingTenant())));
    }

    @Override
    public void upsertActiveDiscovery(IcmpActiveDiscoveryCreateDTO request,
                                StreamObserver<IcmpActiveDiscoveryDTO> responseObserver) {
        var tenant = tenantLookup.lookupTenantId(Context.current());
        if (tenant.isPresent()) {
            var activeDiscovery = discoveryService.getDiscoveryById(request.getId(), tenant.get());
            if (activeDiscovery.isEmpty()) {
                throw new IllegalArgumentException("Discovery with Id " + request.getId() + " doesn't exist");
            }
            var icmpDiscovery = activeDiscovery.get();
            // Discovery task need to be run always whenever there is an update, so first we need to remove current task
            scannerTaskSetService.removeDiscoveryScanTask(Long.parseLong(icmpDiscovery.getLocationId()), icmpDiscovery.getId(), tenant.get());
            var activeDiscoveryConfig = discoveryService.upsertActiveDiscovery(request, tenant.get());
            scannerTaskSetService.sendDiscoveryScannerTask(request.getIpAddressesList(),
                Long.valueOf(request.getLocationId()), tenant.get(), activeDiscoveryConfig.getId());
            responseObserver.onNext(activeDiscoveryConfig);
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(StatusProto.toStatusRuntimeException(createMissingTenant()));
        }
    }

    @Override
    public void deleteActiveDiscovery(com.google.protobuf.Int64Value request,
                                io.grpc.stub.StreamObserver<com.google.protobuf.BoolValue> responseObserver) {

        var tenant = tenantLookup.lookupTenantId(Context.current());
        if (tenant.isPresent()) {
            var activeDiscovery = discoveryService.getDiscoveryById(request.getValue(), tenant.get());
            if (activeDiscovery.isPresent()) {
                var icmpDiscovery = activeDiscovery.get();
                var result = discoveryService.deleteActiveDiscovery(request.getValue(), tenant.get());
                scannerTaskSetService.removeDiscoveryScanTask(Long.parseLong(icmpDiscovery.getLocationId()), icmpDiscovery.getId(), tenant.get());
                responseObserver.onNext(BoolValue.of(result));
                responseObserver.onCompleted();
            } else  {
                responseObserver.onError(StatusProto.toStatusRuntimeException(createInvalidDiscovery()));
            }
        } else {
            responseObserver.onError(StatusProto.toStatusRuntimeException(createMissingTenant()));
        }
    }

    private Status createMissingTenant() {
        return Status.newBuilder().setCode(Code.INVALID_ARGUMENT_VALUE).setMessage("Missing tenantId").build();
    }

    private Status createInvalidDiscovery() {
        return Status.newBuilder().setCode(Code.INVALID_ARGUMENT_VALUE).setMessage("Invalid discovery Id").build();
    }

    private Status createStatus(int code, String message) {
        return Status.newBuilder().setCode(code).setMessage(message).build();
    }
}
