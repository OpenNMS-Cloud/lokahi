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
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import io.leangen.graphql.execution.ResolutionEnvironment;
import java.time.Instant;
import java.util.List;
import org.json.JSONException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.inventory.dto.PassiveDiscoveryDTO;
import org.opennms.horizon.inventory.dto.PassiveDiscoveryListDTO;
import org.opennms.horizon.inventory.dto.PassiveDiscoveryToggleDTO;
import org.opennms.horizon.inventory.dto.PassiveDiscoveryUpsertDTO;
import org.opennms.horizon.server.RestServerApplication;
import org.opennms.horizon.server.service.grpc.InventoryClient;
import org.opennms.horizon.server.test.util.GraphQLWebTestClient;
import org.opennms.horizon.server.utils.ServerHeaderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = RANDOM_PORT, classes = RestServerApplication.class)
class GraphQLPassiveDiscoveryServiceTest {
    @MockBean
    private InventoryClient mockClient;

    @MockBean
    private ServerHeaderUtil mockHeaderUtil;

    private GraphQLWebTestClient webClient;
    private String accessToken;
    private PassiveDiscoveryDTO passiveDiscoveryDTO;

    @BeforeEach
    public void setUp(@Autowired WebTestClient webTestClient) {
        webClient = GraphQLWebTestClient.from(webTestClient);
        accessToken = webClient.getAccessToken();

        passiveDiscoveryDTO = PassiveDiscoveryDTO.newBuilder()
                .setId(1L)
                .setName("passive-discovery-name")
                .setLocationId("Default")
                .setToggle(true)
                .addAllPorts(List.of(161))
                .addAllCommunities(List.of("public"))
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
    void testCreatePassiveDiscovery() throws JSONException {
        doReturn(passiveDiscoveryDTO)
                .when(mockClient)
                .upsertPassiveDiscovery(any(PassiveDiscoveryUpsertDTO.class), eq(accessToken));

        String request = "mutation { " + "    upsertPassiveDiscovery( "
                + "        discovery: { "
                + "            name: \"passive-discovery-name\", "
                + "            locationId: \"Default\", "
                + "            snmpPorts: [ 161 ], "
                + "            snmpCommunities: [ \"public\" ], "
                + "            tags: [ "
                + "                { "
                + "                    name:\"tag-1\" "
                + "                } "
                + "            ] "
                + "        } "
                + "    ) { "
                + "        id, "
                + "        name, "
                + "        locationId, "
                + "        toggle, "
                + "        snmpPorts, "
                + "        snmpCommunities, "
                + "        createTimeMsec "
                + "    } "
                + "}";

        webClient
                .exchangeGraphQLQuery(request)
                .expectCleanResponse()
                .jsonPath("$.data.upsertPassiveDiscovery.id")
                .isEqualTo(passiveDiscoveryDTO.getId())
                .jsonPath("$.data.upsertPassiveDiscovery.name")
                .isEqualTo(passiveDiscoveryDTO.getName())
                .jsonPath("$.data.upsertPassiveDiscovery.toggle")
                .isEqualTo(passiveDiscoveryDTO.getToggle())
                .jsonPath("$.data.upsertPassiveDiscovery.createTimeMsec")
                .isEqualTo(passiveDiscoveryDTO.getCreateTimeMsec());
        verify(mockClient).upsertPassiveDiscovery(any(PassiveDiscoveryUpsertDTO.class), eq(accessToken));
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    void testTogglePassiveDiscovery() throws JSONException {
        doReturn(passiveDiscoveryDTO)
                .when(mockClient)
                .createPassiveDiscoveryToggle((any(PassiveDiscoveryToggleDTO.class)), eq(accessToken));

        String request = "mutation {"
                + "  togglePassiveDiscovery(toggle: { id: 1, toggle: true }) {"
                + "    toggle"
                + "  }"
                + "}";

        webClient
                .exchangeGraphQLQuery(request)
                .expectCleanResponse()
                .jsonPath("$.data.togglePassiveDiscovery.toggle")
                .isEqualTo(passiveDiscoveryDTO.getToggle());

        verify(mockClient).createPassiveDiscoveryToggle(any(PassiveDiscoveryToggleDTO.class), eq(accessToken));
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    void testGetPassiveDiscovery() throws JSONException {
        doReturn(PassiveDiscoveryListDTO.newBuilder()
                        .addDiscoveries(passiveDiscoveryDTO)
                        .build())
                .when(mockClient)
                .listPassiveDiscoveries(eq(accessToken));

        String request = "query { " + "    passiveDiscoveries { "
                + "        id, "
                + "        name, "
                + "        locationId, "
                + "        toggle, "
                + "        snmpPorts, "
                + "        snmpCommunities, "
                + "        createTimeMsec "
                + "    } "
                + "}";

        webClient
                .exchangeGraphQLQuery(request)
                .expectCleanResponse()
                .jsonPath("$.data.passiveDiscoveries[0].id")
                .isEqualTo(passiveDiscoveryDTO.getId())
                .jsonPath("$.data.passiveDiscoveries[0].name")
                .isEqualTo(passiveDiscoveryDTO.getName())
                .jsonPath("$.data.passiveDiscoveries[0].toggle")
                .isEqualTo(passiveDiscoveryDTO.getToggle())
                .jsonPath("$.data.passiveDiscoveries[0].createTimeMsec")
                .isEqualTo(passiveDiscoveryDTO.getCreateTimeMsec());
        verify(mockClient).listPassiveDiscoveries(eq(accessToken));
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }
}
