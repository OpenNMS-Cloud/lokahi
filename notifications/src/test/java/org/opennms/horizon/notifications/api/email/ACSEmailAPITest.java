package org.opennms.horizon.notifications.api.email;

import com.azure.communication.email.EmailClient;
import com.azure.communication.email.implementation.models.ErrorResponseException;
import com.azure.communication.email.models.EmailMessage;
import com.azure.core.http.HttpResponse;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opennms.horizon.notifications.exceptions.NotificationAPIException;
import org.opennms.horizon.notifications.exceptions.NotificationBadDataException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ACSEmailAPITest {

    @InjectMocks
    ACSEmailAPI emailAPI;

    @Mock
    EmailClient acsClient;

    @Captor
    ArgumentCaptor<EmailMessage> emailCaptor;

    @Test
    void canSendEmail() throws Exception {
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

    @Test
    void throwsBadDataExceptionOn400() {
        HttpResponse httpResponse = mock(HttpResponse.class);
        when(httpResponse.getStatusCode()).thenReturn(400);
        when(acsClient.beginSend(any()))
            .thenThrow(new ErrorResponseException("Error", httpResponse));

        String recipient = "";
        String subject = "Alert!";
        String body = "<h1>Read me!</h1>";


        Assertions.assertThatThrownBy(() -> emailAPI.sendEmail(recipient, subject, body))
            .isInstanceOf(NotificationBadDataException.class);
    }

    @Test
    void throwsAPIExceptionOnOtherErrors() {
        HttpResponse httpResponse = mock(HttpResponse.class);
        when(httpResponse.getStatusCode()).thenReturn(401);
        when(acsClient.beginSend(any()))
            .thenThrow(new ErrorResponseException("Error", httpResponse));

        String recipient = "user@example.com";
        String subject = "Alert!";
        String body = "<h1>Read me!</h1>";


        Assertions.assertThatThrownBy(() -> emailAPI.sendEmail(recipient, subject, body))
            .isInstanceOf(NotificationAPIException.class);
    }
}
