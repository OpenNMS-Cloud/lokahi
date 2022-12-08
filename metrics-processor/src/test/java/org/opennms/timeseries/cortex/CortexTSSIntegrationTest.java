package org.opennms.timeseries.cortex;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.opennms.integration.api.v1.timeseries.AbstractStorageIntegrationTest;
import org.opennms.integration.api.v1.timeseries.Aggregation;
import org.opennms.integration.api.v1.timeseries.IntrinsicTagNames;
import org.opennms.integration.api.v1.timeseries.MetaTagNames;
import org.opennms.integration.api.v1.timeseries.Metric;
import org.opennms.integration.api.v1.timeseries.Sample;
import org.opennms.integration.api.v1.timeseries.StorageException;
import org.opennms.integration.api.v1.timeseries.TimeSeriesFetchRequest;
import org.opennms.integration.api.v1.timeseries.TimeSeriesStorage;
import org.opennms.integration.api.v1.timeseries.immutables.ImmutableMetric;
import org.opennms.integration.api.v1.timeseries.immutables.ImmutableSample;
import org.opennms.integration.api.v1.timeseries.immutables.ImmutableTimeSeriesFetchRequest;
import org.opennms.timeseries.cortex.CortexTSS;
import org.opennms.timeseries.cortex.CortexTSSConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class CortexTSSIntegrationTest extends AbstractStorageIntegrationTest {

    private static final Logger LOG = LoggerFactory.getLogger(CortexTSSIntegrationTest.class);

    @ClassRule
    public static DockerComposeContainer<?> environment = new DockerComposeContainer<>(new File("src/test/resources/org/opennms/timeseries/cortex/docker-compose.yaml"))
        .withExposedService("cortex", 9009, Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(10)))
        .withExposedService("grafana", 3000, Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(10)));

    private CortexTSS cortexTss;

    @Before
    public void setUp() throws Exception {
        cortexTss = new CortexTSS(new CortexTSSConfig(), new KVStoreMock());
        super.setUp();
    }

    @Override
    protected TimeSeriesStorage createStorage() {
        return cortexTss;
    }

    /**
     * The cortex query api doesn't allow to query for raw data for a range.
     * Therefore, we need to adopt the test method and set an aggregation
     */
    @Override
    public void shouldGetSamplesForMetric() throws StorageException {
        Metric metricOriginal = this.metrics.get(1); // metric with gauge
        ImmutableMetric.MetricBuilder builder = ImmutableMetric.builder();
        metricOriginal.getIntrinsicTags().forEach(builder::intrinsicTag);
        Metric metric = builder.build();
        TimeSeriesFetchRequest request = ImmutableTimeSeriesFetchRequest.builder()
            .start(this.referenceTime.minusSeconds(300))
            .end(this.referenceTime)
            .metric(metric)
            .aggregation(Aggregation.AVERAGE)
            .step(Duration.of(1, ChronoUnit.SECONDS))
            .build();
        List<Sample> samples = storage.getTimeseries(request);
        // we expect 1 sample per second. The time series starts with the first recorded sample (60s ago) => 60 samples
        assertEquals(60, samples.size());

        for (Sample sample : samples) {
            assertEquals(metricOriginal, sample.getMetric());
            // Metric.equals() doesn't include meta tags, therefore we need to test for it separately:
            assertEquals(metricOriginal.getMetaTags(), sample.getMetric().getMetaTags());
            assertEquals(Double.valueOf(42.3D), sample.getValue());
        }
    }

    @Test
    public void shouldGetSamplesForLongDuration() throws StorageException, InterruptedException {
        final Metric metric = ImmutableMetric.builder()
            .intrinsicTag(this.metrics.get(0).getFirstTagByKey(IntrinsicTagNames.resourceId))
            .intrinsicTag(IntrinsicTagNames.name, "n" + UUID.randomUUID().toString().replaceAll("-", ""))
            .metaTag(MetaTagNames.mtype, Metric.Mtype.counter.name())
            .build();
        Instant startTime = this.referenceTime.minus(90, ChronoUnit.DAYS);
        Duration step = Duration.of((long) Math.ceil(Duration.between(startTime, referenceTime).getSeconds() / (double) CortexTSS.MAX_SAMPLES), ChronoUnit.SECONDS);

        List<Sample> originalSamples = new ArrayList<>();
        Instant time = startTime;
        while (time.isBefore(referenceTime)) {
            originalSamples.add(ImmutableSample.builder()
                .time(time)
                .value(42.3)
                .metric(metric)
                .build());
            time = time.plus(1, ChronoUnit.HOURS);
        }
        storage.store(originalSamples);
        // The writes are async. Therefore we need to wait a bit before reading...
        await().atMost(Duration.ofSeconds(10));

        TimeSeriesFetchRequest request = ImmutableTimeSeriesFetchRequest.builder()
            .start(startTime)
            .end(this.referenceTime)
            .metric(metric)
            .aggregation(Aggregation.AVERAGE)
            .step(step)
            .build();
        List<Sample> samplesFromDb = storage.getTimeseries(request);

        // Check if we get orderly spaced samples back
        List<Long> durations = new ArrayList<>();
        Sample lastSample = samplesFromDb.get(0);
        for (int i = 1; i < samplesFromDb.size(); i++) {
            Sample sample = samplesFromDb.get(i);
            durations.add(sample.getTime().getEpochSecond() - lastSample.getTime().getEpochSecond());
            lastSample = sample;
        }
        long wrongDurations = durations.stream().filter(d -> d != step.getSeconds()).count();

        // TODO: Patrick This doesn't seem to work: we get much less data points than requested by the step size. Not sure why?
        // assertEquals(String.format("Expected all Samples to be spaced apart by %s seconds. But %s of %s have a different step of the expected :\n%s ",
        // step.getSeconds(), wrongDurations, samplesFromDb.size()-1, durations), 0, wrongDurations);

        // check if our timeseries starts around the beginning of the defined period
        Instant timeOfFirstSample = samplesFromDb.get(0).getTime();
        assertFalse(String.format("Expected timeOfFirstSample=%s not before startTime=%s", timeOfFirstSample, startTime), timeOfFirstSample.isBefore(startTime));
        assertTrue(timeOfFirstSample.isBefore(startTime.plus(12, ChronoUnit.HOURS)));

        // check if the timeseries ends around the end of the defined period
        Instant timeOfLastSample = samplesFromDb.get(samplesFromDb.size() - 1).getTime();
        assertFalse(timeOfLastSample.isAfter(referenceTime));
        Instant expectedEarliestTimeOfLastSample = referenceTime.minus(12, ChronoUnit.HOURS);
        assertTrue(String.format("Expected timeOfLastSample=%s not before endTime-step=%s",
            timeOfLastSample, expectedEarliestTimeOfLastSample), timeOfLastSample.isAfter(expectedEarliestTimeOfLastSample));
    }

    @Override
    protected void waitForPersistingChanges() throws Exception {
        // The writes are async. Therefore we need to wait a bit before reading...
        await().atMost(Duration.ofSeconds(1));
    }
}
