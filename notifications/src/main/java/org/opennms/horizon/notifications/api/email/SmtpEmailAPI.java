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

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.notifications.exceptions.NotificationAPIException;
import org.opennms.horizon.notifications.exceptions.NotificationAPIRetryableException;
import org.opennms.horizon.notifications.exceptions.NotificationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.support.RetryTemplate;

@RequiredArgsConstructor
public class SmtpEmailAPI implements EmailAPI {
    @Value("${spring.mail.from}")
    private String fromAddress;

    private final JavaMailSender sender;

    private final RetryTemplate emailRetryTemplate;

    @Override
    public void sendEmail(String emailAddress, String subject, String bodyHtml) throws NotificationException {
        emailRetryTemplate.execute(ctx -> {
            try {
                MimeMessage mimeMessage = sender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);

                helper.setTo(emailAddress);
                helper.setFrom(fromAddress);

                helper.setSubject(subject);
                helper.setText(bodyHtml, true);

                sender.send(helper.getMimeMessage());
                return true;
            } catch (MailSendException e) {
                // Issues like failure to connect, or the SMTP session being disconnected are all covered under
                // this exception.
                throw new NotificationAPIRetryableException("Mail API exception", e);
            } catch (MessagingException | MailException e) {
                throw new NotificationAPIException(e);
            }
        });
    }
}
