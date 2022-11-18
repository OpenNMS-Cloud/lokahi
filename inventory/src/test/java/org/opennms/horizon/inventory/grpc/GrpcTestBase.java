/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
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

import io.grpc.BindableService;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.MetadataUtils;
import org.junit.jupiter.api.AfterAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.util.UUID;

abstract class GrpcTestBase {
    @DynamicPropertySource
    private static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("grpc.server.port", ()->6767);
    }

    protected final String tenantId = new UUID(10, 10).toString();

    protected ManagedChannel channel;

    protected static Server server;

    protected static final Metadata.Key<String> TENANT_ID = Metadata.Key.of("tenant-id", Metadata.ASCII_STRING_MARSHALLER);

    protected void setupGrpc() {
        Metadata metadata = new Metadata();
        metadata.put(TENANT_ID, tenantId.toString());
        channel = ManagedChannelBuilder.forAddress("localhost", 6767)
            .intercept(MetadataUtils.newAttachHeadersInterceptor(metadata))
            .usePlaintext().build();
    }

    protected void setupGrpcWithDifferentTenantID() {
        Metadata metadata = new Metadata();
        metadata.put(TENANT_ID, new UUID(5,5).toString());
        channel = ManagedChannelBuilder.forAddress("localhost", 6767)
            .intercept(MetadataUtils.newAttachHeadersInterceptor(metadata))
            .usePlaintext().build();
    }

    protected void setupGrpcWithOutTenantID() {
        channel = ManagedChannelBuilder.forAddress("localhost", 6767)
            .usePlaintext().build();
    }

    protected static Server startMockServer(String name, BindableService... services) throws IOException {
        InProcessServerBuilder builder = InProcessServerBuilder
            .forName(name).directExecutor();

        if (services != null) {
            for (BindableService service : services) {
                builder.addService(service);
            }
        }
        return builder.build().start();
    }
}
