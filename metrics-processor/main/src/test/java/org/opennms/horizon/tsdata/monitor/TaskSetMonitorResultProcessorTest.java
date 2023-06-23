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

package org.opennms.horizon.tsdata.monitor;

import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.opennms.horizon.tenantmetrics.TenantMetricsTracker;
import org.opennms.horizon.timeseries.cortex.CortexTSS;
import org.opennms.horizon.tsdata.MetricNameConstants;
import org.opennms.taskset.contract.Identity;
import org.opennms.taskset.contract.MonitorResponse;
import org.opennms.taskset.contract.MonitorType;
import org.opennms.taskset.contract.TaskResult;
import prometheus.PrometheusTypes;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import prometheus.PrometheusTypes.TimeSeries;
import prometheus.PrometheusTypes.TimeSeries.Builder;

import static org.mockito.ArgumentMatchers.*;
import static org.opennms.horizon.tsdata.MetricNameConstants.METRICS_NAME_PREFIX_MONITOR;

public class TaskSetMonitorResultProcessorTest {

    public final static String TENANT_ID = "foobar";
    private static final String SYSTEM_ID = "minion-x";
    private static final String LOCATION = "location-y";

    private TaskSetMonitorResultProcessor target;

    private CortexTSS mockCortexTSS;
    private TenantMetricsTracker mockTenantMetricsTracker;

    private TaskResult testTaskResult;
    private MonitorResponse testIcmpMonitorResponse;
    private MonitorResponse testEchoMonitorResponse;
    private MonitorResponse testMonitorResponseWithAdditionalMetrics;

    @BeforeEach
    public void setUp() {
        mockCortexTSS = Mockito.mock(CortexTSS.class);
        mockTenantMetricsTracker = Mockito.mock(TenantMetricsTracker.class);

        testTaskResult =
            TaskResult.newBuilder()
                .setId("x-task-id-x")
                .setIdentity(
                    Identity.newBuilder()
                        .setSystemId("x-system-id-x")
                        .build()
                )
                .build();

        testIcmpMonitorResponse =
            MonitorResponse.newBuilder()
                .setIpAddress("x-ip-address-x")
                .setResponseTimeMs(1313.0)
                .setNodeId(151515L)
                .setMonitorType(MonitorType.ICMP)
                .build();

        testEchoMonitorResponse =
            MonitorResponse.newBuilder()
                .setIpAddress("x-ip-address-x")
                .setResponseTimeMs(1313.0)
                .setNodeId(151515L)
                .setMonitorType(MonitorType.ECHO)
                .build();

        testMonitorResponseWithAdditionalMetrics =
            MonitorResponse.newBuilder()
                .setIpAddress("x-ip-address-x")
                .setResponseTimeMs(1313.0)
                .setNodeId(151515L)
                .setMonitorType(MonitorType.ECHO)
                .putMetrics("x_metric_001_x", 1.001)
                .putMetrics("x_metric_002_x", 2.002)
                .putMetrics("x_metric_003_x", 3.003)
                .build();

        target = new TaskSetMonitorResultProcessor(mockCortexTSS, mockTenantMetricsTracker);
    }

    @Test
    void testProcessIcmpMonitorResponseNoAdditionalMetrics() throws IOException {
        //
        // Execute
        //
        target.processMonitorResponse("x-tenant-id-x", "x-location-x", testTaskResult, testIcmpMonitorResponse);

        //
        // Verify the Results
        //
        var matcher = new PrometheusTimeSeriersBuilderArgumentMatcher(1313.0, MonitorType.ICMP, MetricNameConstants.METRICS_NAME_RESPONSE);
        Mockito.verify(mockCortexTSS).store(eq("x-tenant-id-x"), Mockito.argThat(matcher));
        Mockito.verify(mockTenantMetricsTracker).addTenantMetricSampleCount("x-tenant-id-x", 1);
        Mockito.verifyNoMoreInteractions(mockCortexTSS);
        Mockito.verifyNoMoreInteractions(mockTenantMetricsTracker);
    }

    @Test
    void testProcessEchoMonitorResponseNoAdditionalMetrics() throws IOException {
        //
        // Execute
        //
        target.processMonitorResponse("x-tenant-id-x", "x-location-x", testTaskResult, testEchoMonitorResponse);

        //
        // Verify the Results
        //
        var matcher = new PrometheusTimeSeriersBuilderArgumentMatcher(1313.0, MonitorType.ECHO, MetricNameConstants.METRICS_NAME_RESPONSE);
        Mockito.verify(mockCortexTSS).store(eq("x-tenant-id-x"), Mockito.argThat(matcher));
        Mockito.verify(mockTenantMetricsTracker).addTenantMetricSampleCount("x-tenant-id-x", 1);
        Mockito.verifyNoMoreInteractions(mockCortexTSS);
        Mockito.verifyNoMoreInteractions(mockTenantMetricsTracker);
    }

    @Test
    void testProcessMonitorResponseWithAdditionalMetrics() throws IOException {
        //
        // Execute
        //
        target.processMonitorResponse("x-tenant-id-x", "x-location-x", testTaskResult, testMonitorResponseWithAdditionalMetrics);

        //
        // Verify the Results
        //
        var mainMetricMatcher =
            new PrometheusTimeSeriersBuilderArgumentMatcher(1313.0, MonitorType.ECHO, MetricNameConstants.METRICS_NAME_RESPONSE);
        Mockito.verify(mockCortexTSS).store(eq("x-tenant-id-x"), Mockito.argThat(mainMetricMatcher));

        var matcher1 =
            new PrometheusTimeSeriersBuilderArgumentMatcher(1.001, MonitorType.ECHO, METRICS_NAME_PREFIX_MONITOR + "x_metric_001_x");
        Mockito.verify(mockCortexTSS).store(eq("x-tenant-id-x"), Mockito.argThat(matcher1));

        var matcher2 =
            new PrometheusTimeSeriersBuilderArgumentMatcher(2.002, MonitorType.ECHO, METRICS_NAME_PREFIX_MONITOR + "x_metric_002_x");
        Mockito.verify(mockCortexTSS).store(eq("x-tenant-id-x"), Mockito.argThat(matcher2));

        var matcher3 =
            new PrometheusTimeSeriersBuilderArgumentMatcher(3.003, MonitorType.ECHO, METRICS_NAME_PREFIX_MONITOR + "x_metric_003_x");
        Mockito.verify(mockCortexTSS).store(eq("x-tenant-id-x"), Mockito.argThat(matcher3));

        Mockito.verify(mockTenantMetricsTracker, Mockito.times(4)).addTenantMetricSampleCount("x-tenant-id-x", 1);

        Mockito.verifyNoMoreInteractions(mockCortexTSS);
        Mockito.verifyNoMoreInteractions(mockTenantMetricsTracker);
    }

    @Test
    void testMonitoringResponseWithTimestamp() throws Exception {
        long testTimestamp = 100_000_000L;
        TaskResult result = createMonitorSample(testTimestamp);

        target.processMonitorResponse(TENANT_ID, "x-location-x", result, result.getMonitorResponse());

        Mockito.verify(mockCortexTSS).store(eq(TENANT_ID), argThat(new TimeMatcher(testTimestamp)));
        Mockito.verify(mockTenantMetricsTracker).addTenantMetricSampleCount(TENANT_ID, 1);
    }

    @Test
    void testMonitoringResponseWithEmptyTimestamp() throws Exception {
        TaskResult result = createMonitorSample(0);

        target.processMonitorResponse(TENANT_ID, "x-location-x", result, result.getMonitorResponse());

        Mockito.verify(mockCortexTSS).store(eq(TENANT_ID), argThat(new TimeMatcher(ts -> ts != 0)));
        Mockito.verify(mockTenantMetricsTracker).addTenantMetricSampleCount(TENANT_ID, 1);
    }

    private static TaskResult createMonitorSample(long timestamp) {
        MonitorResponse monitorResponse = MonitorResponse.newBuilder()
            .setResponseTimeMs(10.0)
            .setTimestamp(timestamp)
            .setNodeId(10L)
            .build();

        return TaskResult.newBuilder()
            .setIdentity(
                Identity.newBuilder()
                    .setSystemId(SYSTEM_ID)
                    .build()
            )
            .setId("test-monitor")
            .setMonitorResponse(monitorResponse)
            .build();
    }

//========================================
// Internals
//----------------------------------------

    static class TimeMatcher implements ArgumentMatcher<TimeSeries.Builder> {

        private final Predicate<Long> matcher;

        public TimeMatcher(long timestamp) {
            this(ts -> timestamp == ts);
        }

        public TimeMatcher(Predicate<Long> matcher) {
            this.matcher = matcher;
        }

        @Override
        public boolean matches(Builder argument) {
            return matcher.test(argument.getSamples(0).getTimestamp());
        }

        @Override
        public String toString() {
            return "timestamp matcher";
        }
    }

    private class PrometheusTimeSeriersBuilderArgumentMatcher implements ArgumentMatcher<PrometheusTypes.TimeSeries.Builder> {

        private final double metricValue;
        private final MonitorType monitorType;
        private final String metricName;

        public PrometheusTimeSeriersBuilderArgumentMatcher(double metricValue, MonitorType monitorType, String metricName) {
            this.metricValue = metricValue;
            this.monitorType = monitorType;
            this.metricName = metricName;
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
            boolean isEchoMatcher = (monitorType == MonitorType.ECHO);

            int expectedCount;
            if (isEchoMatcher) {
                expectedCount = 5;
            } else {
                expectedCount = 6;
            }

            if (timeseriesBuilder.getLabelsCount() == expectedCount) {
                Map<String, String> labelMap = new HashMap<>();
                for (var label : timeseriesBuilder.getLabelsList()) {
                    labelMap.put(label.getName(), label.getValue());
                }

                return (
                    (Objects.equals(metricName, labelMap.get(MetricNameConstants.METRIC_NAME_LABEL))) &&
                    (Objects.equals("x-ip-address-x", labelMap.get("instance"))) &&
                    (Objects.equals("x-location-x", labelMap.get("location_id"))) &&
                    (Objects.equals("x-system-id-x", labelMap.get("system_id"))) &&
                    (Objects.equals(monitorType.name(), labelMap.get("monitor"))) &&
                    ((isEchoMatcher) || Objects.equals("151515", labelMap.get("node_id")))
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
