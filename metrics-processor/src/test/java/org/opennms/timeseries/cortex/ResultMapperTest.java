package org.opennms.timeseries.cortex;

import static org.junit.Assert.assertEquals;
import static org.opennms.timeseries.cortex.CortexTSS.CORTEX_TSS;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import org.json.JSONObject;

import org.junit.Before;
import org.junit.Test;
import org.opennms.integration.api.v1.timeseries.IntrinsicTagNames;
import org.opennms.integration.api.v1.timeseries.Metric;
import org.opennms.integration.api.v1.timeseries.Sample;
import org.opennms.integration.api.v1.timeseries.immutables.ImmutableMetric;

public class ResultMapperTest {

    private Metric expectedMetric;
    private KVStoreMock kvstore;

    @Before
    public void setUp() {
        expectedMetric = ImmutableMetric.builder()
            .intrinsicTag(IntrinsicTagNames.name, "na8793e6f6477407bbd105bf6ed36b698")
            .intrinsicTag(IntrinsicTagNames.resourceId, "snmp:1:opennms-jvm:org_opennms_newts_name_ring_buffer_max_size_unit=unknown")
            .metaTag("_idx0", "(snmp,4)")
            .metaTag("_idx1", "(snmp:1,4)")
            .metaTag("_idx2", "(snmp:1:opennms-jvm,4)")
            .metaTag("_idx2w", "(snmp:1,*)")
            .metaTag("_idx3", "(snmp:1:opennms-jvm:OpenNMS_Name_Notifd,4)")
            .externalTag("key", "value")
            .metaTag("host", "myHost1")
            .metaTag("mtype", "counter")
            .build();
        kvstore = new KVStoreMock();
    }

    @Test
    public void shouldMapSeriesQueryResult() throws IOException, URISyntaxException {
        String json = readStringFromFile("seriesQueryResult.json");
        kvstore.put(expectedMetric.getKey(), new JSONObject().put("key", "value"), CORTEX_TSS);
        List<Metric> metrics = ResultMapper.fromSeriesQueryResult(json, kvstore);
        assertEquals(1, metrics.size());
        assertEquals(expectedMetric, metrics.get(0));
    }

    @Test
    public void shouldMapRangeQueryResult() throws IOException, URISyntaxException {
        String json = readStringFromFile("rangeQueryResult.json");
        List<Sample> samples = ResultMapper.fromRangeQueryResult(json, expectedMetric);

        assertEquals(expectedMetric, samples.get(0).getMetric());
        assertEquals(Instant.ofEpochSecond(1602783564), samples.get(0).getTime());
        assertEquals((Double) 42.3, samples.get(0).getValue());
        assertEquals(60, samples.size());
    }

    @Test
    public void testAppendExternalTagsToMetric() throws IOException, URISyntaxException {
        String json = readStringFromFile("seriesQueryResult.json");
        kvstore.put(expectedMetric.getKey(),
            new JSONObject()
                .put("key1", "value1")
                .put("key2", "value2")
                .put("key3", "value3"),
            CORTEX_TSS);
        List<Metric> metrics = ResultMapper.fromSeriesQueryResult(json, kvstore);
        assertEquals(1, metrics.size());
        assertEquals(3, metrics.get(0).getExternalTags().size());
        assertEquals("value3", metrics.get(0).getExternalTags()
            .stream()
            .filter(tag -> tag.getKey().equals("key3"))
            .findFirst()
            .get()
            .getValue());
    }

    private String readStringFromFile(final String fileName) throws IOException, URISyntaxException {
        StringBuilder contentBuilder = new StringBuilder();
        try (Stream<String> stream = Files.lines(
            Paths.get(this.getClass().getResource(fileName).toURI()), StandardCharsets.UTF_8)) {
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
        }
        return contentBuilder.toString();
    }
}
