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
import org.opennms.horizon.events.grpc.config.EventServerInterceptor;
import org.opennms.horizon.shared.constants.GrpcConstants;

public abstract class AbstractGrpcUnitTest {
    protected String serverName;
    protected Server server;

    protected EventServerInterceptor spyInterceptor;

    protected final String tenantId = "test-tenant";
    protected final String authHeader = "Bearer esgs12345";

    protected void startServer(BindableService service) throws IOException, VerificationException {
        spyInterceptor = spy(new EventServerInterceptor(mock(KeycloakDeployment.class)));
        serverName = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(serverName)
                .addService(ServerInterceptors.intercept(service, spyInterceptor))
                .directExecutor()
                .build();
        server.start();
        doReturn(Optional.of(tenantId)).when(spyInterceptor).verifyAccessToken(authHeader);
    }

    protected void stopServer() throws InterruptedException {
        server.shutdownNow();
        server.awaitTermination(10, TimeUnit.SECONDS);
    }

    protected Metadata createHeaders() {
        return createHeaders(authHeader);
    }

    protected Metadata createHeaders(String inAuthHeader) {
        Metadata headers = new Metadata();
        headers.put(GrpcConstants.AUTHORIZATION_METADATA_KEY, inAuthHeader);
        return headers;
    }
}
