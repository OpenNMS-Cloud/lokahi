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

package org.opennms.horizon.server.service.gateway;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;

import graphql.GraphQLContext;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.spqr.spring.autoconfigure.DefaultGlobalContext;
import io.leangen.graphql.util.ContextUtils;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public abstract class GatewayBase {
    private final WebClient webclient;

    public GatewayBase(String baseUrl) {
        webclient = WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    public String getAuthHeader(ResolutionEnvironment env) {
        GraphQLContext graphQLContext = env.dataFetchingEnvironment.getContext();
        DefaultGlobalContext context = (DefaultGlobalContext) ContextUtils.unwrapContext(graphQLContext);
        ServerWebExchange webExchange = (ServerWebExchange) context.getNativeRequest();
        ServerHttpRequest request = webExchange.getRequest();
        return request.getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
    }

    public <T> Mono<T> post(String path, String authToken, Object data, Class<T> returnType) {
        return executeRequest(path, HttpMethod.POST, authToken, data, returnType);
    }

    public <T> Mono<T> get(String path, String authToken, Class<T> returnType) {
        return executeRequest(path, HttpMethod.GET, authToken, null, returnType);
    }

    public <T> Mono<T> put(String path, String authToken, Object data, Class<T> returnType) {
        return executeRequest(path, HttpMethod.PUT, authToken, data, returnType);
    }

    private <T> Mono <T> executeRequest(String path, HttpMethod method, String token, Object data, Class<T> type) {
        WebClient.RequestBodySpec request = webclient
            .method(method)
            .uri(path)
            .header(HttpHeaders.AUTHORIZATION, token);
        if(data !=null) {
            request.contentType(MediaType.APPLICATION_JSON).bodyValue(data);
        }
        return request.retrieve().bodyToMono(type);
    }
}
