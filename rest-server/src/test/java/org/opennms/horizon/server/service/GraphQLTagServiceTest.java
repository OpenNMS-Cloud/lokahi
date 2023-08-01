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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.inventory.dto.TagCreateListDTO;
import org.opennms.horizon.inventory.dto.TagDTO;
import org.opennms.horizon.inventory.dto.TagListDTO;
import org.opennms.horizon.inventory.dto.TagRemoveListDTO;
import org.opennms.horizon.server.RestServerApplication;
import org.opennms.horizon.server.service.grpc.InventoryClient;
import org.opennms.horizon.server.utils.ServerHeaderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT, classes = RestServerApplication.class)
class GraphQLTagServiceTest {
    private static final String GRAPHQL_PATH = "/graphql";
    public static final String TEST_TAG_NAME_1 = "tag-name-1";
    public static final String TEST_TAG_NAME_2 = "tag-name-2";
    public static final String TEST_TENANT_ID = "tenant-id";
    @MockBean
    private InventoryClient mockClient;
    @Autowired
    private WebTestClient webClient;
    @MockBean
    private ServerHeaderUtil mockHeaderUtil;
    private final String accessToken = "test-token-12345";

    @BeforeEach
    public void setUp() {
        doReturn(accessToken).when(mockHeaderUtil).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    void testAddTagsToNodes() throws JSONException {

        TagDTO tagDTO1 = TagDTO.newBuilder().setName(TEST_TAG_NAME_1).setTenantId(TEST_TENANT_ID).setId(1L).build();
        TagDTO tagDTO2 = TagDTO.newBuilder().setName(TEST_TAG_NAME_2).setTenantId(TEST_TENANT_ID).setId(2L).build();
        TagListDTO tagListDTO = TagListDTO.newBuilder().addTags(tagDTO1).addTags(tagDTO2).build();
        when(mockClient.addTags(any(TagCreateListDTO.class), anyString())).thenReturn(tagListDTO);

        String request = "mutation { " +
            "    addTagsToNodes( " +
            "        tags: { " +
            "            nodeIds: [ 1 ], " +
            "            tags: [ " +
            "                { " +
            "                    name: \"" + TEST_TAG_NAME_1 + "\" " +
            "                }, " +
            "                { " +
            "                    name: \"" + TEST_TAG_NAME_2 + "\" " +
            "                } " +
            "            ] " +
            "        } " +
            "    ) { " +
            "        id, " +
            "        name " +
            "    } " +
            "}";
        webClient.post()
            .uri(GRAPHQL_PATH)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createPayload(request))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.data.addTagsToNodes[0].id").isEqualTo(1)
            .jsonPath("$.data.addTagsToNodes[0].name").isEqualTo(TEST_TAG_NAME_1)
            .jsonPath("$.data.addTagsToNodes[1].id").isEqualTo(2)
            .jsonPath("$.data.addTagsToNodes[1].name").isEqualTo(TEST_TAG_NAME_2);

        verify(mockClient).addTags(any(TagCreateListDTO.class), eq(accessToken));
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    void testGetTagsFromNode() throws JSONException {

        TagDTO tagDTO1 = TagDTO.newBuilder().setName(TEST_TAG_NAME_1).setTenantId(TEST_TENANT_ID).setId(1L).build();
        TagDTO tagDTO2 = TagDTO.newBuilder().setName(TEST_TAG_NAME_2).setTenantId(TEST_TENANT_ID).setId(2L).build();
        TagListDTO tagListDTO = TagListDTO.newBuilder().addTags(tagDTO1).addTags(tagDTO2).build();
        when(mockClient.getTagsByNodeId(anyLong(), any(), anyString())).thenReturn(tagListDTO);

        String getRequest = "query { " +
            "    tagsByNodeId (nodeId: 1) { " +
            "        id, " +
            "        name " +
            "    }" +
            "}";
        webClient.post()
            .uri(GRAPHQL_PATH)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createPayload(getRequest))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.data.tagsByNodeId[0].id").isEqualTo(1)
            .jsonPath("$.data.tagsByNodeId[0].name").isEqualTo(TEST_TAG_NAME_1)
            .jsonPath("$.data.tagsByNodeId[1].id").isEqualTo(2)
            .jsonPath("$.data.tagsByNodeId[1].name").isEqualTo(TEST_TAG_NAME_2);

        verify(mockClient, times(1)).getTagsByNodeId(1L, null, accessToken);
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    void testGetTagsFromNodes() throws JSONException {
        TagDTO tagDTO1 = TagDTO.newBuilder().setName(TEST_TAG_NAME_1).setTenantId(TEST_TENANT_ID).setId(1L).build();
        TagDTO tagDTO2 = TagDTO.newBuilder().setName(TEST_TAG_NAME_2).setTenantId(TEST_TENANT_ID).setId(2L).build();
        TagListDTO tagListDTO = TagListDTO.newBuilder().addTags(tagDTO1).addTags(tagDTO2).build();
        when(mockClient.getTagsByNodeId(anyLong(), anyString())).thenReturn(tagListDTO);

        String getRequest = "query { " +
            "    tagsByNodeIds (nodeIds: [1, 2]) { " +
            "        nodeId, " +
            "        tags { " +
            "            id, " +
            "            name " +
            "        } " +
            "    } " +
            "}";
        webClient.post()
            .uri(GRAPHQL_PATH)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createPayload(getRequest))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.data.tagsByNodeIds[0].nodeId").isEqualTo(1)
            .jsonPath("$.data.tagsByNodeIds[0].tags.size()").isEqualTo(2)
            .jsonPath("$.data.tagsByNodeIds[0].tags[0].name").isEqualTo(TEST_TAG_NAME_1)
            .jsonPath("$.data.tagsByNodeIds[0].tags[1].name").isEqualTo(TEST_TAG_NAME_2)
            .jsonPath("$.data.tagsByNodeIds[1].nodeId").isEqualTo(2)
            .jsonPath("$.data.tagsByNodeIds[1].tags.size()").isEqualTo(2)
            .jsonPath("$.data.tagsByNodeIds[1].tags[0].name").isEqualTo(TEST_TAG_NAME_1)
            .jsonPath("$.data.tagsByNodeIds[1].tags[1].name").isEqualTo(TEST_TAG_NAME_2);

        verify(mockClient, times(1)).getTagsByNodeId(1L, accessToken);
        verify(mockClient, times(1)).getTagsByNodeId(2L, accessToken);
        verify(mockHeaderUtil, times(2)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    void testGetTagsFromNodeWithSearchTerm() throws JSONException {

        TagDTO tagDTO1 = TagDTO.newBuilder().setName(TEST_TAG_NAME_1).setTenantId(TEST_TENANT_ID).setId(1L).build();
        TagDTO tagDTO2 = TagDTO.newBuilder().setName(TEST_TAG_NAME_2).setTenantId(TEST_TENANT_ID).setId(2L).build();
        TagListDTO tagListDTO = TagListDTO.newBuilder().addTags(tagDTO1).addTags(tagDTO2).build();
        when(mockClient.getTagsByNodeId(anyLong(), anyString(), anyString())).thenReturn(tagListDTO);

        String getRequest = "query { " +
            "    tagsByNodeId (nodeId: 1, searchTerm: \"abc\") { " +
            "        id, " +
            "        name " +
            "    }" +
            "}";
        webClient.post()
            .uri(GRAPHQL_PATH)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createPayload(getRequest))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.data.tagsByNodeId[0].id").isEqualTo(1)
            .jsonPath("$.data.tagsByNodeId[0].name").isEqualTo(TEST_TAG_NAME_1)
            .jsonPath("$.data.tagsByNodeId[1].id").isEqualTo(2)
            .jsonPath("$.data.tagsByNodeId[1].name").isEqualTo(TEST_TAG_NAME_2);

        verify(mockClient, times(1)).getTagsByNodeId(1L, "abc", accessToken);
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    void testGetTagsFromActiveDiscovery() throws JSONException {

        TagDTO tagDTO1 = TagDTO.newBuilder().setName(TEST_TAG_NAME_1).setTenantId(TEST_TENANT_ID).setId(1L).build();
        TagDTO tagDTO2 = TagDTO.newBuilder().setName(TEST_TAG_NAME_2).setTenantId(TEST_TENANT_ID).setId(2L).build();
        TagListDTO tagListDTO = TagListDTO.newBuilder().addTags(tagDTO1).addTags(tagDTO2).build();
        when(mockClient.getTagsByActiveDiscoveryId(anyLong(), any(), anyString())).thenReturn(tagListDTO);

        String getRequest = "query { " +
            "    tagsByActiveDiscoveryId (activeDiscoveryId: 1) { " +
            "        id, " +
            "        name " +
            "    }" +
            "}";
        webClient.post()
            .uri(GRAPHQL_PATH)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createPayload(getRequest))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.data.tagsByActiveDiscoveryId[0].id").isEqualTo(1)
            .jsonPath("$.data.tagsByActiveDiscoveryId[0].name").isEqualTo(TEST_TAG_NAME_1)
            .jsonPath("$.data.tagsByActiveDiscoveryId[1].id").isEqualTo(2)
            .jsonPath("$.data.tagsByActiveDiscoveryId[1].name").isEqualTo(TEST_TAG_NAME_2);

        verify(mockClient, times(1)).getTagsByActiveDiscoveryId(1L, null, accessToken);
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    void testGetTagsFromActiveDiscoveryWithSearchTerm() throws JSONException {

        TagDTO tagDTO1 = TagDTO.newBuilder().setName(TEST_TAG_NAME_1).setTenantId(TEST_TENANT_ID).setId(1L).build();
        TagDTO tagDTO2 = TagDTO.newBuilder().setName(TEST_TAG_NAME_2).setTenantId(TEST_TENANT_ID).setId(2L).build();
        TagListDTO tagListDTO = TagListDTO.newBuilder().addTags(tagDTO1).addTags(tagDTO2).build();
        when(mockClient.getTagsByActiveDiscoveryId(anyLong(), anyString(), anyString())).thenReturn(tagListDTO);

        String getRequest = "query { " +
            "    tagsByActiveDiscoveryId (activeDiscoveryId: 1, searchTerm: \"abc\") { " +
            "        id, " +
            "        name " +
            "    }" +
            "}";
        webClient.post()
            .uri(GRAPHQL_PATH)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createPayload(getRequest))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.data.tagsByActiveDiscoveryId[0].id").isEqualTo(1)
            .jsonPath("$.data.tagsByActiveDiscoveryId[0].name").isEqualTo(TEST_TAG_NAME_1)
            .jsonPath("$.data.tagsByActiveDiscoveryId[1].id").isEqualTo(2)
            .jsonPath("$.data.tagsByActiveDiscoveryId[1].name").isEqualTo(TEST_TAG_NAME_2);

        verify(mockClient, times(1)).getTagsByActiveDiscoveryId(1L, "abc", accessToken);
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    void testGetTagsFromPassiveDiscovery() throws JSONException {

        TagDTO tagDTO1 = TagDTO.newBuilder().setName(TEST_TAG_NAME_1).setTenantId(TEST_TENANT_ID).setId(1L).build();
        TagDTO tagDTO2 = TagDTO.newBuilder().setName(TEST_TAG_NAME_2).setTenantId(TEST_TENANT_ID).setId(2L).build();
        TagListDTO tagListDTO = TagListDTO.newBuilder().addTags(tagDTO1).addTags(tagDTO2).build();
        when(mockClient.getTagsByPassiveDiscoveryId(anyLong(), any(), anyString())).thenReturn(tagListDTO);

        String getRequest = "query { " +
            "    tagsByPassiveDiscoveryId (passiveDiscoveryId: 1) { " +
            "        id, " +
            "        name " +
            "    }" +
            "}";
        webClient.post()
            .uri(GRAPHQL_PATH)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createPayload(getRequest))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.data.tagsByPassiveDiscoveryId[0].id").isEqualTo(1)
            .jsonPath("$.data.tagsByPassiveDiscoveryId[0].name").isEqualTo(TEST_TAG_NAME_1)
            .jsonPath("$.data.tagsByPassiveDiscoveryId[1].id").isEqualTo(2)
            .jsonPath("$.data.tagsByPassiveDiscoveryId[1].name").isEqualTo(TEST_TAG_NAME_2);

        verify(mockClient, times(1)).getTagsByPassiveDiscoveryId(1L, null, accessToken);
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    void testGetTagsFromPassiveDiscoveryWithSearchTerm() throws JSONException {

        TagDTO tagDTO1 = TagDTO.newBuilder().setName(TEST_TAG_NAME_1).setTenantId(TEST_TENANT_ID).setId(1L).build();
        TagDTO tagDTO2 = TagDTO.newBuilder().setName(TEST_TAG_NAME_2).setTenantId(TEST_TENANT_ID).setId(2L).build();
        TagListDTO tagListDTO = TagListDTO.newBuilder().addTags(tagDTO1).addTags(tagDTO2).build();
        when(mockClient.getTagsByPassiveDiscoveryId(anyLong(), anyString(), anyString())).thenReturn(tagListDTO);

        String getRequest = "query { " +
            "    tagsByPassiveDiscoveryId (passiveDiscoveryId: 1, searchTerm: \"abc\") { " +
            "        id, " +
            "        name " +
            "    }" +
            "}";
        webClient.post()
            .uri(GRAPHQL_PATH)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createPayload(getRequest))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.data.tagsByPassiveDiscoveryId[0].id").isEqualTo(1)
            .jsonPath("$.data.tagsByPassiveDiscoveryId[0].name").isEqualTo(TEST_TAG_NAME_1)
            .jsonPath("$.data.tagsByPassiveDiscoveryId[1].id").isEqualTo(2)
            .jsonPath("$.data.tagsByPassiveDiscoveryId[1].name").isEqualTo(TEST_TAG_NAME_2);

        verify(mockClient, times(1)).getTagsByPassiveDiscoveryId(1L, "abc", accessToken);
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    void testGetTags() throws JSONException {

        TagDTO tagDTO1 = TagDTO.newBuilder().setName(TEST_TAG_NAME_1).setTenantId(TEST_TENANT_ID).setId(1L).build();
        TagDTO tagDTO2 = TagDTO.newBuilder().setName(TEST_TAG_NAME_2).setTenantId(TEST_TENANT_ID).setId(2L).build();
        TagListDTO tagListDTO = TagListDTO.newBuilder().addTags(tagDTO1).addTags(tagDTO2).build();
        when(mockClient.getTags(any(), anyString())).thenReturn(tagListDTO);

        String getRequest = "query { " +
            "    tags { " +
            "        id, " +
            "        name " +
            "    }" +
            "}";
        webClient.post()
            .uri(GRAPHQL_PATH)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createPayload(getRequest))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.data.tags[0].id").isEqualTo(1)
            .jsonPath("$.data.tags[0].name").isEqualTo(TEST_TAG_NAME_1)
            .jsonPath("$.data.tags[1].id").isEqualTo(2)
            .jsonPath("$.data.tags[1].name").isEqualTo(TEST_TAG_NAME_2);

        verify(mockClient, times(1)).getTags(null, accessToken);
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    void testGetTagsWithSearchTerm() throws JSONException {

        TagDTO tagDTO1 = TagDTO.newBuilder().setName(TEST_TAG_NAME_1).setTenantId(TEST_TENANT_ID).setId(1L).build();
        TagDTO tagDTO2 = TagDTO.newBuilder().setName(TEST_TAG_NAME_2).setTenantId(TEST_TENANT_ID).setId(2L).build();
        TagListDTO tagListDTO = TagListDTO.newBuilder().addTags(tagDTO1).addTags(tagDTO2).build();
        when(mockClient.getTags(anyString(), anyString())).thenReturn(tagListDTO);

        String getRequest = "query { " +
            "    tags (searchTerm: \"abc\") { " +
            "        id, " +
            "        name " +
            "    }" +
            "}";
        webClient.post()
            .uri(GRAPHQL_PATH)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createPayload(getRequest))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.data.tags[0].id").isEqualTo(1)
            .jsonPath("$.data.tags[0].name").isEqualTo(TEST_TAG_NAME_1)
            .jsonPath("$.data.tags[1].id").isEqualTo(2)
            .jsonPath("$.data.tags[1].name").isEqualTo(TEST_TAG_NAME_2);

        verify(mockClient, times(1)).getTags("abc", accessToken);
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    void testRemoveTagsFromNode() throws JSONException {
        String request = "mutation { " +
            "    removeTagsFromNodes( " +
            "        tags: { " +
            "            nodeIds: [ 1 ], " +
            "            tagIds: [ 1 ] " +
            "        } " +
            "    ) " +
            "}";
        webClient.post()
            .uri(GRAPHQL_PATH)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(createPayload(request))
            .exchange()
            .expectStatus().isOk()
            .expectBody();

        verify(mockClient).removeTags(any(TagRemoveListDTO.class), eq(accessToken));
        verify(mockHeaderUtil, times(1)).getAuthHeader(any(ResolutionEnvironment.class));
    }

    private String createPayload(String request) throws JSONException {
        return new JSONObject().put("query", request).toString();
    }
}
