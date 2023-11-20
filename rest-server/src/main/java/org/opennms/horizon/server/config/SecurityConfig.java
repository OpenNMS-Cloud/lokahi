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

package org.opennms.horizon.server.config;

import lombok.extern.slf4j.Slf4j;
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
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
    private static final String USER_ROLE_AUTHORITY = "ROLE_USER";

    @Bean
    public SecurityWebFilterChain securityFilterChain(
        @Value("${graphql.spqr.http.endpoint:/graphql}") String graphQLEndpoint,
        ServerHttpSecurity http,
        ReactiveJwtDecoder jwtDecoder
    ) {
        http
            // Disabled because by-default, it is not configured to work with
            // OAuth2
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange((authorize) -> authorize
                .pathMatchers(graphQLEndpoint).hasAuthority(USER_ROLE_AUTHORITY)
                .anyExchange().permitAll()
            )
            .oauth2ResourceServer(configurer ->
                configurer.jwt(jwtSpec ->
                    jwtSpec
                        .jwtDecoder(jwtDecoder)
                        .jwtAuthenticationConverter(grantedAuthoritiesExtractor())
                )
            );

        return http.build();
    }

    Converter<Jwt, Mono<AbstractAuthenticationToken>> grantedAuthoritiesExtractor() {
        var authConverter = new JwtAuthenticationConverter();
        // Right now we don't provide specific scopes or roles for the user
        // aside from their tenant membership, so we'll assume a simple role
        // for now.
        authConverter.setJwtGrantedAuthoritiesConverter(
            jwt -> List.of(new SimpleGrantedAuthority(USER_ROLE_AUTHORITY))
        );
        return new ReactiveJwtAuthenticationConverterAdapter(authConverter);
    }
}
