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
        .withDatabaseName("notifications").withUsername("notifications")
        .withPassword("passw0rd").withExposedPorts(5432);

    public static final GenericContainer<?> mailhog = new GenericContainer<>(DockerImageName.parse("mailhog/mailhog:v1.0.1"))
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
            "spring.mail.port=" + mailhog.getMappedPort(MAILHOG_SMTP_PORT)
        ).applyTo(context.getEnvironment());
    }
}
