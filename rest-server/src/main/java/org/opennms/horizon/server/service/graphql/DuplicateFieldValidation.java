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

import graphql.GraphQLError;
import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.execution.instrumentation.fieldvalidation.FieldValidation;
import graphql.execution.instrumentation.fieldvalidation.FieldValidationEnvironment;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLFieldDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
public class DuplicateFieldValidation implements FieldValidation {

    private final int maxFieldOccurrence;

    @Override
    public List<GraphQLError> validateFields(FieldValidationEnvironment environment) {
        Map<FieldCoordinates, Integer> occurrences = Traversers
            .queryTraverser(environment)
            .reducePreOrder(this::reduceField, new LinkedHashMap<>());

        return occurrences
            .entrySet().stream()
            .filter(entry -> entry.getValue() > maxFieldOccurrence)
            .map(Map.Entry::getKey)
            .map(field -> environment.mkError(
                "Validation error: Field '" + field + "' is repeated too many times"
            ))
            .collect(Collectors.toList());
    }

    public Map<FieldCoordinates, Integer> reduceField(
        QueryVisitorFieldEnvironment env,
        Map<FieldCoordinates, Integer> acc
    ) {
        GraphQLFieldDefinition field = env.getFieldDefinition();

        FieldCoordinates key = env.isTypeNameIntrospectionField()
            ? FieldCoordinates.systemCoordinates(field.getName())
            : FieldCoordinates.coordinates(env.getFieldsContainer(), field);

        acc.merge(key, 1, Integer::sum);

        return acc;
    }
}
