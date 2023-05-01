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

package org.opennms.horizon.minioncertmanager.grpc;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.rotation.JWKPublicKeyLocator;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcConfig {
    private static final int DEFAULT_GRPC_PORT = 8990;

    @Value("${grpc.server.port:" + DEFAULT_GRPC_PORT + "}")
    private int port;
    @Value("${keycloak.base-url}")
    private String keycloakAuthUrl;
    @Value("${keycloak.realm}")
    private String keycloakRealm;

    @Bean
    public KeycloakDeployment createKeycloak() {
        AdapterConfig config = new AdapterConfig();
        config.setAllowAnyHostname(true);
        config.setAuthServerUrl(keycloakAuthUrl);
        config.setRealm(keycloakRealm);
        config.setUseResourceRoleMappings(false);
        config.setPrincipalAttribute("preferred_username");
        config.setSslRequired("false");

        KeycloakDeployment keycloak = new KeycloakDeployment();
        keycloak.setAuthServerBaseUrl(config);
        keycloak.setRealm(keycloakRealm);
        keycloak.setPublicKeyLocator(new JWKPublicKeyLocator());
        keycloak.setPublicKeyCacheTtl(3600);
        HttpClient client = HttpClientBuilder.create().build();
        keycloak.setClient(client);

        return keycloak;
    }

    @Bean(destroyMethod = "stopServer")
    public GrpcServerManager startServer(MinionCertificateManagerImpl minionCertificateManagerImpl, MinionCertServerInterceptor interceptor) {
        GrpcServerManager manager = new GrpcServerManager(port, interceptor);
        manager.startServer(minionCertificateManagerImpl);
        return manager;
    }
}
