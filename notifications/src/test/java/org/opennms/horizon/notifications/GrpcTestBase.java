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
package org.opennms.horizon.notifications;

import static org.mockito.Mockito.*;

import io.grpc.BindableService;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.keycloak.common.VerificationException;
import org.opennms.horizon.notifications.grpc.config.NotificationServerInterceptor;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

public abstract class GrpcTestBase {
    @DynamicPropertySource
    private static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("grpc.server.port", () -> 6767);
    }

    protected final String tenantId = "test-tenant";
    protected final String alternativeTenantId = "other-tenant";
    protected final String authHeader = "Bearer esgs12345";
    protected final String headerWithoutTenant = "Bearer esgs12345invalid";
    protected final String differentTenantHeader = "Bearer esgs12345different";
    public static final String defaultTenant = "opennms-prime";
    protected ManagedChannel channel;

    @Autowired
    @SpyBean
    protected NotificationServerInterceptor spyInterceptor;

    protected void prepareServer() throws VerificationException {
        channel = ManagedChannelBuilder.forAddress("localhost", 6767)
                .usePlaintext()
                .build();
        doReturn(Optional.of(tenantId)).when(spyInterceptor).verifyAccessToken(authHeader);
        doReturn(Optional.of(alternativeTenantId)).when(spyInterceptor).verifyAccessToken(differentTenantHeader);
        doReturn(Optional.empty()).when(spyInterceptor).verifyAccessToken(headerWithoutTenant);
        doThrow(new VerificationException()).when(spyInterceptor).verifyAccessToken(null);
    }

    protected void afterTest() throws InterruptedException {
        channel.shutdownNow();
        channel.awaitTermination(10, TimeUnit.SECONDS);
        verifyNoMoreInteractions(spyInterceptor);
        reset(spyInterceptor);
    }

    protected Metadata createAuthHeader(String value) {
        Metadata headers = new Metadata();
        headers.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, value);
        return headers;
    }

    protected static Server server;

    protected static Server startMockServer(String name, BindableService... services) throws IOException {
        InProcessServerBuilder builder = InProcessServerBuilder.forName(name).directExecutor();

        if (services != null) {
            for (BindableService service : services) {
                builder.addService(service);
            }
        }
        return builder.build().start();
    }
}
