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

import com.google.protobuf.BoolValue;
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
import org.opennms.horizon.inventory.dto.IdList;
import org.opennms.horizon.inventory.dto.MonitoringLocationCreateDTO;
import org.opennms.horizon.inventory.dto.MonitoringLocationDTO;
import org.opennms.horizon.inventory.dto.MonitoringLocationList;
import org.opennms.horizon.inventory.dto.MonitoringLocationServiceGrpc;
import org.opennms.horizon.inventory.service.MonitoringLocationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonitoringLocationGrpcService extends MonitoringLocationServiceGrpc.MonitoringLocationServiceImplBase {
    private static final Logger LOG = LoggerFactory.getLogger(MonitoringLocationGrpcService.class);
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
        List<Long> idList = request.getIdsList().stream().map(Int64Value::getValue).toList();
        responseObserver.onNext(MonitoringLocationList.newBuilder().addAllLocations(service.findByLocationIds(idList)).build());
        responseObserver.onCompleted();
    }

    @Override
    public void searchLocations(StringValue request, StreamObserver<MonitoringLocationList> responseObserver) {
        List<MonitoringLocationDTO> locations = tenantLookup.lookupTenantId(Context.current())
            .map(tenantId -> service.searchLocationsByTenantId(request.getValue(), tenantId)).orElseThrow();
        responseObserver.onNext(MonitoringLocationList.newBuilder().addAllLocations(locations).build());
        responseObserver.onCompleted();
    }

    @Override
    public void createLocation(MonitoringLocationCreateDTO request, StreamObserver<MonitoringLocationDTO> responseObserver) {
        tenantLookup.lookupTenantId(Context.current())
            .ifPresent(tenantId -> {
                try {
                    responseObserver.onNext(service.upsert(getMonitoringLocationDTO(tenantId, request)));
                    responseObserver.onCompleted();
                } catch (Exception e) {
                    LOG.error("Error while creating location with name {}", request.getLocation(), e);
                    Status status = Status.newBuilder()
                        .setCode(Code.INTERNAL_VALUE)
                        .setMessage("Error while creating location with name " + request.getLocation()).build();
                    responseObserver.onError(StatusProto.toStatusRuntimeException(status));
                }
            });
    }

    @Override
    public void updateLocation(MonitoringLocationDTO request, StreamObserver<MonitoringLocationDTO> responseObserver) {
        tenantLookup.lookupTenantId(Context.current())
            .ifPresent(tenantId -> {
                try {
                    responseObserver.onNext(service.upsert(MonitoringLocationDTO.newBuilder(request).setTenantId(tenantId).build()));
                    responseObserver.onCompleted();
                } catch (Exception e) {
                    LOG.error("Error while updating location with ID {}", request.getId(), e);
                    Status status = Status.newBuilder()
                        .setCode(Code.INTERNAL_VALUE)
                        .setMessage("Error while updating location with ID " + request.getId()).build();
                    responseObserver.onError(StatusProto.toStatusRuntimeException(status));
                }
            });
    }

    @Override
    public void deleteLocation(Int64Value request, StreamObserver<BoolValue> responseObserver) {
        tenantLookup.lookupTenantId(Context.current())
            .ifPresent(tenantId -> {
                try {
                    service.delete(request.getValue(), tenantId);
                    responseObserver.onNext(BoolValue.of(true));
                    responseObserver.onCompleted();
                } catch (Exception e) {
                    LOG.error("Error while deleting location with ID {}", request.getValue(), e);
                    Status status = Status.newBuilder()
                        .setCode(Code.INTERNAL_VALUE)
                        .setMessage("Error while deleting location with ID " + request.getValue()).build();
                    responseObserver.onError(StatusProto.toStatusRuntimeException(status));
                }
            });
    }

    private static MonitoringLocationDTO getMonitoringLocationDTO(String tenantId, MonitoringLocationCreateDTO request) {
        return MonitoringLocationDTO.newBuilder()
            .setLocation(request.getLocation())
            .setAddress(request.getAddress())
            .setGeoLocation(request.getGeoLocation())
            .setTenantId(tenantId).build();
    }
}

