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
package org.opennms.horizon.datachoices.grpc;

import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.Context;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.datachoices.dto.DataChoicesServiceGrpc;
import org.opennms.horizon.datachoices.dto.ToggleDataChoicesDTO;
import org.opennms.horizon.datachoices.service.DataChoicesService;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataChoicesGrpcService extends DataChoicesServiceGrpc.DataChoicesServiceImplBase {
    private final TenantLookup tenantLookup;
    private final DataChoicesService service;

    @Override
    public void toggle(ToggleDataChoicesDTO request, StreamObserver<ToggleDataChoicesDTO> responseObserver) {
        Optional<String> tenantIdOptional = tenantLookup.lookupTenantId(Context.current());

        tenantIdOptional.ifPresentOrElse(
                tenantId -> {
                    service.toggle(request, tenantId);
                    responseObserver.onNext(request);
                    responseObserver.onCompleted();
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
