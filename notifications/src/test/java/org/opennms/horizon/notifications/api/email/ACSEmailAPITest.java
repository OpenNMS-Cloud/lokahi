package org.opennms.horizon.notifications.api.email;

import com.azure.communication.email.EmailClient;
import com.azure.communication.email.models.EmailMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class ACSEmailAPITest {

    @InjectMocks
    ACSEmailAPI emailAPI;

    @Mock
    EmailClient acsClient;

    @Captor
    ArgumentCaptor<EmailMessage> emailCaptor;

    @Test
    void canSendEmail() {
        String recipient = "email@company.com";
        String subject = "10 tricks to monitor your network, bandwidth wasters HATE this!";
        String body = "<h1>Read me!</h1>";
        emailAPI.sendEmail(recipient, subject, body);

        Mockito.verify(acsClient, times(1)).beginSend(emailCaptor.capture());
        EmailMessage sentEmail = emailCaptor.getValue();
        assertEquals(1, sentEmail.getToRecipients().size());
        assertEquals(recipient, sentEmail.getToRecipients().get(0).getAddress());
        assertEquals(subject, sentEmail.getSubject());
        assertEquals(body, sentEmail.getBodyHtml());
    }
}
