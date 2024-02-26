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

import com.azure.communication.email.EmailClient;
import com.azure.communication.email.EmailClientBuilder;
import com.azure.core.http.policy.ExponentialBackoffOptions;
import com.azure.core.http.policy.RetryOptions;
import com.azure.core.http.policy.RetryPolicy;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.notifications.api.email.ACSEmailAPI;
import org.opennms.horizon.notifications.api.email.EmailAPI;
import org.opennms.horizon.notifications.api.email.SmtpEmailAPI;
import org.opennms.horizon.notifications.exceptions.NotificationAPIRetryableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.retry.support.RetryTemplate;

@Slf4j
@Configuration
public class EmailConfig {
    @Value("${spring.mail.acs-connection-string:}")
    private String acsConnectionString;

    @Value("${horizon.email.retry.delay:1000}")
    private int retryDelay;

    @Value("${horizon.email.retry.maxDelay:60000}")
    private int maxRetryDelay;

    @Value("${horizon.email.retry.multiplier:2}")
    private int retryMultiplier;

    @Value("${horizon.email.retry.max:10}")
    private int maxNumberOfRetries;

    @Bean
    @ConditionalOnProperty(value = "spring.mail.acs-connection-string")
    public EmailClient acsEmailClient() {
        log.info("ACS Connection String found. Building ACS EmailClient");
        ExponentialBackoffOptions retry = new ExponentialBackoffOptions();
        retry.setBaseDelay(Duration.ofMillis(retryDelay));
        retry.setMaxDelay(Duration.ofMillis(maxRetryDelay));
        retry.setMaxRetries(maxNumberOfRetries);

        return new EmailClientBuilder()
                .connectionString(acsConnectionString)
                .retryPolicy(new RetryPolicy(new RetryOptions(retry)))
                .buildClient();
    }

    @Bean
    @Primary
    @ConditionalOnBean(EmailClient.class)
    public EmailAPI acsEmailAPI(@Value("${spring.mail.from}") String fromAddress, EmailClient client) {
        log.info("Using ACS for email notifications, fromAddress='{}'", fromAddress);
        return new ACSEmailAPI(fromAddress, client);
    }

    @Bean
    @ConditionalOnMissingBean(EmailAPI.class)
    public EmailAPI smtpEmailAPI(JavaMailSender jms) {
        log.info("Using SMTP client for email notifications");
        return new SmtpEmailAPI(jms, emailRetryTemplate());
    }

    private RetryTemplate emailRetryTemplate() {
        // Default exponential backoff, retries after 1s, 3s, 7s, 15s.. At most 60s delay by default.
        return RetryTemplate.builder()
                .retryOn(NotificationAPIRetryableException.class)
                .maxAttempts(maxNumberOfRetries)
                .exponentialBackoff(retryDelay, retryMultiplier, maxRetryDelay)
                .build();
    }
}
