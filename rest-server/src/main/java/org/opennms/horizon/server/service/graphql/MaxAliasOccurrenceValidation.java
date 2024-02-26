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

import graphql.GraphQLError;
import graphql.analysis.QueryTraverser;
import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.execution.instrumentation.fieldvalidation.FieldValidation;
import graphql.execution.instrumentation.fieldvalidation.FieldValidationEnvironment;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class MaxAliasOccurrenceValidation implements FieldValidation {

    private final int maxFieldOccurrence;

    @Override
    public List<GraphQLError> validateFields(FieldValidationEnvironment environment) {
        QueryTraverser queryTraverser = Traversers.queryTraverser(environment);
        var occurrences = queryTraverser.reducePreOrder(this::reduceField, new LinkedHashMap<>());

        if (log.isDebugEnabled()) {
            occurrences.forEach((field, count) -> log.debug(
                    "Field: {}, Occurrences: {} > {}, Over Limit: {}",
                    field,
                    count,
                    maxFieldOccurrence,
                    count > maxFieldOccurrence));
        }

        return occurrences.entrySet().stream()
                .filter(entry -> entry.getValue() > maxFieldOccurrence)
                .map(Map.Entry::getKey)
                .map(field -> environment.mkError("Validation error: Alias '" + field + "' is repeated too many times"))
                .collect(Collectors.toList());
    }

    public Map<String, Integer> reduceField(QueryVisitorFieldEnvironment fieldEnvironment, Map<String, Integer> acc) {
        var alias = fieldEnvironment.getField().getAlias();
        var key = fieldEnvironment.getField().getName();
        if (alias != null) {
            acc.merge(key, 1, Integer::sum);
        }

        return acc;
    }
}
