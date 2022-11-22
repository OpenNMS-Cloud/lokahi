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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.opennms.horizon.tsdata.metrics.MetricsPushAdapter;
import org.opennms.taskset.contract.DetectorResponse;
import org.opennms.taskset.contract.MonitorResponse;
import org.opennms.taskset.contract.TaskResult;
import org.opennms.taskset.contract.TaskSetResults;
import org.springframework.context.annotation.PropertySource;
import org.springframework.kafka.annotation.KafkaListener;
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

    @KafkaListener(topics = "${kafka.topics}", concurrency = "1")
    public void consume(byte[] data) {
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
        Gauge gauge = getGaugeFromName(METRICS_NAME_RESPONSE, true);
        gauge.labels(labelValues).set(response.getResponseTimeMs());
        Map<String, String> labels = new HashMap<>();
        for (int i = 0; i < MONITOR_METRICS_LABEL_NAMES.length; i++) {
            labels.put(MONITOR_METRICS_LABEL_NAMES[i], labelValues[i]);
        }

        if (response.getMetricsMap() != null) {
            response.getMetricsMap().forEach((k, v) -> {
                Gauge extGauge = getGaugeFromName(METRICS_NAME_PREFIX_MONITOR + k, false);
                extGauge.labels(labelValues).set(v);
            });
        }
        pushAdapter.pushMetrics(collectorRegistry, labels);
    }

    private Gauge getGaugeFromName(String name, boolean withDesc) {
        return gauges.compute(name, (key, gauge) -> {
            if (gauge != null) {
                return gauge;
            }
            if (withDesc) {
                return Gauge.build()
                    .name(name)
                    .help("Monitor round trip response time")
                    .unit(METRICS_UNIT_MS)
                    .labelNames(MONITOR_METRICS_LABEL_NAMES)
                    .register(collectorRegistry);
            }
            return Gauge.build()
                .name(name)
                .unit(METRICS_UNIT_MS)
                .labelNames(MONITOR_METRICS_LABEL_NAMES)
                .register(collectorRegistry);
        });
    }
}
