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
import io.grpc.Context;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.AllArgsConstructor;
import org.opennms.horizon.inventory.dto.SyntheticTestCreateDTO;
import org.opennms.horizon.inventory.dto.SyntheticTestDTO;
import org.opennms.horizon.inventory.dto.SyntheticTestLocationConfigurationDTO;
import org.opennms.horizon.inventory.dto.SyntheticTestStatusDTO;
import org.opennms.horizon.inventory.dto.SyntheticTestStatusMapDTO;
import org.opennms.horizon.inventory.dto.SyntheticTestStatusMapDTO.Builder;
import org.opennms.horizon.inventory.dto.SyntheticTransactionCreateDTO;
import org.opennms.horizon.inventory.dto.SyntheticTransactionDTO;
import org.opennms.horizon.inventory.dto.SyntheticTransactionDTOArray;
import org.opennms.horizon.inventory.dto.SyntheticTransactionRequestDTO;
import org.opennms.horizon.inventory.dto.SyntheticTransactionServiceGrpc.SyntheticTransactionServiceImplBase;
import org.opennms.horizon.inventory.dto.SyntheticTransactionTestDTOArray;
import org.opennms.horizon.inventory.dto.SyntheticTransactionTestLocationRequestDTO;
import org.opennms.horizon.inventory.dto.SyntheticTransactionTestRequestDTO;
import org.opennms.horizon.inventory.dto.TenantedId;
import org.opennms.horizon.inventory.service.SyntheticTransactionService;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class SyntheticTransactionGrpcService extends SyntheticTransactionServiceImplBase {

    private final TenantLookup tenantLookup;

    private final SyntheticTransactionService syntheticTransactionService;

    @Override
    public void createSyntheticTransaction(SyntheticTransactionCreateDTO request, StreamObserver<SyntheticTransactionDTO> responseObserver) {
        Optional<String> tenantId = tenantLookup.lookupTenantId(Context.current());
        if (tenantId.isEmpty()) {
            responseObserver.onError(Status.PERMISSION_DENIED.asException());
            return;
        }

        SyntheticTransactionCreateDTO syntheticTransactionDTO = request.toBuilder().setTenantId(tenantId.get()).build();
        responseObserver.onNext(syntheticTransactionService.store(syntheticTransactionDTO));
        responseObserver.onCompleted();
    }

    @Override
    public void getSyntheticTransactions(Empty request, StreamObserver<SyntheticTransactionDTOArray> responseObserver) {
        Optional<String> tenantId = tenantLookup.lookupTenantId(Context.current());
        if (tenantId.isEmpty()) {
            responseObserver.onError(Status.PERMISSION_DENIED.asException());
            return;
        }

        responseObserver.onNext(SyntheticTransactionDTOArray.newBuilder()
            .addAllTransactions(syntheticTransactionService.getSyntheticTransactionsByTenantId(tenantId.get()))
            .build());
        responseObserver.onCompleted();
    }

    @Override
    public void deleteSyntheticTransaction(SyntheticTransactionRequestDTO request, StreamObserver<Empty> responseObserver) {
        Optional<String> tenantId = tenantLookup.lookupTenantId(Context.current());
        if (tenantId.isEmpty()) {
            responseObserver.onError(Status.PERMISSION_DENIED.asException());
            return;
        }

        syntheticTransactionService.deleteTransaction(tenantId.get(), request.getId().getId());
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void createSyntheticTransactionTest(SyntheticTestCreateDTO request, StreamObserver<SyntheticTestDTO> responseObserver) {
        Optional<String> tenantId = tenantLookup.lookupTenantId(Context.current());
        if (tenantId.isEmpty()) {
            responseObserver.onError(Status.PERMISSION_DENIED.asException());
            return;
        }

        TenantedId transactionId = request.getSyntheticTransactionId();
        Optional<SyntheticTransactionDTO> transaction = syntheticTransactionService.getSyntheticTransaction(transactionId);
        if (transaction.isEmpty()) {
            responseObserver.onError(Status.INVALID_ARGUMENT.asException());
            return;
        }

        SyntheticTestDTO output = syntheticTransactionService.storeTest(request);
        responseObserver.onNext(output);
        responseObserver.onCompleted();
    }

    @Override
    public void getSyntheticTransactionTests(SyntheticTransactionRequestDTO request, StreamObserver<SyntheticTransactionTestDTOArray> responseObserver) {
        Optional<String> tenantId = tenantLookup.lookupTenantId(Context.current());
        if (tenantId.isEmpty()) {
            responseObserver.onError(Status.PERMISSION_DENIED.asException());
            return;
        }
        responseObserver.onNext(SyntheticTransactionTestDTOArray.newBuilder()
            .addAllTests(
                syntheticTransactionService.getSyntheticTransactionsTests(request.getId())
            ).build()
        );
        responseObserver.onCompleted();
    }

    @Override
    public void deleteSyntheticTransactionTest(SyntheticTransactionTestRequestDTO request, StreamObserver<Empty> responseObserver) {
        Optional<String> tenantId = tenantLookup.lookupTenantId(Context.current());
        if (tenantId.isEmpty()) {
            responseObserver.onError(Status.PERMISSION_DENIED.asException());
            return;
        }

        syntheticTransactionService.deleteTransactionTest(tenantId.get(), request.getId().getId());
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void executeSyntheticTest(SyntheticTransactionTestRequestDTO request, StreamObserver<SyntheticTestStatusMapDTO> responseObserver) {
        syntheticTransactionService.executeTestInLocationsMatching(request).whenComplete((response, error) -> {
            if (error != null) {
                responseObserver.onError(error);
                return;
            }

            Builder builder = SyntheticTestStatusMapDTO.newBuilder();
            response.forEach(builder::putStatusMap);
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        });
    }

    @Override
    public void executeSyntheticTestInLocation(SyntheticTransactionTestLocationRequestDTO request, StreamObserver<SyntheticTestStatusDTO> responseObserver) {
        call(syntheticTransactionService.executeLocation(request), responseObserver);
    }

    @Override
    public void verifySyntheticTestConfiguration(SyntheticTestLocationConfigurationDTO request, StreamObserver<SyntheticTestStatusDTO> responseObserver) {
        String tenantId = tenantLookup.lookupTenantId(Context.current())
            .orElseThrow(() -> new IllegalArgumentException("Missing tenant information"));

        call(syntheticTransactionService.executeConfiguration(tenantId, request), responseObserver);
    }

    public void call(CompletableFuture<SyntheticTestStatusDTO> completableFuture, StreamObserver<SyntheticTestStatusDTO> responseObserver) {
        completableFuture.whenComplete((response, error) -> {
            if (error != null) {
                responseObserver.onError(error);
                return;
            }
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        });
    }
}
