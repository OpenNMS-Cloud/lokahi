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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opennms.horizon.db.dao.api.MonitoredServiceDao;
import org.opennms.horizon.db.dao.api.NodeDao;
import org.opennms.horizon.db.dao.api.SessionUtils;
import org.opennms.horizon.db.model.OnmsDataChoices;
import org.opennms.horizon.db.model.OnmsNode;
import org.opennms.horizon.shared.dto.datachoices.UsageStatisticsReportDTO;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UsageStatisticsReporterTest {
    private static final long TEST_NODE_DAO_COUNT = 2;
    private static final long TEST_MONITORED_SERVICE_DAO_COUNT = 3;
    private static final int TEST_NODE_ID = 123;
    private static final String TEST_DEFAULT_DEVICE_TYPE = "device";
    private static final long TEST_INTERVAL = 100L;
    private static final int TEST_NODE_COUNT = 1;

    @InjectMocks
    private UsageStatisticsReporter reporter;

    @Mock
    private StateManager stateManager;

    @Mock
    private SessionUtils sessionUtils;

    @Mock
    private NodeDao nodeDao;

    @Mock
    private MonitoredServiceDao monitoredServiceDao;

    private static OnmsDataChoices getOnmsDataChoices(boolean enabled) {
        OnmsDataChoices dataChoices = new OnmsDataChoices();
        dataChoices.setId(1);
        dataChoices.setEnabled(enabled);
        dataChoices.setSystemId(UUID.randomUUID().toString());
        return dataChoices;
    }

    @Before
    public void setup() {
        reporter.setInterval(TEST_INTERVAL);
    }

    @Test
    public void testInitEnabled() {
        OnmsDataChoices dataChoices = getOnmsDataChoices(true);
        when(stateManager.getDataChoices()).thenReturn(dataChoices);

        reporter.init();

        verify(stateManager, times(1))
            .onIsEnabledChanged(any(StateManager.StateChangeHandler.class));
    }

    @Test
    public void testInitDisabled() {
        OnmsDataChoices dataChoices = getOnmsDataChoices(false);
        when(stateManager.getDataChoices()).thenReturn(dataChoices);

        reporter.init();

        verify(stateManager, times(1))
            .onIsEnabledChanged(any(StateManager.StateChangeHandler.class));
    }

    @Test
    public void testDestroy() {
        OnmsDataChoices dataChoices = getOnmsDataChoices(true);
        when(stateManager.getDataChoices()).thenReturn(dataChoices);

        reporter.init();
        reporter.destroy();

        assertTrue(true);
    }

    @Test
    public void testGenerateReport() {
        boolean enabled = true;
        OnmsDataChoices dataChoices = getOnmsDataChoices(enabled);
        when(stateManager.getDataChoices()).thenReturn(dataChoices);

        UsageStatisticsReportDTO report = reporter.generateReport();
        assertEquals(dataChoices.getSystemId(), report.getSystemId());
        assertEquals(1, dataChoices.getId().intValue());
        assertEquals(dataChoices.getEnabled(), enabled);
    }

    @Test
    public void testSetUsageStatsReport() {
        when(nodeDao.countAll()).thenReturn(TEST_NODE_DAO_COUNT);
        when(monitoredServiceDao.countAll()).thenReturn(TEST_MONITORED_SERVICE_DAO_COUNT);

        OnmsNode node = new OnmsNode();
        node.setId(TEST_NODE_ID);
        node.setLabel(TEST_DEFAULT_DEVICE_TYPE);

        when(nodeDao.findAll()).thenReturn(Collections.singletonList(node));

        UsageStatisticsReportDTO report = new UsageStatisticsReportDTO();
        reporter.setUsageStatsReport(report);

        assertEquals(TEST_NODE_DAO_COUNT, report.getNodes());
        assertEquals(TEST_MONITORED_SERVICE_DAO_COUNT, report.getMonitoredServices());

        Map<String, Integer> deviceTypeCounts = report.getDeviceTypeCounts();
        assertEquals(1, deviceTypeCounts.size());

        Map.Entry<String, Integer> entry = deviceTypeCounts
            .entrySet().stream().iterator().next();

        assertEquals(TEST_DEFAULT_DEVICE_TYPE, entry.getKey());
        assertEquals(TEST_NODE_COUNT, entry.getValue().intValue());
    }
}
