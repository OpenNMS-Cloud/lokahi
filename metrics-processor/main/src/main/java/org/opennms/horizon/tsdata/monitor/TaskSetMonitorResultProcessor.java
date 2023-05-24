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

package org.opennms.horizon.tsdata.monitor;

import java.util.Optional;
import org.opennms.horizon.tenantmetrics.TenantMetricsTracker;
import org.opennms.horizon.timeseries.cortex.CortexTSS;
import org.opennms.horizon.tsdata.MetricNameConstants;
import org.opennms.taskset.contract.MonitorResponse;
import org.opennms.taskset.contract.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import prometheus.PrometheusTypes;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

@Component
public class TaskSetMonitorResultProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(TaskSetMonitorResultProcessor.class);

    private final CortexTSS cortexTSS;

    private final TenantMetricsTracker tenantMetricsTracker;

    @Autowired
    public TaskSetMonitorResultProcessor(CortexTSS cortexTSS, TenantMetricsTracker tenantMetricsTracker) {
        this.cortexTSS = cortexTSS;
        this.tenantMetricsTracker = tenantMetricsTracker;
    }

    public void processMonitorResponse(String tenantId, String location, TaskResult taskResult, MonitorResponse monitorResponse) throws IOException {
        LOG.info("Have monitor response: tenant-id={}; location={}; system-id={}; task-id={}",
            tenantId, location, taskResult.getIdentity().getSystemId(), taskResult.getId());

        String[] labelValues =
            {
                monitorResponse.getIpAddress(),
                location,
                taskResult.getIdentity().getSystemId(),
                monitorResponse.getMonitorType().name(),
                String.valueOf(monitorResponse.getNodeId())
            };

        PrometheusTypes.TimeSeries.Builder builder = PrometheusTypes.TimeSeries.newBuilder();

        addLabels(monitorResponse, labelValues, builder);

        builder.addLabels(PrometheusTypes.Label.newBuilder()
            .setName(MetricNameConstants.METRIC_NAME_LABEL)
            .setValue(CortexTSS.sanitizeMetricName(MetricNameConstants.METRICS_NAME_RESPONSE)));

        long timestamp = Optional.of(monitorResponse.getTimestamp())
            .filter(ts -> ts > 0)
            .orElse(Instant.now().toEpochMilli());
        builder.addSamples(PrometheusTypes.Sample.newBuilder()
            .setTimestamp(timestamp)
            .setValue(monitorResponse.getResponseTimeMs()));

        cortexTSS.store(tenantId, builder);
        tenantMetricsTracker.addTenantMetricSampleCount(tenantId, builder.getSamplesCount());

        for (Map.Entry<String, Double> entry : monitorResponse.getMetricsMap().entrySet()) {
            processMetricMaps(entry, monitorResponse, timestamp, labelValues, tenantId);
        }
    }

//========================================
// Internals
//----------------------------------------
    private void processMetricMaps(Map.Entry<String, Double> entry, MonitorResponse response, long timestamp, String[] labelValues, String tenantId) throws IOException {
        prometheus.PrometheusTypes.TimeSeries.Builder builder = prometheus.PrometheusTypes.TimeSeries.newBuilder();
        String key = entry.getKey();
        Double value = entry.getValue();

        addLabels(response, labelValues, builder);

        builder.addLabels(PrometheusTypes.Label.newBuilder()
            .setName(MetricNameConstants.METRIC_NAME_LABEL)
            .setValue(CortexTSS.sanitizeMetricName(MetricNameConstants.METRICS_NAME_PREFIX_MONITOR + key)));

        builder.addSamples(PrometheusTypes.Sample.newBuilder()
            .setTimestamp(timestamp)
            .setValue(value));

        cortexTSS.store(tenantId, builder);
        tenantMetricsTracker.addTenantMetricSampleCount(tenantId, builder.getSamplesCount());
    }

    private void addLabels(MonitorResponse response, String[] labelValues, PrometheusTypes.TimeSeries.Builder builder) {
        for (int i = 0; i < MetricNameConstants.MONITOR_METRICS_LABEL_NAMES.length; i++) {
            if (!"node_id".equals(MetricNameConstants.MONITOR_METRICS_LABEL_NAMES[i]) || !"ECHO".equals(response.getMonitorType().name())) {
                builder.addLabels(PrometheusTypes.Label.newBuilder()
                    .setName(CortexTSS.sanitizeLabelName(MetricNameConstants.MONITOR_METRICS_LABEL_NAMES[i]))
                    .setValue(CortexTSS.sanitizeLabelValue(labelValues[i])));
            }
        }
    }
}
