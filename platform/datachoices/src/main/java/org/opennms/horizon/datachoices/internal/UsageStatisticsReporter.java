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

package org.opennms.horizon.datachoices.internal;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.opennms.core.web.HttpClientWrapper;
import org.opennms.horizon.datachoices.dto.UsageStatisticsReportDTO;
import org.opennms.horizon.datachoices.internal.StateManager.StateChangeHandler;
import org.opennms.horizon.db.model.OnmsDataChoices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

public class UsageStatisticsReporter implements StateChangeHandler {
    private static final Logger LOG = LoggerFactory.getLogger(UsageStatisticsReporter.class);
    private static final String POM_PROPERTIES_FILE_NAME = "properties-from-pom.properties";
    private static final String DISPLAY_VERSION = "display.version";
    private static final String USAGE_REPORT = "hs-usage-report";

    private StateManager stateManager;

    private String url;

    private long interval;

    private Timer timer;

    public synchronized void init() {
        OnmsDataChoices dataChoices = this.stateManager.getDataChoices();
        if (dataChoices.getEnabled()) {
            LOG.info("Scheduling usage statistic reporting");
            schedule();
        } else {
            LOG.info("Usage statistic reporting not enabled");
        }
        stateManager.onIsEnabledChanged(this);
    }

    public synchronized void destroy() {
        if (timer != null) {
            LOG.info("Disabling scheduled report.");
            timer.cancel();
            timer = null;
        }
    }

    @Override
    public void onEnabledChanged(boolean enabled) {
        if (enabled && timer == null) {
            schedule();
        } else if (!enabled && timer != null) {
            destroy();
        }
    }

    public synchronized void schedule() {
        LOG.info("Scheduling usage statistics report every {} ms", interval);
        timer = new Timer();
        timer.schedule(new DataChoicesTimerTask(), 0, interval);
    }

    public UsageStatisticsReportDTO generateReport() {
        OnmsDataChoices dataChoices = this.stateManager.getDataChoices();

        UsageStatisticsReportDTO usageStatsReport = new UsageStatisticsReportDTO();
        usageStatsReport.setSystemId(dataChoices.getSystemId());
        usageStatsReport.setVersion(getVersion());

        return usageStatsReport;
    }

    private String getVersion() {
        Properties properties = getPomProperties();
        return properties.getProperty(DISPLAY_VERSION, "");
    }

    private Properties getPomProperties() {
        Properties properties = new Properties();
        try (InputStream inputStream = new FileInputStream(POM_PROPERTIES_FILE_NAME)) {
            properties.load(inputStream);
        } catch (IOException e) {
            LOG.warn("Unable to load from generated pom properties file", e);
        }
        return properties;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setInterval(long interval) {
        this.interval = interval;
    }

    public void setStateManager(StateManager stateManager) {
        this.stateManager = stateManager;
    }

    private class DataChoicesTimerTask extends TimerTask {

        @Override
        public void run() {
            UsageStatisticsReportDTO usageStatsReport = generateReport();
            String usageStatsReportJson = usageStatsReport.toJson();

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
    }
}
