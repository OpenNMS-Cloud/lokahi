/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2021 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2021 The OpenNMS Group, Inc.
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

package org.opennms.timeseries.cortex;


import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.opennms.taskset.contract.DetectorResponse;
import org.opennms.taskset.contract.MonitorResponse;
import org.opennms.taskset.contract.TaskResult;
import org.opennms.taskset.contract.TaskSetResults;
import org.opennms.timeseries.cortex.shaded.resilience4j.bulkhead.Bulkhead;
import org.opennms.timeseries.cortex.shaded.resilience4j.bulkhead.BulkheadConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.xerial.snappy.Snappy;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.protobuf.InvalidProtocolBufferException;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;


/**
 * Time Series Storage integration for Cortex.
 * We use the cortex api to write data (writes to the ingester) and the prometheus api to read data (reads from the Querier).
 * Even though it's possible to read from the ingester it does only give us the most resent data (still held in memory) therefore
 * we use the querier.
 * Docs:
 * - https://cortexmetrics.io/docs/api/
 * - https://prometheus.io/docs/prometheus/latest/querying/api/
 *
 * @author jwhite
 */
@Slf4j
public class CortexTSS {
    private static final Logger LOG = LoggerFactory.getLogger(CortexTSS.class);

    private static final String X_SCOPE_ORG_ID_HEADER = "X-Scope-OrgID";
    private static final MediaType PROTOBUF_MEDIA_TYPE = MediaType.parse("application/x-protobuf");
    private static final String[] MONITOR_METRICS_LABEL_NAMES = {
        "instance",
        "location",
        "system_id",
        "monitor",
        "node_id"};

    private final OkHttpClient client;

    private final MetricRegistry metrics = new MetricRegistry();
    private final Meter samplesWritten = metrics.meter("samplesWritten");
    private final Meter samplesLost = metrics.meter("samplesLost");

    private final Bulkhead asyncHttpCallsBulkhead;
    private final CortexTSSConfig config;

    public CortexTSS(final CortexTSSConfig config) {
        this.config = Objects.requireNonNull(config);

        ConnectionPool connectionPool = new ConnectionPool(config.getMaxConcurrentHttpConnections(), 5, TimeUnit.MINUTES);
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(config.getMaxConcurrentHttpConnections());
        dispatcher.setMaxRequestsPerHost(config.getMaxConcurrentHttpConnections());

        this.client = new OkHttpClient.Builder()
            .readTimeout(config.getReadTimeoutInMs(), TimeUnit.MILLISECONDS)
            .writeTimeout(config.getWriteTimeoutInMs(), TimeUnit.MILLISECONDS)
            .dispatcher(dispatcher)
            .connectionPool(connectionPool)
            .build();

        BulkheadConfig bulkheadConfig = BulkheadConfig.custom()
            .maxConcurrentCalls(config.getMaxConcurrentHttpConnections() * 6)
            .maxWaitDuration(Duration.ofMillis(config.getBulkheadMaxWaitDurationInMs()))
            .fairCallHandlingStrategyEnabled(true)
            .build();
        asyncHttpCallsBulkhead = Bulkhead.of("asyncHttpCalls", bulkheadConfig);
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
                            store(result);
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

    public void store(final TaskResult result) throws IOException {
        prometheus.PrometheusRemote.WriteRequest.Builder writeBuilder = prometheus.PrometheusRemote.WriteRequest.newBuilder();
        writeBuilder.addTimeseries(toPrometheusTimeSeries(result));

        prometheus.PrometheusRemote.WriteRequest writeRequest = writeBuilder.build();

        // Compress the write request using Snappy
        final byte[] writeRequestCompressed;
        writeRequestCompressed = Snappy.compress(writeRequest.toByteArray());

        // Build the HTTP request
        final RequestBody body = RequestBody.create(PROTOBUF_MEDIA_TYPE, writeRequestCompressed);
        final Request.Builder builder = new Request.Builder()
            .url(config.getWriteUrl())
            .addHeader("X-Prometheus-Remote-Write-Version", "0.1.0")
            .addHeader("Content-Encoding", "snappy")
            .addHeader("User-Agent", CortexTSS.class.getCanonicalName())
            .post(body);
        // Add the OrgId header if set
        String clientID = config.getOrganizationId();
        if (clientID != null && clientID.trim().length() > 0) {
            builder.addHeader(X_SCOPE_ORG_ID_HEADER, clientID);
        }
        final Request request = builder.build();

        LOG.trace("Writing: {}", writeRequest);
        asyncHttpCallsBulkhead.executeCompletionStage(() -> executeAsync(request)).whenComplete((r, ex) -> {
            if (ex == null) {
                samplesWritten.mark(1);
            } else {
                // FIXME: Data loss
                samplesLost.mark(1);
                LOG.error("Error occurred while storing result, sample will be lost.", ex);
            }
        });
    }

    public CompletableFuture<Void> executeAsync(Request request) {
        final CompletableFuture<Void> future = new CompletableFuture<>();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(Call call, Response response) {
                try (ResponseBody body = response.body()) {
                    if (!response.isSuccessful()) {
                        String bodyAsString = "(null)";
                        if (body != null) {
                            try {
                                bodyAsString = body.string();
                            } catch (IOException e) {
                                bodyAsString = "(error reading body)";
                            }
                        }
                        future.completeExceptionally(new RuntimeException(String.format("Writing to Prometheus failed: %s - %s: %s",
                            response.code(),
                            response.message(),
                            bodyAsString)));
                    } else {
                        future.complete(null);
                    }
                }
            }
        });
        return future;
    }

    private static prometheus.PrometheusTypes.TimeSeries.Builder toPrometheusTimeSeries(TaskResult result) {
        MonitorResponse response = result.getMonitorResponse();
        String[] labelValues = {response.getIpAddress(), result.getLocation(), result.getSystemId(), response.getMonitorType().name(), String.valueOf(response.getNodeId())};

        prometheus.PrometheusTypes.TimeSeries.Builder builder = prometheus.PrometheusTypes.TimeSeries.newBuilder();
        for (int i = 0; i < MONITOR_METRICS_LABEL_NAMES.length; i++) {
            builder.addLabels(prometheus.PrometheusTypes.Label.newBuilder()
                .setName(sanitizeLabelName(MONITOR_METRICS_LABEL_NAMES[i]))
                .setValue(sanitizeLabelValue(labelValues[i])));
        }
        builder.addSamples(prometheus.PrometheusTypes.Sample.newBuilder()
            //is now okay ?
            .setTimestamp(Instant.now().toEpochMilli())
            .setValue(response.getResponseTimeMs()));

        return builder;
    }

    public static String sanitizeLabelName(String labelName) {
        // Hard-coded implementation optimized for speed - see
        // See https://github.com/prometheus/common/blob/v0.22.0/model/labels.go#L95
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < labelName.length(); i++) {
            char b = labelName.charAt(i);
            if (!((b >= 'a' && b <= 'z') || (b >= 'A' && b <= 'Z') || b == '_' || (b >= '0' && b <= '9' && i > 0))) {
                sb.append("_");
            } else {
                sb.append(b);
            }
        }
        return sb.toString();
    }

    public static String sanitizeLabelValue(String labelValue) {
        // limit label value to 2048 characters
        return labelValue.substring(0, Math.min(labelValue.length(), 2048));
    }
}
