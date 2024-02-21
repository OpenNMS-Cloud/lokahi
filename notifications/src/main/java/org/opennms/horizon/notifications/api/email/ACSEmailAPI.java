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

import com.azure.communication.email.EmailClient;
import com.azure.communication.email.implementation.models.ErrorResponseException;
import com.azure.communication.email.models.EmailMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.opennms.horizon.notifications.exceptions.NotificationAPIException;
import org.opennms.horizon.notifications.exceptions.NotificationBadDataException;
import org.opennms.horizon.notifications.exceptions.NotificationException;

/**
 * Implements email using Azure Communication Services instead of SMTP
 */
@Slf4j
@RequiredArgsConstructor
public class ACSEmailAPI implements EmailAPI {

    private final String fromAddress;

    private final EmailClient client;

    @Override
    public void sendEmail(String emailAddress, String subject, String bodyHtml) throws NotificationException {
        EmailMessage message = new EmailMessage();

        message.setSenderAddress(fromAddress);
        message.setToRecipients(emailAddress);
        message.setSubject(subject);
        message.setBodyHtml(bodyHtml);

        try {
            client.beginSend(message);
        } catch (ErrorResponseException e) {
            if (e.getResponse().getStatusCode() == HttpStatus.SC_BAD_REQUEST) {
                throw new NotificationBadDataException(e);
            } else {
                throw new NotificationAPIException(e);
            }
        }
    }
}
