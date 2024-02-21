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
package org.opennms.horizon.server.test.config;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

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
        return WebTestClient.bindToApplicationContext(context)
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
                Map.of("a", "value")));
    }
}
