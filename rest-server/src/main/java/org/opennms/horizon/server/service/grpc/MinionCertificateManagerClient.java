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
package org.opennms.horizon.server.service.grpc;

import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.minioncertmanager.proto.GetMinionCertificateResponse;
import org.opennms.horizon.minioncertmanager.proto.MinionCertificateManagerGrpc;
import org.opennms.horizon.minioncertmanager.proto.MinionCertificateRequest;
import org.opennms.horizon.shared.constants.GrpcConstants;

@RequiredArgsConstructor
public class MinionCertificateManagerClient {

    private final ManagedChannel minionCertificateManagerChannel;
    private final long deadline;

    private MinionCertificateManagerGrpc.MinionCertificateManagerBlockingStub minionCertStub;

    @PostConstruct
    protected void initialStubs() {
        minionCertStub = MinionCertificateManagerGrpc.newBlockingStub(minionCertificateManagerChannel);
    }

    @PreDestroy
    public void shutdown() {
        if (minionCertificateManagerChannel != null && !minionCertificateManagerChannel.isShutdown()) {
            minionCertificateManagerChannel.shutdown();
        }
    }

    public GetMinionCertificateResponse getMinionCert(String tenantId, Long locationId, String accessToken) {
        MinionCertificateRequest request = MinionCertificateRequest.newBuilder()
                .setTenantId(tenantId)
                .setLocationId(locationId)
                .build();
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return minionCertStub
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
                .withDeadlineAfter(deadline, TimeUnit.MILLISECONDS)
                .getMinionCert(request);
    }

    public void revokeCertificate(String tenantId, Long locationId, String accessToken) {
        MinionCertificateRequest request = MinionCertificateRequest.newBuilder()
                .setTenantId(tenantId)
                .setLocationId(locationId)
                .build();
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        minionCertStub
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
                .withDeadlineAfter(deadline, TimeUnit.MILLISECONDS)
                .revokeMinionCert(request);
    }
}
