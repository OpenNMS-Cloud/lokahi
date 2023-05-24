/*******************************************************************************
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
 *******************************************************************************/

package org.opennms.horizon.tsdata;

import com.google.protobuf.InvalidProtocolBufferException;
import nl.altindag.log.LogCaptor;
import nl.altindag.log.model.LogEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opennms.taskset.contract.TaskResult;
import org.opennms.taskset.contract.TenantLocationSpecificTaskSetResults;

import java.util.Objects;
import java.util.function.Predicate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class TSDataProcessorTest {

    private TaskSetResultProcessor mockTaskSetMonitorResultProcessor;

    private TaskResult testTaskResult1;
    private TaskResult testTaskResult2;
    private TenantLocationSpecificTaskSetResults testTenantLocationSpecificTaskSetResults;
    private TenantLocationSpecificTaskSetResults testTenantLocationSpecificTaskSetResultsBlankTenant;
    
    private TSDataProcessor target;

    @BeforeEach
    public void setup() {
        mockTaskSetMonitorResultProcessor = Mockito.mock(TaskSetResultProcessor.class);

        testTaskResult1 =
            TaskResult.newBuilder()
                .setId("x-task1-result-x")
                .build();

        testTaskResult2 =
            TaskResult.newBuilder()
                .setId("x-task2-result-x")
                .build();
        
        testTenantLocationSpecificTaskSetResults =
            TenantLocationSpecificTaskSetResults.newBuilder()
                .setTenantId("x-tenant-id-x")
                .setLocation("x-location-x")
                .addResults(testTaskResult1)
                .addResults(testTaskResult2)
                .build();

        testTenantLocationSpecificTaskSetResultsBlankTenant =
            TenantLocationSpecificTaskSetResults.newBuilder()
                .setTenantId("")
                .build();

        target = new TSDataProcessor(mockTaskSetMonitorResultProcessor);
        
    }

    @Test
    void testConsumeFromKafka() {
        //
        // Execute
        //
        target.setSubmitForExecutionOp(this::testExecutionSubmissionOp);
        target.consume(testTenantLocationSpecificTaskSetResults.toByteArray());

        //
        // Verify the Results
        //
        Mockito.verify(mockTaskSetMonitorResultProcessor).processTaskResult("x-tenant-id-x", "x-location-x", testTaskResult1);
        Mockito.verify(mockTaskSetMonitorResultProcessor).processTaskResult("x-tenant-id-x", "x-location-x", testTaskResult2);
        Mockito.verifyNoMoreInteractions(mockTaskSetMonitorResultProcessor);
    }

    @Test
    void testBlankTenantId() {
        //
        // Execute
        //
        Exception actualException = null;
        try {
            target.consume(testTenantLocationSpecificTaskSetResultsBlankTenant.toByteArray());
            fail("Missing expected exception");
        } catch (Exception exc) {
            actualException = exc;
        }

        //
        // Verify the Results
        //
        assertEquals("Missing tenant id", actualException.getMessage());
    }

    @Test
    void testExceptionProcessingResults() {
        //
        // Setup Test Data and Interactions
        //
        try (LogCaptor logCaptor = LogCaptor.forClass(TSDataProcessor.class)) {
            //
            // Execute
            //
            target.consume("----INVALID----".getBytes());

            //
            // Verify the Results
            //
            Predicate<LogEvent> matcher =
                (logEvent) ->
                    (
                        Objects.equals("Invalid data from kafka", logEvent.getMessage() ) &&
                        (logEvent.getArguments().size() == 0) &&
                        (logEvent.getThrowable().orElse(null) instanceof InvalidProtocolBufferException)
                    );

            assertTrue(logCaptor.getLogEvents().stream().anyMatch(matcher));

            Mockito.verifyNoInteractions(mockTaskSetMonitorResultProcessor);
        }
    }

//========================================
// Internals
//----------------------------------------

    /**
     * Test async execution submission operation that directly executes the operation immediately, to simplify the tests
     *  and avoid multi-threaded testing complexity.
     *
     * @param runnable
     */
    private void testExecutionSubmissionOp(Runnable runnable) {
        // Immediately pass-through the call
        runnable.run();
    }
}
