/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
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

package org.opennms.horizon.inventory.component;

import com.google.protobuf.InvalidProtocolBufferException;
import nl.altindag.log.LogCaptor;
import nl.altindag.log.model.LogEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opennms.horizon.inventory.exception.InventoryRuntimeException;
import org.opennms.horizon.inventory.service.taskset.response.ScannerResponseService;
import org.opennms.taskset.contract.CollectorResponse;
import org.opennms.taskset.contract.ScannerResponse;
import org.opennms.taskset.contract.TaskContext;
import org.opennms.taskset.contract.TaskResult;
import org.opennms.taskset.contract.TenantLocationSpecificTaskSetResults;

import java.util.Objects;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

public class TaskSetResultsConsumerTest {

    public static final long TEST_LOCATION_ID = 1313L;
    public static final String TEST_LOCATION_ID_TEXT = String.valueOf(TEST_LOCATION_ID);

    private TaskSetResultsConsumer target;

    private ScannerResponseService mockScannerResponseService;

    @BeforeEach
    public void setUp() {
        mockScannerResponseService = Mockito.mock(ScannerResponseService.class);


        target = new TaskSetResultsConsumer(mockScannerResponseService);
    }

    @Test
    void testReceiveZeroResults() {
        //
        // Setup Test Data and Interactions
        //
        var testTenantLocationSpecificTaskSetResults =
            TenantLocationSpecificTaskSetResults.newBuilder()
                .setTenantId("x-tenant-id-x")
                .setLocationId("x-location-x")
                .build();

        //
        // Execute
        //
        var messageBytes = testTenantLocationSpecificTaskSetResults.toByteArray();
        target.receiveMessage(messageBytes);

        //
        // Verify the Results
        //
        Mockito.verifyNoInteractions(mockScannerResponseService);
    }

    @Test
    void testReceiveOneScannerResult() throws InvalidProtocolBufferException {
        //
        // Setup Test Data and Interactions
        //
        var testScannerResponse =
            ScannerResponse.newBuilder()
                .build();

        var testResult = makeSacnnerTaskResult("x-task-id-001-x", testScannerResponse);

        var testTenantLocationSpecificTaskSetResults =
            TenantLocationSpecificTaskSetResults.newBuilder()
                .setTenantId("x-tenant-id-x")
                .setLocationId(TEST_LOCATION_ID_TEXT)
                .addResults(
                    testResult
                )
                .build();

        //
        // Execute
        //
        var messageBytes = testTenantLocationSpecificTaskSetResults.toByteArray();
        target.receiveMessage(messageBytes);

        //
        // Verify the Results
        //
        Mockito.verify(mockScannerResponseService).accept("x-tenant-id-x", TEST_LOCATION_ID, testScannerResponse, testScannerResponse.getContext());
    }

    @Test
    void testReceiveOneNonScannerResult() throws InvalidProtocolBufferException {
        //
        // Setup Test Data and Interactions
        //
        var testCollectorResponse =
            CollectorResponse.newBuilder()
                .build();

        var testResult =
            TaskResult.newBuilder()
                .setId("x-task-id-001-x")
                .setCollectorResponse(
                    testCollectorResponse
                )
                .build();

        var testTenantLocationSpecificTaskSetResults =
            TenantLocationSpecificTaskSetResults.newBuilder()
                .setTenantId("x-tenant-id-x")
                .setLocationId(TEST_LOCATION_ID_TEXT)
                .addResults(
                    testResult
                )
                .build();

        //
        // Execute
        //
        var messageBytes = testTenantLocationSpecificTaskSetResults.toByteArray();
        target.receiveMessage(messageBytes);

        //
        // Verify the Results
        //
        Mockito.verifyNoInteractions(mockScannerResponseService);
    }

    @Test
    void testReceive3ScannerResults() throws InvalidProtocolBufferException {
        //
        // Setup Test Data and Interactions
        //
        var testScannerResponse1 = ScannerResponse.newBuilder().setReason("x-reason1-x").build();
        var testScannerResponse2 = ScannerResponse.newBuilder().setReason("x-reason2-x").build();
        var testScannerResponse3 = ScannerResponse.newBuilder().setReason("x-reason3-x").build();

        var testResult1 = makeSacnnerTaskResult("x-task-id-001-x", testScannerResponse1);
        var testResult2 = makeSacnnerTaskResult("x-task-id-002-x", testScannerResponse2);
        var testResult3 = makeSacnnerTaskResult("x-task-id-003-x", testScannerResponse3);

        var testTenantLocationSpecificTaskSetResults =
            TenantLocationSpecificTaskSetResults.newBuilder()
                .setTenantId("x-tenant-id-x")
                .setLocationId(TEST_LOCATION_ID_TEXT)
                .addResults(testResult1)
                .addResults(testResult2)
                .addResults(testResult3)
                .build();

        //
        // Execute
        //
        var messageBytes = testTenantLocationSpecificTaskSetResults.toByteArray();
        target.receiveMessage(messageBytes);

        //
        // Verify the Results
        //
        Mockito.verify(mockScannerResponseService).accept("x-tenant-id-x", TEST_LOCATION_ID, testScannerResponse1, testScannerResponse1.getMetadata());
        Mockito.verify(mockScannerResponseService).accept("x-tenant-id-x", TEST_LOCATION_ID, testScannerResponse2, testScannerResponse2.getMetadata());
        Mockito.verify(mockScannerResponseService).accept("x-tenant-id-x", TEST_LOCATION_ID, testScannerResponse3, testScannerResponse3.getMetadata());
        Mockito.verifyNoMoreInteractions(mockScannerResponseService);
    }

    @Test
    void testEmptyTenantId() {
        //
        // Setup Test Data and Interactions
        //
        var testScannerResponse =
            ScannerResponse.newBuilder()
                .build();

        var testResult = makeSacnnerTaskResult("x-task-id-001-x", testScannerResponse);

        var testTenantLocationSpecificTaskSetResults =
            TenantLocationSpecificTaskSetResults.newBuilder()
                .setTenantId("")
                .setLocationId(TEST_LOCATION_ID_TEXT)
                .addResults(
                    testResult
                )
                .build();

        //
        // Execute
        //
        try (LogCaptor logCaptor = LogCaptor.forClass(TaskSetResultsConsumer.class)) {
            var messageBytes = testTenantLocationSpecificTaskSetResults.toByteArray();
            target.receiveMessage(messageBytes);

            //
            // Verify the Results
            //
            Predicate<LogEvent> matcher =
                (logEvent) ->
                    (
                        Objects.equals("Error while processing kafka message for TaskResults: ", logEvent.getMessage() ) &&
                        (logEvent.getArguments().size() == 0) &&
                        (logEvent.getThrowable().orElse(null) instanceof InventoryRuntimeException) &&
                        (Objects.equals("Missing tenant id", logEvent.getThrowable().orElse(null).getMessage()))
                    );

            assertTrue(logCaptor.getLogEvents().stream().anyMatch(matcher));
            Mockito.verifyNoInteractions(mockScannerResponseService);
        }
    }


//========================================
// Internals
//----------------------------------------

    private TaskResult makeSacnnerTaskResult(String id, ScannerResponse scannerResponse) {
        return
            TaskResult.newBuilder()
                .setId(id)
                .setScannerResponse(scannerResponse)
                .build();
    }
}
