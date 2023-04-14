package org.opennms.horizon.notifications.api.email;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.Builder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opennms.horizon.alerts.proto.Alert;
import org.opennms.horizon.notifications.exceptions.NotificationAPIException;
import org.opennms.horizon.notifications.exceptions.NotificationException;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
public class SmtpEmailAPITest {

    private static final String FROM_ADDRESS = "noreply@test";

    @InjectMocks
    private SmtpEmailAPI emailAPI;

    @Mock
    JavaMailSender sender;

    @BeforeEach
    public void setup() {
        ReflectionTestUtils.setField(emailAPI, "fromAddress", FROM_ADDRESS);
        Mockito.when(sender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
    }

    @Test
    public void canSendEmailToSingleRecipient() throws NotificationException {
        List<String> userEmail = List.of("support@yourcompany.com");
        Alert alert = Alert.newBuilder().setLogMessage("Some exciting message").setDescription("A description").build();
        emailAPI.postNotification(userEmail, alert);

        MessageMatcher matcher = MessageMatcher.builder()
            .expectedToAddresses(userEmail)
            .expectedSubject(alert.getLogMessage())
            .expectedBody(alert.getDescription())
            .build();
        Mockito.verify(sender, times(1)).send(argThat(matcher));
    }

    @Test
    public void throwsOnFailure() {
        Mockito.doThrow(new MailSendException("Connection failure")).when(sender).send(any(MimeMessage.class));
        assertThrows(NotificationAPIException.class, () -> emailAPI.postNotification(List.of(), Alert.newBuilder().build()));
    }

    @Builder
    private static class MessageMatcher implements ArgumentMatcher<MimeMessage> {
        private List<String> expectedToAddresses;
        private String expectedSubject;
        private String expectedBody;

        private MediaType expectedType;

        private List<String> convert(Address[] addresses) {
            if (addresses == null) {
                return null;
            }
            return Arrays.stream(addresses)
                .filter(InternetAddress.class::isInstance)
                .map(InternetAddress.class::cast)
                .map(InternetAddress::getAddress)
                .toList();
        }

        @Override
        public boolean matches(MimeMessage mimeMessage) {
            try {
                assertEquals(expectedToAddresses, convert(mimeMessage.getRecipients(Message.RecipientType.TO)));
                assertEquals(List.of(FROM_ADDRESS), convert(mimeMessage.getFrom()));
                if (expectedType != null) {
                    assertEquals(expectedType.toString(), mimeMessage.getContentType());
                }
                if (expectedSubject != null) {
                    assertEquals(expectedSubject, mimeMessage.getSubject());
                }
                if (expectedBody != null) {
                    assertEquals(expectedBody, mimeMessage.getContent());
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            return true;
        }
    }
}
