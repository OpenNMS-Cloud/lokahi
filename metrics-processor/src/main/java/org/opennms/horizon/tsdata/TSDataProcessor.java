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

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.base.Strings;
import com.google.protobuf.Any;
import org.opennms.horizon.snmp.api.SnmpResponseMetric;
import org.opennms.horizon.snmp.api.SnmpValueType;
import org.opennms.horizon.tsdata.metrics.MetricsPushAdapter;
import org.opennms.taskset.contract.CollectorResponse;
import org.opennms.taskset.contract.DetectorResponse;
import org.opennms.taskset.contract.MonitorResponse;
import org.opennms.taskset.contract.MonitorType;
import org.opennms.taskset.contract.TaskResult;
import org.opennms.taskset.contract.TaskSetResults;
import org.springframework.context.annotation.PropertySource;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import com.google.protobuf.InvalidProtocolBufferException;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Gauge;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@PropertySource("classpath:application.yml")
public class TSDataProcessor {
    private static final String METRICS_NAME_PREFIX_MONITOR = "monitor_";
    private static final String METRICS_UNIT_MS = "msec";
    private static final String METRICS_NAME_RESPONSE = "response_time";

    private static final String[] MONITOR_METRICS_LABEL_NAMES = {
        "instance",
        "location",
        "system_id",
        "monitor",
        "node_id"};
    private final CollectorRegistry collectorRegistry = new CollectorRegistry();
    private final Map<String, Gauge> gauges = new ConcurrentHashMap<>();
    private final MetricsPushAdapter pushAdapter;

    public TSDataProcessor(MetricsPushAdapter pushAdapter) {
        this.pushAdapter = pushAdapter;
    }

    //headers for future use.
    @KafkaListener(topics = "${kafka.topics}", concurrency = "1")
    public void consume(@Payload byte[] data, @Headers Map<String, Object> headers) {
        try {
            TaskSetResults results = TaskSetResults.parseFrom(data);
            results.getResultsList().forEach(result -> CompletableFuture.supplyAsync(() -> {
                try {
                    if (result != null) {
                        log.info("Processing task set result {}", result);
                        if (result.hasMonitorResponse()) {
                            processMonitorResponse(result);
                        } else if (result.hasDetectorResponse()) {
                            DetectorResponse detectorResponse = result.getDetectorResponse();
                            // TBD: how to process?
                            log.info("Have detector response: task-id={}; detected={}", result.getId(), detectorResponse.getDetected());
                        } else if(result.hasCollectorResponse()) {
                            processCollectorResponse(result);
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

    private void processMonitorResponse(TaskResult result) {
        MonitorResponse response = result.getMonitorResponse();
        String[] labelValues = {response.getIpAddress(), result.getLocation(), result.getSystemId(), response.getMonitorType().name(), String.valueOf(response.getNodeId())};
        Gauge gauge = getGaugeFrom(METRICS_NAME_RESPONSE, "Monitor round trip response time", METRICS_UNIT_MS);
        gauge.labels(labelValues).set(response.getResponseTimeMs());
        Map<String, String> labels = new HashMap<>();
        for (int i = 0; i < MONITOR_METRICS_LABEL_NAMES.length; i++) {
            labels.put(MONITOR_METRICS_LABEL_NAMES[i], labelValues[i]);
        }

        response.getMetricsMap().forEach((k, v) -> {
            Gauge extGauge = getGaugeFrom(METRICS_NAME_PREFIX_MONITOR + k, null, null);
            extGauge.labels(labelValues).set(v);
        });

        pushAdapter.pushMetrics(collectorRegistry, labels);
    }

    private Gauge getGaugeFrom(String name, String description, String unit) {
        return gauges.compute(name, (key, gauge) -> {
            if (gauge != null) {
                return gauge;
            }
            var builder = Gauge.build().name(name).labelNames(TSDataProcessor.MONITOR_METRICS_LABEL_NAMES);

            if (!Strings.isNullOrEmpty(description)) {
                builder.help(description);
            }
            if (!Strings.isNullOrEmpty(unit)) {
                builder.unit(unit);
            }

            return builder.register(collectorRegistry);
        });
    }

    private void processCollectorResponse(TaskResult result) {
        CollectorResponse response = result.getCollectorResponse();
        String[] labelValues = {response.getIpAddress(), result.getLocation(), result.getSystemId(),
            response.getMonitorType().name(), String.valueOf(response.getNodeId())};
        if (response.hasResult() && response.getMonitorType().equals(MonitorType.SNMP)) {
            Any collectorMetric = response.getResult();
            try {
                var snmpResponse = collectorMetric.unpack(SnmpResponseMetric.class);
                snmpResponse.getResultsList().forEach((snmpResult) -> {
                    String metricName = snmpResult.getAlias();
                    String description = metricName + " with oid " + snmpResult.getBase();
                    int type = snmpResult.getValue().getTypeValue();
                    switch (type) {
                        case SnmpValueType.INT32_VALUE:
                            Gauge int32Value = getGaugeFrom(metricName, description, null);
                            int32Value.labels(labelValues).set(snmpResult.getValue().getSint64());
                            break;
                        case SnmpValueType.COUNTER32_VALUE:
                            // TODO: Can't set a counter through prometheus API, may be possible with remote write
                        case SnmpValueType.TIMETICKS_VALUE:
                        case SnmpValueType.GAUGE32_VALUE:
                            Gauge uint64Value = getGaugeFrom(metricName, description, null);
                            uint64Value.labels(labelValues).set(snmpResult.getValue().getUint64());
                            break;
                        case SnmpValueType.COUNTER64_VALUE:
                            double metric = new BigInteger(snmpResult.getValue().getBytes().toByteArray()).doubleValue();
                            Gauge gauge = getGaugeFrom(metricName, description, null);
                            gauge.labels(labelValues).set(metric);
                            break;
                    }

                });
            } catch (InvalidProtocolBufferException e) {
                log.warn("Exception while parsing protobuf ", e);

            }
        }
        Map<String, String> labels = new HashMap<>();
        for (int i = 0; i < MONITOR_METRICS_LABEL_NAMES.length; i++) {
            labels.put(MONITOR_METRICS_LABEL_NAMES[i], labelValues[i]);
        }
        pushAdapter.pushMetrics(collectorRegistry, labels);

    }
}
