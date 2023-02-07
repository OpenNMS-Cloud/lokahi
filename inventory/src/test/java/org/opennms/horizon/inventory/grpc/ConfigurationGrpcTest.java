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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.common.VerificationException;
import org.opennms.horizon.inventory.dto.ConfigurationDTO;
import org.opennms.horizon.inventory.dto.ConfigurationList;
import org.opennms.horizon.inventory.dto.ConfigurationServiceGrpc;
import org.opennms.horizon.inventory.service.ConfigurationService;

import com.google.protobuf.Empty;
import com.google.protobuf.StringValue;

import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.stub.MetadataUtils;

//This is an example of gRPC integration tests underline mock services.
public class ConfigurationGrpcTest extends AbstractGrpcUnitTest {
    private ConfigurationServiceGrpc.ConfigurationServiceBlockingStub stub;
    private ConfigurationService mockConfigurationService;
    private ConfigurationDTO configuration1, configuration2;

    @BeforeEach
    public void prepareTest() throws VerificationException, IOException {
        mockConfigurationService = mock(ConfigurationService.class);
        ConfigurationGrpcService grpcService = new ConfigurationGrpcService(mockConfigurationService, tenantLookup);
        startServer(grpcService);
        ManagedChannel channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
        stub = ConfigurationServiceGrpc.newBlockingStub(channel);
        configuration1 = ConfigurationDTO.newBuilder().setTenantId(tenantId).setKey("key1").build();
        configuration2 = ConfigurationDTO.newBuilder().setTenantId(tenantId).setKey("key2").build();
    }

    @AfterEach
    public void afterTest() {
        verifyNoMoreInteractions(mockConfigurationService);
        verifyNoMoreInteractions(spyInterceptor);
        reset(mockConfigurationService, spyInterceptor);
    }


    @Test
    void testListConfigurations() throws VerificationException {
        doReturn(Arrays.asList(configuration1, configuration2)).when(mockConfigurationService).findByTenantId(anyString());
        ConfigurationList result = stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createHeaders())).listConfigurations(Empty.newBuilder().build());
        assertThat(result.getConfigurationsList().size()).isEqualTo(2);
        verify(mockConfigurationService).findByTenantId(tenantId);
        verify(spyInterceptor).verifyAccessToken(authHeader);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    @Test
    void testListConfigurationsByLocation() throws VerificationException {
        doReturn(Arrays.asList(configuration1, configuration2)).when(mockConfigurationService).findByLocation(anyString(), anyString());
        ConfigurationList result = stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createHeaders())).listConfigurationsByLocation(StringValue.of("location"));
        assertThat(result.getConfigurationsList().size()).isEqualTo(2);
        verify(mockConfigurationService).findByLocation(tenantId, "location");
        verify(spyInterceptor).verifyAccessToken(authHeader);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    @Test
    void testListConfigurationsByKey() throws VerificationException {
        doReturn(Optional.of(configuration1)).when(mockConfigurationService).findByKey(anyString(), anyString());
        ConfigurationDTO result = stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createHeaders())).getConfigurationsByKey(StringValue.of("key1"));
        assertThat(result).isNotNull();
        assertThat(result.getKey()).isEqualTo("key1");
        verify(mockConfigurationService).findByKey(tenantId, "key1");
        verify(spyInterceptor).verifyAccessToken(authHeader);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }
}
