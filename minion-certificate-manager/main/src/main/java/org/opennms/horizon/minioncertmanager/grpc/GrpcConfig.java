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
package org.opennms.horizon.minioncertmanager.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.rotation.JWKPublicKeyLocator;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.opennms.horizon.minioncertmanager.grpc.client.InventoryClient;
import org.springframework.beans.factory.annotation.Qualifier;
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

    @Value("${grpc.server.deadline:60000}")
    private long deadline;

    @Value("${grpc.inventory.url}")
    private String inventoryUrl;

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

    @Bean(name = "inventoryChannel")
    public ManagedChannel createInventoryChannel() {
        return ManagedChannelBuilder.forTarget(inventoryUrl)
                .keepAliveWithoutCalls(true)
                .usePlaintext()
                .build();
    }

    @Bean(destroyMethod = "shutdown", initMethod = "initialStubs")
    public InventoryClient createInventoryClient(@Qualifier("inventoryChannel") ManagedChannel channel) {
        return new InventoryClient(channel, deadline);
    }

    @Bean(destroyMethod = "stopServer")
    public GrpcServerManager startServer(
            MinionCertificateManagerImpl minionCertificateManagerImpl, MinionCertServerInterceptor interceptor) {
        GrpcServerManager manager = new GrpcServerManager(port, interceptor);
        manager.startServer(minionCertificateManagerImpl);
        return manager;
    }
}
