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
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.Context;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.inventory.dto.AddressCreateDTO;
import org.opennms.horizon.inventory.dto.AddressDTO;
import org.opennms.horizon.inventory.dto.AddressList;
import org.opennms.horizon.inventory.dto.AddressServiceGrpc;
import org.opennms.horizon.inventory.dto.AddressUpdateDTO;
import org.opennms.horizon.inventory.service.AddressService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AddressGrpcService extends AddressServiceGrpc.AddressServiceImplBase {
    private static final Logger LOG = LoggerFactory.getLogger(AddressGrpcService.class);
    public static final String ERROR_WHILE_DELETING_ADDRESS_WITH_ID = "Error while deleting address with ID ";
    public static final String ERROR_WHILE_UPDATING_ADDRESS_WITH_ID = "Error while updating address with ID ";
    private final AddressService service;
    private final TenantLookup tenantLookup;

    @Override
    public void listAddresses(Empty request, StreamObserver<AddressList> responseObserver) {
        List<AddressDTO> result = service.findAll();
        responseObserver.onNext(AddressList.newBuilder().addAllAddresses(result).build());
        responseObserver.onCompleted();
    }

    @Override
    public void getAddressById(Int64Value request, StreamObserver<AddressDTO> responseObserver) {
        Optional<AddressDTO> address = tenantLookup.lookupTenantId(Context.current())
            .map(tenantId -> service.findById(request.getValue())).orElseThrow();
        if (address.isPresent()) {
            responseObserver.onNext(address.get());
            responseObserver.onCompleted();
        } else {
            Status status = Status.newBuilder()
                .setCode(Code.NOT_FOUND_VALUE)
                .setMessage("Location with id: " + request.getValue() + " doesn't exist.").build();
            responseObserver.onError(StatusProto.toStatusRuntimeException(status));
        }
    }

    @Override
    public void createAddress(AddressCreateDTO request, StreamObserver<AddressDTO> responseObserver) {
        try {
            responseObserver.onNext(service.create(getAddressDTO(request)));
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOG.error("Error while creating address {}", request, e);
            Status status = Status.newBuilder()
                .setCode(Code.INTERNAL_VALUE)
                .setMessage("Error while creating address " + request).build();
            responseObserver.onError(StatusProto.toStatusRuntimeException(status));
        }
    }

    @Override
    public void updateAddress(AddressUpdateDTO request, StreamObserver<AddressDTO> responseObserver) {
        try {
            responseObserver.onNext(service.update(getAddressDTO(request)));
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOG.error(ERROR_WHILE_UPDATING_ADDRESS_WITH_ID + "{}", request.getId(), e);
            Status status = Status.newBuilder()
                .setCode(Code.INTERNAL_VALUE)
                .setMessage(ERROR_WHILE_UPDATING_ADDRESS_WITH_ID + request.getId()).build();
            responseObserver.onError(StatusProto.toStatusRuntimeException(status));
        }
    }

    @Override
    public void deleteAddress(Int64Value request, StreamObserver<BoolValue> responseObserver) {
        try {
            service.delete(request.getValue());
            responseObserver.onNext(BoolValue.of(true));
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOG.error(ERROR_WHILE_DELETING_ADDRESS_WITH_ID + "{}", request.getValue(), e);
            Status status = Status.newBuilder()
                .setCode(Code.INTERNAL_VALUE)
                .setMessage(ERROR_WHILE_DELETING_ADDRESS_WITH_ID + request.getValue()).build();
            responseObserver.onError(StatusProto.toStatusRuntimeException(status));
        }
    }

    private static AddressDTO getAddressDTO(AddressCreateDTO request) {
        return AddressDTO.newBuilder()
            .setAddressLine1(request.getAddressLine1())
            .setAddressLine2(request.getAddressLine2())
            .setCity(request.getCity())
            .setCountry(request.getCountry())
            .setPostalCode(request.getPostalCode())
            .setState(request.getState())
            .build();
    }

    private AddressDTO getAddressDTO(AddressUpdateDTO request) {
        return AddressDTO.newBuilder()
            .setId(request.getId())
            .setAddressLine1(request.getAddressLine1())
            .setAddressLine2(request.getAddressLine2())
            .setCity(request.getCity())
            .setCountry(request.getCountry())
            .setPostalCode(request.getPostalCode())
            .setState(request.getState())
            .build();
    }
}

