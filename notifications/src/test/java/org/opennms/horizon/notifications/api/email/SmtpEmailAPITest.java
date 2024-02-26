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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Arrays;
import java.util.List;
import lombok.Builder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opennms.horizon.notifications.exceptions.NotificationAPIException;
import org.opennms.horizon.notifications.exceptions.NotificationAPIRetryableException;
import org.opennms.horizon.notifications.exceptions.NotificationException;
import org.springframework.http.MediaType;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class SmtpEmailAPITest {

    private static final String FROM_ADDRESS = "noreply@test";

    @InjectMocks
    private SmtpEmailAPI emailAPI;

    @Spy
    RetryTemplate emailRetryTemplate = RetryTemplate.builder()
            .retryOn(NotificationAPIRetryableException.class)
            .maxAttempts(3)
            .fixedBackoff(10)
            .build();

    @Mock
    JavaMailSender sender;

    @BeforeEach
    public void setup() {
        ReflectionTestUtils.setField(emailAPI, "fromAddress", FROM_ADDRESS);
        Mockito.when(sender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
    }

    @Test
    void canSendEmailToSingleRecipient() throws NotificationException {
        String userEmail = "support@yourcompany.com";
        emailAPI.sendEmail(userEmail, "A new alert", "More details about the alert");

        MessageMatcher matcher = MessageMatcher.builder()
                .expectedToAddresses(List.of(userEmail))
                .expectedSubject("A new alert")
                .expectedBody("More details about the alert")
                .build();
        Mockito.verify(sender, times(1)).send(argThat(matcher));
    }

    @Test
    void canRetryOnSendFailure() throws NotificationException {
        Mockito.doThrow(new MailSendException("Connection failure"))
                .doNothing()
                .when(sender)
                .send(any(MimeMessage.class));
        emailAPI.sendEmail("support@company", "subject", "body");

        verify(sender, times(2)).send(any(MimeMessage.class));
    }

    @Test
    void throwsOnFailure() {
        Mockito.doThrow(new MailAuthenticationException("Bad creds"))
                .when(sender)
                .send(any(MimeMessage.class));
        assertThrows(NotificationAPIException.class, () -> emailAPI.sendEmail("support@company", "subject", "body"));

        verify(sender, times(1)).send(any(MimeMessage.class));
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
