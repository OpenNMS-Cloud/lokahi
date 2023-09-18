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

package org.opennms.horizon.server.config;

import graphql.GraphQL;
import graphql.analysis.MaxQueryComplexityInstrumentation;
import graphql.analysis.MaxQueryDepthInstrumentation;
import graphql.execution.DataFetcherExceptionHandler;
import graphql.execution.instrumentation.Instrumentation;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.GraphQLRuntime;
import io.leangen.graphql.spqr.spring.autoconfigure.SpqrProperties;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.server.service.graphql.BffDataFetchExceptionHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.List;

/**
 * Provides fine-tuned configuration for GraphQL.
 *
 * @see io.leangen.graphql.spqr.spring.autoconfigure.BaseAutoConfiguration
 */
@Configuration
@Slf4j
public class GraphqlConfig {

    @Bean
    @ConditionalOnExpression("${lokahi.bff.max-query-depth:-1} > 1")
    @Order(1)
    public Instrumentation maxDepthInstrumentation(
        BffProperties bffProperties
    ) {
        log.info("Limiting max query depth to {}", bffProperties.getMaxQueryDepth());
        return new MaxQueryDepthInstrumentation(bffProperties.getMaxQueryDepth());
    }

    @Bean
    @ConditionalOnExpression("${graphql.spqr.max-complexity:-1} > 1")
    @Order(2)
    public Instrumentation maxComplexityInstrumentation(
        SpqrProperties spqrProperties
    ) {
        log.info("Limiting max query complexity to {}", spqrProperties.getMaxComplexity());
        return new MaxQueryComplexityInstrumentation(spqrProperties.getMaxComplexity());
    }

    @Bean
    public DataFetcherExceptionHandler exceptionResolver() {
        return new BffDataFetchExceptionHandler();
    }

    @Bean
    public GraphQL graphQL(
        GraphQLSchema schema,
        List<Instrumentation> instrumentations,
        DataFetcherExceptionHandler exceptionResolver
    ) {
        log.info("Configured Instrumentations: {}", instrumentations);

        GraphQLRuntime.Builder builder = GraphQLRuntime.newGraphQL(schema);
        instrumentations.forEach(builder::instrumentation);
        builder.defaultDataFetcherExceptionHandler(exceptionResolver);

        return builder.build();
    }
}
