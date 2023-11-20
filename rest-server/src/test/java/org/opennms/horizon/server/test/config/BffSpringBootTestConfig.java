/*
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
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
 */

package org.opennms.horizon.server.test.config;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;

/**
 * Test-only configuration that is required to run an
 * {@link org.springframework.boot.test.context.SpringBootTest}.
 * <p>
 * Note: This will be automatically picked up in the component scan for any
 * {@link org.springframework.boot.test.context.SpringBootTest}.
 */
@Configuration
public class BffSpringBootTestConfig {
    @Bean
    public WebTestClient webTestClient(ApplicationContext context) {
        return WebTestClient
            .bindToApplicationContext(context)
            .apply(springSecurity())
            .configureClient()
            .apply(mockJwt())
            .build();
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        return token -> Mono.just(new Jwt(
            token,
            Instant.now().minus(5, ChronoUnit.MINUTES),
            Instant.now().plus(1, ChronoUnit.HOURS),
            // Headers and claims can not be empty, so provide a dummy entry
            Map.of("a", "value"),
            Map.of("a", "value")
        ));
    }
}
