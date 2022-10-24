package org.opennms.horizon.datachoices.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.opennms.core.web.HttpClientWrapper;
import org.opennms.horizon.shared.dto.datachoices.UsageStatisticsReportDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.InternalServerErrorException;
import java.io.IOException;
import java.util.TimerTask;

public class DataChoicesTimerTask extends TimerTask {
    private static final Logger LOG = LoggerFactory.getLogger(DataChoicesTimerTask.class);

    private static final String USAGE_REPORT = "hs-usage-report";

    private final UsageStatisticsReporter reporter;

    private final String url;

    public DataChoicesTimerTask(UsageStatisticsReporter reporter, String url) {
        this.reporter = reporter;
        this.url = url;
    }

    @Override
    public void run() {
        UsageStatisticsReportDTO usageStatsReport = reporter.generateReport();
        String usageStatsReportJson = reportToJson(usageStatsReport);

        try (HttpClientWrapper clientWrapper = HttpClientWrapper.create();
             CloseableHttpClient client = clientWrapper.getClient()) {

            HttpPost httpRequest = new HttpPost(url + USAGE_REPORT);
            httpRequest.setEntity(new StringEntity(usageStatsReportJson, ContentType.APPLICATION_JSON));

            LOG.info("Sending usage statistics report to {}: {}", httpRequest.getURI(), usageStatsReportJson);
            client.execute(httpRequest);
            LOG.info("Successfully sent usage statistics report.");

        } catch (IOException e) {
            LOG.error("The usage statistics report was not successfully delivered", e);
        }
    }

    private String reportToJson(UsageStatisticsReportDTO usageStatsReport) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectWriter writer = mapper.writer();
        try {
            return writer.writeValueAsString(usageStatsReport);
        } catch (JsonProcessingException e) {
            throw new InternalServerErrorException("Failed to write usage statistics to json string", e);
        }
    }
}
