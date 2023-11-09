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

import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.Context;
import io.grpc.protobuf.StatusProto;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.inventory.dto.MonitoredServiceGrpc;
import org.opennms.horizon.inventory.service.MonitoredServiceService;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class MonitoredServiceGrpcService extends MonitoredServiceGrpc.MonitoredServiceImplBase {

    private final MonitoredServiceService monitoredServiceService;
    private final TenantLookup tenantLookup;

    @Override
    public void getMonitoredService(org.opennms.horizon.inventory.dto.MonitoredServiceQuery request,
                                    io.grpc.stub.StreamObserver<org.opennms.horizon.inventory.dto.MonitoredServiceDTO> responseObserver) {

        String tenantId = tenantLookup.lookupTenantId(Context.current()).orElseThrow();
        var optionalService = monitoredServiceService.findMonitoredService(tenantId,
            request.getIpAddress(), request.getMonitoredServiceType(), request.getNodeId());
        if (optionalService.isPresent()) {
            var monitoredService = optionalService.get();
            responseObserver.onNext(monitoredService);
            responseObserver.onCompleted();
        } else {
            Status status = Status.newBuilder()
                .setCode(Code.NOT_FOUND_VALUE)
                .setMessage("Monitor service not found with the given request")
                .build();
            responseObserver.onError(StatusProto.toStatusRuntimeException(status));
        }

    }
}
