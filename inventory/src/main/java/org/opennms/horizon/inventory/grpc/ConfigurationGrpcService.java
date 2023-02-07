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

import org.opennms.horizon.inventory.dto.ConfigurationDTO;
import org.opennms.horizon.inventory.dto.ConfigurationList;
import org.opennms.horizon.inventory.dto.ConfigurationServiceGrpc;
import org.opennms.horizon.inventory.service.ConfigurationService;
import org.springframework.stereotype.Component;

import com.google.protobuf.Empty;
import com.google.protobuf.StringValue;
import com.google.rpc.Code;
import com.google.rpc.Status;

import io.grpc.Context;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConfigurationGrpcService extends ConfigurationServiceGrpc.ConfigurationServiceImplBase {
    private final ConfigurationService service;
    private final TenantLookup tenantLookup;

    @Override
    public void listConfigurations(Empty request, StreamObserver<ConfigurationList> responseObserver) {
        List<ConfigurationDTO> result = tenantLookup.lookupTenantId(Context.current())
            .map(service::findByTenantId)
            .orElseThrow();
        responseObserver.onNext(ConfigurationList.newBuilder().addAllConfigurations(result).build());
        responseObserver.onCompleted();
    }

    @Override
    public void getConfigurationsByKey(StringValue key, StreamObserver<ConfigurationDTO> responseObserver) {
        Optional<ConfigurationDTO> configurationDTO = tenantLookup.lookupTenantId(Context.current())
            .map(tenantId -> service.findByKey(tenantId, key.getValue()))
            .orElseThrow();
        if (configurationDTO.isPresent()) {
            responseObserver.onNext(configurationDTO.get());
            responseObserver.onCompleted();
        } else {
            Status status = Status.newBuilder()
                .setCode(Code.NOT_FOUND_VALUE)
                .setMessage("Configuration with key: " + key.getValue() + " doesn't exist")
                .build();
            responseObserver.onError(StatusProto.toStatusRuntimeException(status));
        }
    }

    @Override
    public void listConfigurationsByLocation(StringValue location, StreamObserver<ConfigurationList> responseObserver) {
        List<ConfigurationDTO> configurationDTOS = tenantLookup.lookupTenantId(Context.current())
            .map(tenantId -> service.findByLocation(tenantId, location.getValue()))
            .orElseThrow();
        if (!configurationDTOS.isEmpty()) {
            responseObserver.onNext(ConfigurationList.newBuilder().addAllConfigurations(configurationDTOS).build());
            responseObserver.onCompleted();
        } else {
            Status status = Status.newBuilder()
                .setCode(Code.NOT_FOUND_VALUE)
                .setMessage("Configuration with location: " + location.getValue() + " doesn't exist")
                .build();
            responseObserver.onError(StatusProto.toStatusRuntimeException(status));
        }
    }
}

