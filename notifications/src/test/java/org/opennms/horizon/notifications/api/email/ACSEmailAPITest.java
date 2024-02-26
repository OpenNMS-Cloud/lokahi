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
package org.opennms.horizon.notifications.api.email;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

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
        when(acsClient.beginSend(any())).thenThrow(new ErrorResponseException("Error", httpResponse));

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
        when(acsClient.beginSend(any())).thenThrow(new ErrorResponseException("Error", httpResponse));

        String recipient = "user@example.com";
        String subject = "Alert!";
        String body = "<h1>Read me!</h1>";

        Assertions.assertThatThrownBy(() -> emailAPI.sendEmail(recipient, subject, body))
                .isInstanceOf(NotificationAPIException.class);
    }
}
