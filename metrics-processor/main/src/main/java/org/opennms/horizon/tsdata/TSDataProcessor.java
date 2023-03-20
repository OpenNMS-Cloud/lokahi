/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
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

package org.opennms.horizon.tsdata;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.opennms.horizon.azure.api.AzureResponseMetric;
import org.opennms.horizon.azure.api.AzureResultMetric;
import org.opennms.horizon.azure.api.AzureValueType;
import org.opennms.horizon.timeseries.cortex.CortexTSS;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.opennms.horizon.snmp.api.SnmpResponseMetric;
import org.opennms.horizon.snmp.api.SnmpResultMetric;
import org.opennms.horizon.snmp.api.SnmpValueType;
import org.opennms.taskset.contract.CollectorResponse;
import org.opennms.taskset.contract.DetectorResponse;
import org.opennms.taskset.contract.MonitorResponse;
import org.opennms.taskset.contract.MonitorType;
import org.opennms.taskset.contract.TaskContext;
import org.opennms.taskset.contract.TaskResult;
import org.opennms.taskset.contract.TaskSetResults;
import org.springframework.context.annotation.PropertySource;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;

import lombok.extern.slf4j.Slf4j;
import prometheus.PrometheusTypes;
import prometheus.PrometheusTypes.TimeSeries.Builder;

@Slf4j
@Service
@PropertySource("classpath:application.yml")
public class TSDataProcessor {
    private static final String METRICS_NAME_PREFIX_MONITOR = "monitor_";
    private static final String METRICS_NAME_RESPONSE = "response_time_msec";
    public static final String METRIC_NAME_LABEL = "__name__";

    public static final String NODE_ID_KEY = "node_id";
    private static final String[] MONITOR_METRICS_LABEL_NAMES = {
        "instance",
        "location",
        "system_id",
        "monitor"};
    private final CortexTSS cortexTSS;

    public TSDataProcessor(CortexTSS cortexTSS) {
        this.cortexTSS = cortexTSS;
    }

    //headers for future use.
    @KafkaListener(topics = "${kafka.topics}", concurrency = "1")
    public void consume(@Payload byte[] data, @Headers Map<String, Object> headers) {
        String tenantId = getTenantId(headers);
        try {
            TaskSetResults results = TaskSetResults.parseFrom(data);
            results.getResultsList().forEach(result -> CompletableFuture.supplyAsync(() -> {
                try {
                    if (result != null) {
                        log.info("Processing task set result {}", result);
                        if (result.hasMonitorResponse()) {
                            log.info("Have monitor response, tenant-id: {}; task-id={};", tenantId, result.getId());
                            processMonitorResponse(tenantId, result);
                        } else if (result.hasDetectorResponse()) {
                            DetectorResponse detectorResponse = result.getDetectorResponse();
                            // TBD: how to process?
                            log.info("Have detector response, tenant-id: {}; task-id={}; detected={}", tenantId, result.getId(), detectorResponse.getDetected());
                        }
                        else if(result.hasCollectorResponse()) {
                            log.info("Have collector response, tenant-id: {}; task-id={};", tenantId, result.getId());
                            processCollectorResponse(tenantId, result);
                        }
                    } else {
                        log.warn("Task result appears to be missing the echo response details with results {}", results);
                    }
                } catch (Exception exc) {
                    // TODO: throttle
                    log.warn("Error processing task result", exc);
                }
                return null;
            }));
        } catch (InvalidProtocolBufferException e) {
            log.error("Invalid data from kafka", e);
        }
    }

    private void processMonitorResponse(String tenantId, TaskResult result) throws IOException {
        MonitorResponse response = result.getMonitorResponse();
        String[] labelValues = {response.getIpAddress(), result.getLocation(), result.getSystemId(), response.getMonitorType().name()};

        prometheus.PrometheusTypes.TimeSeries.Builder builder = prometheus.PrometheusTypes.TimeSeries.newBuilder();
        Optional<String> nodeId = nodeId(result);
        addLabels(response, nodeId, labelValues, builder, result.getContext());

        builder.addLabels(PrometheusTypes.Label.newBuilder()
            .setName(METRIC_NAME_LABEL)
            .setValue(CortexTSS.sanitizeMetricName(METRICS_NAME_RESPONSE)));
        builder.addSamples(PrometheusTypes.Sample.newBuilder()
            .setTimestamp(Instant.now().toEpochMilli())
            .setValue(response.getResponseTimeMs()));
        cortexTSS.store(tenantId, builder);

        for (Map.Entry<String, Double> entry : response.getMetricsMap().entrySet()) {
            processMetricMaps(nodeId, entry, response, labelValues, tenantId, result.getContext());
        }
    }

    private void processMetricMaps(Optional<String> nodeId, Map.Entry<String, Double> entry, MonitorResponse response, String[] labelValues, String tenantId, TaskContext taskContext) throws IOException {
        prometheus.PrometheusTypes.TimeSeries.Builder builder = prometheus.PrometheusTypes.TimeSeries.newBuilder();
        String k = entry.getKey();
        Double v = entry.getValue();
        addLabels(response, nodeId, labelValues, builder, taskContext);
        builder.addLabels(PrometheusTypes.Label.newBuilder()
            .setName(METRIC_NAME_LABEL)
            .setValue(CortexTSS.sanitizeMetricName(METRICS_NAME_PREFIX_MONITOR + k)));
        builder.addSamples(PrometheusTypes.Sample.newBuilder()
            .setTimestamp(Instant.now().toEpochMilli())
            .setValue(v));
        cortexTSS.store(tenantId, builder);
    }

    private void addLabels(MonitorResponse response, Optional<String> nodeId, String[] labelValues, Builder builder, TaskContext taskContext) {
        nodeId.ifPresent(node -> builder.addLabels(PrometheusTypes.Label.newBuilder()
            .setName(CortexTSS.sanitizeLabelName(NODE_ID_KEY))
            .setValue(CortexTSS.sanitizeLabelValue(node)))
        );
        for (int i = 0; i < MONITOR_METRICS_LABEL_NAMES.length; i++) {
            if (!NODE_ID_KEY.equals(MONITOR_METRICS_LABEL_NAMES[i]) || !"ECHO".equals(response.getMonitorType().name())) {
                builder.addLabels(PrometheusTypes.Label.newBuilder()
                    .setName(CortexTSS.sanitizeLabelName(MONITOR_METRICS_LABEL_NAMES[i]))
                    .setValue(CortexTSS.sanitizeLabelValue(labelValues[i])));
            }
        }
        addContext(builder, taskContext);
    }

    private void processCollectorResponse(String tenantId, TaskResult result) throws IOException {
        CollectorResponse response = result.getCollectorResponse();

        String[] labelValues = {response.getIpAddress(), result.getLocation(), result.getSystemId(),
            response.getMonitorType().name()};

        if (response.hasResult()) {
            MonitorType monitorType = response.getMonitorType();
            if (monitorType.equals(MonitorType.SNMP)) {
                processSnmpCollectorResponse(tenantId, response, labelValues, result.getContext());
            } else if (monitorType.equals(MonitorType.AZURE)) {
                processAzureCollectorResponse(tenantId, response, labelValues, result.getContext());
            } else {
                log.warn("Unrecognized monitor type");
            }
        } else {
            log.warn("No result in response");
        }
    }

    private void processSnmpCollectorResponse(String tenantId, CollectorResponse response, String[] labelValues, TaskContext taskContext)  throws IOException {
        Any collectorMetric = response.getResult();
        var snmpResponse = collectorMetric.unpack(SnmpResponseMetric.class);
        long now = Instant.now().toEpochMilli();
        for (SnmpResultMetric snmpResult : snmpResponse.getResultsList()) {
            try {
                PrometheusTypes.TimeSeries.Builder builder = prometheus.PrometheusTypes.TimeSeries.newBuilder();
                builder.addLabels(PrometheusTypes.Label.newBuilder()
                    .setName(METRIC_NAME_LABEL)
                    .setValue(CortexTSS.sanitizeMetricName(snmpResult.getAlias())));
                for (int i = 0; i < MONITOR_METRICS_LABEL_NAMES.length; i++) {
                    builder.addLabels(prometheus.PrometheusTypes.Label.newBuilder()
                        .setName(CortexTSS.sanitizeLabelName(MONITOR_METRICS_LABEL_NAMES[i]))
                        .setValue(CortexTSS.sanitizeLabelValue(labelValues[i])));
                }
                addContext(builder, taskContext);
                int type = snmpResult.getValue().getTypeValue();
                switch (type) {
                    case SnmpValueType.INT32_VALUE:
                        builder.addSamples(PrometheusTypes.Sample.newBuilder()
                            .setTimestamp(now)
                            .setValue(snmpResult.getValue().getSint64()));
                        break;
                    case SnmpValueType.COUNTER32_VALUE:
                        // TODO: Can't set a counter through prometheus API, may be possible with remote write
                    case SnmpValueType.TIMETICKS_VALUE:
                    case SnmpValueType.GAUGE32_VALUE:
                        builder.addSamples(PrometheusTypes.Sample.newBuilder()
                            .setTimestamp(now)
                            .setValue(snmpResult.getValue().getUint64()));
                        break;
                    case SnmpValueType.COUNTER64_VALUE:
                        double metric = new BigInteger(snmpResult.getValue().getBytes().toByteArray()).doubleValue();
                        builder.addSamples(PrometheusTypes.Sample.newBuilder()
                            .setTimestamp(now)
                            .setValue(metric));
                        break;
                }
                cortexTSS.store(tenantId, builder);
            } catch (Exception e) {
                log.warn("Exception parsing metrics ", e);
            }
        }
    }

    private void processAzureCollectorResponse(String tenantId, CollectorResponse response, String[] labelValues, TaskContext taskContext) throws InvalidProtocolBufferException {
        Any collectorMetric = response.getResult();
        var azureResponse = collectorMetric.unpack(AzureResponseMetric.class);
        long now = Instant.now().toEpochMilli();

        for (AzureResultMetric azureResult : azureResponse.getResultsList()) {
            try {
                PrometheusTypes.TimeSeries.Builder builder = PrometheusTypes.TimeSeries.newBuilder();
                builder.addLabels(PrometheusTypes.Label.newBuilder()
                    .setName(METRIC_NAME_LABEL)
                    .setValue(CortexTSS.sanitizeMetricName(azureResult.getAlias())));

                for (int i = 0; i < MONITOR_METRICS_LABEL_NAMES.length; i++) {
                    builder.addLabels(PrometheusTypes.Label.newBuilder()
                        .setName(CortexTSS.sanitizeLabelName(MONITOR_METRICS_LABEL_NAMES[i]))
                        .setValue(CortexTSS.sanitizeLabelValue(labelValues[i])));
                }
                addContext(builder, taskContext);

                int type = azureResult.getValue().getTypeValue();
                switch (type) {
                    case AzureValueType.INT64_VALUE:
                        builder.addSamples(PrometheusTypes.Sample.newBuilder()
                            .setTimestamp(now)
                            .setValue(azureResult.getValue().getUint64()));
                        break;
                    default:
                        log.warn("Unrecognized azure value type");
                }
                cortexTSS.store(tenantId, builder);
            } catch (Exception e) {
                log.warn("Exception parsing azure metrics ", e);
            }
        }
    }

    private String getTenantId(Map<String, Object> headers) {
        return Optional.ofNullable(headers.get(GrpcConstants.TENANT_ID_KEY))
            .map(tenantId -> {
                if (tenantId instanceof byte[]) {
                    return new String((byte[]) tenantId);
                }
                if (tenantId instanceof String) {
                    return (String) tenantId;
                }
                return "" + tenantId;
            })
            .orElseThrow(() -> new RuntimeException("Could not determine tenant id"));
    }


    private static Optional<String> nodeId(TaskResult result) {
        return Optional.of(result.getContext())
            .map(TaskContext::getNodeId)
            .filter(value -> value > 0L)
            .map(val -> Long.toString(val));
    }

    private static void addContext(Builder builder, Message contextData) {
        Descriptor descriptor = contextData.getDescriptorForType();
        for (FieldDescriptor field : descriptor.getFields()) {
            if (field.getName().equals(NODE_ID_KEY)) {
                continue;
            }

            Object value = contextData.getField(field);
            if (value instanceof Number) {
                builder.addLabels(PrometheusTypes.Label.newBuilder()
                    .setName(CortexTSS.sanitizeLabelName(field.getName()))
                    .setValue(CortexTSS.sanitizeLabelValue("" + value)));
            } else if (value instanceof Message) {
                addContext(builder, (Message) value);
            }
        }
    }

}
