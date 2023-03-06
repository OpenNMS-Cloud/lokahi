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

import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.Context;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.inventory.dto.AzureCredentialCreateDTO;
import org.opennms.horizon.inventory.dto.AzureCredentialDTO;
import org.opennms.horizon.inventory.dto.AzureCredentialServiceGrpc;
import org.opennms.horizon.inventory.service.AzureCredentialService;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AzureCredentialGrpcService extends AzureCredentialServiceGrpc.AzureCredentialServiceImplBase {
    private final TenantLookup tenantLookup;
    private final AzureCredentialService service;

    @Override
    public void createCredentials(AzureCredentialCreateDTO request, StreamObserver<AzureCredentialDTO> responseObserver) {
        Optional<String> tenantIdOptional = tenantLookup.lookupTenantId(Context.current());

        tenantIdOptional.ifPresentOrElse(tenantId -> {

            try {
                AzureCredentialDTO credentials = service.createCredentials(tenantId, request);

                responseObserver.onNext(credentials);
                responseObserver.onCompleted();

            } catch (Exception e) {

                Status status = Status.newBuilder()
                    .setCode(Code.INTERNAL_VALUE)
                    .setMessage(e.getMessage())
                    .build();
                responseObserver.onError(StatusProto.toStatusRuntimeException(status));
            }
        }, () -> {

            Status status = Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage("Tenant Id can't be empty")
                .build();
            responseObserver.onError(StatusProto.toStatusRuntimeException(status));
         });
    }
}
