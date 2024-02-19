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
package org.opennms.horizon.server.config;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.server.web.BffGraphQLController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

@Slf4j
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
    private static final String USER_ROLE_AUTHORITY = "ROLE_USER";

    @Bean
    public SecurityWebFilterChain securityFilterChain(
            @Value(BffGraphQLController.GRAPHQL_ENDPOINT) String graphQLEndpoint,
            ServerHttpSecurity http,
            CorsConfigurationSource corsConfigurationSource,
            ReactiveJwtDecoder jwtDecoder) {
        http
                // Disabled because by-default, it is not configured to work with
                // OAuth2
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(corsSpec -> corsSpec.configurationSource(corsConfigurationSource))
                .authorizeExchange((authorize) -> authorize
                        .pathMatchers(graphQLEndpoint)
                        .hasAuthority(USER_ROLE_AUTHORITY)
                        .anyExchange()
                        .permitAll())
                .oauth2ResourceServer(configurer -> configurer.jwt(jwtSpec ->
                        jwtSpec.jwtDecoder(jwtDecoder).jwtAuthenticationConverter(grantedAuthoritiesExtractor())));

        return http.build();
    }

    Converter<Jwt, Mono<AbstractAuthenticationToken>> grantedAuthoritiesExtractor() {
        var authConverter = new JwtAuthenticationConverter();
        // Right now we don't provide specific scopes or roles for the user
        // aside from their tenant membership, so we'll assume a simple role
        // for now.
        authConverter.setJwtGrantedAuthoritiesConverter(
                jwt -> List.of(new SimpleGrantedAuthority(USER_ROLE_AUTHORITY)));
        return new ReactiveJwtAuthenticationConverterAdapter(authConverter);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value(BffGraphQLController.GRAPHQL_ENDPOINT) String graphQLEndpoint, BffProperties bffProperties) {
        var source = new UrlBasedCorsConfigurationSource();

        if (bffProperties.isCorsAllowed()) {
            log.info("Allowing all CORS requests");
            source.registerCorsConfiguration(graphQLEndpoint, new CorsConfiguration().applyPermitDefaultValues());
        }
        return source;
    }
}
