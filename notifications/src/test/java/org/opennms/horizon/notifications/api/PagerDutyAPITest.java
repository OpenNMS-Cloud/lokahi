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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opennms.horizon.alerts.proto.Alert;
import org.opennms.horizon.alerts.proto.Severity;
import org.opennms.horizon.notifications.dto.PagerDutyConfigDTO;
import org.opennms.horizon.notifications.exceptions.NotificationAPIException;
import org.opennms.horizon.notifications.exceptions.NotificationAPIRetryableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class PagerDutyAPITest {
    @InjectMocks
    PagerDutyAPI pagerDutyAPI;

    @Mock
    PagerDutyEventFactory eventFactory;

    @Mock
    RestTemplate restTemplate;

    @Spy
    RetryTemplate retryTemplate = RetryTemplate.builder()
        .retryOn(NotificationAPIRetryableException.class)
        .maxAttempts(3)
        .fixedBackoff(10)
        .build();

    @Mock
    PagerDutyDao pagerDutyDao;

    @Test
    public void postNotifications() throws Exception {
        Alert alert = getAlert();
        pagerDutyAPI.postNotification(alert);
    }

    @Test
    public void saveConfig() {
        pagerDutyAPI.saveConfig(getConfigDTO());
    }

    @Test
    public void postNotificationsWithRetry() throws Exception {
        // Depending on the response, we should retry
        Mockito.when(restTemplate.exchange(any(), any(), any(), any(Class.class)))
            .thenThrow(new RestClientResponseException("Failed", HttpStatus.TOO_MANY_REQUESTS, "Failed", null, null, null))
            .thenReturn(ResponseEntity.ok(null));

        pagerDutyAPI.postNotification(getAlert());
        verify(restTemplate, times(2)).exchange(any(), any(), any(), any(Class.class));
    }

    @Test
    public void postNotificationsWithoutRetry() throws Exception {
        // Some exceptions should just fail and not retry.
        Mockito.when(restTemplate.exchange(any(), any(), any(), any(Class.class)))
            .thenThrow(new RestClientResponseException("Failed", HttpStatus.BAD_REQUEST, "Failed", null, null, null));

        assertThrows(NotificationAPIException.class, () -> pagerDutyAPI.postNotification(getAlert()));
        verify(restTemplate, times(1)).exchange(any(), any(), any(), any(Class.class));
    }

    private PagerDutyConfigDTO getConfigDTO() {
        return PagerDutyConfigDTO.newBuilder().setIntegrationKey("integration_key").build();
    }

    private Alert getAlert() {
        return Alert.newBuilder()
            .setLogMessage("Exciting message to go here")
            .setReductionKey("srv01/mysql")
            .setSeverity(Severity.MAJOR)
            .build();
    }
}
