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

package org.opennms.horizon.inventory.grpc;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.common.VerificationException;
import org.opennms.horizon.shared.constants.GrpcConstants;

import io.grpc.BindableService;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;

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
            .addService(ServerInterceptors.intercept(service, spyInterceptor)).directExecutor().build();
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
