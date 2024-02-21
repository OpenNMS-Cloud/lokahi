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

import graphql.ExecutionResult;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.SimpleInstrumentationContext;
import graphql.execution.instrumentation.SimplePerformantInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationCreateStateParameters;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExecutionTimingInstrumentation extends SimplePerformantInstrumentation {

    @Getter
    public static class EventTimes implements InstrumentationState {
        private Long beginExecution = null;
        private Long executionDuration = null;

        public void recordBegin() {
            beginExecution = System.currentTimeMillis();
        }

        public void recordEnd() {
            executionDuration = System.currentTimeMillis() - beginExecution;
        }
    }

    @Override
    public InstrumentationState createState(InstrumentationCreateStateParameters parameters) {
        return new EventTimes();
    }

    @Override
    public InstrumentationContext<ExecutionResult> beginExecution(
            InstrumentationExecutionParameters parameters, InstrumentationState state) {
        EventTimes times = InstrumentationState.ofState(state);
        times.recordBegin();
        return new SimpleInstrumentationContext<>() {
            @Override
            public void onCompleted(ExecutionResult result, Throwable t) {
                times.recordEnd();
                log.info("Execution performed in {} ms", times.getExecutionDuration());
            }
        };
    }
}
