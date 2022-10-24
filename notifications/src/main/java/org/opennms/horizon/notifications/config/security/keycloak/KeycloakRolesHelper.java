package org.opennms.horizon.notifications.config.security.keycloak;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class KeycloakRolesHelper {
    private final Logger LOG = LoggerFactory.getLogger(KeycloakRolesHelper.class);

    @Value("${horizon.keycloak.realm}")
    String realm;

    @Value("${horizon.keycloak.admin-realm}")
    String adminRealm;

    @Value("${horizon.keycloak.base-url}")
    String serverUrl;

    @Value("${horizon.keycloak.admin-username}")
    String adminUsername;

    @Value("${horizon.keycloak.admin-password}")
    String adminPassword;

    @Value("${horizon.keycloak.client-id}")
    String clientId;

    public List<String> getRoles(String username) {
        List<String> roles = new ArrayList<>();

        Keycloak keycloak = KeycloakBuilder.builder()
            .serverUrl(serverUrl)
            .realm(adminRealm)
            .grantType(OAuth2Constants.PASSWORD)
            .username(adminUsername)
            .password(adminPassword)
            .clientId(clientId)
            .clientSecret("unused")
            .resteasyClient(new ResteasyClientBuilder()
                .connectionPoolSize(10)
                .build())
            .build();

        String userId = keycloak.realm(realm)
            .users()
            .search(username)
            .get(0)
            .getId();

        UserResource user = keycloak
            .realm("opennms")
            .users()
            .get(userId);

        RoleMappingResource roleMappings = user.roles();

        for (RoleRepresentation role: roleMappings.getAll().getRealmMappings()) {
            String roleName = role.getName();
            roles.add(roleName);
        }

        return roles;
    }
}
