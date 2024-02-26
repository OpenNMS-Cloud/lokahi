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
package org.opennms.horizon.server.service.graphql;

import graphql.schema.GraphQLSchema;
import graphql.schema.visibility.NoIntrospectionGraphqlFieldVisibility;
import io.leangen.graphql.GraphQLSchemaGenerator;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.spqr.spring.autoconfigure.BaseAutoConfiguration;
import io.leangen.graphql.spqr.spring.autoconfigure.SpqrProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * Takes the previously auto-configured {@link GraphQLSchemaGenerator}
 * instance and additionally configures it to disable introspection. This is
 * done to re-use the relatively complex original autoconfiguration instead
 * of replacing it.
 *
 * @see BaseAutoConfiguration#graphQLSchemaGenerator(SpqrProperties)
 */
@Slf4j
@RequiredArgsConstructor
public class IntrospectionDisabler implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof GraphQLSchemaGenerator schemaGenerator) {
            log.info("Updating GraphQLSchemaGenerator to disable introspection");
            schemaGenerator.withSchemaProcessors(this::disableIntrospection);
        }

        return bean;
    }

    private GraphQLSchema.Builder disableIntrospection(GraphQLSchema.Builder schemaBuilder, BuildContext buildContext) {
        buildContext.codeRegistry.fieldVisibility(
                NoIntrospectionGraphqlFieldVisibility.NO_INTROSPECTION_FIELD_VISIBILITY);
        schemaBuilder.codeRegistry(buildContext.codeRegistry.build());
        return schemaBuilder;
    }
}
