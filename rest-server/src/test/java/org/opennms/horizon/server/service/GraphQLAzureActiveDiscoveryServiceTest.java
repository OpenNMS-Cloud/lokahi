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
package org.opennms.horizon.server.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.opennms.horizon.server.test.util.GraphQLWebTestClient.createPayload;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import io.leangen.graphql.execution.ResolutionEnvironment;
import java.time.Instant;
import org.json.JSONException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.inventory.dto.AzureActiveDiscoveryCreateDTO;
import org.opennms.horizon.inventory.dto.AzureActiveDiscoveryDTO;
import org.opennms.horizon.server.RestServerApplication;
import org.opennms.horizon.server.service.grpc.InventoryClient;
import org.opennms.horizon.server.test.util.GraphQLWebTestClient;
import org.opennms.horizon.server.utils.ServerHeaderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = RANDOM_PORT, classes = RestServerApplication.class)
class GraphQLAzureActiveDiscoveryServiceTest {

    @MockBean
    private InventoryClient mockClient;

    @MockBean
    private ServerHeaderUtil mockHeaderUtil;

    private GraphQLWebTestClient webClient;
    private String accessToken;
    private AzureActiveDiscoveryDTO azureActiveDiscoveryDTO;

    @BeforeEach
    public void setUp(@Autowired WebTestClient webTestClient) {
        webClient = GraphQLWebTestClient.from(webTestClient);
        accessToken = webClient.getAccessToken();

        azureActiveDiscoveryDTO = AzureActiveDiscoveryDTO.newBuilder()
                .setId(1L)
                .setLocationId("Default")
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
    void testCreateAzureActiveDiscovery() throws JSONException {
        doReturn(azureActiveDiscoveryDTO)
                .when(mockClient)
                .createAzureActiveDiscovery(any(AzureActiveDiscoveryCreateDTO.class), eq(accessToken));

        String request = createPayload("mutation { " + "    createAzureActiveDiscovery( "
                + "        discovery: { "
                + "            locationId: \"Default\", "
                + "            name: \"name\", "
                + "            clientId: \"client-id\", "
                + "            clientSecret: \"client-secret\", "
                + "            subscriptionId: \"subscription-id\", "
                + "            directoryId: \"directory-id\" "
                + "            tags: [ "
                + "                {"
                + "                    name:\"tag-1\""
                + "                },"
                + "                {"
                + "                    name:\"tag-2\""
                + "                }"
                + "            ] "
                + "        } "
                + "    ) { "
                + "        id, "
                + "        locationId, "
                + "        name, "
                + "        clientId, "
                + "        subscriptionId, "
                + "        directoryId, "
                + "        createTimeMsec "
                + "    } "
                + "}");

        webClient
                .exchangePost(request)
                .expectCleanResponse()
                .jsonPath("$.data.createAzureActiveDiscovery.id")
                .isEqualTo(azureActiveDiscoveryDTO.getId())
                .jsonPath("$.data.createAzureActiveDiscovery.locationId")
                .isEqualTo(azureActiveDiscoveryDTO.getLocationId())
                .jsonPath("$.data.createAzureActiveDiscovery.name")
                .isEqualTo(azureActiveDiscoveryDTO.getName())
                .jsonPath("$.data.createAzureActiveDiscovery.clientId")
                .isEqualTo(azureActiveDiscoveryDTO.getClientId())
                .jsonPath("$.data.createAzureActiveDiscovery.subscriptionId")
                .isEqualTo(azureActiveDiscoveryDTO.getSubscriptionId())
                .jsonPath("$.data.createAzureActiveDiscovery.directoryId")
                .isEqualTo(azureActiveDiscoveryDTO.getDirectoryId())
                .jsonPath("$.data.createAzureActiveDiscovery.createTimeMsec")
                .isEqualTo(azureActiveDiscoveryDTO.getCreateTimeMsec());
        verify(mockClient).createAzureActiveDiscovery(any(AzureActiveDiscoveryCreateDTO.class), eq(accessToken));
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }
}
