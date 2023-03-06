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

package org.opennms.horizon.server.service;

import io.leangen.graphql.execution.ResolutionEnvironment;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.inventory.dto.AzureCredentialCreateDTO;
import org.opennms.horizon.inventory.dto.AzureCredentialDTO;
import org.opennms.horizon.server.RestServerApplication;
import org.opennms.horizon.server.service.grpc.InventoryClient;
import org.opennms.horizon.server.utils.ServerHeaderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT, classes = RestServerApplication.class)
class GraphQLAzureCredentialServiceTest {
    private static final String GRAPHQL_PATH = "/graphql";
    @MockBean
    private InventoryClient mockClient;
    @Autowired
    private WebTestClient webClient;
    @MockBean
    private ServerHeaderUtil mockHeaderUtil;
    private final String accessToken = "test-token-12345";
    private AzureCredentialDTO azureCredentialDTO1;

    @BeforeEach
    public void setUp() {
        azureCredentialDTO1 = AzureCredentialDTO.newBuilder()
            .setId(1L)
            .setLocation("Default")
            .setName("name")
            .setTenantId("tenant-id")
            .setClientId("client-id")
            .setDirectoryId("directory-id")
            .setSubscriptionId("subscription-id")
            .setCreateTimeMsec(Instant.now().toEpochMilli())
            .build();

        doReturn(accessToken).when(mockHeaderUtil).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @AfterEach
    public void afterTest() {
        verifyNoMoreInteractions(mockClient);
        verifyNoMoreInteractions(mockHeaderUtil);
    }

    @Test
    void testCreateAzureCredential() throws JSONException {
        doReturn(azureCredentialDTO1).when(mockClient).createNewAzureCredential(any(AzureCredentialCreateDTO.class), eq(accessToken));

        String request = createPayload("mutation { " +
            "    addAzureCredential( " +
            "        azureCredential: { " +
            "            location: \"Default\", " +
            "            name: \"name\", " +
            "            clientId: \"client-id\", " +
            "            clientSecret: \"client-secret\", " +
            "            subscriptionId: \"subscription-id\", " +
            "            directoryId: \"directory-id\" " +
            "            tags: [ " +
            "                {" +
            "                    name:\"tag-1\"" +
            "                }," +
            "                {" +
            "                    name:\"tag-2\"" +
            "                }" +
            "            ] " +
            "        } " +
            "    ) { " +
            "        id, " +
            "        location, " +
            "        name, " +
            "        tenantId, " +
            "        clientId, " +
            "        subscriptionId, " +
            "        directoryId, " +
            "        createTimeMsec " +
            "    } " +
            "}");

        webClient.post()
            .uri(GRAPHQL_PATH)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.data.addAzureCredential.id").isEqualTo(azureCredentialDTO1.getId())
            .jsonPath("$.data.addAzureCredential.location").isEqualTo(azureCredentialDTO1.getLocation())
            .jsonPath("$.data.addAzureCredential.name").isEqualTo(azureCredentialDTO1.getName())
            .jsonPath("$.data.addAzureCredential.tenantId").isEqualTo(azureCredentialDTO1.getTenantId())
            .jsonPath("$.data.addAzureCredential.clientId").isEqualTo(azureCredentialDTO1.getClientId())
            .jsonPath("$.data.addAzureCredential.subscriptionId").isEqualTo(azureCredentialDTO1.getSubscriptionId())
            .jsonPath("$.data.addAzureCredential.directoryId").isEqualTo(azureCredentialDTO1.getDirectoryId())
            .jsonPath("$.data.addAzureCredential.createTimeMsec").isEqualTo(azureCredentialDTO1.getCreateTimeMsec());
        verify(mockClient).createNewAzureCredential(any(AzureCredentialCreateDTO.class), eq(accessToken));
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    private String createPayload(String request) throws JSONException {
        return new JSONObject().put("query", request).toString();
    }
}
