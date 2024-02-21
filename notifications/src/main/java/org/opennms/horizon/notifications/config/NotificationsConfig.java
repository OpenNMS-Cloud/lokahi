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
