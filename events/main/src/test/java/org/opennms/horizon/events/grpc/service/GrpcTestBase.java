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
package org.opennms.horizon.events.grpc.service;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import java.util.UUID;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

public abstract class GrpcTestBase {

    @DynamicPropertySource
    private static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("grpc.server.port", () -> 6767);
    }

    protected final String tenantId = new UUID(10, 10).toString();

    protected ManagedChannel channel;

    protected void setupGrpc() {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_BYPASS_KEY, String.valueOf(true));
        metadata.put(GrpcConstants.TENANT_ID_BYPASS_KEY, tenantId);
        channel = ManagedChannelBuilder.forAddress("localhost", 6767)
                .intercept(MetadataUtils.newAttachHeadersInterceptor(metadata))
                .usePlaintext()
                .build();
    }

    protected void setupGrpcWithDifferentTenantID() {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_BYPASS_KEY, String.valueOf(true));
        metadata.put(GrpcConstants.TENANT_ID_BYPASS_KEY, new UUID(5, 5).toString());
        channel = ManagedChannelBuilder.forAddress("localhost", 6767)
                .intercept(MetadataUtils.newAttachHeadersInterceptor(metadata))
                .usePlaintext()
                .build();
    }
}
