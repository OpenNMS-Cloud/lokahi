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
package org.opennms.horizon.alertservice.grpc;

import com.google.protobuf.BoolValue;
import com.google.protobuf.Empty;
import com.google.protobuf.Int64Value;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.Context;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.alerts.proto.MonitorPolicyList;
import org.opennms.horizon.alerts.proto.MonitorPolicyProto;
import org.opennms.horizon.alerts.proto.MonitorPolicyServiceGrpc;
import org.opennms.horizon.alertservice.db.tenant.TenantLookup;
import org.opennms.horizon.alertservice.service.MonitorPolicyService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MonitorPolicyGrpc extends MonitorPolicyServiceGrpc.MonitorPolicyServiceImplBase {
    private final MonitorPolicyService service;
    private final TenantLookup tenantLookup;

    @Override
    public void createPolicy(MonitorPolicyProto request, StreamObserver<MonitorPolicyProto> responseObserver) {
        tenantLookup
                .lookupTenantId(Context.current())
                .ifPresentOrElse(
                        tenantId -> {
                            try {
                                MonitorPolicyProto created = service.createPolicy(request, tenantId);
                                responseObserver.onNext(created);
                                responseObserver.onCompleted();
                            } catch (IllegalArgumentException e) {
                                responseObserver.onError(StatusProto.toStatusRuntimeException(
                                        createStatus(Code.INVALID_ARGUMENT_VALUE, e.getMessage())));
                            }
                        },
                        () -> responseObserver.onError(StatusProto.toStatusRuntimeException(badTenant())));
    }

    @Override
    public void listPolicies(Empty request, StreamObserver<MonitorPolicyList> responseObserver) {
        tenantLookup
                .lookupTenantId(Context.current())
                .ifPresentOrElse(
                        tenantId -> {
                            List<MonitorPolicyProto> list = service.listAll(tenantId);
                            responseObserver.onNext(MonitorPolicyList.newBuilder()
                                    .addAllPolicies(list)
                                    .build());
                            responseObserver.onCompleted();
                        },
                        () -> responseObserver.onError(StatusProto.toStatusRuntimeException(badTenant())));
    }

    @Override
    public void getPolicyById(Int64Value request, StreamObserver<MonitorPolicyProto> responseObserver) {
        tenantLookup
                .lookupTenantId(Context.current())
                .ifPresentOrElse(
                        tenantId -> service.findById(request.getValue(), tenantId)
                                .ifPresentOrElse(
                                        policy -> {
                                            responseObserver.onNext(policy);
                                            responseObserver.onCompleted();
                                        },
                                        () -> responseObserver.onError(
                                                StatusProto.toStatusRuntimeException(createStatus(
                                                        Code.NOT_FOUND_VALUE,
                                                        "Policy with ID (" + request.getValue() + ") doesn't exist")))),
                        () -> responseObserver.onError(StatusProto.toStatusRuntimeException(badTenant())));
    }

    @Override
    public void getDefaultPolicy(Empty request, StreamObserver<MonitorPolicyProto> responseObserver) {
        tenantLookup
                .lookupTenantId(Context.current())
                .ifPresentOrElse(
                        tenantId -> service.getDefaultPolicyProto(tenantId)
                                .ifPresentOrElse(
                                        policy -> {
                                            responseObserver.onNext(policy);
                                            responseObserver.onCompleted();
                                        },
                                        () -> responseObserver.onError(
                                                StatusProto.toStatusRuntimeException(createStatus(
                                                        Code.NOT_FOUND_VALUE,
                                                        "Default monitoring policy doesn't exist")))),
                        () -> responseObserver.onError(StatusProto.toStatusRuntimeException(badTenant())));
    }

    @Override
    public void deletePolicyById(Int64Value request, StreamObserver<BoolValue> responseObserver) {
        tenantLookup
                .lookupTenantId(Context.current())
                .ifPresentOrElse(
                        tenantId -> {
                            try {
                                service.deletePolicyById(request.getValue(), tenantId);
                                responseObserver.onNext(BoolValue.of(true));
                                responseObserver.onCompleted();
                            } catch (Exception e) {
                                Status status = Status.newBuilder()
                                        .setCode(Code.INTERNAL_VALUE)
                                        .setMessage(e.getMessage())
                                        .build();
                                responseObserver.onError(StatusProto.toStatusRuntimeException(status));
                            }
                        },
                        () -> responseObserver.onError(StatusProto.toStatusRuntimeException(badTenant())));
    }

    @Override
    public void deleteRuleById(Int64Value request, StreamObserver<BoolValue> responseObserver) {
        tenantLookup
                .lookupTenantId(Context.current())
                .ifPresentOrElse(
                        tenantId -> {
                            try {
                                service.deleteRuleById(request.getValue(), tenantId);
                                responseObserver.onNext(BoolValue.of(true));
                                responseObserver.onCompleted();
                            } catch (Exception e) {
                                Status status = Status.newBuilder()
                                        .setCode(Code.INTERNAL_VALUE)
                                        .setMessage(e.getMessage())
                                        .build();
                                responseObserver.onError(StatusProto.toStatusRuntimeException(status));
                            }
                        },
                        () -> responseObserver.onError(StatusProto.toStatusRuntimeException(badTenant())));
    }

    @Override
    public void countAlertByPolicyId(Int64Value request, StreamObserver<Int64Value> responseObserver) {
        tenantLookup
                .lookupTenantId(Context.current())
                .ifPresentOrElse(
                        tenantId -> {
                            try {
                                var count = service.countAlertByPolicyId(request.getValue(), tenantId);
                                responseObserver.onNext(Int64Value.of(count));
                                responseObserver.onCompleted();
                            } catch (Exception e) {
                                Status status = Status.newBuilder()
                                        .setCode(Code.INTERNAL_VALUE)
                                        .setMessage(e.getMessage())
                                        .build();
                                responseObserver.onError(StatusProto.toStatusRuntimeException(status));
                            }
                        },
                        () -> responseObserver.onError(StatusProto.toStatusRuntimeException(badTenant())));
    }

    @Override
    public void countAlertByRuleId(Int64Value request, StreamObserver<Int64Value> responseObserver) {
        tenantLookup
                .lookupTenantId(Context.current())
                .ifPresentOrElse(
                        tenantId -> {
                            try {
                                var count = service.countAlertByRuleId(request.getValue(), tenantId);
                                responseObserver.onNext(Int64Value.of(count));
                                responseObserver.onCompleted();
                            } catch (Exception e) {
                                Status status = Status.newBuilder()
                                        .setCode(Code.INTERNAL_VALUE)
                                        .setMessage(e.getMessage())
                                        .build();
                                responseObserver.onError(StatusProto.toStatusRuntimeException(status));
                            }
                        },
                        () -> responseObserver.onError(StatusProto.toStatusRuntimeException(badTenant())));
    }

    private Status badTenant() {
        return createStatus(Code.INVALID_ARGUMENT_VALUE, "Tenant Id can't be empty");
    }

    private Status createStatus(int code, String msg) {
        return Status.newBuilder().setCode(code).setMessage(msg).build();
    }
}
