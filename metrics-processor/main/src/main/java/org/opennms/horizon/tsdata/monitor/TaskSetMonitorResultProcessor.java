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
package org.opennms.horizon.tsdata.monitor;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
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

    public void processMonitorResponse(
            String tenantId, String locationId, TaskResult taskResult, MonitorResponse monitorResponse)
            throws IOException {
        LOG.info(
                "Have monitor response: tenantId={}; locationId={}; systemId={}; taskId={}",
                tenantId,
                locationId,
                taskResult.getIdentity().getSystemId(),
                taskResult.getId());

        String[] labelValues = {
            monitorResponse.getIpAddress(),
            locationId,
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

    // ========================================
    // Internals
    // ----------------------------------------
    private void processMetricMaps(
            Map.Entry<String, Double> entry,
            MonitorResponse response,
            long timestamp,
            String[] labelValues,
            String tenantId)
            throws IOException {
        prometheus.PrometheusTypes.TimeSeries.Builder builder = prometheus.PrometheusTypes.TimeSeries.newBuilder();
        String key = entry.getKey();
        Double value = entry.getValue();

        addLabels(response, labelValues, builder);

        builder.addLabels(PrometheusTypes.Label.newBuilder()
                .setName(MetricNameConstants.METRIC_NAME_LABEL)
                .setValue(CortexTSS.sanitizeMetricName(MetricNameConstants.METRICS_NAME_PREFIX_MONITOR + key)));

        builder.addSamples(
                PrometheusTypes.Sample.newBuilder().setTimestamp(timestamp).setValue(value));

        cortexTSS.store(tenantId, builder);
        tenantMetricsTracker.addTenantMetricSampleCount(tenantId, builder.getSamplesCount());
    }

    private void addLabels(MonitorResponse response, String[] labelValues, PrometheusTypes.TimeSeries.Builder builder) {
        for (int i = 0; i < MetricNameConstants.MONITOR_METRICS_LABEL_NAMES.length; i++) {
            if (!"node_id".equals(MetricNameConstants.MONITOR_METRICS_LABEL_NAMES[i])
                    || !"ECHO".equals(response.getMonitorType().name())) {
                builder.addLabels(PrometheusTypes.Label.newBuilder()
                        .setName(CortexTSS.sanitizeLabelName(MetricNameConstants.MONITOR_METRICS_LABEL_NAMES[i]))
                        .setValue(CortexTSS.sanitizeLabelValue(labelValues[i])));
            }
        }
    }
}
