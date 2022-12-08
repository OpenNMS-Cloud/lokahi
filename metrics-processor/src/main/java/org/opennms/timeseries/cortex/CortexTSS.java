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


import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.json.JSONObject;
import org.opennms.integration.api.v1.distributed.KeyValueStore;
import org.opennms.integration.api.v1.timeseries.Aggregation;
import org.opennms.integration.api.v1.timeseries.IntrinsicTagNames;
import org.opennms.integration.api.v1.timeseries.MetaTagNames;
import org.opennms.integration.api.v1.timeseries.Metric;
import org.opennms.integration.api.v1.timeseries.Sample;
import org.opennms.integration.api.v1.timeseries.StorageException;
import org.opennms.integration.api.v1.timeseries.Tag;
import org.opennms.integration.api.v1.timeseries.TagMatcher;
import org.opennms.integration.api.v1.timeseries.TimeSeriesFetchRequest;
import org.opennms.integration.api.v1.timeseries.TimeSeriesStorage;
import org.opennms.integration.api.v1.timeseries.immutables.ImmutableTagMatcher.TagMatcherBuilder;
import org.opennms.timeseries.cortex.shaded.resilience4j.bulkhead.Bulkhead;
import org.opennms.timeseries.cortex.shaded.resilience4j.bulkhead.BulkheadConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xerial.snappy.Snappy;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Sets;

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
public class CortexTSS implements TimeSeriesStorage {
    private static final Logger LOG = LoggerFactory.getLogger(CortexTSS.class);

    // Label name indicating the metric name of a timeseries.
    public static final String METRIC_NAME_LABEL = "__name__";

    private static final String X_SCOPE_ORG_ID_HEADER = "X-Scope-OrgID";

    private static final MediaType PROTOBUF_MEDIA_TYPE = MediaType.parse("application/x-protobuf");

    protected static final Set<String> INTRINSIC_TAG_NAMES = Sets.newHashSet(IntrinsicTagNames.name, IntrinsicTagNames.resourceId);

    protected static final Set<Aggregation> SUPPORTED_AGGREGATION = new HashSet<>(Arrays.asList(Aggregation.AVERAGE, Aggregation.MAX, Aggregation.MIN));

    static final int MAX_SAMPLES = 1200;
    public static final String EXCEPTION_OCCURRED_PERSISTING_EXTERNAL_TAG_FOR_METRIC = "Exception occurred persisting external tag for metric: {}";

    private final OkHttpClient client;

    private final MetricRegistry metrics = new MetricRegistry();
    private final Meter samplesWritten = metrics.meter("samplesWritten");
    private final Meter samplesLost = metrics.meter("samplesLost");

    // when retrieving aggregated time series data we loose the metric information and thus take it from cache
    private final Cache<String, Metric> metricCache;
    private final Bulkhead asyncHttpCallsBulkhead;
    private final CortexTSSConfig config;

    public static final String CORTEX_TSS = "CORTEX_TSS";
    private final KeyValueStore<String> kvStore;
    private final Cache<String, String> externalTagsCache;
    private final Meter extTagsModified = metrics.meter("extTagsModified");
    private final Meter extTagsCacheUsed = metrics.meter("extTagsCacheUsed");
    private final Meter extTagsCacheMissed = metrics.meter("extTagsCacheMissed");
    private final Meter extTagPutTransactionFailed = metrics.meter("extTagPutTransactionFailed");

    public CortexTSS(final CortexTSSConfig config, final KeyValueStore<String> keyValueStore) {
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

        this.externalTagsCache = CacheBuilder.newBuilder().maximumSize(config.getExternalTagsCacheSize()).build();
        this.kvStore = keyValueStore;

        this.metricCache = CacheBuilder.newBuilder().maximumSize(config.getMetricCacheSize()).build();

        BulkheadConfig bulkheadConfig = BulkheadConfig.custom()
            .maxConcurrentCalls(config.getMaxConcurrentHttpConnections() * 6)
            .maxWaitDuration(Duration.ofMillis(config.getBulkheadMaxWaitDurationInMs()))
            .fairCallHandlingStrategyEnabled(true)
            .build();
        asyncHttpCallsBulkhead = Bulkhead.of("asyncHttpCalls", bulkheadConfig);

        // Expose HTTP client statistics
        metrics.register("connectionCount", (Gauge<Integer>) () -> client.connectionPool().connectionCount());
        metrics.register("idleConnectionCount", (Gauge<Integer>) () -> client.connectionPool().idleConnectionCount());
        metrics.register("queuedCallsCount", (Gauge<Integer>) () -> client.dispatcher().queuedCallsCount());
        metrics.register("runningCallsCount", (Gauge<Integer>) () -> client.dispatcher().runningCallsCount());
        metrics.register("availableConcurrentCalls", (Gauge<Integer>) () -> asyncHttpCallsBulkhead.getMetrics().getAvailableConcurrentCalls());
        metrics.register("maxAllowedConcurrentCalls", (Gauge<Integer>) () -> asyncHttpCallsBulkhead.getMetrics().getMaxAllowedConcurrentCalls());

        this.kvStore.enumerateContextAsync(CORTEX_TSS).thenAccept(externalTagsCache::putAll);
    }

    @Override
    public void store(final List<Sample> samples) throws StorageException {
        store(samples, config.getOrganizationId());
    }

    public void store(final List<Sample> samples, String clientID) throws StorageException {
        final List<Sample> samplesSorted = samples.stream() // Cortex doesn't like the Samples to be out of time order
            .sorted(Comparator.comparing(Sample::getTime))
            .collect(Collectors.toList());

        prometheus.PrometheusRemote.WriteRequest.Builder writeBuilder = prometheus.PrometheusRemote.WriteRequest.newBuilder();
        samplesSorted.forEach(s -> {
            writeBuilder.addTimeseries(toPrometheusTimeSeries(s));
            persistExternalTags(s);
        });

        prometheus.PrometheusRemote.WriteRequest writeRequest = writeBuilder.build();

        // Compress the write request using Snappy
        final byte[] writeRequestCompressed;
        try {
            writeRequestCompressed = Snappy.compress(writeRequest.toByteArray());
        } catch (IOException e) {
            throw new StorageException(e);
        }

        // Build the HTTP request
        final RequestBody body = RequestBody.create(PROTOBUF_MEDIA_TYPE, writeRequestCompressed);
        final Request.Builder builder = new Request.Builder()
            .url(config.getWriteUrl())
            .addHeader("X-Prometheus-Remote-Write-Version", "0.1.0")
            .addHeader("Content-Encoding", "snappy")
            .addHeader("User-Agent", CortexTSS.class.getCanonicalName())
            .post(body);
        // Add the OrgId header if set
        if (clientID != null && clientID.trim().length() > 0) {
            builder.addHeader(X_SCOPE_ORG_ID_HEADER, clientID);
        }
        final Request request = builder.build();

        LOG.trace("Writing: {}", writeRequest);
        asyncHttpCallsBulkhead.executeCompletionStage(() -> executeAsync(request)).whenComplete((r, ex) -> {
            if (ex == null) {
                samplesWritten.mark(samplesSorted.size());
            } else {
                // FIXME: Data loss
                samplesLost.mark(samples.size());
                LOG.error("Error occurred while storing samples, sample will be lost.", ex);
            }
        });
    }

    private void persistExternalTags(final Sample s) {
        // save external tags on a separate database
        String key = s.getMetric().getKey();
        var externalTags = externalTagsCache.getIfPresent(key);
        boolean needUpsert = false;

        JSONObject jsonMetrics = null;
        JSONObject jsonNewMetric = new JSONObject();

        if (config.getExternalTagsCacheSize() > 0 && externalTags != null) {
            jsonMetrics = new JSONObject(externalTags);
            for (Tag tag : s.getMetric().getExternalTags()) {
                if (!jsonMetrics.has(tag.getKey())) {
                    jsonMetrics.put(tag.getKey(), tag.getValue());
                    needUpsert = true;
                }
            }
            if (needUpsert) {
                kvStore.putAsync(key, jsonMetrics.toString(), CORTEX_TSS)
                    .whenComplete((res, ex) -> {
                        if (ex != null) {
                            LOG.debug(EXCEPTION_OCCURRED_PERSISTING_EXTERNAL_TAG_FOR_METRIC, key);
                            extTagPutTransactionFailed.mark();
                        }
                    });
                extTagsModified.mark();
            }
            extTagsCacheUsed.mark();
        } else {
            var externalMetricFromDb = kvStore.get(s.getMetric().getKey(), CORTEX_TSS);
            if (externalMetricFromDb.isPresent()) {
                jsonMetrics = new JSONObject(externalMetricFromDb.get());
                for (Tag tag : s.getMetric().getExternalTags()) {
                    if (!jsonMetrics.has(tag.getKey())) {
                        jsonMetrics.put(tag.getKey(), tag.getValue());
                        needUpsert = true;
                    }
                }
                if (needUpsert) {
                    kvStore.putAsync(key, jsonMetrics.toString(), CORTEX_TSS)
                        .whenComplete((res, ex) -> {
                            if (ex != null) {
                                LOG.debug(EXCEPTION_OCCURRED_PERSISTING_EXTERNAL_TAG_FOR_METRIC, key);
                                extTagPutTransactionFailed.mark();
                            }
                        });
                    externalTagsCache.put(key, jsonMetrics.toString());
                    extTagsModified.mark();
                }
                //missed caching this record
                extTagsCacheMissed.mark();
            } else {
                for (Tag tag : s.getMetric().getExternalTags()) {
                    jsonNewMetric.put(tag.getKey(), tag.getValue());
                }
                kvStore.putAsync(key, jsonNewMetric.toString(), CORTEX_TSS)
                    .whenComplete((res, ex) -> {
                        if (ex != null) {
                            LOG.debug(EXCEPTION_OCCURRED_PERSISTING_EXTERNAL_TAG_FOR_METRIC, key);
                            extTagPutTransactionFailed.mark();
                        }
                    });
                externalTagsCache.put(key, jsonNewMetric.toString());
            }
        }
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
                        future.completeExceptionally(new StorageException(String.format("Writing to Prometheus failed: %s - %s: %s",
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

    private static prometheus.PrometheusTypes.TimeSeries.Builder toPrometheusTimeSeries(Sample sample) {
        prometheus.PrometheusTypes.TimeSeries.Builder builder = prometheus.PrometheusTypes.TimeSeries.newBuilder();
        // Convert all of the tags to labels
        Stream.concat(sample.getMetric().getIntrinsicTags().stream(), sample.getMetric().getMetaTags().stream())
            .forEach(tag -> {
                // Special handling for the metric name
                if (IntrinsicTagNames.name.equals(tag.getKey())) {
                    builder.addLabels(prometheus.PrometheusTypes.Label.newBuilder()
                        .setName(METRIC_NAME_LABEL)
                        .setValue(sanitizeMetricName(tag.getValue())));
                } else {
                    builder.addLabels(prometheus.PrometheusTypes.Label.newBuilder()
                        .setName(sanitizeLabelName(tag.getKey()))
                        .setValue(sanitizeLabelValue(tag.getValue())));
                }
            });

        // Add the sample timestamp & value
        builder.addSamples(prometheus.PrometheusTypes.Sample.newBuilder()
            .setTimestamp(sample.getTime().toEpochMilli())
            .setValue(sample.getValue()));
        return builder;
    }

    public static String sanitizeMetricName(String metricName) {
        // Hard-coded implementation optimized for speed - see
        // See https://github.com/prometheus/common/blob/v0.22.0/model/metric.go#L92
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < metricName.length(); i++) {
            char b = metricName.charAt(i);
            if (!((b >= 'a' && b <= 'z') || (b >= 'A' && b <= 'Z') || b == '_' || b == ':' || (b >= '0' && b <= '9' && i > 0))) {
                sb.append("_");
            } else {
                sb.append(b);
            }
        }
        return sb.toString();
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

    @Override
    public List<Metric> findMetrics(Collection<TagMatcher> tagMatchers) throws StorageException {
        return findMetrics(tagMatchers, config.getOrganizationId());
    }

    public List<Metric> findMetrics(Collection<TagMatcher> tagMatchers, String clientID) throws StorageException {
        LOG.info("Retrieving metrics for tagMatchers: {}", tagMatchers);
        Objects.requireNonNull(tagMatchers);
        if (tagMatchers.isEmpty()) {
            throw new IllegalArgumentException("tagMatchers cannot be null");
        }
        String url = String.format("%s/series?match[]={%s}",
            config.getReadUrl(),
            tagMatchersToQuery(tagMatchers));
        String json = makeCallToQueryApi(url, clientID);
        List<Metric> metricList = ResultMapper.fromSeriesQueryResult(json, kvStore);
        metricList.forEach(m -> this.metricCache.put(m.getKey(), m));
        return metricList;
    }

    /**
     * Returns the full metric (incl. meta data from the database).
     * This is only needed if not in cache already - which it should be.
     */
    private Optional<Metric> loadMetric(final Metric metric) throws StorageException {
        Metric loadedMetric = this.metricCache.getIfPresent(metric.getKey());
        if (loadedMetric == null) {
            List<TagMatcher> matchers = metric.getIntrinsicTags().stream()
                .map(TagMatcherBuilder::of) // build matcher that matches this tag
                .map(TagMatcherBuilder::build)
                .collect(Collectors.toList());
            List<Metric> metricList = findMetrics(matchers);
            if (metricList.isEmpty()) {
                return Optional.empty();
            }
            loadedMetric = metricList.get(0);
            this.metricCache.put(loadedMetric.getKey(), loadedMetric);
        }
        return Optional.of(loadedMetric);
    }

    @Override
    public List<Sample> getTimeseries(TimeSeriesFetchRequest request) throws StorageException {
        return getTimeseries(request, config.getOrganizationId());
    }

    public List<Sample> getTimeseries(TimeSeriesFetchRequest request, String clientID) throws StorageException {

        // first load the original metric - we need it for the meta data
        Optional<Metric> metric = loadMetric(request.getMetric());
        if (!metric.isPresent()) {
            return Collections.emptyList();
        }

        String query = createQuery(request, metric.get());
        String url = String.format("%s/query_range?query=%s&start=%s&end=%s&step=%ss",
            config.getReadUrl(),
            query,
            request.getStart().getEpochSecond(),
            request.getEnd().getEpochSecond(),
            determineStepInSeconds(request));
        LOG.info("Retrieving time series for metric: {} with query {}", request, url);


        String json = makeCallToQueryApi(url, clientID);
        return ResultMapper.fromRangeQueryResult(json, metric.get());
    }

    private String createQuery(final TimeSeriesFetchRequest request, final Metric metric) {
        // We build the query from inside out
        StringBuilder query = new StringBuilder();


        // metrics
        query.append("{");
        query.append(tagsToQuery(request.getMetric().getIntrinsicTags()));
        query.append("}");

        // rate
        long interval = (long) (determineStepInSeconds(request) * 2.1d); // make sure we always have at least 2 samples captured
        String type = metric.getFirstTagByKey(MetaTagNames.mtype).getValue();
        if (Metric.Mtype.count.name().equals(type) || Metric.Mtype.counter.name().equals(type)) {
            query.insert(0, "rate(");
            query.append("[");
            query.append(interval);
            query.append("s])");
        }

        // aggregation
        if (!Aggregation.NONE.equals(request.getAggregation())) {
            query.insert(0, "(");
            query.insert(0, toFunction(request.getAggregation()));
            query.append(")");
        }

        return query.toString();
    }

    private String toFunction(final Aggregation aggregation) {
        if (Aggregation.AVERAGE == aggregation) {
            return "avg";
        } else if (Aggregation.MAX == aggregation) {
            return "max";
        } else if (Aggregation.MIN == aggregation) {
            return "min";
        } else {
            throw new IllegalArgumentException("We don't support aggregation " + aggregation);
        }
    }

    private String makeCallToQueryApi(final String url, String clientID) throws StorageException {

        final Request.Builder builder = new Request.Builder()
            .url(url)
            .addHeader("User-Agent", CortexTSS.class.getCanonicalName())
            .get();
        if (clientID != null && clientID.trim().length() > 0) {
            builder.addHeader(X_SCOPE_ORG_ID_HEADER, clientID);
        }
        // Add the Org Id header if set
        if (config.hasOrganizationId()) {
            builder.addHeader(X_SCOPE_ORG_ID_HEADER, config.getOrganizationId());
        }
        final Request httpRequest = builder.build();

        try (Response response = client.newCall(httpRequest).execute()) {
            try (ResponseBody responseBody = response.body()) {
                if (!response.isSuccessful()) {
                    String bodyMsg = "";
                    if (responseBody != null) {
                        bodyMsg = responseBody.string();
                    }
                    throw new StorageException(String.format("Call to %s failed: response code:%s, response message:%s, bodyMessage:%s", url, response.code(), response.message(), bodyMsg));
                }
                if (responseBody != null) {
                    return responseBody.string();
                } else {
                    throw new StorageException(String.format("Call to %s delivered no body.", url));
                }
            }
        } catch (IOException | StorageException e) {
            throw new StorageException(String.format("Call to %s failed.", url), e);
        }
    }

    static long determineStepInSeconds(TimeSeriesFetchRequest request) {
        // step cannot be 0, Prometheus always aggregates in a range query.
        // so we try to calculate a small step but not too small for the range since we don't want to have too many results
        // the maximum seems to be 11,000
        if (request.getStep().getSeconds() > 0) {
            return request.getStep().getSeconds();
        }
        long durationInSeconds = request.getEnd().getEpochSecond() - request.getStart().getEpochSecond();
        double step = Math.ceil(durationInSeconds / (double) MAX_SAMPLES);
        return Math.max(1L, (long) step);
    }

    protected static String tagsToQuery(final Collection<Tag> tags) {
        StringBuilder b = new StringBuilder();
        for (Tag tag : tags) {
            String key;
            String value;
            if (IntrinsicTagNames.name.equals(tag.getKey())) {
                key = METRIC_NAME_LABEL;
                value = sanitizeMetricName(tag.getValue());
            } else {
                key = sanitizeLabelName(tag.getKey());
                // see NMS-13157: the backslash must be escaped since it is the escape character itself
                value = tag.getValue().replace("\\\\", "\\\\\\\\");
            }
            if (b.length() > 0) {
                b.append(", ");
            }
            b.append(key).append("=\"").append(value).append("\"");
        }
        return b.toString();
    }

    protected static String tagMatchersToQuery(final Collection<TagMatcher> tagMatchers) {
        StringBuilder b = new StringBuilder();
        for (TagMatcher matcher : tagMatchers) {
            String key;
            String value;
            if (IntrinsicTagNames.name.equals(matcher.getKey())) {
                key = METRIC_NAME_LABEL;
                if (TagMatcher.Type.EQUALS == matcher.getType() || TagMatcher.Type.NOT_EQUALS == matcher.getType()) {
                    value = sanitizeMetricName(matcher.getValue());
                } else {
                    value = matcher.getValue();
                }
            } else {
                key = sanitizeLabelName(matcher.getKey());
                // see NMS-13157: the backslash must be escaped since it is the escape character itself
                value = matcher.getValue().replace("\\\\", "\\\\\\\\");
            }
            if (b.length() > 0) {
                b.append(", ");
            }
            b.append(key).append(" ").append(toCortexEquals(matcher)).append(" \"").append(value).append("\"");
        }
        return b.toString();
    }

    private static String toCortexEquals(final TagMatcher matcher) {
        // see https://prometheus.io/docs/prometheus/latest/querying/basics/
        switch (matcher.getType()) {
            case EQUALS:
                return "=";
            case NOT_EQUALS:
                return "!=";
            case EQUALS_REGEX:
                return "=~";
            case NOT_EQUALS_REGEX:
                return "!~";
            default:
                throw new IllegalArgumentException("Unknown TagMatcher.Type. Fix me!");
        }
    }

    @Override
    public void delete(Metric metric) {
        LOG.warn("Deletes are not currently supported. Ignoring delete for: {}", metric);
        // delete can only be done when enabled:
        // --web.enable-admin-api flag to Prometheus through start-up script or docker-compose file, depending on installation method.
        // curl -X POST -g 'http://localhost:9090/api/v1/admin/tsdb/delete_series?match[]={foo="bar"}'

    }

    public void destroy() {
    }

    public MetricRegistry getMetrics() {
        return metrics;
    }

    @Override
    public boolean supportsAggregation(Aggregation aggregation) {
        return SUPPORTED_AGGREGATION.contains(aggregation);
    }
}
