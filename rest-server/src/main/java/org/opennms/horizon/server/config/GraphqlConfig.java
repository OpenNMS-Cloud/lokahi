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
package org.opennms.horizon.server.config;

import graphql.GraphQL;
import graphql.execution.DataFetcherExceptionHandler;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.fieldvalidation.FieldValidationInstrumentation;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.GraphQLRuntime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.server.service.graphql.BffDataFetchExceptionHandler;
import org.opennms.horizon.server.service.graphql.DuplicateFieldValidation;
import org.opennms.horizon.server.service.graphql.ExecutionTimingInstrumentation;
import org.opennms.horizon.server.service.graphql.IntrospectionDisabler;
import org.opennms.horizon.server.service.graphql.MaxAliasOccurrenceValidation;
import org.opennms.horizon.server.service.graphql.MaxComplexityInstrumentation;
import org.opennms.horizon.server.service.graphql.MaxDepthInstrumentation;
import org.opennms.horizon.server.service.graphql.MaxDirectiveOccurrenceInstrumentation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * Provides fine-tuned configuration for GraphQL.
 *
 * @see io.leangen.graphql.spqr.spring.autoconfigure.BaseAutoConfiguration
 */
@Configuration
@Slf4j
public class GraphqlConfig {

    @Bean
    @Order(1)
    public Instrumentation timingInstrumentation() {
        return new ExecutionTimingInstrumentation();
    }

    @Bean
    @ConditionalOnExpression("${lokahi.bff.max-query-depth:-1} > 1")
    @Order(2)
    public Instrumentation maxDepthInstrumentation(BffProperties properties) {
        log.info("Limiting max query depth to {}", properties.getMaxQueryDepth());
        return new MaxDepthInstrumentation(properties.getMaxQueryDepth());
    }

    @Bean
    @ConditionalOnExpression("${lokahi.bff.max-complexity:-1} > 1")
    @Order(3)
    public Instrumentation maxComplexityInstrumentation(BffProperties properties) {
        log.info("Limiting max query complexity to {}", properties.getMaxComplexity());
        return new MaxComplexityInstrumentation(properties.getMaxComplexity());
    }

    @Bean
    @ConditionalOnExpression("${lokahi.bff.max-directive-occurrence:-1} > 0")
    @ConditionalOnBean
    @Order(4)
    public Instrumentation maxDirectiveOccurrenceInstrumentation(BffProperties properties) {
        log.info("Limiting directive occurrences to {} or less", properties.getMaxDirectiveOccurrence());
        return new MaxDirectiveOccurrenceInstrumentation(properties.getMaxDirectiveOccurrence());
    }

    @Bean
    @ConditionalOnExpression("${lokahi.bff.max-alias-occurrence:-1} > 0")
    @Order(5)
    public Instrumentation maxAliasOccurrenceInstrumentation(BffProperties properties) {
        log.info("Limiting alias occurrences to {} or less", properties.getMaxAliasOccurrence());
        return new FieldValidationInstrumentation(new MaxAliasOccurrenceValidation(properties.getMaxAliasOccurrence()));
    }

    @Bean
    @ConditionalOnExpression("${lokahi.bff.max-field-occurrence:-1} > 0")
    @Order(6)
    public Instrumentation fieldDuplicationInstrumentation(BffProperties properties) {
        log.info("Limiting field occurrences to {} or less", properties.getMaxFieldOccurrence());
        return new FieldValidationInstrumentation(new DuplicateFieldValidation(properties.getMaxFieldOccurrence()));
    }

    @Bean
    public DataFetcherExceptionHandler exceptionResolver() {
        return new BffDataFetchExceptionHandler();
    }

    @Bean
    @ConditionalOnExpression("${lokahi.bff.introspection-enabled:false} == false")
    public IntrospectionDisabler introspectionDisabler() {
        log.info("Disabling introspection in graphql");
        return new IntrospectionDisabler();
    }

    @Bean
    public GraphQL graphQL(
            GraphQLSchema schema,
            List<Instrumentation> instrumentations,
            DataFetcherExceptionHandler exceptionResolver) {
        if (log.isInfoEnabled()) {
            log.info(
                    "Configured Instrumentations: {}",
                    instrumentations.stream()
                            .map(i -> i.getClass().getSimpleName())
                            .toList());
        }

        GraphQLRuntime.Builder builder = GraphQLRuntime.newGraphQL(schema);
        instrumentations.forEach(builder::instrumentation);
        builder.defaultDataFetcherExceptionHandler(exceptionResolver);

        return builder.build();
    }
}
