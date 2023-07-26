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
import nl.altindag.log.model.LogEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.opennms.horizon.azure.api.AzureResponseMetric;
import org.opennms.horizon.azure.api.AzureResultMetric;
import org.opennms.horizon.azure.api.AzureValueMetric;
import org.opennms.horizon.azure.api.AzureValueType;
import org.opennms.horizon.tenantmetrics.TenantMetricsTracker;
import org.opennms.horizon.timeseries.cortex.CortexTSS;
import org.opennms.horizon.tsdata.MetricNameConstants;
import org.opennms.taskset.contract.CollectorResponse;
import org.opennms.taskset.contract.MonitorType;
import prometheus.PrometheusTypes;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

public class TaskSetCollectorAzureResponseProcessorTest {

    public static final long TEST_AZURE_METRIC_VALUE = 27272727L;

    private TaskSetCollectorAzureResponseProcessor target;

    private CortexTSS mockCortexTSS;
    private TenantMetricsTracker mockTenantMetricsTracker;

    private CollectorResponse testCollectorResponse;
    private AzureResponseMetric testAzureResponseMetric;
    private String[] testLabelValues;

    @BeforeEach
    public void setUp() {
        mockCortexTSS = Mockito.mock(CortexTSS.class);
        mockTenantMetricsTracker = Mockito.mock(TenantMetricsTracker.class);

        testLabelValues = new String[] {
            "x-instance-x",
            "x-location-x",
            "x-system-id-x",
            MonitorType.ICMP.name(),
            "131313"
        };

        target = new TaskSetCollectorAzureResponseProcessor(mockCortexTSS, mockTenantMetricsTracker);
    }

    @Test
    void testInterfaceProcessCollectorResponse() throws IOException {
        createTestAzureResponseData("resourceName", MetricNameConstants.METRIC_AZURE_PUBLIC_IP_TYPE);
        //
        // Execute
        //
        target.processAzureCollectorResponse("x-tenant-id-x", "x-location-x", testCollectorResponse, testLabelValues);

        //
        // Verify the Results
        //
        var timeSeriesBuilderMatcher = new PrometheusTimeSeriersBuilderArgumentMatcher(TEST_AZURE_METRIC_VALUE,
            MonitorType.ICMP, "x_alias_x", MetricNameConstants.METRIC_AZURE_PUBLIC_IP_TYPE + "/resourceName");
        Mockito.verify(mockCortexTSS).store(Mockito.eq("x-tenant-id-x"), Mockito.argThat(timeSeriesBuilderMatcher));
    }

    @Test
    void testNodeProcessCollectorResponse() throws IOException {
        createTestAzureResponseData("resourceName", MetricNameConstants.METRIC_AZURE_NODE_TYPE);
        //
        // Execute
        //
        target.processAzureCollectorResponse("x-tenant-id-x", "x-location-x", testCollectorResponse, testLabelValues);

        //
        // Verify the Results
        //
        var timeSeriesBuilderMatcher = new PrometheusTimeSeriersBuilderArgumentMatcher(TEST_AZURE_METRIC_VALUE,
            MonitorType.ICMP, "x_alias_x", MetricNameConstants.METRIC_AZURE_NODE_TYPE);
        Mockito.verify(mockCortexTSS).store(Mockito.eq("x-tenant-id-x"), Mockito.argThat(timeSeriesBuilderMatcher));
    }

    @Test
    void testExceptionOnSendToCortex() throws IOException {
        createTestAzureResponseData("resourceName", MetricNameConstants.METRIC_AZURE_NODE_TYPE);
        //
        // Setup Test Data and Interactions
        //
        IOException testException = new IOException("x-test-exception-x");
        Mockito.doThrow(testException).when(mockCortexTSS).store(Mockito.anyString(), Mockito.any(PrometheusTypes.TimeSeries.Builder.class));

        try (LogCaptor logCaptor = LogCaptor.forClass(TaskSetCollectorAzureResponseProcessor.class)) {
            //
            // Execute
            //
            target.processAzureCollectorResponse("x-tenant-id-x", "x-location-x", testCollectorResponse, testLabelValues);

            //
            // Verify the Results
            //
            Predicate<LogEvent> matcher =
                (logEvent) ->
                    (
                        Objects.equals("Exception parsing azure metrics", logEvent.getMessage() ) &&
                        (logEvent.getArguments().size() == 0) &&
                        (logEvent.getThrowable().orElse(null) == testException)
                    );

            Assertions.assertTrue(logCaptor.getLogEvents().stream().anyMatch(matcher));
            Mockito.verifyNoInteractions(mockTenantMetricsTracker);
        }
    }

//========================================
// Internals
//----------------------------------------

    private void createTestAzureResponseData(String resourceName, String type) {
        AzureValueMetric azureValueMetric =
            AzureValueMetric.newBuilder()
                .setType(AzureValueType.INT64)
                .setUint64(TEST_AZURE_METRIC_VALUE)
                .build();

        AzureResultMetric azureResultMetric =
            AzureResultMetric.newBuilder().setResourceName(resourceName)
                .setAlias("x_alias_x")
                .setValue(azureValueMetric)
                .setType(type)
                .setResourceName("resourceName")
                .build();

        testAzureResponseMetric =
            AzureResponseMetric.newBuilder()
                .addResults(azureResultMetric)
                .build();

        testCollectorResponse =
            CollectorResponse.newBuilder()
                .setIpAddress("x-ip-address-x")
                .setNodeId(131313L)
                .setMonitorType(MonitorType.ICMP)
                .setResult(Any.pack(testAzureResponseMetric))
                .build();

    }

    private class PrometheusTimeSeriersBuilderArgumentMatcher implements ArgumentMatcher<PrometheusTypes.TimeSeries.Builder> {

        private final double metricValue;
        private final MonitorType monitorType;
        private final String metricName;

        private final String instance;

        public PrometheusTimeSeriersBuilderArgumentMatcher(double metricValue, MonitorType monitorType, String metricName, String instance) {
            this.metricValue = metricValue;
            this.monitorType = monitorType;
            this.metricName = metricName;
            this.instance = instance;
        }

        @Override
        public boolean matches(PrometheusTypes.TimeSeries.Builder timeseriesBuilder) {
            if (
                (labelMatches(timeseriesBuilder)) &&
                (sampleMatches(timeseriesBuilder))
            ) {
                return true;
            }
            return false;
        }

        private boolean labelMatches(PrometheusTypes.TimeSeries.Builder timeseriesBuilder) {
            if (timeseriesBuilder.getLabelsCount() == 6) {
                Map<String, String> labelMap = new HashMap<>();
                for (var label : timeseriesBuilder.getLabelsList()) {
                    labelMap.put(label.getName(), label.getValue());
                }

                return (
                    (Objects.equals(metricName, labelMap.get(MetricNameConstants.METRIC_NAME_LABEL))) &&
                    (Objects.equals(instance, labelMap.get("instance"))) &&
                    (Objects.equals("x-location-x", labelMap.get("location_id"))) &&
                    (Objects.equals("x-system-id-x", labelMap.get("system_id"))) &&
                    (Objects.equals(monitorType.name(), labelMap.get("monitor"))) &&
                    (Objects.equals("131313", labelMap.get("node_id")))
                );
            }

            return false;
        }

        private boolean sampleMatches(PrometheusTypes.TimeSeries.Builder timeseriesBuilder) {
            if (timeseriesBuilder.getSamplesCount() == 1) {
                PrometheusTypes.Sample sample = timeseriesBuilder.getSamples(0);

                if (Math.abs(metricValue - sample.getValue()) < 0.0000001) {
                    return true;
                }
            }

            return false;
        }
    }
}
