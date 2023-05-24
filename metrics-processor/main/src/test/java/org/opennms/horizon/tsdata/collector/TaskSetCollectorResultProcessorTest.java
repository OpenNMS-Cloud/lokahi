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
 *
 */

package org.opennms.horizon.tsdata.collector;

import com.google.protobuf.Any;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opennms.taskset.contract.CollectorResponse;
import org.opennms.taskset.contract.Identity;
import org.opennms.taskset.contract.MonitorType;
import org.opennms.taskset.contract.TaskResult;

import java.io.IOException;
import java.util.Objects;

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

        testTaskResult =
            TaskResult.newBuilder()
                .setIdentity(
                    Identity.newBuilder()
                        .setSystemId("x-system-id-x")
                        .build()
                )
                .build();

        var templateCollectoResponse =
            CollectorResponse.newBuilder()
                .setIpAddress("x-ip-address-x")
                .setNodeId(131313L)
                .build();

        // Don't need any real type of result, just need the result field to be set.
        var testResultAny = Any.getDefaultInstance();

        testCollectorResponseAzure =
            templateCollectoResponse.toBuilder()
                .setMonitorType(MonitorType.AZURE)
                .setResult(testResultAny)
                .build();

        testCollectorResponseSnmp =
            templateCollectoResponse.toBuilder()
                .setMonitorType(MonitorType.SNMP)
                .setResult(testResultAny)
                .build();

        testCollectorResponseMissingResult =
            templateCollectoResponse.toBuilder()
                .setMonitorType(MonitorType.SNMP)
                .build();

        testCollectorResponseUnrecognizedMonitorType =
            templateCollectoResponse.toBuilder()
                .setMonitorType(MonitorType.UNKNOWN)
                .setResult(testResultAny)
                .build();

        target = new TaskSetCollectorResultProcessor(mockTaskSetCollectorSnmpResponseProcessor, mockTaskSetCollectorAzureResponseProcessor);
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
                Mockito.eq(new String[]{
                    "x-ip-address-x",
                    "x-location-x",
                    "x-system-id-x",
                    MonitorType.AZURE.name(),
                    "131313"
                })
            );
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
                Mockito.eq("x-tenant-id-x"),
                Mockito.eq("x-location-x"),
                Mockito.same(testTaskResult)
            );
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
            target.processCollectorResponse("x-tenant-id-x", "x-location-x", testTaskResult, testCollectorResponseMissingResult);

            //
            // Verify the Results
            //
            Assertions.assertTrue(logCaptor.getLogEvents().stream().anyMatch(logEvent -> Objects.equals("No result in response", logEvent.getMessage())));
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
            target.processCollectorResponse("x-tenant-id-x", "x-location-x", testTaskResult, testCollectorResponseUnrecognizedMonitorType);

            //
            // Verify the Results
            //
            Assertions.assertTrue(logCaptor.getLogEvents().stream().anyMatch(logEvent -> Objects.equals("Unrecognized monitor type", logEvent.getMessage())));
        }
    }
}
