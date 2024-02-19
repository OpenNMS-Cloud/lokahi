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

import graphql.analysis.FieldComplexityCalculator;
import graphql.analysis.MaxQueryComplexityInstrumentation;
import graphql.analysis.QueryComplexityInfo;
import graphql.execution.AbortExecutionException;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MaxComplexityInstrumentation extends MaxQueryComplexityInstrumentation {
    public MaxComplexityInstrumentation(int maxComplexity) {
        super(maxComplexity);
    }

    public MaxComplexityInstrumentation(
            int maxComplexity, Function<QueryComplexityInfo, Boolean> complexityExceededFunction) {
        super(maxComplexity, complexityExceededFunction);
    }

    public MaxComplexityInstrumentation(int maxComplexity, FieldComplexityCalculator calculator) {
        super(maxComplexity, calculator);
    }

    public MaxComplexityInstrumentation(
            int maxComplexity,
            FieldComplexityCalculator calculator,
            Function<QueryComplexityInfo, Boolean> complexityExceededFunction) {
        super(maxComplexity, calculator, complexityExceededFunction);
    }

    @Override
    protected AbortExecutionException mkAbortException(int totalComplexity, int maxComplexity) {
        log.debug("maximum query complexity exceeded {} > {}", totalComplexity, maxComplexity);
        return new AbortExecutionException("maximum query complexity exceeded");
    }
}
