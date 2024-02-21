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
package org.opennms.horizon.notifications.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

import com.azure.communication.email.EmailClient;
import com.azure.communication.email.EmailClientBuilder;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.opennms.horizon.notifications.api.email.ACSEmailAPI;
import org.opennms.horizon.notifications.api.email.EmailAPI;
import org.opennms.horizon.notifications.api.email.SmtpEmailAPI;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mail.javamail.JavaMailSenderImpl;

class EmailConfigTest {
    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(EmailConfig.class);

    @Test
    void testACSUsed() {
        EmailClient mockClient = Mockito.mock(EmailClient.class);

        try (MockedConstruction<EmailClientBuilder> builder =
                Mockito.mockConstruction(EmailClientBuilder.class, (mock, ctx) -> {
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
        contextRunner.withBean(JavaMailSenderImpl.class).run(ctx -> {
            assertThat(ctx).doesNotHaveBean(EmailClient.class);
            assertThat(ctx).getBean(EmailAPI.class).isInstanceOf(SmtpEmailAPI.class);
        });
    }
}
