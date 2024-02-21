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

import static org.opennms.horizon.tsdata.MetricNameConstants.METRIC_AZURE_NODE_TYPE;
import static org.opennms.horizon.tsdata.MetricNameConstants.METRIC_INSTANCE_LABEL;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import org.opennms.horizon.azure.api.AzureResponseMetric;
import org.opennms.horizon.azure.api.AzureResultMetric;
import org.opennms.horizon.azure.api.AzureValueType;
import org.opennms.horizon.tenantmetrics.TenantMetricsTracker;
import org.opennms.horizon.timeseries.cortex.CortexTSS;
import org.opennms.horizon.tsdata.MetricNameConstants;
import org.opennms.taskset.contract.CollectorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import prometheus.PrometheusTypes;

@Component
public class TaskSetCollectorAzureResponseProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(TaskSetCollectorAzureResponseProcessor.class);

    private final CortexTSS cortexTSS;
    private final TenantMetricsTracker tenantMetricsTracker;

    public TaskSetCollectorAzureResponseProcessor(CortexTSS cortexTSS, TenantMetricsTracker tenantMetricsTracker) {
        this.cortexTSS = cortexTSS;
        this.tenantMetricsTracker = tenantMetricsTracker;
    }

    public void processAzureCollectorResponse(
            String tenantId, String location, CollectorResponse response, String[] labelValues)
            throws InvalidProtocolBufferException {
        Any collectorMetric = response.getResult();
        var azureResponse = collectorMetric.unpack(AzureResponseMetric.class);

        for (AzureResultMetric azureResult : azureResponse.getResultsList()) {
            try {
                PrometheusTypes.TimeSeries.Builder builder = PrometheusTypes.TimeSeries.newBuilder();
                builder.addLabels(PrometheusTypes.Label.newBuilder()
                        .setName(MetricNameConstants.METRIC_NAME_LABEL)
                        .setValue(CortexTSS.sanitizeMetricName(azureResult.getAlias())));

                for (int i = 0; i < MetricNameConstants.MONITOR_METRICS_LABEL_NAMES.length; i++) {
                    final var label = MetricNameConstants.MONITOR_METRICS_LABEL_NAMES[i];
                    var value = labelValues[i];
                    if (METRIC_INSTANCE_LABEL.equals(label)) {
                        value = getInstance(azureResult);
                    }
                    builder.addLabels(PrometheusTypes.Label.newBuilder()
                            .setName(CortexTSS.sanitizeLabelName(label))
                            .setValue(CortexTSS.sanitizeLabelValue(value)));
                }

                int type = azureResult.getValue().getTypeValue();
                if (type == AzureValueType.INT64_VALUE) {
                    builder.addSamples(PrometheusTypes.Sample.newBuilder()
                            .setTimestamp(response.getTimestamp())
                            .setValue(azureResult.getValue().getUint64()));
                    cortexTSS.store(tenantId, builder);
                    tenantMetricsTracker.addTenantMetricSampleCount(tenantId, builder.getSamplesCount());
                } else {
                    LOG.warn("SKIP Unrecognized azure value type: {} azureResult: {}", type, azureResult);
                }
            } catch (Exception e) {
                LOG.warn("Exception parsing azure metrics", e);
            }
        }
    }

    private String getInstance(AzureResultMetric azureResult) {
        if (METRIC_AZURE_NODE_TYPE.equals(azureResult.getType())) {
            return METRIC_AZURE_NODE_TYPE;
        } else {
            return azureResult.getType() + "/" + azureResult.getResourceName();
        }
    }
}
