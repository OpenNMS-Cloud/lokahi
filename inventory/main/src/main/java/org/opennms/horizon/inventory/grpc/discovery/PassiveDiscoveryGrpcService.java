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
package org.opennms.horizon.inventory.grpc.discovery;

import com.google.protobuf.BoolValue;
import com.google.protobuf.Empty;
import com.google.protobuf.Int64Value;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.Context;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.inventory.dto.PassiveDiscoveryDTO;
import org.opennms.horizon.inventory.dto.PassiveDiscoveryListDTO;
import org.opennms.horizon.inventory.dto.PassiveDiscoveryServiceGrpc;
import org.opennms.horizon.inventory.dto.PassiveDiscoveryToggleDTO;
import org.opennms.horizon.inventory.dto.PassiveDiscoveryUpsertDTO;
import org.opennms.horizon.inventory.exception.InventoryRuntimeException;
import org.opennms.horizon.inventory.exception.LocationNotFoundException;
import org.opennms.horizon.inventory.grpc.TenantLookup;
import org.opennms.horizon.inventory.service.discovery.PassiveDiscoveryService;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PassiveDiscoveryGrpcService extends PassiveDiscoveryServiceGrpc.PassiveDiscoveryServiceImplBase {
    private final TenantLookup tenantLookup;
    private final PassiveDiscoveryService service;

    @Override
    public void upsertDiscovery(
            PassiveDiscoveryUpsertDTO request, StreamObserver<PassiveDiscoveryDTO> responseObserver) {
        Optional<String> tenantIdOptional = tenantLookup.lookupTenantId(Context.current());

        tenantIdOptional.ifPresentOrElse(
                tenantId -> {
                    try {
                        PassiveDiscoveryDTO response;
                        if (request.hasId()) {
                            response = service.updateDiscovery(tenantId, request);
                        } else {
                            response = service.createDiscovery(tenantId, request);
                        }

                        responseObserver.onNext(response);
                        responseObserver.onCompleted();

                    } catch (LocationNotFoundException e) {
                        Status status = Status.newBuilder()
                                .setCode(Code.NOT_FOUND_VALUE)
                                .setMessage(e.getMessage())
                                .build();
                        responseObserver.onError(StatusProto.toStatusRuntimeException(status));
                    } catch (InventoryRuntimeException e) {
                        Status status = Status.newBuilder()
                                .setCode(Code.INVALID_ARGUMENT_VALUE)
                                .setMessage(e.getMessage())
                                .build();
                        responseObserver.onError(StatusProto.toStatusRuntimeException(status));
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
                            .setMessage("Tenant Id can't be empty")
                            .build();
                    responseObserver.onError(StatusProto.toStatusRuntimeException(status));
                });
    }

    @Override
    public void listAllDiscoveries(Empty request, StreamObserver<PassiveDiscoveryListDTO> responseObserver) {
        Optional<String> tenantIdOptional = tenantLookup.lookupTenantId(Context.current());

        tenantIdOptional.ifPresentOrElse(
                tenantId -> {
                    try {
                        List<PassiveDiscoveryDTO> discoveries = service.getPassiveDiscoveries(tenantId);
                        responseObserver.onNext(PassiveDiscoveryListDTO.newBuilder()
                                .addAllDiscoveries(discoveries)
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
                            .setMessage("Tenant Id can't be empty")
                            .build();
                    responseObserver.onError(StatusProto.toStatusRuntimeException(status));
                });
    }

    @Override
    public void toggleDiscovery(
            PassiveDiscoveryToggleDTO request, StreamObserver<PassiveDiscoveryDTO> responseObserver) {
        Optional<String> tenantIdOptional = tenantLookup.lookupTenantId(Context.current());

        tenantIdOptional.ifPresentOrElse(
                tenantId -> {
                    try {
                        PassiveDiscoveryDTO response = service.toggleDiscovery(tenantId, request);
                        responseObserver.onNext(response);
                        responseObserver.onCompleted();
                    } catch (Exception e) {
                        Status status = Status.newBuilder()
                                .setCode(Code.NOT_FOUND_VALUE)
                                .setMessage(e.getMessage())
                                .build();
                        responseObserver.onError(StatusProto.toStatusRuntimeException(status));
                    }
                },
                () -> {
                    Status status = Status.newBuilder()
                            .setCode(Code.INVALID_ARGUMENT_VALUE)
                            .setMessage("Tenant Id can't be empty")
                            .build();
                    responseObserver.onError(StatusProto.toStatusRuntimeException(status));
                });
    }

    @Override
    public void deleteDiscovery(Int64Value request, StreamObserver<BoolValue> responseObserver) {
        Optional<String> tenantIdOptional = tenantLookup.lookupTenantId(Context.current());

        tenantIdOptional.ifPresentOrElse(
                tenantId -> {
                    try {
                        service.deleteDiscovery(tenantId, request.getValue());
                        responseObserver.onNext(BoolValue.of(true));
                        responseObserver.onCompleted();
                    } catch (InventoryRuntimeException e) {
                        Status status = Status.newBuilder()
                                .setCode(Code.NOT_FOUND_VALUE)
                                .setMessage(e.getMessage())
                                .build();
                        responseObserver.onError(StatusProto.toStatusRuntimeException(status));
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
                            .setMessage("Tenant Id can't be empty")
                            .build();
                    responseObserver.onError(StatusProto.toStatusRuntimeException(status));
                });
    }
}
