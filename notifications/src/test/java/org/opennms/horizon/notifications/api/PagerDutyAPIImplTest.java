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

package org.opennms.horizon.notifications.api;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opennms.horizon.shared.dto.notifications.PagerDutyConfigDTO;
import org.opennms.horizon.shared.dto.event.AlarmDTO;
import org.opennms.horizon.shared.dto.event.EventDTO;
import org.opennms.horizon.shared.dto.event.EventParameterDTO;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

@RunWith(MockitoJUnitRunner.class)
public class PagerDutyAPIImplTest {
    @InjectMocks
    PagerDutyAPIImpl pagerDutyAPI;

    @Mock
    RestTemplate restTemplate;

    @Mock
    PagerDutyDao pagerDutyDao;

    @Test
    public void postNotifications() throws Exception {
        Mockito.when(pagerDutyDao.getConfig()).thenReturn(getConfigDTO());
        AlarmDTO alarm = getAlarm(false);
        pagerDutyAPI.postNotification(alarm);
    }

    @Test
    public void postNotificationsWithAlarmClash() throws Exception {
        Mockito.when(pagerDutyDao.getConfig()).thenReturn(getConfigDTO());
        AlarmDTO alarm = getAlarm(true);
        pagerDutyAPI.postNotification(alarm);
    }

    @Test
    public void saveConfig() throws Exception {
        pagerDutyAPI.saveConfig(getConfigDTO());
    }

    private PagerDutyConfigDTO getConfigDTO() {
        return new PagerDutyConfigDTO("integration_key");
    }

    private AlarmDTO getAlarm(boolean includeParams) {
        AlarmDTO alarmDTO = new AlarmDTO();
        alarmDTO.setLogMessage("Exciting message to go here");
        alarmDTO.setReductionKey("srv01/mysql");
        alarmDTO.setSeverity("Indeterminate");

        EventDTO lastEvent = new EventDTO();
        if (includeParams) {
            EventParameterDTO param = new EventParameterDTO();
            param.setName("alarm");
            param.setValue("value");

            lastEvent.setParameters(Arrays.asList(param));
        }
        alarmDTO.setLastEvent(lastEvent);
        return alarmDTO;
    }
}
