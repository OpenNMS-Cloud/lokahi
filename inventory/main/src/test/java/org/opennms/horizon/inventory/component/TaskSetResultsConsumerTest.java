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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opennms.horizon.inventory.service.taskset.response.DetectorResponseService;
import org.opennms.horizon.inventory.service.taskset.response.ScannerResponseService;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.opennms.taskset.contract.DetectorResponse;
import org.opennms.taskset.contract.ScannerResponse;
import org.opennms.taskset.contract.TaskContext;
import org.opennms.taskset.contract.TaskResult;
import org.opennms.taskset.contract.TaskSetResults;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.times;

@RunWith(MockitoJUnitRunner.class)
public class TaskSetResultsConsumerTest {
    private static final String TEST_LOCATION = "Default";
    private static final String TEST_TENANT_ID = "opennms-prime";

    private static final long TEST_NODE_ID = 10L;

    @InjectMocks
    private TaskSetResultsConsumer consumer;

    @Mock
    private DetectorResponseService detectorService;

    @Mock
    private ScannerResponseService scannerService;

    @Test
    public void testReceiveScannerMessage() throws Exception {

        ScannerResponse response = ScannerResponse.newBuilder().build();

        TaskResult taskResult = TaskResult.newBuilder()
            .setLocation(TEST_LOCATION)
            .setScannerResponse(response)
            .build();

        TaskSetResults results = TaskSetResults.newBuilder()
            .addResults(taskResult).build();

        Map<String, Object> headers = new HashMap<>();
        headers.put(GrpcConstants.TENANT_ID_KEY, TEST_TENANT_ID.getBytes());

        consumer.receiveMessage(results.toByteArray(), headers);

        Mockito.verify(scannerService, times(1))
            .accept(TEST_TENANT_ID, TEST_LOCATION, response, taskResult.getContext());
    }

    @Test
    public void testReceiveDetectorMessage() {

        DetectorResponse response = DetectorResponse.newBuilder().build();

        TaskResult taskResult = TaskResult.newBuilder()
            .setLocation(TEST_LOCATION)
            .setDetectorResponse(response)
            .setContext(TaskContext.newBuilder()
                .setNodeId(TEST_NODE_ID)
                .build()
            )
            .build();

        TaskSetResults results = TaskSetResults.newBuilder()
            .addResults(taskResult).build();

        Map<String, Object> headers = new HashMap<>();
        headers.put(GrpcConstants.TENANT_ID_KEY, TEST_TENANT_ID.getBytes());

        consumer.receiveMessage(results.toByteArray(), headers);

        Mockito.verify(detectorService, times(1))
            .accept(TEST_TENANT_ID, TEST_LOCATION, response, taskResult.getContext());
    }
}
