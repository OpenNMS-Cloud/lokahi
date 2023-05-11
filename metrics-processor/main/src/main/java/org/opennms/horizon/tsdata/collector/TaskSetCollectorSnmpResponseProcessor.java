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
import org.opennms.horizon.snmp.api.SnmpResponseMetric;
import org.opennms.horizon.snmp.api.SnmpResultMetric;
import org.opennms.horizon.snmp.api.SnmpValueType;
import org.opennms.horizon.tenantmetrics.TenantMetricsTracker;
import org.opennms.horizon.timeseries.cortex.CortexTSS;
import org.opennms.horizon.tsdata.MetricNameConstants;
import org.opennms.taskset.contract.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import prometheus.PrometheusTypes;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

@Component
public class TaskSetCollectorSnmpResponseProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(TaskSetCollectorSnmpResponseProcessor.class);

    private final CortexTSS cortexTSS;
    private final TenantMetricsTracker tenantMetricsTracker;
    
    public TaskSetCollectorSnmpResponseProcessor(CortexTSS cortexTSS, TenantMetricsTracker tenantMetricsTracker) {
        this.cortexTSS = cortexTSS;
        this.tenantMetricsTracker = tenantMetricsTracker;
    }

    public void processSnmpCollectorResponse(String tenantId, TaskResult taskResult) throws IOException {
        var response = taskResult.getCollectorResponse();
        Any collectorMetric = response.getResult();
        Map<String, String> labels = new HashMap<>();
        labels.put("location", taskResult.getLocation());
        labels.put("system_id", taskResult.getSystemId());
        labels.put("monitor", response.getMonitorType().name());
        labels.put("node_id", String.valueOf(response.getNodeId()));
        var snmpResponse = collectorMetric.unpack(SnmpResponseMetric.class);
        for (SnmpResultMetric snmpResult : snmpResponse.getResultsList()) {
            try {
                PrometheusTypes.TimeSeries.Builder builder = prometheus.PrometheusTypes.TimeSeries.newBuilder();
                builder.addLabels(PrometheusTypes.Label.newBuilder()
                    .setName(MetricNameConstants.METRIC_NAME_LABEL)
                    .setValue(CortexTSS.sanitizeMetricName(snmpResult.getAlias())));

                labels.forEach((name, value) -> builder.addLabels(PrometheusTypes.Label.newBuilder()
                    .setName(CortexTSS.sanitizeLabelName(name))
                    .setValue(CortexTSS.sanitizeLabelValue(value))));

                builder.addLabels(PrometheusTypes.Label.newBuilder().setName("if_name")
                    .setValue(snmpResult.getIfName()));

                if (snmpResult.hasIpAddress()) {
                    builder.addLabels(PrometheusTypes.Label.newBuilder().setName("ip_address")
                        .setValue(snmpResult.getIpAddress()));
                    // TODO: Remove instance from ip_address
                    builder.addLabels(PrometheusTypes.Label.newBuilder().setName("instance")
                        .setValue(snmpResult.getIpAddress()));
                }

                int type = snmpResult.getValue().getTypeValue();
                switch (type) {
                    case SnmpValueType.INT32_VALUE:
                        builder.addSamples(PrometheusTypes.Sample.newBuilder()
                            .setTimestamp(response.getTimestamp())
                            .setValue(snmpResult.getValue().getSint64()));
                        break;
                    case SnmpValueType.COUNTER32_VALUE:
                    case SnmpValueType.TIMETICKS_VALUE:
                    case SnmpValueType.GAUGE32_VALUE:
                        builder.addSamples(PrometheusTypes.Sample.newBuilder()
                            .setTimestamp(response.getTimestamp())
                            .setValue(snmpResult.getValue().getUint64()));
                        break;
                    case SnmpValueType.COUNTER64_VALUE:
                        double metric = new BigInteger(snmpResult.getValue().getBytes().toByteArray()).doubleValue();
                        builder.addSamples(PrometheusTypes.Sample.newBuilder()
                            .setTimestamp(response.getTimestamp())
                            .setValue(metric));
                        break;
                }

                cortexTSS.store(tenantId, builder);
                tenantMetricsTracker.addTenantMetricSampleCount(tenantId, builder.getSamplesCount());
            } catch (Exception e) {
                LOG.warn("Exception parsing metrics", e);
            }
        }
    }
}
