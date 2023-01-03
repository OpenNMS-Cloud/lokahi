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

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.common.VerificationException;
import org.opennms.horizon.inventory.SpringContextTestInitializer;
import org.opennms.horizon.inventory.dto.AzureCredentialCreateDTO;
import org.opennms.horizon.inventory.dto.AzureCredentialDTO;
import org.opennms.horizon.inventory.dto.AzureCredentialServiceGrpc;
import org.opennms.horizon.inventory.model.AzureCredential;
import org.opennms.horizon.inventory.repository.AzureCredentialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ContextConfiguration(initializers = {SpringContextTestInitializer.class})
class AzureCredentialGrpcItTest extends GrpcTestBase {
    private static final String TEST_CLIENT_ID = "client-id";
    private static final String TEST_CLIENT_SECRET = "client-secret";
    private static final String TEST_SUBSCRIPTION_ID = "subscription-id";
    private static final String TEST_DIRECTORY_ID = "directory-id";
    private static final String TEST_RESOURCE_GROUP = "resource-group";

    private AzureCredentialServiceGrpc.AzureCredentialServiceBlockingStub serviceStub;

    @Autowired
    private AzureCredentialRepository azureCredentialRepository;

    @BeforeEach
    public void prepare() throws VerificationException {
        prepareServer();
        serviceStub = AzureCredentialServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    public void cleanUp() throws InterruptedException {
        azureCredentialRepository.deleteAll();
        afterTest();
    }

    @Test
    void testCreateAzureCredentials() throws Exception {

        AzureCredentialCreateDTO createDTO = AzureCredentialCreateDTO.newBuilder()
            .setClientId(TEST_CLIENT_ID)
            .setClientSecret(TEST_CLIENT_SECRET)
            .setSubscriptionId(TEST_SUBSCRIPTION_ID)
            .setDirectoryId(TEST_DIRECTORY_ID)
            .setResourceGroup(TEST_RESOURCE_GROUP)
            .build();

        AzureCredentialDTO credentials = serviceStub.withInterceptors(MetadataUtils
                .newAttachHeadersInterceptor(createAuthHeader(authHeader)))
            .createCredentials(createDTO);

        assertEquals(1, credentials.getId());
        assertEquals(createDTO.getClientId(), credentials.getClientId());
        assertEquals(createDTO.getSubscriptionId(), credentials.getSubscriptionId());
        assertEquals(createDTO.getDirectoryId(), credentials.getDirectoryId());
        assertEquals(createDTO.getResourceGroup(), credentials.getResourceGroup());
        assertTrue(credentials.getCreateTime() > 0L);

        List<AzureCredential> list = azureCredentialRepository.findAll();
        assertEquals(1, list.size());

        AzureCredential azureCredential = list.get(0);
        assertEquals(1, credentials.getId());
        assertEquals(createDTO.getClientId(), azureCredential.getClientId());
        assertEquals(createDTO.getClientSecret(), azureCredential.getClientSecret());
        assertEquals(createDTO.getSubscriptionId(), azureCredential.getSubscriptionId());
        assertEquals(createDTO.getDirectoryId(), azureCredential.getDirectoryId());
        assertEquals(createDTO.getResourceGroup(), azureCredential.getResourceGroup());
        assertNotNull(azureCredential.getCreateTime());

        verify(spyInterceptor).verifyAccessToken(authHeader);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

    @Test
    void testCreateAzureCredentialsWithoutTenantId() throws VerificationException {

        AzureCredentialCreateDTO createDTO = AzureCredentialCreateDTO.newBuilder()
            .setClientId(TEST_CLIENT_ID)
            .setClientSecret(TEST_CLIENT_SECRET)
            .setSubscriptionId(TEST_SUBSCRIPTION_ID)
            .setDirectoryId(TEST_DIRECTORY_ID)
            .setResourceGroup(TEST_RESOURCE_GROUP)
            .build();

        StatusRuntimeException exception = Assertions.assertThrows(StatusRuntimeException.class, () ->
            serviceStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(createAuthHeader(headerWithoutTenant)))
                .createCredentials(createDTO));
        assertThat(exception.getStatus().getCode()).isEqualTo(io.grpc.Status.Code.UNAUTHENTICATED);
        assertThat(exception.getMessage()).contains("Missing tenant id");
        verify(spyInterceptor).verifyAccessToken(headerWithoutTenant);
        verify(spyInterceptor).interceptCall(any(ServerCall.class), any(Metadata.class), any(ServerCallHandler.class));
    }

}
