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
