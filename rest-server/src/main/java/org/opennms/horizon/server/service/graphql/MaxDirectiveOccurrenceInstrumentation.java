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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class MaxDirectiveOccurrenceInstrumentation extends SimplePerformantInstrumentation {

    private final int maxDirectiveOccurrence;

    // This needs to be done at the beginning of the validation phase instead of
    // as a field validation, otherwise other validations will take precedence.
    @Override
    public InstrumentationContext<List<ValidationError>> beginValidation(
            InstrumentationValidationParameters parameters, InstrumentationState state) {
        var nodeVisitor = new DirectiveCounterVisitor();
        new NodeTraverser().preOrder(nodeVisitor, parameters.getDocument().getChildren());
        Map<String, Integer> occurrences = nodeVisitor.getOccurrences();

        if (log.isDebugEnabled()) {
            occurrences.forEach((field, count) -> log.debug(
                    "Field: {}, Occurrences: {} > {}, Over Limit: {}",
                    field,
                    count,
                    maxDirectiveOccurrence,
                    count > maxDirectiveOccurrence));
        }

        var errors = occurrences.entrySet().stream()
                .filter(entry -> entry.getValue() > maxDirectiveOccurrence)
                .map(Map.Entry::getKey)
                .map(field -> GraphQLError.newError()
                        .errorType(ErrorType.ValidationError)
                        .message("Validation error: Directive '" + field + "' is repeated too many times")
                        .build())
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
