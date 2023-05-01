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

import com.azure.communication.email.EmailClient;
import com.azure.communication.email.models.EmailMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;

/**
 * Implements email using Azure Communication Services instead of SMTP
 */
@RequiredArgsConstructor
@ConditionalOnBean(EmailClient.class)
public class ACSEmailAPI implements EmailAPI {
    @Value("${spring.mail.from}")
    private String fromAddress;

    private final EmailClient client;

    @Override
    public void sendEmail(String emailAddress, String subject, String bodyHtml) {
        EmailMessage message = new EmailMessage();

        message.setSenderAddress(fromAddress);
        message.setToRecipients(emailAddress);
        message.setSubject(subject);
        message.setBodyHtml(bodyHtml);

        client.beginSend(message);
    }
}
