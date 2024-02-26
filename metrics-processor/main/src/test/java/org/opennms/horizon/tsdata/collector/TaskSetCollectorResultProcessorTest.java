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
package org.opennms.horizon.tsdata.collector;

import com.google.protobuf.Any;
import java.io.IOException;
import java.util.Objects;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opennms.taskset.contract.CollectorResponse;
import org.opennms.taskset.contract.Identity;
import org.opennms.taskset.contract.MonitorType;
import org.opennms.taskset.contract.TaskResult;

public class TaskSetCollectorResultProcessorTest {

    private TaskSetCollectorResultProcessor target;

    private TaskSetCollectorSnmpResponseProcessor mockTaskSetCollectorSnmpResponseProcessor;
    private TaskSetCollectorAzureResponseProcessor mockTaskSetCollectorAzureResponseProcessor;

    private TaskResult testTaskResult;
    private CollectorResponse testCollectorResponseAzure;
    private CollectorResponse testCollectorResponseSnmp;
    private CollectorResponse testCollectorResponseUnrecognizedMonitorType;
    private CollectorResponse testCollectorResponseMissingResult;

    @BeforeEach
    public void setUp() {
        mockTaskSetCollectorSnmpResponseProcessor = Mockito.mock(TaskSetCollectorSnmpResponseProcessor.class);
        mockTaskSetCollectorAzureResponseProcessor = Mockito.mock(TaskSetCollectorAzureResponseProcessor.class);

        testTaskResult = TaskResult.newBuilder()
                .setIdentity(Identity.newBuilder().setSystemId("x-system-id-x").build())
                .build();

        var templateCollectoResponse = CollectorResponse.newBuilder()
                .setIpAddress("x-ip-address-x")
                .setNodeId(131313L)
                .build();

        // Don't need any real type of result, just need the result field to be set.
        var testResultAny = Any.getDefaultInstance();

        testCollectorResponseAzure = templateCollectoResponse.toBuilder()
                .setMonitorType(MonitorType.AZURE)
                .setResult(testResultAny)
                .build();

        testCollectorResponseSnmp = templateCollectoResponse.toBuilder()
                .setMonitorType(MonitorType.SNMP)
                .setResult(testResultAny)
                .build();

        testCollectorResponseMissingResult = templateCollectoResponse.toBuilder()
                .setMonitorType(MonitorType.SNMP)
                .build();

        testCollectorResponseUnrecognizedMonitorType = templateCollectoResponse.toBuilder()
                .setMonitorType(MonitorType.UNKNOWN)
                .setResult(testResultAny)
                .build();

        target = new TaskSetCollectorResultProcessor(
                mockTaskSetCollectorSnmpResponseProcessor, mockTaskSetCollectorAzureResponseProcessor);
    }

    @Test
    void testProcessAzureResult() throws IOException {
        //
        // Setup Test Data and Interactions
        //

        //
        // Execute
        //
        target.processCollectorResponse("x-tenant-id-x", "x-location-x", testTaskResult, testCollectorResponseAzure);

        //
        // Verify the Results
        //
        Mockito.verify(mockTaskSetCollectorAzureResponseProcessor)
                .processAzureCollectorResponse(
                        Mockito.eq("x-tenant-id-x"),
                        Mockito.eq("x-location-x"),
                        Mockito.same(testCollectorResponseAzure),
                        Mockito.eq(new String[] {
                            "x-ip-address-x", "x-location-x", "x-system-id-x", MonitorType.AZURE.name(), "131313"
                        }));
    }

    @Test
    void testProcessSnmpResult() throws IOException {
        //
        // Setup Test Data and Interactions
        //

        //
        // Execute
        //
        target.processCollectorResponse("x-tenant-id-x", "x-location-x", testTaskResult, testCollectorResponseSnmp);

        //
        // Verify the Results
        //
        Mockito.verify(mockTaskSetCollectorSnmpResponseProcessor)
                .processSnmpCollectorResponse(
                        Mockito.eq("x-tenant-id-x"), Mockito.eq("x-location-x"), Mockito.same(testTaskResult));
    }

    @Test
    void testProcessMissingResult() throws IOException {
        //
        // Setup Test Data and Interactions
        //

        try (LogCaptor logCaptor = LogCaptor.forClass(TaskSetCollectorResultProcessor.class)) {
            //
            // Execute
            //
            target.processCollectorResponse(
                    "x-tenant-id-x", "x-location-x", testTaskResult, testCollectorResponseMissingResult);

            //
            // Verify the Results
            //
            Assertions.assertTrue(logCaptor.getLogEvents().stream()
                    .anyMatch(logEvent -> Objects.equals("No result in response", logEvent.getMessage())));
        }
    }

    @Test
    void testProcessUnrecognizedMonitorType() throws IOException {
        //
        // Setup Test Data and Interactions
        //

        try (LogCaptor logCaptor = LogCaptor.forClass(TaskSetCollectorResultProcessor.class)) {
            //
            // Execute
            //
            target.processCollectorResponse(
                    "x-tenant-id-x", "x-location-x", testTaskResult, testCollectorResponseUnrecognizedMonitorType);

            //
            // Verify the Results
            //
            Assertions.assertTrue(logCaptor.getLogEvents().stream()
                    .anyMatch(logEvent -> Objects.equals("Unrecognized monitor type", logEvent.getMessage())));
        }
    }
}
