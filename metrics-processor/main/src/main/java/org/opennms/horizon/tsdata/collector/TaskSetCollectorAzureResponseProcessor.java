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

import java.time.Instant;

@Component
public class TaskSetCollectorAzureResponseProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(TaskSetCollectorAzureResponseProcessor.class);

    private final CortexTSS cortexTSS;
    private final TenantMetricsTracker tenantMetricsTracker;

    public TaskSetCollectorAzureResponseProcessor(CortexTSS cortexTSS, TenantMetricsTracker tenantMetricsTracker) {
        this.cortexTSS = cortexTSS;
        this.tenantMetricsTracker = tenantMetricsTracker;
    }

    public void processAzureCollectorResponse(String tenantId, CollectorResponse response, String[] labelValues) throws InvalidProtocolBufferException {
        Any collectorMetric = response.getResult();
        var azureResponse = collectorMetric.unpack(AzureResponseMetric.class);
        long now = Instant.now().toEpochMilli();

        for (AzureResultMetric azureResult : azureResponse.getResultsList()) {
            try {
                PrometheusTypes.TimeSeries.Builder builder = PrometheusTypes.TimeSeries.newBuilder();
                builder.addLabels(PrometheusTypes.Label.newBuilder()
                    .setName(MetricNameConstants.METRIC_NAME_LABEL)
                    .setValue(CortexTSS.sanitizeMetricName(azureResult.getAlias())));

                for (int i = 0; i < MetricNameConstants.MONITOR_METRICS_LABEL_NAMES.length; i++) {
                    builder.addLabels(PrometheusTypes.Label.newBuilder()
                        .setName(CortexTSS.sanitizeLabelName(MetricNameConstants.MONITOR_METRICS_LABEL_NAMES[i]))
                        .setValue(CortexTSS.sanitizeLabelValue(labelValues[i])));
                }

                int type = azureResult.getValue().getTypeValue();
                switch (type) {
                    case AzureValueType.INT64_VALUE:
                        builder.addSamples(PrometheusTypes.Sample.newBuilder()
                            .setTimestamp(now)
                            .setValue(azureResult.getValue().getUint64()));
                        break;
                    default:
                        LOG.warn("Unrecognized azure value type");
                        // TODO: in this case, should cortexTSS.store() be called?
                }

                cortexTSS.store(tenantId, builder);
                tenantMetricsTracker.addTenantMetricSampleCount(tenantId, builder.getSamplesCount());
            } catch (Exception e) {
                LOG.warn("Exception parsing azure metrics", e);
            }
        }
    }
}
