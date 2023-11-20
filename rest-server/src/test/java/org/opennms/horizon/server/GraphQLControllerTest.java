/*
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
 */

package org.opennms.horizon.server;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.spqr.spring.web.GraphQLExecutor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opennms.horizon.inventory.dto.GeoLocation;
import org.opennms.horizon.inventory.dto.MonitoringLocationDTO;
import org.opennms.horizon.server.service.grpc.InventoryClient;
import org.opennms.horizon.server.test.util.GraphQLWebTestClient;
import org.opennms.horizon.server.utils.ServerHeaderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@Slf4j
@SpringBootTest(webEnvironment = RANDOM_PORT, classes = RestServerApplication.class)
public class GraphQLControllerTest {

    private static final String QUERY = """
        mutation {
            createLocation(location: {
                location: "foo",
                address: "bar",
                latitude: 0.0
                longitude: 0.0
            }) {
                id
                location
                latitude
                longitude
                address
            }
        }
        """;
    private static final MonitoringLocationDTO RESPONSE_DTO = MonitoringLocationDTO.newBuilder()
        .setId(5L)
        .setLocation("foo")
        .setAddress("bar")
        .setGeoLocation(GeoLocation.newBuilder()
            .setLatitude(0)
            .setLongitude(0)
            .build())
        .build();
    private static final String RESPONSE_DTO_JSON = """
        {
          "data": {
            "createLocation": {
              "id": 5,
              "location": "foo",
              "address": "bar",
              "longitude": 0.0,
              "latitude": 0.0
            }
          }
        }
        """;

    @MockBean
    private InventoryClient inventoryClient;

    @SpyBean
    @SuppressWarnings("rawtypes")
    private GraphQLExecutor graphQLExecutor;

    @MockBean
    private ServerHeaderUtil mockHeaderUtil;

    private GraphQLWebTestClient webClient;

    @BeforeEach
    void setUp(@Autowired WebTestClient webTestClient) {
        webClient = GraphQLWebTestClient.from(webTestClient);

        doReturn(webClient.getAccessToken())
            .when(mockHeaderUtil)
            .getAuthHeader(any(ResolutionEnvironment.class));
    }

    @Test
    void doesNotAllowRequestsViaGet() {
        webClient.exchangeGet(webClient.getEndpoint() + "?query={q}", Map.of("q", QUERY))
            .expectJsonResponse(HttpStatus.METHOD_NOT_ALLOWED)
            .jsonPath("$.error").isEqualTo("Method Not Allowed")
            .jsonPath("$.message").isEqualTo("Request method 'GET' is not supported.");
    }

    @ValueSource(strings = {
        MediaType.APPLICATION_XML_VALUE,
        MediaType.APPLICATION_FORM_URLENCODED_VALUE
    })
    @ParameterizedTest
    void doesNotAllowUnsupportedMediaTypes(String mediaType) {
        webClient
            .withContentType(MediaType.parseMediaType(mediaType))
            .exchangeGraphQLQuery(QUERY)
            .expectJsonResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
            .jsonPath("$.error").isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE.getReasonPhrase())
            .jsonPath("$.message").isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE.getReasonPhrase());
    }

    @Test
    void allowsPostRequests() {
        when(inventoryClient.createLocation(any(), any())).thenReturn(RESPONSE_DTO);

        webClient
            .exchangeGraphQLQuery(QUERY)
            .expectCleanResponse()
            .json(RESPONSE_DTO_JSON);
    }

    @ValueSource(strings = {
        "NONSENSE", "{\"query\":"
    })
    @ParameterizedTest
    void invalidRequestsBodyShouldReturn400Error(String requestBody) {
        webClient
            .exchangePost(requestBody)
            .expectJsonResponse(HttpStatus.BAD_REQUEST)
            .jsonPath("$.error").isEqualTo("Bad Request")
            .jsonPath("$.message").isEqualTo("Failed to read HTTP message");
    }

    @Test
    @SuppressWarnings("unchecked")
    void anUnexpectedWebLayerErrorDoesNotLeakInternalDetails() {
        doThrow(new RuntimeException("internal error details"))
            .when(graphQLExecutor).execute(any(), any());

        webClient
            .exchangeGraphQLQuery(QUERY)
            .expectJsonResponse(HttpStatus.INTERNAL_SERVER_ERROR)
            .jsonPath("$.error").isEqualTo("Internal Server Error")
            .jsonPath("$.message").isEqualTo("Internal Server Error");
    }

    @Test
    void anUnexpectedErrorDoesNotLeakInternalDetails() {
        when(inventoryClient.createLocation(any(), any()))
            .thenThrow(new RuntimeException("internal error details"));

        webClient
            .exchangeGraphQLQuery(QUERY)
            .expectJsonResponse(HttpStatus.OK)
            .jsonPath("$.errors").isArray()
            .jsonPath("$.errors[0].message").isEqualTo(
                "Exception while fetching data (/createLocation) : Internal Error"
            )
            .jsonPath("$.errors[0].path[0]").isEqualTo("createLocation");
    }

    @Test
    void anUnexpectedGRPCErrorDoesNotLeakInternalDetails() {
        when(inventoryClient.deleteLocation(anyLong(), anyString()))
            .thenThrow(new StatusRuntimeException(Status.DATA_LOSS));

        String query = """
            mutation {
                deleteLocation(id: 1)
            }
            """;

        webClient
            .exchangeGraphQLQuery(query)
            .expectJsonResponse(HttpStatus.OK)
            .jsonPath("$.errors").isArray()
            .jsonPath("$.errors[0].message").isEqualTo(
                "Exception while fetching data (/deleteLocation) : Internal Error"
            )
            .jsonPath("$.errors[0].path[0]").isEqualTo("deleteLocation");
    }
}
