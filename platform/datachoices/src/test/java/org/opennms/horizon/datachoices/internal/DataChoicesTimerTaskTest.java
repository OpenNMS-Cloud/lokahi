package org.opennms.horizon.datachoices.internal;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opennms.horizon.shared.dto.datachoices.UsageStatisticsReportDTO;

import java.util.Collections;

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.badRequest;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DataChoicesTimerTaskTest {
    private static final String TEST_DEVICE_TYPE = "device";
    private static final int TEST_DEVICE_TYPE_COUNT = 1;
    private static final String TEST_SYSTEM_ID = "1234";
    private static final String TEST_VERSION = "1.0.0";
    private static final int TEST_NODE_COUNT = 1;
    private static final int TEST_MONITORED_SERVICES = 2;

    private DataChoicesTimerTask timerTask;

    @Mock
    private UsageStatisticsReporter reporter;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig().dynamicPort());

    @Before
    public void setup() {
        String url = String.format("http://localhost:%d/", wireMockRule.port());
        timerTask = new DataChoicesTimerTask(reporter, url);
    }

    @Test
    public void testRun() {
        UsageStatisticsReportDTO report = getReport();

        when(reporter.generateReport()).thenReturn(report);

        wireMockRule.stubFor(post(urlEqualTo("/hs-usage-report"))
            .withRequestBody(equalToJson(getReportJson()))
            .willReturn(ok()));

        timerTask.run();

        wireMockRule.verify(1, anyRequestedFor(urlEqualTo("/hs-usage-report")));
    }

    @Test
    public void testRunFailed() {
        UsageStatisticsReportDTO report = getReport();

        when(reporter.generateReport()).thenReturn(report);

        wireMockRule.stubFor(post(urlEqualTo("/hs-usage-report"))
            .withRequestBody(equalToJson(getReportJson()))
            .willReturn(badRequest()));

        timerTask.run();

        assertTrue(true);
    }

    private static UsageStatisticsReportDTO getReport() {
        UsageStatisticsReportDTO report = new UsageStatisticsReportDTO();
        report.setSystemId(TEST_SYSTEM_ID);
        report.setVersion(TEST_VERSION);
        report.setNodes(TEST_NODE_COUNT);
        report.setMonitoredServices(TEST_MONITORED_SERVICES);
        report.setDeviceTypeCounts(Collections.singletonMap(TEST_DEVICE_TYPE, TEST_DEVICE_TYPE_COUNT));
        return report;
    }

    private static String getReportJson() {
        return "{\"systemId\":\"1234\",\"version\":\"1.0.0\",\"nodes\":1," +
            "\"monitoredServices\":2,\"deviceTypeCounts\":{\"device\":1}}";
    }
}
