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

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.opennms.horizon.tsdata.metrics.GaugeFactory;
import org.opennms.horizon.tsdata.metrics.MetricsPushAdapter;
import org.opennms.taskset.contract.DetectorResponse;
import org.opennms.taskset.contract.MonitorResponse;
import org.opennms.taskset.contract.TaskResult;
import org.opennms.taskset.contract.TaskSetResults;
import org.springframework.context.annotation.PropertySource;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.google.protobuf.InvalidProtocolBufferException;

import io.prometheus.client.Gauge;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@PropertySource("classpath:application.yml")
public class TSDataProcessor {
    public static final Long INVALID_UP_TIME = -1L;
    private final MetricsPushAdapter pushAdapter;
    private final GaugeFactory gaugeFactory;
    private final Map<String, Long> snmpUpTimeCache = new ConcurrentHashMap<>();

    public TSDataProcessor(MetricsPushAdapter pushAdapter, GaugeFactory gaugeFactory) {
        this.pushAdapter = pushAdapter;
        this.gaugeFactory = gaugeFactory;
    }

    @KafkaListener(topics = "${kafka.topics}", concurrency = "1")
    public void consume(byte[] data) {
        try {
            TaskSetResults results = TaskSetResults.parseFrom(data);
            results.getResultsList().forEach(result -> CompletableFuture.supplyAsync(()->{
                try {
                    if (result != null) {
                        if (result.hasMonitorResponse()) {
                            MonitorResponse monitorResponse = result.getMonitorResponse();

                            switch (monitorResponse.getMonitorType()) {
                                case ICMP:
                                    processIcmpMonitorResponse(result, monitorResponse);
                                    break;

                                case SNMP:
                                    processSnmpMonitorResponse(result, monitorResponse);
                                    break;

                                default:
                                    log.warn("Have response for unrecognized monitor type: type={}", monitorResponse.getMonitorType());
                                    break;
                            }
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

    private void processIcmpMonitorResponse(TaskResult taskResult, MonitorResponse monitorResponse) {
        double responseTimeMs = taskResult.getMonitorResponse().getResponseTimeMs();

        updateIcmpMetrics(
            monitorResponse.getIpAddress(),
            taskResult.getLocation(),
            taskResult.getSystemId(),
            responseTimeMs,
            monitorResponse.getMetricsMap()
        );
    }

    private void processSnmpMonitorResponse(TaskResult taskResult, MonitorResponse monitorResponse) {
        double responseTimeMs = taskResult.getMonitorResponse().getResponseTimeMs();

        updateSnmpMetrics(
            monitorResponse.getIpAddress(),
            taskResult.getLocation(),
            taskResult.getSystemId(),
            monitorResponse.getStatus(),
            responseTimeMs,
            monitorResponse.getMetricsMap()
        );
    }

    private void updateIcmpMetrics(String ipAddress, String location, String systemId, double responseTime, Map<String, Double> metrics) {
        commonUpdateMonitorMetrics(gaugeFactory.lookupGauge(GaugeFactory.METRICS_NAME_ICMP_TRIP), ipAddress, location, systemId, responseTime, metrics);
    }

    private void updateSnmpMetrics(String ipAddress, String location, String systemId, String status, double responseTime, Map<String, Double> metrics) {
        String[] labelValues = commonUpdateMonitorMetrics(gaugeFactory.lookupGauge(GaugeFactory.METRICS_NAME_SNMP_TRIP), ipAddress, location, systemId, responseTime, metrics);
        updateSnmpUptime(ipAddress, location, status, labelValues);
    }

    private void updateSnmpUptime(String ipAddress, String location, String status, String[] labelValues) {
        if ("Up".equalsIgnoreCase(status)) {
            Long firstUpTime = snmpUpTimeCache.get(ipAddress);
            long totalUpTimeInNanoSec = 0;
            if ((firstUpTime != null) && (firstUpTime.longValue() != INVALID_UP_TIME)) {
                totalUpTimeInNanoSec = System.nanoTime() - firstUpTime;
            } else {
                snmpUpTimeCache.put(ipAddress, System.nanoTime());
            }
            long totalUpTimeInSec = TimeUnit.NANOSECONDS.toSeconds(totalUpTimeInNanoSec);

            gaugeFactory.lookupGauge(GaugeFactory.METRICS_NAME_SNMP_UP).labels(labelValues).set(totalUpTimeInSec);

            log.info("Total upTime of SNMP for {} at location {} : {} sec", ipAddress, location, totalUpTimeInSec);
        } else {
            snmpUpTimeCache.put(ipAddress, INVALID_UP_TIME);
        }
    }

    private String[] commonUpdateMonitorMetrics(Gauge gauge, String ipAddress, String location, String systemId, double responseTime, Map<String, Double> metrics) {
        String[] labelValues = {ipAddress, location, systemId};

        // Update the response-time gauge
        gauge.labels(labelValues).set(responseTime);

        // Also update the gauges for additional metrics from the monitor
        for (Map.Entry<String, Double> oneMetric : metrics.entrySet()) {
            try {
                Gauge dynamicMetricGauge = gaugeFactory.lookupGauge(oneMetric.getKey());
                dynamicMetricGauge.labels(labelValues).set(oneMetric.getValue());
            } catch (Exception exc) {
                log.warn("Failed to record metric: metric-name={}; value={}", oneMetric.getKey(), oneMetric.getValue(), exc);
            }
        }

        pushMetrics(labelValues);

        return labelValues;
    }

    private void pushMetrics(String[] labelValues) {
        var groupingKey =
            IntStream
                .range(0, GaugeFactory.LABEL_NAMES.length)
                .boxed()
                .collect(Collectors.toMap(i -> GaugeFactory.LABEL_NAMES[i], i -> labelValues[i]));

        pushAdapter.pushMetrics(gaugeFactory.getCollectorRegistry(), groupingKey);
    }
}
