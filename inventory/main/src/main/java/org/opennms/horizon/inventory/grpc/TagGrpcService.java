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
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.Context;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.inventory.dto.DeleteTagsDTO;
import org.opennms.horizon.inventory.dto.ListAllTagsParamsDTO;
import org.opennms.horizon.inventory.dto.ListTagsByEntityIdParamsDTO;
import org.opennms.horizon.inventory.dto.TagCreateListDTO;
import org.opennms.horizon.inventory.dto.TagDTO;
import org.opennms.horizon.inventory.dto.TagListDTO;
import org.opennms.horizon.inventory.dto.TagRemoveListDTO;
import org.opennms.horizon.inventory.dto.TagServiceGrpc;
import org.opennms.horizon.inventory.service.NodeService;
import org.opennms.horizon.inventory.service.TagService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TagGrpcService extends TagServiceGrpc.TagServiceImplBase {
    public static final String EMPTY_TENANT_ID_MSG = "Tenant Id can't be empty";
    private final TagService service;
    private final TenantLookup tenantLookup;
    private final NodeService nodeService;

    @Override
    public void addTags(TagCreateListDTO request, StreamObserver<TagListDTO> responseObserver) {
        Optional<String> tenantIdOptional = tenantLookup.lookupTenantId(Context.current());

        tenantIdOptional.ifPresentOrElse(tenantId -> {
            try {
                List<TagDTO> tags = service.addTags(tenantId, request);
                request.getEntityIdsList().forEach(entityIdDTO -> {
                    if (entityIdDTO.hasNodeId()) {
                        var nodeId = entityIdDTO.getNodeId();
                        nodeService.updateNodeMonitoredState(nodeId, tenantId);
                    }
                });
                responseObserver.onNext(TagListDTO.newBuilder().addAllTags(tags).build());
                responseObserver.onCompleted();
            } catch (Exception e) {
                Status status = Status.newBuilder()
                    .setCode(Code.INTERNAL_VALUE)
                    .setMessage(e.getMessage())
                    .build();
                responseObserver.onError(StatusProto.toStatusRuntimeException(status));
            }
        }, () -> responseObserver.onError(getStatusRuntimeException(Code.INVALID_ARGUMENT_VALUE, EMPTY_TENANT_ID_MSG)));
    }

    @Override
    public void removeTags(TagRemoveListDTO request, StreamObserver<BoolValue> responseObserver) {
        Optional<String> tenantIdOptional = tenantLookup.lookupTenantId(Context.current());

        tenantIdOptional.ifPresentOrElse(tenantId -> {
            try {
                service.removeTags(tenantId, request);
                request.getEntityIdsList().forEach(entityIdDTO -> {
                    if (entityIdDTO.hasNodeId()) {
                        var nodeId = entityIdDTO.getNodeId();
                        nodeService.updateNodeMonitoredState(nodeId, tenantId);
                    }
                });
                responseObserver.onNext(BoolValue.of(true));
                responseObserver.onCompleted();
            } catch (Exception e) {
                Status status = Status.newBuilder()
                    .setCode(Code.INTERNAL_VALUE)
                    .setMessage(e.getMessage())
                    .build();
                responseObserver.onError(StatusProto.toStatusRuntimeException(status));
            }
        }, () -> responseObserver.onError(getStatusRuntimeException(Code.INVALID_ARGUMENT_VALUE, EMPTY_TENANT_ID_MSG)));
    }

    @Override
    public void getTagsByEntityId(ListTagsByEntityIdParamsDTO request, StreamObserver<TagListDTO> responseObserver) {
        Optional<String> tenantIdOptional = tenantLookup.lookupTenantId(Context.current());

        tenantIdOptional.ifPresentOrElse(tenantId -> {
            try {
                List<TagDTO> tags = service.getTagsByEntityId(tenantId, request);
                responseObserver.onNext(TagListDTO.newBuilder().addAllTags(tags).build());
                responseObserver.onCompleted();
            } catch (Exception e) {
                Status status = Status.newBuilder()
                    .setCode(Code.INTERNAL_VALUE)
                    .setMessage(e.getMessage())
                    .build();
                responseObserver.onError(StatusProto.toStatusRuntimeException(status));
            }
        }, () -> responseObserver.onError(getStatusRuntimeException(Code.INVALID_ARGUMENT_VALUE, EMPTY_TENANT_ID_MSG)));
    }

    @Override
    public void getTags(ListAllTagsParamsDTO request, StreamObserver<TagListDTO> responseObserver) {
        Optional<String> tenantIdOptional = tenantLookup.lookupTenantId(Context.current());

        tenantIdOptional.ifPresentOrElse(tenantId -> {
            try {
                List<TagDTO> tags = service.getTags(tenantId, request);
                responseObserver.onNext(TagListDTO.newBuilder().addAllTags(tags).build());
                responseObserver.onCompleted();
            } catch (Exception e) {

                Status status = Status.newBuilder()
                    .setCode(Code.INTERNAL_VALUE)
                    .setMessage(e.getMessage())
                    .build();
                responseObserver.onError(StatusProto.toStatusRuntimeException(status));
            }
        }, () -> responseObserver.onError(getStatusRuntimeException(Code.INVALID_ARGUMENT_VALUE, EMPTY_TENANT_ID_MSG)));
    }

    @Override
    public void deleteTags(DeleteTagsDTO request, StreamObserver<BoolValue> responseObserver) {
        Optional<String> tenantIdOptional = tenantLookup.lookupTenantId(Context.current());

        tenantIdOptional.ifPresentOrElse(tenantId -> {
            try {
                service.deleteTags(tenantId, request);

                responseObserver.onNext(BoolValue.of(true));
                responseObserver.onCompleted();
            } catch (Exception e) {

                Status status = Status.newBuilder()
                    .setCode(Code.INTERNAL_VALUE)
                    .setMessage(e.getMessage())
                    .build();
                responseObserver.onError(StatusProto.toStatusRuntimeException(status));
            }
        }, () -> responseObserver.onError(getStatusRuntimeException(Code.INVALID_ARGUMENT_VALUE, EMPTY_TENANT_ID_MSG)));
    }

    private StatusRuntimeException getStatusRuntimeException(int code, String message){
        Status status = Status.newBuilder()
            .setCode(code)
            .setMessage(message)
            .build();
        return StatusProto.toStatusRuntimeException(status);
    }
}
