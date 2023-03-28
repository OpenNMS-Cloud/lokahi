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

package org.opennms.horizon.tsdata.detector;

import nl.altindag.log.LogCaptor;
import nl.altindag.log.model.LogEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opennms.taskset.contract.DetectorResponse;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

class TaskSetDetectorResultProcessorTest {

    private TaskSetDetectorResultProcessor target;

    @BeforeEach
    void setUp() {
        target = new TaskSetDetectorResultProcessor();
    }

    @Test
    void testProcessing() throws IOException {
        //
        // Setup Test Data and Interactions
        //
        DetectorResponse detectorResponse =
            DetectorResponse.newBuilder()
                .setIpAddress("127.0.0.1")
                .setDetected(true)
                .build();

        try (LogCaptor logCaptor = LogCaptor.forClass(TaskSetDetectorResultProcessor.class)) {
            //
            // Execute
            //
            target.processDetectorResponse("x-tenant-id-x", "x-task-id-x", detectorResponse);

            //
            // Verify the Results
            //
            Predicate<LogEvent> matcher =
                (logEvent) ->
                    (
                        Objects.equals("Have detector response, tenant-id: {}; task-id={}; detected={}", logEvent.getMessage() ) &&
                        (logEvent.getArguments().size() == 3) &&
                        (logEvent.getArguments().get(0).equals("x-tenant-id-x")) &&
                        (logEvent.getArguments().get(1).equals("x-task-id-x")) &&
                        (Boolean.TRUE.equals(logEvent.getArguments().get(2)))
                    );

            assertTrue(logCaptor.getLogEvents().stream().anyMatch(matcher));
        }
    }
}
