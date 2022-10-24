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

import org.opennms.horizon.datachoices.internal.StateManager.StateChangeHandler;
import org.opennms.horizon.db.dao.api.MonitoredServiceDao;
import org.opennms.horizon.db.dao.api.NodeDao;
import org.opennms.horizon.db.dao.api.SessionUtils;
import org.opennms.horizon.db.model.OnmsDataChoices;
import org.opennms.horizon.db.model.OnmsNode;
import org.opennms.horizon.shared.dto.datachoices.UsageStatisticsReportDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;

public class UsageStatisticsReporter implements StateChangeHandler {
    private static final Logger LOG = LoggerFactory.getLogger(UsageStatisticsReporter.class);
    private static final String POM_PROPERTIES_FILE_NAME = "properties-from-pom.properties";
    private static final String DISPLAY_VERSION = "display.version";

    private StateManager stateManager;

    private SessionUtils sessionUtils;

    private NodeDao nodeDao;

    private MonitoredServiceDao monitoredServiceDao;

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

        DataChoicesTimerTask timerTask = new DataChoicesTimerTask(this, url);
        timer.schedule(timerTask, 0, interval);
    }

    public UsageStatisticsReportDTO generateReport() {
        OnmsDataChoices dataChoices = this.stateManager.getDataChoices();

        UsageStatisticsReportDTO usageStatsReport = new UsageStatisticsReportDTO();
        usageStatsReport.setSystemId(dataChoices.getSystemId());
        usageStatsReport.setVersion(getVersion());

        sessionUtils.withReadOnlyTransaction(() -> setUsageStatsReport(usageStatsReport));

        return usageStatsReport;
    }

    protected void setUsageStatsReport(UsageStatisticsReportDTO usageStatsReport) {
        usageStatsReport.setNodes(nodeDao.countAll());
        usageStatsReport.setMonitoredServices(monitoredServiceDao.countAll());
        usageStatsReport.setDeviceTypeCounts(getDeviceTypeCounts());
    }

    /*
     * Counting the number of occurrences of a string.
     *
     * Currently this takes a default value as the DeviceType as this field is currently not available.
     * TODO: Update to a DeviceType (ie. router, server, storage) when available
     */
    private Map<String, Integer> getDeviceTypeCounts() {
        Map<String, Integer> deviceTypeCounts = new HashMap<>();

        for (OnmsNode node : nodeDao.findAll()) {
            String deviceType = "device";
            if (deviceTypeCounts.containsKey(deviceType)) {
                int currentCount = deviceTypeCounts.get(deviceType);
                deviceTypeCounts.put(deviceType, ++currentCount);
            } else {
                deviceTypeCounts.put(deviceType, 1);
            }
        }

        return deviceTypeCounts;
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

    public void setSessionUtils(SessionUtils sessionUtils) {
        this.sessionUtils = sessionUtils;
    }

    public void setNodeDao(NodeDao nodeDao) {
        this.nodeDao = nodeDao;
    }

    public void setMonitoredServiceDao(MonitoredServiceDao monitoredServiceDao) {
        this.monitoredServiceDao = monitoredServiceDao;
    }
}
