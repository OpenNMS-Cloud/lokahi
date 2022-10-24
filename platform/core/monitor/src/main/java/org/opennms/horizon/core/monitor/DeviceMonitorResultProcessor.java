package org.opennms.horizon.core.monitor;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Gauge;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.opennms.horizon.metrics.api.OnmsMetricsAdapter;
import org.opennms.taskset.contract.DetectorResponse;
import org.opennms.taskset.contract.MonitorResponse;
import org.opennms.taskset.contract.TaskResult;
import org.opennms.taskset.contract.TaskSetResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Process results received from Minions
 */
public class DeviceMonitorResultProcessor implements Processor {

    public static final Long INVALID_UP_TIME = -1L;

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(DeviceMonitorResultProcessor.class);
    private static final String[] labelNames = {"instance", "location", "system_id"};
    private final OnmsMetricsAdapter onmsMetricsAdapter;
    private final Map<String, Long> snmpUpTimeCache = new ConcurrentHashMap<>();

    private final CollectorRegistry collectorRegistry = new CollectorRegistry();

    private final Gauge icmpResponseTimeGauge =
        Gauge.build()
            .name("icmp_round_trip_time")
            .help("ICMP round trip time")
            .unit("msec")
            .labelNames(labelNames)
            .register(collectorRegistry);

    private final Gauge snmpResponseTimeGauge =
        Gauge.build()
            .name("snmp_round_trip_time")
            .help("SNMP round trip time")
            .unit("msec")
            .labelNames(labelNames)
            .register(collectorRegistry);

    private final Gauge snmpUpTimeGauge =
        Gauge.build()
            .name("snmp_uptime_sec")
            .help("SNMP UP time")
            .unit("sec")
            .labelNames(labelNames)
            .register(collectorRegistry);

    private Logger log = DEFAULT_LOGGER;
    private Map<String, Gauge> gauges = new ConcurrentHashMap<>();

    //========================================
    // Constructor
    //----------------------------------------

    public DeviceMonitorResultProcessor(OnmsMetricsAdapter onmsMetricsAdapter) {
        this.onmsMetricsAdapter = onmsMetricsAdapter;
    }


    //========================================
    // Processing
    //----------------------------------------

    @Override
    public void process(Exchange exchange) throws Exception {
        TaskSetResults results = exchange.getIn().getMandatoryBody(TaskSetResults.class);

        List<TaskResult> resultsList = results.getResultsList();
        for (TaskResult oneResult : resultsList) {

            // TODO: update all returned metrics from the monitor
            // TODO: support monitor results vs detector results
            try {
                if (oneResult != null) {
                    if (oneResult.hasMonitorResponse()) {
                        MonitorResponse monitorResponse = oneResult.getMonitorResponse();

                        switch (monitorResponse.getMonitorType()) {
                            case ICMP:
                                processIcmpMonitorResponse(oneResult, monitorResponse);
                                break;

                            case SNMP:
                                processSnmpMonitorResponse(oneResult, monitorResponse);
                            break;

                            default:
                                log.warn("Have response for unrecognized monitor type: type={}", monitorResponse.getMonitorType());
                                break;
                        }
                    } else if (oneResult.hasDetectorResponse()) {
                        DetectorResponse detectorResponse = oneResult.getDetectorResponse();

                        // TBD: how to process?
                        log.info("Have detector response: task-id={}; detected={}", oneResult.getId(), detectorResponse.getDetected());
                    }
                } else {
                    log.warn("Task result appears to be missing the echo response details");
                }
            } catch (Exception exc) {
                // TODO: throttle
                log.warn("Error processing task result", exc);
            }
        }
    }

//========================================
// Internals
//----------------------------------------

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
        commonUpdateMonitorMetrics(icmpResponseTimeGauge, ipAddress, location, systemId, responseTime, metrics);
    }

    private void updateSnmpMetrics(String ipAddress, String location, String systemId, String status, double responseTime, Map<String, Double> metrics) {
        String[] labelValues = commonUpdateMonitorMetrics(snmpResponseTimeGauge, ipAddress, location, systemId, responseTime, metrics);
        updateSnmpUptime(ipAddress, location, systemId, status, labelValues);
    }

    private void updateSnmpUptime(String ipAddress, String location, String systemId, String status, String[] labelValues) {
        if ("Up".equalsIgnoreCase(status)) {
            Long firstUpTime = snmpUpTimeCache.get(ipAddress);
            long totalUpTimeInNanoSec = 0;
            if ((firstUpTime != null) && (firstUpTime.longValue() != INVALID_UP_TIME)) {
                totalUpTimeInNanoSec = System.nanoTime() - firstUpTime;
            } else {
                snmpUpTimeCache.put(ipAddress, System.nanoTime());
            }
            long totalUpTimeInSec = TimeUnit.NANOSECONDS.toSeconds(totalUpTimeInNanoSec);

            snmpUpTimeGauge.labels(labelValues).set(totalUpTimeInSec);

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
                Gauge dynamicMetricGauge = lookupGauge(oneMetric.getKey());
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
                .range(0, labelNames.length)
                .boxed()
                .collect(Collectors.toMap(i -> labelNames[i], i -> labelValues[i]));

        onmsMetricsAdapter.pushMetrics(collectorRegistry, groupingKey);
    }

    private Gauge lookupGauge(String name) {
        Gauge result = gauges.compute(name, (key, gauge) -> {
            if (gauge != null) {
                return gauge;
            }

            return
                Gauge.build()
                    .name(name)
                    .unit("msec")
                    .labelNames(labelNames)
                    .register(collectorRegistry)
                ;
        });

        return result;
    }
}
