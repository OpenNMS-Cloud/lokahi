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
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.notifications.dto.NotificationServiceGrpc;
import org.opennms.horizon.notifications.dto.PagerDutyConfigDTO;
import org.opennms.horizon.shared.constants.GrpcConstants;

@RequiredArgsConstructor
public class NotificationClient {
    private final ManagedChannel channel;

    private NotificationServiceGrpc.NotificationServiceBlockingStub notificationStub;

    protected void initialStubs() {
        notificationStub = NotificationServiceGrpc.newBlockingStub(channel);
    }

    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
        }
    }

    public PagerDutyConfigDTO postPagerDutyConfig(PagerDutyConfigDTO configDTO, String accessToken) {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, accessToken);
        return notificationStub
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
                .postPagerDutyConfig(configDTO);
    }
}
