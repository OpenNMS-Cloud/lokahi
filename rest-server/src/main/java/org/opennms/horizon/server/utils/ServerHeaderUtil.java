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

package org.opennms.horizon.server.utils;

import com.nimbusds.jwt.SignedJWT;
import java.util.List;

import java.util.Optional;

import org.opennms.horizon.shared.constants.GrpcConstants;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import graphql.GraphQLContext;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.spqr.spring.autoconfigure.DefaultGlobalContext;
import io.leangen.graphql.util.ContextUtils;
import io.opentelemetry.api.trace.Span;

public class ServerHeaderUtil {
    private final JWTValidator validator;

    public ServerHeaderUtil(JWTValidator validator) {
        this.validator = validator;
    }

    public String getAuthHeader(ResolutionEnvironment env) {
        String authHeader = retrieveAuthHeader(env);
        try {
            if (authHeader != null) {
                // return token only if its valid
                validator.validate(authHeader.substring(7));
                try {
                    String tenantId = parseHeader(authHeader);
                    if (tenantId != null) {
                        var span = Span.current();
                        if (span.isRecording()) {
                            span.setAttribute("user", tenantId);
                        }
                    }
                } catch (Exception e) {
                    // ignore
                }
                return authHeader;
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Invalid token", e);
        }
    }

    public String extractTenant(ResolutionEnvironment env) {
        String header = getAuthHeader(env);
        return header != null ? parseHeader(header) : "opennms-prime";
    }
    private String retrieveAuthHeader(ResolutionEnvironment env) {
        GraphQLContext graphQLContext = env.dataFetchingEnvironment.getContext();
        DefaultGlobalContext context = (DefaultGlobalContext) ContextUtils.unwrapContext(graphQLContext);
        ServerWebExchange webExchange = (ServerWebExchange) context.getNativeRequest();
        ServerHttpRequest request = webExchange.getRequest();
        List<String> authHeaders = request.getHeaders().get(HttpHeaders.AUTHORIZATION);
        return authHeaders != null? authHeaders.get(0): null;
    }

    private static String parseHeader(String header) {
        try {
            SignedJWT jwt = SignedJWT.parse(header.substring(7));
            return Optional.ofNullable(jwt.getJWTClaimsSet().getStringClaim(GrpcConstants.TENANT_ID_KEY))
                .orElseThrow();
        } catch (Exception e) {
            throw new RuntimeException("Could not extract tenant information", e);
        }
    }
}
