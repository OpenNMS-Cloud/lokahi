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

package org.opennms.horizon.server.test.util;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.With;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.CookieAssertions;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.HeaderAssertions;
import org.springframework.test.web.reactive.server.StatusAssertions;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

@Builder
@Getter
@Slf4j
@With
public class GraphQLWebTestClient {

    private final WebTestClient webClient;

    @Builder.Default
    private final String endpoint = "/graphql";

    @Builder.Default
    private final MediaType contentType = MediaType.APPLICATION_JSON;

    @Builder.Default
    private final String accessToken = "test-token-12345";

    public static GraphQLWebTestClient from(WebTestClient webTestClient) {
        return GraphQLWebTestClient.builder().webClient(webTestClient).build();
    }

    public static String createPayload(String graphQLQuery) {
        try {
            return new JSONObject().put("query", graphQLQuery).toString();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Takes a graphql query, creates a json request body for it, and submits a
     * POST request for it.
     *
     * @see GraphQLWebTestClient#createPayload(String)
     */
    public GraphQLResponseSpec exchangeGraphQLQuery(String query) {
        return this.exchangePost(createPayload(query));
    }

    public GraphQLResponseSpec exchangePost(String body) {
        WebTestClient.ResponseSpec base = webClient.post()
            .uri(endpoint)
            .header(HttpHeaders.CONTENT_TYPE, contentType.toString())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .bodyValue(body)
            .exchange();
        return new GraphQLResponseSpec(base);
    }

    public GraphQLResponseSpec exchangeGet(String uri, Map<String, ?> uriVariables) {
        WebTestClient.ResponseSpec base = webClient.get()
            .uri(uri, uriVariables)
            .header(HttpHeaders.CONTENT_TYPE, contentType.toString())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .exchange();
        return new GraphQLResponseSpec(base);
    }

    public WebTestClient.RequestHeadersUriSpec<?> get() {
        return webClient.get();
    }

    public WebTestClient.RequestBodyUriSpec post() {
        return webClient.post();
    }

    @RequiredArgsConstructor
    public static class GraphQLResponseSpec implements WebTestClient.ResponseSpec {

        private final WebTestClient.ResponseSpec delegate;

        public @NonNull WebTestClient.BodyContentSpec expectCleanResponse() {
            return this.expectStatus().isEqualTo(HttpStatus.OK)
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .consumeWith(this::logBody)
                .jsonPath("$.errors").doesNotExist()
                .jsonPath("$.trace").doesNotExist()
                .jsonPath("$.data").exists();
        }

        public @NonNull WebTestClient.BodyContentSpec expectJsonResponse() {
            return this.expectJsonResponse(HttpStatus.OK);
        }

        public @NonNull WebTestClient.BodyContentSpec expectJsonResponse(HttpStatus status) {
            return this.expectStatus().isEqualTo(status)
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .consumeWith(this::logBody)
                .jsonPath("$.trace").doesNotExist();
        }

        public @NonNull WebTestClient.BodyContentSpec expectGraphQLErrorResponse() {
            return this.expectStatus().isEqualTo(HttpStatus.OK)
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .consumeWith(this::logBody)
                .jsonPath("$.data").doesNotExist()
                .jsonPath("$.trace").doesNotExist()
                .jsonPath("$.errors").isArray();
        }

        public void logBody(EntityExchangeResult<byte[]> rawBody) {
            String body = rawBody.getResponseBody() == null
                ? null
                : new String(rawBody.getResponseBody());
            log.info("Response Body: {}", body);
        }

        @Override
        public WebTestClient.ResponseSpec expectAll(ResponseSpecConsumer... consumers) {
            return delegate.expectAll(consumers);
        }

        @Override
        public @NonNull StatusAssertions expectStatus() {
            return delegate.expectStatus();
        }

        @Override
        public @NonNull HeaderAssertions expectHeader() {
            return delegate.expectHeader();
        }

        @Override
        public @NonNull CookieAssertions expectCookie() {
            return delegate.expectCookie();
        }

        @Override
        public @NonNull <B> WebTestClient.BodySpec<B, ?> expectBody(Class<B> bodyType) {
            return delegate.expectBody(bodyType);
        }

        @Override
        public @NonNull <B> WebTestClient.BodySpec<B, ?> expectBody(ParameterizedTypeReference<B> bodyType) {
            return delegate.expectBody(bodyType);
        }

        @Override
        public @NonNull <E> WebTestClient.ListBodySpec<E> expectBodyList(Class<E> elementType) {
            return delegate.expectBodyList(elementType);
        }

        @Override
        public @NonNull <E> WebTestClient.ListBodySpec<E> expectBodyList(ParameterizedTypeReference<E> elementType) {
            return delegate.expectBodyList(elementType);
        }

        @Override
        public @NonNull WebTestClient.BodyContentSpec expectBody() {
            return delegate.expectBody();
        }

        @Override
        public @NonNull <T> FluxExchangeResult<T> returnResult(Class<T> elementClass) {
            return delegate.returnResult(elementClass);
        }

        @Override
        public @NonNull <T> FluxExchangeResult<T> returnResult(ParameterizedTypeReference<T> elementTypeRef) {
            return delegate.returnResult(elementTypeRef);
        }
    }
}
