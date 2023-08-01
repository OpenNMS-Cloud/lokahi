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

package org.opennms.horizon.tsdata.collector;

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

import static org.opennms.horizon.tsdata.MetricNameConstants.METRIC_AZURE_NODE_TYPE;
import static org.opennms.horizon.tsdata.MetricNameConstants.METRIC_INSTANCE_LABEL;

@Component
public class TaskSetCollectorAzureResponseProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(TaskSetCollectorAzureResponseProcessor.class);

    private final CortexTSS cortexTSS;
    private final TenantMetricsTracker tenantMetricsTracker;

    public TaskSetCollectorAzureResponseProcessor(CortexTSS cortexTSS, TenantMetricsTracker tenantMetricsTracker) {
        this.cortexTSS = cortexTSS;
        this.tenantMetricsTracker = tenantMetricsTracker;
    }

    public void processAzureCollectorResponse(String tenantId, String location, CollectorResponse response, String[] labelValues) throws InvalidProtocolBufferException {
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
                    if (METRIC_INSTANCE_LABEL.equals(label)){
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
                    LOG.warn("SKIP Unrecognized azure value type: {} azureResult: {}", type,  azureResult);
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
