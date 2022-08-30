package org.opennms.horizon.core.monitor;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Value;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Gauge;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.opennms.horizon.grpc.tasksets.contract.TaskSetResults;
import org.opennms.horizon.metrics.api.OnmsMetricsAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Process results received from Minions
 */
public class DeviceMonitorResultProcessor implements Processor {

    public static final String RESPONSE_TIME_PARAMETER = "response.time";

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(DeviceMonitorResultProcessor.class);

    private Logger log = DEFAULT_LOGGER;

    private final OnmsMetricsAdapter onmsMetricsAdapter;

    private final CollectorRegistry collectorRegistry = new CollectorRegistry();
    private static final String[] labelNames = {"instance", "location"};
    private final Gauge rttGauge = Gauge.build().name("icmp_round_trip_time").help("ICMP round trip time")
        .unit("msec").labelNames(labelNames).register(collectorRegistry);

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

        List<TaskSetResults.TaskResult> resultsList = results.getResultsList();
        for (TaskSetResults.TaskResult oneResult : resultsList) {
            try {
            Double responseTime =
                Optional.of(oneResult)
                    .map(TaskSetResults.TaskResult::getParametersMap)
                    .map((map) -> map.get("response.time"))
                    .map(this::extractResponseTimeFromProtoAny)
                    .orElse(null)
                    ;

                if (responseTime == null) {
                    // TODO: throttle
                    log.warn("Task result appears to be missing the response time parameter: parameter={}", RESPONSE_TIME_PARAMETER);
                }

                // Convert to milliseconds
                double responseTimeMs = responseTime / 1000.0;

                // TBD888: NEED actual IP address and location name
                updateIcmpMetric(responseTimeMs, "TBD-IP-ADDR", "TBD-LOCATION");
            } catch (Exception exc) {
                // TODO: throttle
                log.warn("Error processing task result", exc);
            }
        }
    }

//========================================
// Internals
//----------------------------------------

    private Double extractResponseTimeFromProtoAny(Any any) {
        try {
            Value doubleValue = Value.parseFrom(any.getValue());
            return doubleValue.getNumberValue();
        } catch (InvalidProtocolBufferException ipbExc) {
            throw new RuntimeException(ipbExc);
        }
    }

    private void updateIcmpMetric(double responseTime, String ipAddress, String location) {
        String[] labelValues = {ipAddress, location};
        var groupingKey = IntStream.range(0, labelNames.length).boxed()
            .collect(Collectors.toMap(i -> labelNames[i], i -> labelValues[i]));
        rttGauge.labels(labelValues).set(responseTime);
        onmsMetricsAdapter.pushMetrics(collectorRegistry, groupingKey);
    }

}
