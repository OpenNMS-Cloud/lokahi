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

import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.Context;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.inventory.dto.AzureActiveDiscoveryCreateDTO;
import org.opennms.horizon.inventory.dto.AzureActiveDiscoveryDTO;
import org.opennms.horizon.inventory.dto.AzureActiveDiscoveryServiceGrpc;
import org.opennms.horizon.inventory.exception.InventoryRuntimeException;
import org.opennms.horizon.inventory.grpc.TenantLookup;
import org.opennms.horizon.inventory.service.discovery.active.AzureActiveDiscoveryService;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AzureActiveDiscoveryGrpcService
        extends AzureActiveDiscoveryServiceGrpc.AzureActiveDiscoveryServiceImplBase {
    private final TenantLookup tenantLookup;
    private final AzureActiveDiscoveryService service;

    @Override
    public void createDiscovery(
            AzureActiveDiscoveryCreateDTO request, StreamObserver<AzureActiveDiscoveryDTO> responseObserver) {
        Optional<String> tenantIdOptional = tenantLookup.lookupTenantId(Context.current());

        tenantIdOptional.ifPresentOrElse(
                tenantId -> {
                    try {
                        AzureActiveDiscoveryDTO discovery = service.createActiveDiscovery(tenantId, request);
                        responseObserver.onNext(discovery);
                        responseObserver.onCompleted();
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
}
