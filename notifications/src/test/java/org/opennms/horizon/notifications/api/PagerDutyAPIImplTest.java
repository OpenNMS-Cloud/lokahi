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
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opennms.horizon.notifications.api.dto.PagerDutyConfigDTO;
import org.opennms.horizon.notifications.exceptions.NotificationBadDataException;
import org.opennms.horizon.shared.dto.event.AlarmDTO;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

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
        AlarmDTO notificationDTO = getAlarm();
        pagerDutyAPI.postNotification(notificationDTO);
    }

    @Test
    public void saveConfig() throws Exception {
        pagerDutyAPI.saveConfig(getConfigDTO());
    }

    @Test
    public void validateConfig() throws Exception {
        pagerDutyAPI.validateConfig(getConfigDTO());
    }

    @Test
    public void validateConfigInvalidToken() throws Exception {
        doThrow(HttpClientErrorException.class)
            .when(restTemplate).exchange(ArgumentMatchers.any(URI.class),
                ArgumentMatchers.any(HttpMethod.class),
                ArgumentMatchers.any(HttpEntity.class),
                ArgumentMatchers.any(Class.class));

        boolean exceptionCaught = false;
        try{
            pagerDutyAPI.validateConfig(getConfigDTO());
        } catch (NotificationBadDataException e) {
            assertEquals("Invalid PagerDuty token", e.getMessage());
            exceptionCaught = true;
        }

        assertTrue(exceptionCaught);
    }

    private PagerDutyConfigDTO getConfigDTO() {
        return new PagerDutyConfigDTO("token", "integration_key");
    }

    private AlarmDTO getAlarm() {
        AlarmDTO notificationDTO = new AlarmDTO();
        notificationDTO.setLogMessage("Exciting message to go here");
        notificationDTO.setReductionKey("srv01/mysql");
        notificationDTO.setSeverity("Indeterminate");
        return notificationDTO;
    }
}
