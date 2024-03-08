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
package org.opennms.horizon.minioncertmanager.grpc.client;

import com.google.protobuf.Int64Value;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.inventory.dto.MonitoringLocationDTO;
import org.opennms.horizon.inventory.dto.MonitoringLocationServiceGrpc;
import org.opennms.horizon.shared.constants.GrpcConstants;

@RequiredArgsConstructor
public class InventoryClient {
    private final ManagedChannel channel;
    private final long deadline;

    private MonitoringLocationServiceGrpc.MonitoringLocationServiceBlockingStub locationStub;

    protected void initialStubs() {
        locationStub = MonitoringLocationServiceGrpc.newBlockingStub(channel);
    }

    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
        }
    }

    private Metadata getMetadata(boolean bypassAuthorization, String tenantId) {
        var metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_BYPASS_KEY, String.valueOf(bypassAuthorization));
        metadata.put(GrpcConstants.TENANT_ID_BYPASS_KEY, tenantId);
        return metadata;
    }

    public MonitoringLocationDTO getLocationById(long nodeId, String tenantId) {
        Metadata metadata = getMetadata(true, tenantId);

        return locationStub
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
                .withDeadlineAfter(deadline, TimeUnit.MILLISECONDS)
                .getLocationById(Int64Value.of(nodeId));
    }
}
