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

import com.google.protobuf.BoolValue;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.Context;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.Optional;
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

        tenantIdOptional.ifPresentOrElse(
                tenantId -> {
                    try {
                        List<TagDTO> tags = service.addTags(tenantId, request);
                        request.getEntityIdsList().forEach(entityIdDTO -> {
                            if (entityIdDTO.hasNodeId()) {
                                var nodeId = entityIdDTO.getNodeId();
                                nodeService.updateNodeMonitoredState(nodeId, tenantId);
                            }
                        });
                        responseObserver.onNext(
                                TagListDTO.newBuilder().addAllTags(tags).build());
                        responseObserver.onCompleted();
                    } catch (Exception e) {
                        Status status = Status.newBuilder()
                                .setCode(Code.INTERNAL_VALUE)
                                .setMessage(e.getMessage())
                                .build();
                        responseObserver.onError(StatusProto.toStatusRuntimeException(status));
                    }
                },
                () -> responseObserver.onError(
                        getStatusRuntimeException(Code.INVALID_ARGUMENT_VALUE, EMPTY_TENANT_ID_MSG)));
    }

    @Override
    public void removeTags(TagRemoveListDTO request, StreamObserver<BoolValue> responseObserver) {
        Optional<String> tenantIdOptional = tenantLookup.lookupTenantId(Context.current());

        tenantIdOptional.ifPresentOrElse(
                tenantId -> {
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
                },
                () -> responseObserver.onError(
                        getStatusRuntimeException(Code.INVALID_ARGUMENT_VALUE, EMPTY_TENANT_ID_MSG)));
    }

    @Override
    public void getTagsByEntityId(ListTagsByEntityIdParamsDTO request, StreamObserver<TagListDTO> responseObserver) {
        Optional<String> tenantIdOptional = tenantLookup.lookupTenantId(Context.current());

        tenantIdOptional.ifPresentOrElse(
                tenantId -> {
                    try {
                        List<TagDTO> tags = service.getTagsByEntityId(tenantId, request);
                        responseObserver.onNext(
                                TagListDTO.newBuilder().addAllTags(tags).build());
                        responseObserver.onCompleted();
                    } catch (Exception e) {
                        Status status = Status.newBuilder()
                                .setCode(Code.INTERNAL_VALUE)
                                .setMessage(e.getMessage())
                                .build();
                        responseObserver.onError(StatusProto.toStatusRuntimeException(status));
                    }
                },
                () -> responseObserver.onError(
                        getStatusRuntimeException(Code.INVALID_ARGUMENT_VALUE, EMPTY_TENANT_ID_MSG)));
    }

    @Override
    public void getTags(ListAllTagsParamsDTO request, StreamObserver<TagListDTO> responseObserver) {
        Optional<String> tenantIdOptional = tenantLookup.lookupTenantId(Context.current());

        tenantIdOptional.ifPresentOrElse(
                tenantId -> {
                    try {
                        List<TagDTO> tags = service.getTags(tenantId, request);
                        responseObserver.onNext(
                                TagListDTO.newBuilder().addAllTags(tags).build());
                        responseObserver.onCompleted();
                    } catch (Exception e) {

                        Status status = Status.newBuilder()
                                .setCode(Code.INTERNAL_VALUE)
                                .setMessage(e.getMessage())
                                .build();
                        responseObserver.onError(StatusProto.toStatusRuntimeException(status));
                    }
                },
                () -> responseObserver.onError(
                        getStatusRuntimeException(Code.INVALID_ARGUMENT_VALUE, EMPTY_TENANT_ID_MSG)));
    }

    @Override
    public void deleteTags(DeleteTagsDTO request, StreamObserver<BoolValue> responseObserver) {
        Optional<String> tenantIdOptional = tenantLookup.lookupTenantId(Context.current());

        tenantIdOptional.ifPresentOrElse(
                tenantId -> {
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
                },
                () -> responseObserver.onError(
                        getStatusRuntimeException(Code.INVALID_ARGUMENT_VALUE, EMPTY_TENANT_ID_MSG)));
    }

    private StatusRuntimeException getStatusRuntimeException(int code, String message) {
        Status status = Status.newBuilder().setCode(code).setMessage(message).build();
        return StatusProto.toStatusRuntimeException(status);
    }
}
