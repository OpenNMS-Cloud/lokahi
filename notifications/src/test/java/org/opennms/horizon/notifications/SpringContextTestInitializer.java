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
package org.opennms.horizon.notifications;

import org.jetbrains.annotations.NotNull;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public class SpringContextTestInitializer implements ApplicationContextInitializer<GenericApplicationContext> {
    public static final Integer MAILHOG_SMTP_PORT = 1025;
    public static final Integer MAILHOG_WEB_PORT = 8025;

    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14.5-alpine")
            .withDatabaseName("notifications")
            .withUsername("notifications")
            .withPassword("passw0rd")
            .withExposedPorts(5432);

    public static final GenericContainer<?> mailhog = new GenericContainer<>(
                    DockerImageName.parse("mailhog/mailhog:v1.0.1"))
            .withExposedPorts(MAILHOG_WEB_PORT, MAILHOG_SMTP_PORT);

    static {
        postgres.start();
        mailhog.start();
    }

    @Override
    public void initialize(@NotNull GenericApplicationContext context) {
        initDatasourceParams(context);
    }

    private void initDatasourceParams(GenericApplicationContext context) {
        TestPropertyValues.of(
                        "spring.datasource.url=" + postgres.getJdbcUrl(),
                        "spring.datasource.username=" + postgres.getUsername(),
                        "spring.datasource.password=" + postgres.getPassword(),
                        "spring.mail.host=" + mailhog.getHost(),
                        "spring.mail.port=" + mailhog.getMappedPort(MAILHOG_SMTP_PORT))
                .applyTo(context.getEnvironment());
    }
}
