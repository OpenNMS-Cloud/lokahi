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
import org.opennms.horizon.inventory.dto.PassiveDiscoveryDTO;
import org.opennms.horizon.inventory.dto.PassiveDiscoveryListDTO;
import org.opennms.horizon.inventory.dto.PassiveDiscoveryUpsertDTO;
import org.opennms.horizon.inventory.dto.PassiveDiscoveryToggleDTO;
import org.opennms.horizon.server.RestServerApplication;
import org.opennms.horizon.server.service.grpc.InventoryClient;
import org.opennms.horizon.server.utils.ServerHeaderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT, classes = RestServerApplication.class)
class GraphQLPassiveDiscoveryServiceTest {
    private static final String GRAPHQL_PATH = "/graphql";
    @MockBean
    private InventoryClient mockClient;
    @Autowired
    private WebTestClient webClient;
    @MockBean
    private ServerHeaderUtil mockHeaderUtil;
    private final String accessToken = "test-token-12345";
    private PassiveDiscoveryDTO passiveDiscoveryDTO;

    @BeforeEach
    public void setUp() {
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
        doReturn(passiveDiscoveryDTO).when(mockClient).upsertPassiveDiscovery(any(PassiveDiscoveryUpsertDTO.class), eq(accessToken));

        String request = createPayload("mutation { " +
            "    upsertPassiveDiscovery( " +
            "        discovery: { " +
            "            name: \"passive-discovery-name\", " +
            "            locationId: \"Default\", " +
            "            snmpPorts: [ 161 ], " +
            "            snmpCommunities: [ \"public\" ], " +
            "            tags: [ " +
            "                { " +
            "                    name:\"tag-1\" " +
            "                } " +
            "            ] " +
            "        } " +
            "    ) { " +
            "        id, " +
            "        name, " +
            "        locationId, " +
            "        toggle, " +
            "        snmpPorts, " +
            "        snmpCommunities, " +
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
            .jsonPath("$.data.upsertPassiveDiscovery.id").isEqualTo(passiveDiscoveryDTO.getId())
            .jsonPath("$.data.upsertPassiveDiscovery.name").isEqualTo(passiveDiscoveryDTO.getName())
            .jsonPath("$.data.upsertPassiveDiscovery.toggle").isEqualTo(passiveDiscoveryDTO.getToggle())
            .jsonPath("$.data.upsertPassiveDiscovery.createTimeMsec").isEqualTo(passiveDiscoveryDTO.getCreateTimeMsec());
        verify(mockClient).upsertPassiveDiscovery(any(PassiveDiscoveryUpsertDTO.class), eq(accessToken));
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }
    
    @Test
    void testTogglePassiveDiscovery() throws JSONException {
        doReturn(passiveDiscoveryDTO).when(mockClient).createPassiveDiscoveryToggle((any(PassiveDiscoveryToggleDTO.class)), eq(accessToken));

        String request = createPayload(
            "mutation {"
                +"  togglePassiveDiscovery(toggle: { id: 1, toggle: true }) {"
                +"    toggle"
                +"  }"
                +"}");

        webClient.post()
            .uri(GRAPHQL_PATH)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.data.togglePassiveDiscovery.toggle").isEqualTo(passiveDiscoveryDTO.getToggle());

        verify(mockClient).createPassiveDiscoveryToggle(any(PassiveDiscoveryToggleDTO.class), eq(accessToken));
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    void testGetPassiveDiscovery() throws JSONException {
        doReturn(PassiveDiscoveryListDTO.newBuilder().addDiscoveries(passiveDiscoveryDTO).build())
            .when(mockClient).listPassiveDiscoveries(eq(accessToken));

        String request = createPayload("query { " +
            "    passiveDiscoveries { " +
            "        id, " +
            "        name, " +
            "        locationId, " +
            "        toggle, " +
            "        snmpPorts, " +
            "        snmpCommunities, " +
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
            .jsonPath("$.data.passiveDiscoveries[0].id").isEqualTo(passiveDiscoveryDTO.getId())
            .jsonPath("$.data.passiveDiscoveries[0].name").isEqualTo(passiveDiscoveryDTO.getName())
            .jsonPath("$.data.passiveDiscoveries[0].toggle").isEqualTo(passiveDiscoveryDTO.getToggle())
            .jsonPath("$.data.passiveDiscoveries[0].createTimeMsec").isEqualTo(passiveDiscoveryDTO.getCreateTimeMsec());
        verify(mockClient).listPassiveDiscoveries(eq(accessToken));
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    private String createPayload(String request) throws JSONException {
        return new JSONObject().put("query", request).toString();
    }
}
