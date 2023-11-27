/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
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
import org.opennms.horizon.inventory.dto.MonitoringSystemDTO;
import org.opennms.horizon.inventory.dto.MonitoringSystemList;
import org.opennms.horizon.inventory.dto.MonitoringSystemQuery;
import org.opennms.horizon.inventory.dto.MonitoringSystemServiceGrpc;
import org.opennms.horizon.inventory.exception.LocationNotFoundException;
import org.opennms.horizon.inventory.service.MonitoringSystemService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonitoringSystemGrpcService extends MonitoringSystemServiceGrpc.MonitoringSystemServiceImplBase {
    private final MonitoringSystemService service;
    private final TenantLookup tenantLookup;
    @Override
    public void listMonitoringSystem(Empty request, StreamObserver<MonitoringSystemList> responseObserver) {
        List<MonitoringSystemDTO> list = tenantLookup.lookupTenantId(Context.current())
            .map(service::findByTenantId)
            .orElseThrow();
        responseObserver.onNext(MonitoringSystemList.newBuilder().addAllSystems(list).build());
        responseObserver.onCompleted();
    }

    @Override
    public void listMonitoringSystemByLocationId(Int64Value locationId, StreamObserver<MonitoringSystemList> responseObserver) {
        try {
            List<MonitoringSystemDTO> list = tenantLookup.lookupTenantId(Context.current())
                .map(tenantId -> service.findByMonitoringLocationIdAndTenantId(locationId.getValue(), tenantId))
                .orElseThrow();
            responseObserver.onNext(MonitoringSystemList.newBuilder().addAllSystems(list).build());
            responseObserver.onCompleted();
        } catch (LocationNotFoundException e) {
            var status = Status.newBuilder()
                .setCode(Code.NOT_FOUND_VALUE)
                .setMessage("Location with id " + locationId.getValue() + " does not exist")
                .build();
            responseObserver.onError(StatusProto.toStatusRuntimeException(status));
        }
    }

    @Override
    public void getMonitoringSystemById(Int64Value id, StreamObserver<MonitoringSystemDTO> responseObserver) {
        Optional<MonitoringSystemDTO> monitoringSystem = tenantLookup.lookupTenantId(Context.current())
            .map(tenantId -> service.findById(id.getValue(), tenantId))
            .orElseThrow();
        monitoringSystem.ifPresentOrElse(
            systemDTO -> {
                responseObserver.onNext(systemDTO);
                responseObserver.onCompleted();
            },
            () -> responseObserver.onError(StatusProto.toStatusRuntimeException(createStatusNotExist(id.getValue())))
        );
    }

    @Override
    public void getMonitoringSystemByQuery(MonitoringSystemQuery request,
                                           StreamObserver<MonitoringSystemDTO> responseObserver)
    {
        var optional = tenantLookup.lookupTenantId(Context.current())
            .map(tenantId -> service.findByLocationAndSystemId(request.getLocation(), request.getSystemId(), tenantId))
            .orElseThrow();
        optional.ifPresentOrElse(systemDTO -> {
            responseObserver.onNext(systemDTO);
            responseObserver.onCompleted();

        }, () -> responseObserver.onError(StatusProto.toStatusRuntimeException(createStatusNotExist(request.getSystemId()))));

    }


    @Override
    public void deleteMonitoringSystem(Int64Value request, StreamObserver<BoolValue> responseObserver) {
        Optional<MonitoringSystemDTO> monitoringSystem = tenantLookup
            .lookupTenantId(Context.current())
            .map(tenantId -> service.findById(request.getValue(), tenantId))
            .orElseThrow();
        monitoringSystem.ifPresentOrElse(system -> {
            try {
                service.deleteMonitoringSystem(system.getId());
                responseObserver.onNext(BoolValue.newBuilder().setValue(true).build());
                responseObserver.onCompleted();
            } catch (Exception e) {
                log.error("Error while deleting monitoring system with systemId {}", system.getSystemId(), e);
                Status status = Status.newBuilder()
                    .setCode(Code.INTERNAL_VALUE)
                    .setMessage("Error while deleting monitoring system with systemId " + system.getSystemId()).build();
                responseObserver.onError(StatusProto.toStatusRuntimeException(status));
            }}, () -> responseObserver.onError(StatusProto.toStatusRuntimeException(createStatusNotExist(request.getValue())))
        );
    }

    private Status createStatusNotExist(String systemId) {
        return Status.newBuilder()
            .setCode(Code.NOT_FOUND_VALUE)
            .setMessage("Monitoring system with system id " + systemId + " does not exist")
            .build();
    }

    private Status createStatusNotExist(long id) {
        return Status.newBuilder()
            .setCode(Code.NOT_FOUND_VALUE)
            .setMessage("Monitoring system with id " + id + " does not exist")
            .build();
    }
}
