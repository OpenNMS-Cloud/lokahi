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
import com.google.protobuf.ByteString;
import nl.altindag.log.LogCaptor;
import nl.altindag.log.model.LogEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.opennms.horizon.snmp.api.SnmpResponseMetric;
import org.opennms.horizon.snmp.api.SnmpResultMetric;
import org.opennms.horizon.snmp.api.SnmpValueMetric;
import org.opennms.horizon.snmp.api.SnmpValueType;
import org.opennms.horizon.tenantmetrics.TenantMetricsTracker;
import org.opennms.horizon.timeseries.cortex.CortexTSS;
import org.opennms.horizon.tsdata.MetricNameConstants;
import org.opennms.taskset.contract.CollectorResponse;
import org.opennms.taskset.contract.MonitorType;
import org.opennms.taskset.contract.TaskResult;
import prometheus.PrometheusTypes;

import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TaskSetCollectorSnmpResponseProcessorTest {

    private TaskSetCollectorSnmpResponseProcessor target;

    private CortexTSS mockCortexTSS;
    private TenantMetricsTracker mockTenantMetricsTracker;

    private CollectorResponse testCollectorResponseAllResultTypes;
    private CollectorResponse testCollectorResponse1ResultType;
    private String[] testLabelValues;
    private TaskResult testTaskResult;

    @BeforeEach
    public void setUp() {
        mockCortexTSS = Mockito.mock(CortexTSS.class);
        mockTenantMetricsTracker = Mockito.mock(TenantMetricsTracker.class);

        prepareCollectorResponseTestData();

        testLabelValues = new String[] {
            "x-instance-x",
            "x-location-x",
            "x-system-id-x",
            MonitorType.SNMP.name(),
            "131313"
        };

        target = new TaskSetCollectorSnmpResponseProcessor(mockCortexTSS, mockTenantMetricsTracker);
    }

    @Test
    void testProcessResponse() throws IOException {
        //
        // Execute
        //
        target.processSnmpCollectorResponse("x-tenant-id-x",  testTaskResult);

        //
        // Verify the Results
        //
        var int32ResultStoreMatcher = new PrometheusTimeSeriersBuilderArgumentMatcher(3200320032.0, MonitorType.SNMP, "x_int32_alias_x");
        Mockito.verify(mockCortexTSS).store(Mockito.eq("x-tenant-id-x"), Mockito.argThat(int32ResultStoreMatcher));

        var counter32ResultStoreMatcher = new PrometheusTimeSeriersBuilderArgumentMatcher(640001.0, MonitorType.SNMP, "x_counter32_alias_x");
        Mockito.verify(mockCortexTSS).store(Mockito.eq("x-tenant-id-x"), Mockito.argThat(counter32ResultStoreMatcher));

        var timeticksResultStoreMatcher = new PrometheusTimeSeriersBuilderArgumentMatcher(640002.0, MonitorType.SNMP, "x_timeticks_alias_x");
        Mockito.verify(mockCortexTSS).store(Mockito.eq("x-tenant-id-x"), Mockito.argThat(timeticksResultStoreMatcher));

        var gauge32ResultStoreMatcher = new PrometheusTimeSeriersBuilderArgumentMatcher(640003.0, MonitorType.SNMP, "x_gauge32_alias_x");
        Mockito.verify(mockCortexTSS).store(Mockito.eq("x-tenant-id-x"), Mockito.argThat(gauge32ResultStoreMatcher));

        var counter64ResultStoreMatcher = new PrometheusTimeSeriersBuilderArgumentMatcher(640004.0, MonitorType.SNMP, "x_counter64_alias_x");
        Mockito.verify(mockCortexTSS).store(Mockito.eq("x-tenant-id-x"), Mockito.argThat(counter64ResultStoreMatcher));

        Mockito.verifyNoMoreInteractions(mockCortexTSS);
    }

    @Test
    void testProcessException() throws IOException {
        //
        // Setup Test Data and Interactions
        //
        RuntimeException testException = new RuntimeException("x-test-exc-x");
        Mockito.doThrow(testException).when(mockCortexTSS).store(Mockito.eq("x-tenant-id-x"), Mockito.any(prometheus.PrometheusTypes.TimeSeries.Builder.class));

        try (LogCaptor logCaptor = LogCaptor.forClass(TaskSetCollectorSnmpResponseProcessor.class)) {
            //
            // Execute
            //
            target.processSnmpCollectorResponse("x-tenant-id-x", testTaskResult);

            //
            // Verify the Results
            //
            Predicate<LogEvent> matcher =
                (logEvent) ->
                    (
                        Objects.equals("Exception parsing metrics", logEvent.getMessage() ) &&
                        (logEvent.getArguments().size() == 0) &&
                        (logEvent.getThrowable().orElse(null) == testException)
                    );

            assertTrue(logCaptor.getLogEvents().stream().anyMatch(matcher));
        }
    }

    @Test
    void testCollectorTimestamps() throws IOException {
        var snmpResult = SnmpResultMetric.newBuilder()
            .setAlias("x_int32_alias_x")
            .setValue(SnmpValueMetric.newBuilder().setType(SnmpValueType.INT32).setSint64(3200320032L)
                    .build()).build();
        SnmpResponseMetric snmpResponse =
            SnmpResponseMetric.newBuilder()
                .addResults(snmpResult)
                .build();
        Instant timestamp = Instant.parse("2023-01-01T00:00:00Z");
        CollectorResponse collectorResponse = CollectorResponse.newBuilder()
            .setResult(Any.pack(snmpResponse))
            .setTimestamp(timestamp.toEpochMilli())
                .setMonitorType(MonitorType.SNMP).build();
        TaskResult taskResult = TaskResult.newBuilder().setCollectorResponse(collectorResponse).build();
        target.processSnmpCollectorResponse("x-tenant-id-x", taskResult);
        var timeSeriesTimeStampMatcher = new PrometheusTimeSeriesTimeStampMatcher(timestamp.toEpochMilli());
        Mockito.verify(mockCortexTSS).store(Mockito.eq("x-tenant-id-x"), Mockito.argThat(timeSeriesTimeStampMatcher));
    }

//========================================
// Internals
//----------------------------------------

    private void prepareCollectorResponseTestData() {
        var int32Result =
            SnmpResultMetric.newBuilder()
                .setAlias("x_int32_alias_x")
                .setValue(
                    SnmpValueMetric.newBuilder()
                        .setType(SnmpValueType.INT32)
                        .setSint64(3200320032L)
                        .build()
                    )
                .build();

        var counter32Result =
            SnmpResultMetric.newBuilder()
                .setAlias("x_counter32_alias_x")
                .setValue(
                    SnmpValueMetric.newBuilder()
                        .setType(SnmpValueType.COUNTER32)
                        .setUint64(640001L)
                        .build()
                    )
                .build();

        var timeticksResult =
            SnmpResultMetric.newBuilder()
                .setAlias("x_timeticks_alias_x")
                .setValue(
                    SnmpValueMetric.newBuilder()
                        .setType(SnmpValueType.TIMETICKS)
                        .setUint64(640002L)
                        .build()
                );

        var gauge32Result =
            SnmpResultMetric.newBuilder()
                .setAlias("x_gauge32_alias_x")
                .setValue(
                    SnmpValueMetric.newBuilder()
                        .setType(SnmpValueType.GAUGE32)
                        .setUint64(640003L)
                        .build()
                );


        var counter64Result =
            SnmpResultMetric.newBuilder()
                .setAlias("x_counter64_alias_x")
                .setValue(
                    SnmpValueMetric.newBuilder()
                        .setType(SnmpValueType.COUNTER64)
                        .setBytes(ByteString.copyFrom(BigInteger.valueOf(640004L).toByteArray()))
                        .build()
                );

        SnmpResponseMetric snmpResponseMetricAllTypes =
            SnmpResponseMetric.newBuilder()
                .addResults(int32Result)
                .addResults(counter32Result)
                .addResults(timeticksResult)
                .addResults(gauge32Result)
                .addResults(counter64Result)
                .build();

        testCollectorResponseAllResultTypes =
            CollectorResponse.newBuilder()
                .setIpAddress("x-instance-x")
                .setNodeId(131313)
                .setMonitorType(MonitorType.SNMP)
                .setResult(Any.pack(snmpResponseMetricAllTypes))
                .build();

        testTaskResult = TaskResult.newBuilder()
            .setLocation("x-location-x")
            .setSystemId("x-system-id-x")
            .setCollectorResponse(testCollectorResponseAllResultTypes)
            .build();

        SnmpResponseMetric snmpResponseMetric1Type =
            SnmpResponseMetric.newBuilder()
                .addResults(int32Result)
                .build();

        testCollectorResponse1ResultType =
            CollectorResponse.newBuilder()
                .setIpAddress("x-ip-address-x")
                .setNodeId(131313)
                .setResult(Any.pack(snmpResponseMetric1Type))
                .build();
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
            if (timeseriesBuilder.getLabelsCount() == 6) {
                Map<String, String> labelMap = new HashMap<>();
                for (var label : timeseriesBuilder.getLabelsList()) {
                    labelMap.put(label.getName(), label.getValue());
                }

                return (
                    (Objects.equals(metricName, labelMap.get(MetricNameConstants.METRIC_NAME_LABEL))) &&
                    (Objects.equals("x-location-x", labelMap.get("location"))) &&
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

    private class PrometheusTimeSeriesTimeStampMatcher implements ArgumentMatcher<PrometheusTypes.TimeSeries.Builder> {

        private long timeStamp;
        public PrometheusTimeSeriesTimeStampMatcher(long timeStamp) {
            this.timeStamp = timeStamp;
        }

        @Override
        public boolean matches(PrometheusTypes.TimeSeries.Builder argument) {
            return argument.getSamples(0).getTimestamp() == timeStamp;
        }
    }
}
