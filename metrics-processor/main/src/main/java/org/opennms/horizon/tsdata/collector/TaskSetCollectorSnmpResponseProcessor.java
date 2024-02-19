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
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

@Component
public class TaskSetCollectorSnmpResponseProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(TaskSetCollectorSnmpResponseProcessor.class);

    private final CortexTSS cortexTSS;
    private final TenantMetricsTracker tenantMetricsTracker;

    public TaskSetCollectorSnmpResponseProcessor(CortexTSS cortexTSS, TenantMetricsTracker tenantMetricsTracker) {
        this.cortexTSS = cortexTSS;
        this.tenantMetricsTracker = tenantMetricsTracker;
    }

    public void processSnmpCollectorResponse(String tenantId, String location, TaskResult taskResult)
            throws IOException {
        var response = taskResult.getCollectorResponse();
        Any collectorMetric = response.getResult();
        Map<String, String> labels = new HashMap<>();
        labels.put("location", location);
        labels.put("system_id", taskResult.getIdentity().getSystemId());
        labels.put("monitor", response.getMonitorType().name());
        labels.put("node_id", String.valueOf(response.getNodeId()));

        List<PrometheusTypes.TimeSeries> timeSeriesList = new ArrayList<>();
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

                builder.addLabels(
                        PrometheusTypes.Label.newBuilder().setName("if_name").setValue(snmpResult.getIfName()));

                if (snmpResult.hasIpAddress()) {
                    builder.addLabels(PrometheusTypes.Label.newBuilder()
                            .setName("ip_address")
                            .setValue(snmpResult.getIpAddress()));
                    // TODO: Remove instance from ip_address
                    builder.addLabels(PrometheusTypes.Label.newBuilder()
                            .setName("instance")
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
                        double metric =
                                new BigInteger(snmpResult.getValue().getBytes().toByteArray()).doubleValue();
                        builder.addSamples(PrometheusTypes.Sample.newBuilder()
                                .setTimestamp(response.getTimestamp())
                                .setValue(metric));
                        break;
                }
                tenantMetricsTracker.addTenantMetricSampleCount(tenantId, builder.getSamplesCount());
                timeSeriesList.add(builder.build());
            } catch (Exception e) {
                LOG.warn("Exception parsing metrics", e);
            }
        }
        cortexTSS.store(tenantId, timeSeriesList);
    }
}
