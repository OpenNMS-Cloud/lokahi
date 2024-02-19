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

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import io.grpc.BindableService;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.common.VerificationException;
import org.opennms.horizon.shared.constants.GrpcConstants;

public abstract class AbstractGrpcUnitTest {

    protected InventoryServerInterceptor spyInterceptor;
    protected TenantLookup tenantLookup = new GrpcTenantLookupImpl();
    protected String serverName;
    protected Server server;

    protected static final String TENANT_ID = "test-tenant";
    protected static final String AUTH_HEADER = "Bearer esgs12345";

    protected void startServer(BindableService service) throws IOException, VerificationException {
        spyInterceptor = spy(new InventoryServerInterceptor(mock(KeycloakDeployment.class)));
        serverName = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(serverName)
                .addService(ServerInterceptors.intercept(service, spyInterceptor))
                .directExecutor()
                .build();
        server.start();
        doReturn(Optional.of(TENANT_ID)).when(spyInterceptor).verifyAccessToken(AUTH_HEADER);
    }

    protected void stopServer() throws InterruptedException {
        server.shutdownNow();
        server.awaitTermination(10, TimeUnit.SECONDS);
    }

    protected Metadata createHeaders() {
        Metadata headers = new Metadata();
        headers.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, AUTH_HEADER);
        return headers;
    }
}
