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

package org.opennms.horizon.alertservice.grpc;

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
import org.opennms.horizon.alerts.proto.MonitorPolicyList;
import org.opennms.horizon.alerts.proto.MonitorPolicyProto;
import org.opennms.horizon.alerts.proto.MonitorPolicyServiceGrpc;
import org.opennms.horizon.alertservice.db.tenant.TenantLookup;
import org.opennms.horizon.alertservice.service.MonitorPolicyService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MonitorPolicyGrpc extends MonitorPolicyServiceGrpc.MonitorPolicyServiceImplBase {
    private final MonitorPolicyService service;
    private final TenantLookup tenantLookup;

    @Override
    public void createPolicy(MonitorPolicyProto request, StreamObserver<MonitorPolicyProto> responseObserver) {
        tenantLookup.lookupTenantId(Context.current())
            .ifPresentOrElse(tenantId -> {
                MonitorPolicyProto created = service.createPolicy(request, tenantId);
                responseObserver.onNext(created);
                responseObserver.onCompleted();
            }, () -> responseObserver.onError(StatusProto.toStatusRuntimeException(badTenant())));
    }

    @Override
    public void listPolicies(Empty request, StreamObserver<MonitorPolicyList> responseObserver) {
        tenantLookup.lookupTenantId(Context.current())
            .ifPresentOrElse(tenantId -> {
                List<MonitorPolicyProto> list = service.listAll(tenantId);
                responseObserver.onNext(MonitorPolicyList.newBuilder().addAllPolicies(list).build());
                responseObserver.onCompleted();
            }, () -> responseObserver.onError(StatusProto.toStatusRuntimeException(badTenant())));
    }

    @Override
    public void getPolicyById(Int64Value request, StreamObserver<MonitorPolicyProto> responseObserver) {
        tenantLookup.lookupTenantId(Context.current())
            .ifPresentOrElse(tenantId -> service.findById(request.getValue(), tenantId)
                .ifPresentOrElse(policy -> {
                    responseObserver.onNext(policy);
                    responseObserver.onCompleted();
                    }, () -> responseObserver.onError(StatusProto.toStatusRuntimeException(createStatus(Code.NOT_FOUND_VALUE,
                        "Policy with ID (" + request.getValue() + ") doesn't exist")))),
                () -> responseObserver.onError(StatusProto.toStatusRuntimeException(badTenant())));
    }

    @Override
    public void getDefaultPolicy(Empty request, StreamObserver<MonitorPolicyProto> responseObserver) {
        tenantLookup.lookupTenantId(Context.current())
            .ifPresentOrElse(tenantId -> service.getDefaultPolicy()
                    .ifPresentOrElse(policy -> {
                        responseObserver.onNext(policy);
                        responseObserver.onCompleted();
                        },
                        () -> responseObserver.onError(StatusProto.toStatusRuntimeException(createStatus(Code.NOT_FOUND_VALUE,
                            "Default monitoring policy doesn't exist")))),
                () -> responseObserver.onError(StatusProto.toStatusRuntimeException(badTenant())));
    }

    @Override
    public void deletePolicyById(Int64Value request, StreamObserver<BoolValue> responseObserver) {
        tenantLookup.lookupTenantId(Context.current())
            .ifPresentOrElse(tenantId -> {
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
            }, () -> responseObserver.onError(StatusProto.toStatusRuntimeException(badTenant())));
    }

    @Override
    public void deleteRuleById(Int64Value request, StreamObserver<BoolValue> responseObserver) {
        tenantLookup.lookupTenantId(Context.current())
            .ifPresentOrElse(tenantId ->
            {
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
            }, () -> responseObserver.onError(StatusProto.toStatusRuntimeException(badTenant())));
    }

    @Override
    public void countAlertByPolicyId(Int64Value request, StreamObserver<Int64Value> responseObserver) {
        tenantLookup.lookupTenantId(Context.current())
            .ifPresentOrElse(tenantId ->
            {
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
            }, () -> responseObserver.onError(StatusProto.toStatusRuntimeException(badTenant())));
    }

    @Override
    public void countAlertByRuleId(Int64Value request, StreamObserver<Int64Value> responseObserver) {
        tenantLookup.lookupTenantId(Context.current())
            .ifPresentOrElse(tenantId ->
            {
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
            }, () -> responseObserver.onError(StatusProto.toStatusRuntimeException(badTenant())));
    }

    private Status badTenant() {
        return createStatus(Code.INVALID_ARGUMENT_VALUE, "Tenant Id can't be empty");
    }

    private Status createStatus(int code, String msg) {
        return Status.newBuilder()
            .setCode(code)
            .setMessage(msg)
            .build();
    }
}
