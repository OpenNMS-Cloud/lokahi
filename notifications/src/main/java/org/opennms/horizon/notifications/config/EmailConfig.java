package org.opennms.horizon.notifications.config;

import com.azure.communication.email.EmailClient;
import com.azure.communication.email.EmailClientBuilder;
import com.azure.core.http.policy.ExponentialBackoffOptions;
import com.azure.core.http.policy.RetryOptions;
import com.azure.core.http.policy.RetryPolicy;
import org.opennms.horizon.notifications.api.email.ACSEmailAPI;
import org.opennms.horizon.notifications.api.email.EmailAPI;
import org.opennms.horizon.notifications.api.email.SmtpEmailAPI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Duration;

@Configuration
public class EmailConfig {
    @Value("${spring.mail.acs-connection-string:}")
    private String acsConnectionString;

    @Value("${horizon.email.retry.delay:1000}")
    private int retryDelay;

    @Value("${horizon.email.retry.maxDelay:60000}")
    private int maxRetryDelay;

    @Value("${horizon.email.retry.max:10}")
    private int maxNumberOfRetries;

    @Bean
    @ConditionalOnProperty(
        value = "spring.mail.acs-connection-string"
    )
    public EmailClient acsEmailClient() {
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
    public EmailAPI acsEmailAPI(EmailClient client) {
        return new ACSEmailAPI(client);
    }

    @Bean
    @ConditionalOnMissingBean(EmailAPI.class)
    public EmailAPI smtpEmailAPI(JavaMailSender jms) {
        return new SmtpEmailAPI(jms);
    }
}
