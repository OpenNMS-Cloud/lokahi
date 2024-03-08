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
package org.opennms.horizon.server.utils;

import com.nimbusds.jwt.SignedJWT;
import graphql.GraphQLContext;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.spqr.spring.autoconfigure.DefaultGlobalContext;
import io.leangen.graphql.util.ContextUtils;
import io.opentelemetry.api.trace.Span;
import java.util.List;
import java.util.Optional;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

public class ServerHeaderUtil {

    public String getAuthHeader(ResolutionEnvironment env) {
        String authHeader = retrieveAuthHeader(env);
        try {
            if (authHeader != null) {
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
        return header != null ? parseHeader(header) : null;
    }

    private String retrieveAuthHeader(ResolutionEnvironment env) {
        GraphQLContext graphQLContext = env.dataFetchingEnvironment.getContext();
        DefaultGlobalContext context = ContextUtils.unwrapContext(graphQLContext);
        ServerWebExchange webExchange = (ServerWebExchange) context.getNativeRequest();
        ServerHttpRequest request = webExchange.getRequest();
        List<String> authHeaders = request.getHeaders().get(HttpHeaders.AUTHORIZATION);
        return authHeaders != null ? authHeaders.get(0) : null;
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
