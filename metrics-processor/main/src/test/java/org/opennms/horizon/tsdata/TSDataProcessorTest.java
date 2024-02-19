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
package org.opennms.horizon.tsdata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Objects;
import java.util.function.Predicate;
import nl.altindag.log.LogCaptor;
import nl.altindag.log.model.LogEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opennms.taskset.contract.TaskResult;
import org.opennms.taskset.contract.TenantLocationSpecificTaskSetResults;

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

        testTaskResult1 = TaskResult.newBuilder().setId("x-task1-result-x").build();

        testTaskResult2 = TaskResult.newBuilder().setId("x-task2-result-x").build();

        testTenantLocationSpecificTaskSetResults = TenantLocationSpecificTaskSetResults.newBuilder()
                .setTenantId("x-tenant-id-x")
                .setLocationId("x-location-x")
                .addResults(testTaskResult1)
                .addResults(testTaskResult2)
                .build();

        testTenantLocationSpecificTaskSetResultsBlankTenant = TenantLocationSpecificTaskSetResults.newBuilder()
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
        Mockito.verify(mockTaskSetMonitorResultProcessor)
                .processTaskResult("x-tenant-id-x", "x-location-x", testTaskResult1);
        Mockito.verify(mockTaskSetMonitorResultProcessor)
                .processTaskResult("x-tenant-id-x", "x-location-x", testTaskResult2);
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
                    (logEvent) -> (Objects.equals("Invalid data from kafka", logEvent.getMessage())
                            && (logEvent.getArguments().size() == 0)
                            && (logEvent.getThrowable().orElse(null) instanceof InvalidProtocolBufferException));

            assertTrue(logCaptor.getLogEvents().stream().anyMatch(matcher));

            Mockito.verifyNoInteractions(mockTaskSetMonitorResultProcessor);
        }
    }

    // ========================================
    // Internals
    // ----------------------------------------

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
