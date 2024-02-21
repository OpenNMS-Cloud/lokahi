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
package org.opennms.horizon.server.web;

import graphql.GraphQL;
import io.leangen.graphql.spqr.spring.web.GraphQLController;
import io.leangen.graphql.spqr.spring.web.dto.GraphQLRequest;
import io.leangen.graphql.spqr.spring.web.reactive.GraphQLReactiveExecutor;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.MethodNotAllowedException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;

/**
 * Replaces the {@link io.leangen.graphql.spqr.spring.web.reactive.DefaultGraphQLController},
 * disallowing unused but potentially insecure request mappings.
 */
@RestController
public class BffGraphQLController extends GraphQLController<ServerWebExchange> {

    /**
     * The configuration property and default value as specified in the base
     * {@link GraphQLController}.
     */
    public static final String GRAPHQL_ENDPOINT = "${graphql.spqr.http.endpoint:/graphql}";

    public BffGraphQLController(GraphQL graphQL, GraphQLReactiveExecutor executor) {
        super(graphQL, executor);
    }

    @Override
    public Object executeGet(GraphQLRequest graphQLRequest, ServerWebExchange request) {
        throw new MethodNotAllowedException(HttpMethod.GET, List.of(HttpMethod.POST));
    }

    @Override
    public Object executeGetEventStream(GraphQLRequest graphQLRequest, ServerWebExchange request) {
        throw new MethodNotAllowedException(HttpMethod.GET, List.of(HttpMethod.POST));
    }

    @Override
    public Object executeFormPost(
            Map<String, String> queryParams, GraphQLRequest graphQLRequest, ServerWebExchange request) {
        throw new UnsupportedMediaTypeStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE.getReasonPhrase());
    }
}
