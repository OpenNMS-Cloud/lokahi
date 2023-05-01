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

import java.util.List;

import org.opennms.horizon.alert.tag.proto.PolicyTagProto;
import org.opennms.horizon.alert.tag.proto.TagListProto;
import org.opennms.horizon.alert.tag.proto.TagProto;
import org.opennms.horizon.alert.tag.proto.TagServiceGrpc;
import org.opennms.horizon.alertservice.db.tenant.TenantLookup;
import org.opennms.horizon.alertservice.service.TagService;
import org.springframework.stereotype.Service;

import com.google.protobuf.BoolValue;
import com.google.rpc.Code;
import com.google.rpc.Status;

import io.grpc.Context;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GrpcTagServiceImpl extends TagServiceGrpc.TagServiceImplBase {
    private final TagService tagService;
    private final TenantLookup tenantLookup;

    @Override
    public void removeTags(PolicyTagProto request, StreamObserver<BoolValue> responseObserver) {
        super.removeTags(request, responseObserver);
    }

    @Override
    public void listTags(TagListProto request, StreamObserver<TagListProto> responseObserver) {
        tenantLookup.lookupTenantId(Context.current())
            .ifPresentOrElse(tenanId -> {
                List<TagProto> list = tagService.listAllTags(tenanId);
                responseObserver.onNext(TagListProto.newBuilder().addAllTags(list).build());
                responseObserver.onCompleted();
            }, () -> responseObserver.onError(StatusProto.toStatusRuntimeException(badTenant())));
    }

    @Override
    public void assignTags(PolicyTagProto request, StreamObserver<BoolValue> responseObserver) {
        super.assignTags(request, responseObserver);
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

