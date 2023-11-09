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
        InstrumentationExecutionParameters parameters, InstrumentationState state
    ) {
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
