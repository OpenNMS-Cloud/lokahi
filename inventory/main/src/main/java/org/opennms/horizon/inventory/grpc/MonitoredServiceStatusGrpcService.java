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

import com.google.protobuf.Int64Value;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.Context;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.inventory.dto.MonitorStatusServiceGrpc;
import org.opennms.horizon.inventory.dto.MonitoredServiceQuery;
import org.opennms.horizon.inventory.dto.MonitoredServiceStatusDTO;
import org.opennms.horizon.inventory.service.MonitoredServiceService;
import org.opennms.horizon.inventory.service.MonitoredStatusService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MonitoredServiceStatusGrpcService extends MonitorStatusServiceGrpc.MonitorStatusServiceImplBase {

    private final MonitoredStatusService monitorStatusService;
    private final MonitoredServiceService monitoredServiceService;
    private final TenantLookup tenantLookup;

    @Override
    public void getServiceStatus(Int64Value request, StreamObserver<MonitoredServiceStatusDTO> responseObserver) {
        String tenantId = tenantLookup.lookupTenantId(Context.current()).orElseThrow();
        var optional = monitorStatusService.getServiceStatus(tenantId, request.getValue());
        if (optional.isPresent()) {
            var monitorStatus = optional.get();
            responseObserver.onNext(monitorStatus);
            responseObserver.onCompleted();
        } else {
            Status status = Status.newBuilder()
                    .setCode(Code.NOT_FOUND_VALUE)
                    .setMessage("Invalid monitor service id")
                    .build();
            responseObserver.onError(StatusProto.toStatusRuntimeException(status));
        }
    }

    @Override
    public void getMonitoredServiceStatus(
            MonitoredServiceQuery request, StreamObserver<MonitoredServiceStatusDTO> responseObserver) {
        String tenantId = tenantLookup.lookupTenantId(Context.current()).orElseThrow();
        var optionalService = monitoredServiceService.findMonitoredService(
                tenantId, request.getIpAddress(), request.getMonitoredServiceType(), request.getNodeId());
        if (optionalService.isPresent()) {
            var monitoredService = optionalService.get();
            var monitorStatus = monitorStatusService.getServiceStatus(tenantId, monitoredService.getId());
            if (monitorStatus.isPresent()) {
                responseObserver.onNext(monitorStatus.get());
                responseObserver.onCompleted();
            } else {
                Status status = Status.newBuilder()
                        .setCode(Code.NOT_FOUND_VALUE)
                        .setMessage("Invalid monitor service id")
                        .build();
                responseObserver.onError(StatusProto.toStatusRuntimeException(status));
            }
        } else {
            Status status = Status.newBuilder()
                    .setCode(Code.NOT_FOUND_VALUE)
                    .setMessage("Monitor service not found with the given request")
                    .build();
            responseObserver.onError(StatusProto.toStatusRuntimeException(status));
        }
    }
}
