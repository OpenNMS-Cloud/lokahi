package org.opennms.horizon.core.monitor;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Gauge;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.opennms.horizon.metrics.api.OnmsMetricsAdapter;
import org.opennms.taskset.contract.TaskResult;
import org.opennms.taskset.contract.TaskSetResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Process results received from Minions
 */
public class DeviceMonitorResultProcessor implements Processor {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(DeviceMonitorResultProcessor.class);
    private static final String[] labelNames = {"instance", "location", "system_id"};
    private final OnmsMetricsAdapter onmsMetricsAdapter;

    private final CollectorRegistry collectorRegistry = new CollectorRegistry();
    private final Gauge rttGauge = Gauge.build().name("icmp_round_trip_time").help("ICMP round trip time")
        .unit("msec").labelNames(labelNames).register(collectorRegistry);
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
                    // Update the response time metric
                    double responseTime = oneResult.getResponseTime();

                    // Convert from us to ms.  TODO: use consistent units
                    double responseTimeMs = responseTime / 1000.0;

                    updateIcmpMetrics(
                        oneResult.getIpAddress(),
                        oneResult.getLocation(),
                        oneResult.getSystemId(),
                        responseTimeMs,
                        oneResult.getMetricsMap()
                    );
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

    private void updateIcmpMetrics(String ipAddress, String location, String systemId, double responseTime, Map<String, Double> metrics) {
        String[] labelValues = {ipAddress, location, systemId};

        // Update the response-time gauge
        rttGauge.labels(labelValues).set(responseTime);

        // Also update the gauges for additional metrics from the monitor
        for (Map.Entry<String, Double> oneMetric : metrics.entrySet()) {
            try {
                Gauge gauge = lookupGauge(oneMetric.getKey());
                gauge.labels(labelValues).set(oneMetric.getValue());
            } catch (Exception exc) {
                log.warn("Failed to record metric: metric-name={}; value={}", oneMetric.getKey(), oneMetric.getValue(), exc);
            }
        }

        pushMetrics(labelValues);
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
