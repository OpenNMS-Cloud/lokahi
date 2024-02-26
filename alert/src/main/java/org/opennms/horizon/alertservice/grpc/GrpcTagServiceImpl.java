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
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.Context;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.alert.tag.proto.PolicyTagProto;
import org.opennms.horizon.alert.tag.proto.TagListProto;
import org.opennms.horizon.alert.tag.proto.TagProto;
import org.opennms.horizon.alert.tag.proto.TagServiceGrpc;
import org.opennms.horizon.alertservice.db.tenant.TenantLookup;
import org.opennms.horizon.alertservice.service.TagService;
import org.springframework.stereotype.Service;

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
        tenantLookup
                .lookupTenantId(Context.current())
                .ifPresentOrElse(
                        tenanId -> {
                            List<TagProto> list = tagService.listAllTags(tenanId);
                            responseObserver.onNext(
                                    TagListProto.newBuilder().addAllTags(list).build());
                            responseObserver.onCompleted();
                        },
                        () -> responseObserver.onError(StatusProto.toStatusRuntimeException(badTenant())));
    }

    @Override
    public void assignTags(PolicyTagProto request, StreamObserver<BoolValue> responseObserver) {
        super.assignTags(request, responseObserver);
    }

    private Status badTenant() {
        return createStatus(Code.INVALID_ARGUMENT_VALUE, "Tenant Id can't be empty");
    }

    private Status createStatus(int code, String msg) {
        return Status.newBuilder().setCode(code).setMessage(msg).build();
    }
}
