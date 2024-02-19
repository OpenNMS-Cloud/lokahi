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
package org.opennms.horizon.alertservice.config;

import com.google.common.base.Strings;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
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
        try {
            byte[] byteKey = Base64.decode(key.getBytes());
            X509EncodedKeySpec x509PublicKey = new X509EncodedKeySpec(byteKey);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePublic(x509PublicKey);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Bean(destroyMethod = "stopServer")
    public GrpcServerManager startServer(
            AlertGrpcService alertGrpc,
            MonitorPolicyGrpc policyGrpc,
            GrpcTagServiceImpl tagGrpc,
            AlertEventDefinitionGrpcService alertEventDefinitionGrpc,
            AlertServerInterceptor interceptor) {
        GrpcServerManager manager = new GrpcServerManager(port, interceptor);
        manager.startServer(alertGrpc, policyGrpc, tagGrpc, alertEventDefinitionGrpc);
        return manager;
    }
}
