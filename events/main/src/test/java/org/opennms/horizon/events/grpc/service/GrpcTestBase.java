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

package org.opennms.horizon.events.grpc.service;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;

import org.opennms.horizon.shared.constants.GrpcConstants;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.UUID;

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
            .usePlaintext().build();
    }

    protected void setupGrpcWithDifferentTenantID() {
        Metadata metadata = new Metadata();
        metadata.put(GrpcConstants.AUTHORIZATION_BYPASS_KEY, String.valueOf(true));
        metadata.put(GrpcConstants.TENANT_ID_BYPASS_KEY, new UUID(5, 5).toString());
        channel = ManagedChannelBuilder.forAddress("localhost", 6767)
            .intercept(MetadataUtils.newAttachHeadersInterceptor(metadata))
            .usePlaintext().build();
    }
}
