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

import com.google.protobuf.Any;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opennms.horizon.azure.api.AzureResponseMetric;
import org.opennms.horizon.azure.api.AzureResultMetric;
import org.opennms.horizon.azure.api.AzureValueMetric;
import org.opennms.horizon.azure.api.AzureValueType;
import org.opennms.horizon.tenantmetrics.TenantMetricsTracker;
import org.opennms.horizon.timeseries.cortex.CortexTSS;
import org.opennms.horizon.tsdata.collector.TaskSetCollectorAzureResponseProcessor;
import org.opennms.horizon.tsdata.collector.TaskSetCollectorResultProcessor;
import org.opennms.horizon.tsdata.collector.TaskSetCollectorSnmpResponseProcessor;
import org.opennms.horizon.tsdata.detector.TaskSetDetectorResultProcessor;
import org.opennms.horizon.tsdata.monitor.TaskSetMonitorResultProcessor;
import org.opennms.taskset.contract.CollectorResponse;
import org.opennms.taskset.contract.MonitorType;
import org.opennms.taskset.contract.TaskResult;
import org.opennms.taskset.contract.TenantedTaskSetResults;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TSDataProcessorTest {

    public static final String TENANT_ID = "sometenant";
    @Mock
    private CortexTSS cortexTSS;

    @Mock
    private TenantMetricsTracker metricsTracker;

    private TSDataProcessor processor;

    @BeforeEach
    public void setup() {
        processor = new TSDataProcessor(new TaskSetResultProcessor(
            new TaskSetDetectorResultProcessor(),
            new TaskSetMonitorResultProcessor(cortexTSS, metricsTracker),
            new TaskSetCollectorResultProcessor(
                new TaskSetCollectorSnmpResponseProcessor(cortexTSS, metricsTracker),
                new TaskSetCollectorAzureResponseProcessor(cortexTSS, metricsTracker)
            )
        ));
    }

    @Test
    void testConsumeAzure() throws Exception {

        List<TaskResult> taskResultList = new ArrayList<>();

        List<AzureResultMetric> azureResultMetrics = new ArrayList<>();
        azureResultMetrics.add(AzureResultMetric.newBuilder()
            .setResourceName("resource-name")
            .setResourceGroup("resource-group")
            .setAlias("netInBytes")
            .setValue(AzureValueMetric.newBuilder()
                .setType(AzureValueType.INT64)
                .setUint64(1234L)
                .build())
            .build());

        CollectorResponse collectorResponse = CollectorResponse.newBuilder()
            .setResult(Any.pack(AzureResponseMetric.newBuilder()
                .addAllResults(azureResultMetrics)
                .build()))
            .setMonitorType(MonitorType.AZURE)
            .build();
        taskResultList.add(TaskResult.newBuilder()
            .setCollectorResponse(collectorResponse)
            .build());

        TenantedTaskSetResults taskSetResults = TenantedTaskSetResults.newBuilder()
            .setTenantId(TENANT_ID)
            .addAllResults(taskResultList)
            .build();

        processor.consume(taskSetResults.toByteArray(), Collections.EMPTY_MAP);

        verify(cortexTSS, timeout(5000).only()).store(anyString(), any(prometheus.PrometheusTypes.TimeSeries.Builder.class));
        verify(metricsTracker, times(1)).addTenantMetricSampleCount(TENANT_ID, 1);
    }

    @Test
    void testMissingTenantId() {
        //
        // Setup Test Data and Interactions
        //
        TenantedTaskSetResults taskSetResults = TenantedTaskSetResults.newBuilder()
            .build();

        //
        // Execute
        //
        Exception caught = null;
        try {
            processor.consume(taskSetResults.toByteArray(), Collections.EMPTY_MAP);
            fail("Missing expected exception");
        } catch (Exception exc) {
            caught = exc;
        }

        //
        // Verify the Results
        //
        assertEquals("Missing tenant id", caught.getMessage());
    }
}
