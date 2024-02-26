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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
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

        testLabelValues =
                new String[] {"x-instance-x", "x-location-x", "x-system-id-x", MonitorType.ICMP.name(), "131313"};

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
        var timeSeriesBuilderMatcher = new PrometheusTimeSeriersBuilderArgumentMatcher(
                TEST_AZURE_METRIC_VALUE,
                MonitorType.ICMP,
                "x_alias_x",
                MetricNameConstants.METRIC_AZURE_PUBLIC_IP_TYPE + "/resourceName");
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
        var timeSeriesBuilderMatcher = new PrometheusTimeSeriersBuilderArgumentMatcher(
                TEST_AZURE_METRIC_VALUE, MonitorType.ICMP, "x_alias_x", MetricNameConstants.METRIC_AZURE_NODE_TYPE);
        Mockito.verify(mockCortexTSS).store(Mockito.eq("x-tenant-id-x"), Mockito.argThat(timeSeriesBuilderMatcher));
    }

    @Test
    void testExceptionOnSendToCortex() throws IOException {
        createTestAzureResponseData("resourceName", MetricNameConstants.METRIC_AZURE_NODE_TYPE);
        //
        // Setup Test Data and Interactions
        //
        IOException testException = new IOException("x-test-exception-x");
        Mockito.doThrow(testException)
                .when(mockCortexTSS)
                .store(Mockito.anyString(), Mockito.any(PrometheusTypes.TimeSeries.Builder.class));

        try (LogCaptor logCaptor = LogCaptor.forClass(TaskSetCollectorAzureResponseProcessor.class)) {
            //
            // Execute
            //
            target.processAzureCollectorResponse(
                    "x-tenant-id-x", "x-location-x", testCollectorResponse, testLabelValues);

            //
            // Verify the Results
            //
            Predicate<LogEvent> matcher =
                    (logEvent) -> (Objects.equals("Exception parsing azure metrics", logEvent.getMessage())
                            && (logEvent.getArguments().size() == 0)
                            && (logEvent.getThrowable().orElse(null) == testException));

            Assertions.assertTrue(logCaptor.getLogEvents().stream().anyMatch(matcher));
            Mockito.verifyNoInteractions(mockTenantMetricsTracker);
        }
    }

    // ========================================
    // Internals
    // ----------------------------------------

    private void createTestAzureResponseData(String resourceName, String type) {
        AzureValueMetric azureValueMetric = AzureValueMetric.newBuilder()
                .setType(AzureValueType.INT64)
                .setUint64(TEST_AZURE_METRIC_VALUE)
                .build();

        AzureResultMetric azureResultMetric = AzureResultMetric.newBuilder()
                .setResourceName(resourceName)
                .setAlias("x_alias_x")
                .setValue(azureValueMetric)
                .setType(type)
                .setResourceName("resourceName")
                .build();

        testAzureResponseMetric =
                AzureResponseMetric.newBuilder().addResults(azureResultMetric).build();

        testCollectorResponse = CollectorResponse.newBuilder()
                .setIpAddress("x-ip-address-x")
                .setNodeId(131313L)
                .setMonitorType(MonitorType.ICMP)
                .setResult(Any.pack(testAzureResponseMetric))
                .build();
    }

    private class PrometheusTimeSeriersBuilderArgumentMatcher
            implements ArgumentMatcher<PrometheusTypes.TimeSeries.Builder> {

        private final double metricValue;
        private final MonitorType monitorType;
        private final String metricName;

        private final String instance;

        public PrometheusTimeSeriersBuilderArgumentMatcher(
                double metricValue, MonitorType monitorType, String metricName, String instance) {
            this.metricValue = metricValue;
            this.monitorType = monitorType;
            this.metricName = metricName;
            this.instance = instance;
        }

        @Override
        public boolean matches(PrometheusTypes.TimeSeries.Builder timeseriesBuilder) {
            if ((labelMatches(timeseriesBuilder)) && (sampleMatches(timeseriesBuilder))) {
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

                return ((Objects.equals(metricName, labelMap.get(MetricNameConstants.METRIC_NAME_LABEL)))
                        && (Objects.equals(instance, labelMap.get("instance")))
                        && (Objects.equals("x-location-x", labelMap.get("location_id")))
                        && (Objects.equals("x-system-id-x", labelMap.get("system_id")))
                        && (Objects.equals(monitorType.name(), labelMap.get("monitor")))
                        && (Objects.equals("131313", labelMap.get("node_id"))));
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
