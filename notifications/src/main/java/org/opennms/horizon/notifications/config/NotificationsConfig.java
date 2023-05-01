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

package org.opennms.horizon.notifications.config;

import org.opennms.horizon.notifications.exceptions.NotificationAPIRetryableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;

@Configuration
public class NotificationsConfig {
    @Value("${horizon.pagerduty.retry.delay:1000}")
    private int retryDelay;

    @Value("${horizon.pagerduty.retry.maxDelay:60000}")
    private int maxRetryDelay;

    @Value("${horizon.pagerduty.retry.multiplier:2}")
    private int retryMultiplier;

    @Value("${horizon.pagerduty.retry.max:10}")
    private int maxNumberOfRetries;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

    @Bean
    public RetryTemplate retryTemplate() {
        // Default exponential backoff, retries after 1s, 3s, 7s, 15s.. At most 60s delay by default.
        return RetryTemplate.builder()
            .retryOn(NotificationAPIRetryableException.class)
            .maxAttempts(maxNumberOfRetries)
            .exponentialBackoff(retryDelay, retryMultiplier, maxRetryDelay)
            .build();
    }
}
