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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.InvalidProtocolBufferException;
import java.net.URI;
import java.net.URISyntaxException;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.alerts.proto.Alert;
import org.opennms.horizon.notifications.api.dto.PagerDutyEventDTO;
import org.opennms.horizon.notifications.dto.PagerDutyConfigDTO;
import org.opennms.horizon.notifications.exceptions.NotificationAPIException;
import org.opennms.horizon.notifications.exceptions.NotificationAPIRetryableException;
import org.opennms.horizon.notifications.exceptions.NotificationBadDataException;
import org.opennms.horizon.notifications.exceptions.NotificationException;
import org.opennms.horizon.notifications.exceptions.NotificationInternalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

@RequiredArgsConstructor
@Service
public class PagerDutyAPI {
    private static final Logger LOG = LoggerFactory.getLogger(PagerDutyAPI.class);

    private final PagerDutyDao pagerDutyDao;

    private final RestTemplate restTemplate;

    private final RetryTemplate retryTemplate;

    private final PagerDutyEventFactory pagerDutyEventFactory;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void postNotification(Alert alert) throws NotificationException {
        try {
            PagerDutyEventDTO event = pagerDutyEventFactory.createEvent(alert);

            String baseUrl = "https://events.pagerduty.com/v2/enqueue";
            URI uri = new URI(baseUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/vnd.pagerduty+json;version=2");
            headers.set("Content-Type", "application/json");

            String eventJson = objectMapper.writeValueAsString(event);
            HttpEntity<String> requestEntity = new HttpEntity<>(eventJson, headers);

            LOG.info("Posting alert with id={} for tenant={} to PagerDuty", alert.getDatabaseId(), alert.getTenantId());
            postPagerDutyNotification(uri, requestEntity);
        } catch (URISyntaxException e) {
            throw new NotificationInternalException("Bad PagerDuty url", e);
        } catch (JsonProcessingException e) {
            throw new NotificationBadDataException("JSON error processing alertDTO", e);
        } catch (InvalidProtocolBufferException e) {
            throw new NotificationInternalException("Failed to encode/decode alert: " + alert, e);
        }
    }

    public void saveConfig(PagerDutyConfigDTO config) {
        pagerDutyDao.saveConfig(config);
    }

    private void postPagerDutyNotification(URI uri, HttpEntity<String> requestEntity) throws NotificationAPIException {
        retryTemplate.execute(ctx -> {
            try {
                restTemplate.exchange(uri, HttpMethod.POST, requestEntity, String.class);
                return true;
            } catch (RestClientException e) {
                Throwable cause = e.getMostSpecificCause();

                if (cause instanceof ResourceAccessException) {
                    // Some low level IO issue, retry in case it was transient.
                    throw new NotificationAPIRetryableException("PagerDuty API Exception", e);
                } else if (cause instanceof RestClientResponseException) {
                    // Check the response status to see if we should retry.
                    // See
                    // https://developer.pagerduty.com/api-reference/YXBpOjI3NDgyNjU-pager-duty-v2-events-api#api-response-codes--retry-logic
                    HttpStatusCode responseStatus = ((RestClientResponseException) e).getStatusCode();
                    if (responseStatus == HttpStatus.TOO_MANY_REQUESTS || responseStatus.is5xxServerError()) {
                        throw new NotificationAPIRetryableException("PagerDuty API exception", e);
                    }
                }

                // Not retryable, let's bail.
                throw new NotificationAPIException("PagerDuty API exception", e);
            }
        });
    }
}
