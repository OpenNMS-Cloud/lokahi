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
package org.opennms.horizon.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.inventory.dto.GeoLocation;
import org.opennms.horizon.inventory.dto.MonitoringLocationDTO;
import org.opennms.horizon.server.service.grpc.InventoryClient;
import org.opennms.horizon.server.test.util.GraphQLWebTestClient;
import org.opennms.horizon.server.utils.ServerHeaderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@Slf4j
@SpringBootTest(webEnvironment = RANDOM_PORT, classes = RestServerApplication.class)
public class GraphQLAuthTest {
    private static final String QUERY =
            """
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
            .setGeoLocation(
                    GeoLocation.newBuilder().setLatitude(0).setLongitude(0).build())
            .build();

    private static final String RESPONSE_DTO_JSON =
            """
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

    @MockBean
    private ReactiveJwtDecoder mockJwtDecoder;

    @MockBean
    private ServerHeaderUtil mockHeaderUtil;

    private GraphQLWebTestClient webClient;

    @BeforeEach
    void setUp(@Autowired WebTestClient webTestClient) {
        webClient = GraphQLWebTestClient.from(webTestClient);
    }

    @Test
    void allowsQueriesIfAuthenticated() {
        when(mockJwtDecoder.decode(anyString()))
                .thenReturn(Mono.just(new Jwt(
                        webClient.getAccessToken(),
                        Instant.now().minus(5, ChronoUnit.MINUTES),
                        Instant.now().plus(1, ChronoUnit.HOURS),
                        // Headers and claims can not be empty, so provide a dummy entry
                        Map.of("a", "value"),
                        Map.of("a", "value"))));

        when(inventoryClient.createLocation(any(), any())).thenReturn(RESPONSE_DTO);

        webClient.exchangeGraphQLQuery(QUERY).expectCleanResponse().json(RESPONSE_DTO_JSON);
    }

    @Test
    void doesNotAllowQueriesIfAuthTokenNotProvided() {
        webClient
                .post()
                .uri(webClient.getEndpoint())
                .header(HttpHeaders.CONTENT_TYPE, webClient.getContentType().toString())
                .bodyValue(GraphQLWebTestClient.createPayload(QUERY))
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectBody()
                .isEmpty();
    }

    @Test
    void doesNotAllowQueriesIfAuthTokenExpired() {
        var exception = new JwtValidationException("Expired", List.of(new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN)));
        when(mockJwtDecoder.decode(anyString())).thenReturn(Mono.error(exception));

        webClient
                .post()
                .uri(webClient.getEndpoint())
                .header(HttpHeaders.CONTENT_TYPE, webClient.getContentType().toString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer expired-token")
                .bodyValue(GraphQLWebTestClient.createPayload(QUERY))
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectHeader()
                .value(HttpHeaders.WWW_AUTHENTICATE, s -> assertThat(s)
                        .isEqualTo(
                                """
                    Bearer error="invalid_token", error_description="Expired", error_uri="https://tools.ietf.org/html/rfc6750#section-3.1"
                    """
                                        .strip()))
                .expectBody()
                .isEmpty();
    }

    @Test
    void doesNotAllowQueriesIfAuthTokenInvalid() {
        when(mockJwtDecoder.decode(anyString())).thenThrow(new BadJwtException("bad"));

        webClient
                .post()
                .uri(webClient.getEndpoint())
                .header(HttpHeaders.CONTENT_TYPE, webClient.getContentType().toString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer badvalue")
                .bodyValue(GraphQLWebTestClient.createPayload(QUERY))
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectHeader()
                .value(HttpHeaders.WWW_AUTHENTICATE, s -> assertThat(s)
                        .isEqualTo(
                                """
                    Bearer error="invalid_token", error_description="bad", error_uri="https://tools.ietf.org/html/rfc6750#section-3.1"
                    """
                                        .strip()))
                .expectBody()
                .isEmpty();
    }

    @Test
    void anUnexpectedDecoderExceptionDoesNotLeakInternalDetails() {
        when(mockJwtDecoder.decode(anyString())).thenThrow(new IllegalStateException("Could not obtain the keys"));

        webClient
                .post()
                .uri(webClient.getEndpoint())
                .header(HttpHeaders.CONTENT_TYPE, webClient.getContentType().toString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer badvalue")
                .bodyValue(GraphQLWebTestClient.createPayload(QUERY))
                .exchange()
                .expectStatus()
                .is5xxServerError()
                .expectBody()
                .jsonPath("$.trace")
                .doesNotExist()
                .jsonPath("$.error")
                .isEqualTo("Internal Server Error")
                .jsonPath("$.message")
                .isEqualTo("Internal Server Error");
    }
}
