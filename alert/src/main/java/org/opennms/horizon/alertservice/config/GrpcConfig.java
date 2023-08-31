/*******************************************************************************
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
 *******************************************************************************/

package org.opennms.horizon.alertservice.config;

import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.rotation.HardcodedPublicKeyLocator;
import org.keycloak.adapters.rotation.JWKPublicKeyLocator;
import org.keycloak.common.util.Base64;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.opennms.horizon.alertservice.grpc.AlertEventDefinitionGrpcService;
import org.opennms.horizon.alertservice.grpc.AlertGrpcService;
import org.opennms.horizon.alertservice.grpc.AlertServerInterceptor;
import org.opennms.horizon.alertservice.grpc.GrpcServerManager;
import org.opennms.horizon.alertservice.grpc.GrpcTagServiceImpl;
import org.opennms.horizon.alertservice.grpc.MonitorPolicyGrpc;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

@RequiredArgsConstructor
@Configuration
public class GrpcConfig {

    @Value("${grpc.server.port}")
    private int port;
    @Value("${keycloak.base-url}")
    private String keycloakAuthUrl;
    @Value("${keycloak.realm}")
    private String keycloakRealm;
    @Value("${keycloak.public-key}")
    private String keycloakPublicKey;
    @Value("${grpc.server.deadline:60000}")
    private long deadline;

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
        if (!Strings.isNullOrEmpty(keycloakPublicKey)) {
            // Use the given public key
            keycloak.setPublicKeyLocator(new HardcodedPublicKeyLocator(getKey(keycloakPublicKey)));
        } else {
            keycloak.setPublicKeyLocator(new JWKPublicKeyLocator());
        }
        keycloak.setPublicKeyCacheTtl(3600);
        HttpClient client = HttpClientBuilder.create().build();
        keycloak.setClient(client);

        return keycloak;
    }

    private static PublicKey getKey(String key) {
        try{
            byte[] byteKey = Base64.decode(key.getBytes());
            X509EncodedKeySpec x509PublicKey = new X509EncodedKeySpec(byteKey);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePublic(x509PublicKey);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Bean(destroyMethod = "stopServer")
    public GrpcServerManager startServer(AlertGrpcService alertGrpc,
                                         MonitorPolicyGrpc policyGrpc,
                                         GrpcTagServiceImpl tagGrpc,
                                         AlertEventDefinitionGrpcService alertEventDefinitionGrpc,
                                         AlertServerInterceptor interceptor) {
        GrpcServerManager manager = new GrpcServerManager(port, interceptor);
        manager.startServer(alertGrpc, policyGrpc, tagGrpc, alertEventDefinitionGrpc);
        return manager;
    }
}
