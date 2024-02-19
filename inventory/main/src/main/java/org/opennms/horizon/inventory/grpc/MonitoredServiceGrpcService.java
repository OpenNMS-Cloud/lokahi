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
    public void getMonitoredService(
            org.opennms.horizon.inventory.dto.MonitoredServiceQuery request,
            io.grpc.stub.StreamObserver<org.opennms.horizon.inventory.dto.MonitoredServiceDTO> responseObserver) {

        String tenantId = tenantLookup.lookupTenantId(Context.current()).orElseThrow();
        var optionalService = monitoredServiceService.findMonitoredService(
                tenantId, request.getIpAddress(), request.getMonitoredServiceType(), request.getNodeId());
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
