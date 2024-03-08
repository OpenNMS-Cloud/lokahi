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

import com.google.protobuf.BoolValue;
import com.google.protobuf.Empty;
import com.google.protobuf.Int64Value;
import com.google.protobuf.StringValue;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.Context;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.inventory.dto.IdList;
import org.opennms.horizon.inventory.dto.MonitoringLocationCreateDTO;
import org.opennms.horizon.inventory.dto.MonitoringLocationDTO;
import org.opennms.horizon.inventory.dto.MonitoringLocationList;
import org.opennms.horizon.inventory.dto.MonitoringLocationServiceGrpc;
import org.opennms.horizon.inventory.exception.InventoryRuntimeException;
import org.opennms.horizon.inventory.service.ConfigUpdateService;
import org.opennms.horizon.inventory.service.MonitoringLocationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonitoringLocationGrpcService extends MonitoringLocationServiceGrpc.MonitoringLocationServiceImplBase {
    private static final Logger LOG = LoggerFactory.getLogger(MonitoringLocationGrpcService.class);
    private final MonitoringLocationService service;
    private final TenantLookup tenantLookup;
    private final ConfigUpdateService configUpdateService;

    @Override
    public void listLocations(Empty request, StreamObserver<MonitoringLocationList> responseObserver) {
        List<MonitoringLocationDTO> result = tenantLookup
                .lookupTenantId(Context.current())
                .map(service::findByTenantId)
                .orElseThrow();
        responseObserver.onNext(
                MonitoringLocationList.newBuilder().addAllLocations(result).build());
        responseObserver.onCompleted();
    }

    @Override
    public void getLocationByName(StringValue locationName, StreamObserver<MonitoringLocationDTO> responseObserver) {
        Optional<MonitoringLocationDTO> location = tenantLookup
                .lookupTenantId(Context.current())
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
        Optional<MonitoringLocationDTO> location = tenantLookup
                .lookupTenantId(Context.current())
                .map(tenantId -> service.getByIdAndTenantId(request.getValue(), tenantId))
                .orElseThrow();
        if (location.isPresent()) {
            responseObserver.onNext(location.get());
            responseObserver.onCompleted();
        } else {
            Status status = Status.newBuilder()
                    .setCode(Code.NOT_FOUND_VALUE)
                    .setMessage("Location with id: " + request.getValue() + " doesn't exist.")
                    .build();
            responseObserver.onError(StatusProto.toStatusRuntimeException(status));
        }
    }

    @Override
    public void listLocationsByIds(IdList request, StreamObserver<MonitoringLocationList> responseObserver) {
        List<Long> idList =
                request.getIdsList().stream().map(Int64Value::getValue).toList();
        responseObserver.onNext(MonitoringLocationList.newBuilder()
                .addAllLocations(service.findByLocationIds(idList))
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void searchLocations(StringValue request, StreamObserver<MonitoringLocationList> responseObserver) {
        List<MonitoringLocationDTO> locations = tenantLookup
                .lookupTenantId(Context.current())
                .map(tenantId -> service.searchLocationsByTenantId(request.getValue(), tenantId))
                .orElseThrow();
        responseObserver.onNext(
                MonitoringLocationList.newBuilder().addAllLocations(locations).build());
        responseObserver.onCompleted();
    }

    @Override
    public void createLocation(
            MonitoringLocationCreateDTO request, StreamObserver<MonitoringLocationDTO> responseObserver) {
        tenantLookup.lookupTenantId(Context.current()).ifPresent(tenantId -> {
            try {
                responseObserver.onNext(service.upsert(getMonitoringLocationDTO(tenantId, request)));
                responseObserver.onCompleted();
            } catch (Exception e) {
                LOG.error("Error while creating location with name {}", request.getLocation(), e);
                Status status = handleException(e);
                responseObserver.onError(StatusProto.toStatusRuntimeException(status));
            }
        });
    }

    @Override
    public void updateLocation(MonitoringLocationDTO request, StreamObserver<MonitoringLocationDTO> responseObserver) {
        tenantLookup.lookupTenantId(Context.current()).ifPresent(tenantId -> {
            try {
                responseObserver.onNext(service.upsert(MonitoringLocationDTO.newBuilder(request)
                        .setTenantId(tenantId)
                        .build()));
                responseObserver.onCompleted();
            } catch (Exception e) {
                LOG.error("Error while updating location with ID : {}", request.getId(), e);
                Status status = handleException(e);
                responseObserver.onError(StatusProto.toStatusRuntimeException(status));
            }
        });
    }

    @Override
    public void deleteLocation(Int64Value request, StreamObserver<BoolValue> responseObserver) {
        tenantLookup.lookupTenantId(Context.current()).ifPresent(tenantId -> {
            try {
                service.delete(request.getValue(), tenantId);
                configUpdateService.removeConfigsFromTaskSet(tenantId, request.getValue());
                responseObserver.onNext(BoolValue.of(true));
                responseObserver.onCompleted();
            } catch (Exception e) {
                LOG.error("Error while deleting location with ID : {}", request.getValue(), e);
                Status status = handleException(e);
                responseObserver.onError(StatusProto.toStatusRuntimeException(status));
            }
        });
    }

    private static MonitoringLocationDTO getMonitoringLocationDTO(
            String tenantId, MonitoringLocationCreateDTO request) {
        return MonitoringLocationDTO.newBuilder()
                .setLocation(request.getLocation())
                .setAddress(request.getAddress())
                .setGeoLocation(request.getGeoLocation())
                .setTenantId(tenantId)
                .build();
    }

    private Status handleException(Throwable e) {
        if (e instanceof InventoryRuntimeException) {
            return Status.newBuilder()
                    .setCode(Code.INVALID_ARGUMENT_VALUE)
                    .setMessage(e.getMessage())
                    .build();
        } else {
            return Status.newBuilder()
                    .setCode(Code.INTERNAL_VALUE)
                    .setMessage(e.getMessage())
                    .build();
        }
    }
}
