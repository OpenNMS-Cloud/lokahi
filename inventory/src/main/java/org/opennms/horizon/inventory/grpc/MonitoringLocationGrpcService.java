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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.opennms.horizon.inventory.dto.IdList;
import org.opennms.horizon.inventory.dto.MonitoringLocationDTO;
import org.opennms.horizon.inventory.dto.MonitoringLocationList;
import org.opennms.horizon.inventory.dto.MonitoringLocationServiceGrpc;
import org.opennms.horizon.inventory.service.MonitoringLocationService;

import com.google.protobuf.Empty;
import com.google.protobuf.Int64Value;
import com.google.protobuf.StringValue;
import com.google.rpc.Code;
import com.google.rpc.Status;

import io.grpc.Context;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonitoringLocationGrpcService extends MonitoringLocationServiceGrpc.MonitoringLocationServiceImplBase {
    private final MonitoringLocationService service;
    private final TenantLookup tenantLookup;

    @Override
    public void listLocations(Empty request, StreamObserver<MonitoringLocationList> responseObserver) {
        List<MonitoringLocationDTO> result = tenantLookup.lookupTenantId(Context.current())
            .map(service::findByTenantId)
            .orElseThrow();
        responseObserver.onNext(MonitoringLocationList.newBuilder().addAllLocations(result).build());
        responseObserver.onCompleted();
    }

    @Override
    public void getLocationByName(StringValue locationName, StreamObserver<MonitoringLocationDTO> responseObserver) {
        Optional<MonitoringLocationDTO> location = tenantLookup.lookupTenantId(Context.current())
            .map(tenantId -> service.findByLocationAndTenantId(locationName.getValue(), tenantId))
            .orElseThrow();
        if (location.isPresent()) {
            responseObserver.onNext(location.get());
            responseObserver.onCompleted();
        } else {
            Status status = Status.newBuilder()
                .setCode(Code.NOT_FOUND_VALUE)
                .setMessage("Location with name: " + locationName.getValue() + " doesn't exist")
                .build();
            responseObserver.onError(StatusProto.toStatusRuntimeException(status));
        }
    }

    @Override
    public void getLocationById(Int64Value request, StreamObserver<MonitoringLocationDTO> responseObserver) {
        Optional<MonitoringLocationDTO> location = tenantLookup.lookupTenantId(Context.current())
            .map(tenantId -> service.getByIdAndTenantId(request.getValue(), tenantId)).orElseThrow();
        if (location.isPresent()) {
            responseObserver.onNext(location.get());
            responseObserver.onCompleted();
        } else {
            Status status = Status.newBuilder()
                .setCode(Code.NOT_FOUND_VALUE)
                .setMessage("Location with id: " + request.getValue() + " doesn't exist.").build();
            responseObserver.onError(StatusProto.toStatusRuntimeException(status));
        }
    }

    @Override
    public void listLocationsByIds(IdList request, StreamObserver<MonitoringLocationList> responseObserver) {
        List<Long> idList = request.getIdsList().stream().map(Int64Value::getValue).collect(Collectors.toList());
        responseObserver.onNext(MonitoringLocationList.newBuilder().addAllLocations(service.findByLocationIds(idList)).build());
        responseObserver.onCompleted();
    }
}

