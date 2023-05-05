/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

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
            } catch (MessagingException|MailException e) {
                throw new NotificationAPIException(e);
            }
        });

    }
}
