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

import graphql.ErrorType;
import graphql.GraphQLError;
import graphql.execution.AbortExecutionException;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.SimplePerformantInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationValidationParameters;
import graphql.language.Directive;
import graphql.language.Node;
import graphql.language.NodeTraverser;
import graphql.language.NodeVisitorStub;
import graphql.util.TraversalControl;
import graphql.util.TraverserContext;
import graphql.validation.ValidationError;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class MaxDirectiveOccurrenceInstrumentation extends SimplePerformantInstrumentation {

    private final int maxDirectiveOccurrence;

    // This needs to be done at the beginning of the validation phase instead of
    // as a field validation, otherwise other validations will take precedence.
    @Override
    public InstrumentationContext<List<ValidationError>> beginValidation(
        InstrumentationValidationParameters parameters,
        InstrumentationState state
    ) {
        var nodeVisitor = new DirectiveCounterVisitor();
        new NodeTraverser().preOrder(nodeVisitor, parameters.getDocument().getChildren());

        var errors = nodeVisitor.getOccurrences().entrySet().stream()
            .filter(entry -> entry.getValue() > maxDirectiveOccurrence)
            .map(Map.Entry::getKey)
            .map(field -> GraphQLError.newError()
                .errorType(ErrorType.ValidationError)
                .message("Validation error: Directive '" + field + "' is repeated too many times")
                .build()
            )
            .toList();
        if (!errors.isEmpty()) {
            throw new AbortExecutionException(errors);
        }
        return super.beginValidation(parameters, state);
    }

    private static class DirectiveCounterVisitor extends NodeVisitorStub {

        @Getter
        private final Map<String, Integer> occurrences;

        public DirectiveCounterVisitor() {
            occurrences = new LinkedHashMap<>();
        }

        @Override
        public TraversalControl visitDirective(Directive node, TraverserContext<Node> context) {
            occurrences.merge(node.getName(), 1, Integer::sum);
            return TraversalControl.CONTINUE;
        }
    }
}
