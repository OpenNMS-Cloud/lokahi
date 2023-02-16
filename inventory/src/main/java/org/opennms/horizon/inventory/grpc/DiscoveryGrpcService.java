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

import org.opennms.horizon.inventory.dto.DiscoveryRequest;
import org.opennms.horizon.inventory.dto.DiscoveryServiceGrpc;
import org.opennms.horizon.inventory.service.taskset.ScannerTaskSetService;
import org.springframework.stereotype.Component;

import com.google.protobuf.Empty;

import io.grpc.Context;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DiscoveryGrpcService extends DiscoveryServiceGrpc.DiscoveryServiceImplBase {

    private final TenantLookup tenantLookup;
    private final ScannerTaskSetService scannerService;

    @Override
    public void discoverServices(DiscoveryRequest request, StreamObserver<Empty> responseObserver) {
        Optional<String> tenantId = tenantLookup.lookupTenantId(Context.current());
        String location = request.getLocation();
        sendTaskSetsToMinion(request.getIpAddressesList(), tenantId.orElseThrow(), location, request.getRequisitionName());
        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

    private void sendTaskSetsToMinion(List<String> ipAddresses, String tenantId, String location, String requisitionName) {
        scannerService.sendDiscoveryScannerTask(ipAddresses,location, tenantId, requisitionName);
    }
}
