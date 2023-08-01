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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.InvalidProtocolBufferException;
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

import java.net.URI;
import java.net.URISyntaxException;

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
                    // See https://developer.pagerduty.com/api-reference/YXBpOjI3NDgyNjU-pager-duty-v2-events-api#api-response-codes--retry-logic
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
