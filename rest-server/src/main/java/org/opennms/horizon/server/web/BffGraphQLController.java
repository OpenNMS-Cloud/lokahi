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

package org.opennms.horizon.server.web;

import graphql.GraphQL;
import io.leangen.graphql.spqr.spring.web.GraphQLController;
import io.leangen.graphql.spqr.spring.web.dto.GraphQLRequest;
import io.leangen.graphql.spqr.spring.web.reactive.GraphQLReactiveExecutor;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.MethodNotAllowedException;
import org.springframework.web.server.ServerWebExchange;

import java.util.List;

/**
 * Replaces the {@link io.leangen.graphql.spqr.spring.web.reactive.DefaultGraphQLController},
 * disallowing GET requests from being processed.
 */
@RestController
public class BffGraphQLController extends GraphQLController<ServerWebExchange> {

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
}
