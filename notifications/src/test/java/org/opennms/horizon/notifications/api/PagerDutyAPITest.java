/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.opennms.horizon.notifications.api;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
                .thenThrow(new RestClientResponseException(
                        "Failed", HttpStatus.TOO_MANY_REQUESTS, "Failed", null, null, null))
                .thenReturn(ResponseEntity.ok(null));

        pagerDutyAPI.postNotification(getAlert());
        verify(restTemplate, times(2)).exchange(any(), any(), any(), any(Class.class));
    }

    @Test
    public void postNotificationsWithoutRetry() throws Exception {
        // Some exceptions should just fail and not retry.
        Mockito.when(restTemplate.exchange(any(), any(), any(), any(Class.class)))
                .thenThrow(
                        new RestClientResponseException("Failed", HttpStatus.BAD_REQUEST, "Failed", null, null, null));

        assertThrows(NotificationAPIException.class, () -> pagerDutyAPI.postNotification(getAlert()));
        verify(restTemplate, times(1)).exchange(any(), any(), any(), any(Class.class));
    }

    private PagerDutyConfigDTO getConfigDTO() {
        return PagerDutyConfigDTO.newBuilder()
                .setIntegrationKey("integration_key")
                .build();
    }

    private Alert getAlert() {
        return Alert.newBuilder()
                .setLogMessage("Exciting message to go here")
                .setReductionKey("srv01/mysql")
                .setSeverity(Severity.MAJOR)
                .build();
    }
}
