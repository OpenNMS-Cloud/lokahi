package org.opennms.horizon.notifications.config;

import com.azure.communication.email.EmailClient;
import com.azure.communication.email.EmailClientBuilder;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.opennms.horizon.notifications.api.email.ACSEmailAPI;
import org.opennms.horizon.notifications.api.email.EmailAPI;
import org.opennms.horizon.notifications.api.email.SmtpEmailAPI;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

class EmailConfigTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(EmailConfig.class);

    @Test
    void testACSUsed() {
        EmailClient mockClient = Mockito.mock(EmailClient.class);

        try(MockedConstruction<EmailClientBuilder> builder = Mockito.mockConstruction(EmailClientBuilder.class, (mock, ctx) -> {
            Mockito.when(mock.retryPolicy(any())).thenReturn(mock);
            Mockito.when(mock.connectionString(any())).thenReturn(mock);
            Mockito.when(mock.buildClient()).thenReturn(mockClient);
        })) {
            contextRunner
                .withPropertyValues("spring.mail.acs-connection-string=foo")
                .run(ctx -> {
                    assertThat(ctx).getBean(EmailClient.class).isEqualTo(mockClient);
                    assertThat(ctx).getBean(EmailAPI.class).isInstanceOf(ACSEmailAPI.class);
                });
        }
    }

    @Test
    void testSMTPUsed() {
        contextRunner
            .withBean(JavaMailSenderImpl.class)
            .run(ctx -> {
                assertThat(ctx).doesNotHaveBean(EmailClient.class);
                assertThat(ctx).getBean(EmailAPI.class).isInstanceOf(SmtpEmailAPI.class);
        });
    }
}
