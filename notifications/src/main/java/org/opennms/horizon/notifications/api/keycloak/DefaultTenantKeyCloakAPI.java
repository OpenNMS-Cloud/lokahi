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

package org.opennms.horizon.notifications.api.keycloak;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Component
public class DefaultTenantKeyCloakAPI implements KeyCloakAPI {

    @Value("${horizon.keycloak.base-url}")
    private String baseUrl;

    @Value("${horizon.keycloak.realm}")
    private String realm;

    @Value("${horizon.keycloak.admin-username}")
    private String username;

    @Value("${horizon.keycloak.admin-password}")
    private String password;

    @Value("${horizon.keycloak.client-id}")
    private String clientId;

    @Override
    public List<String> getTenantEmailAddresses(String tenant) {
        // Only do lookups for the default tenant
        if (Objects.equals(tenant, GrpcConstants.DEFAULT_TENANT_ID)) {
            try (Keycloak client = Keycloak.getInstance(baseUrl, realm, username, password, clientId)) {
                return client.realm(realm).users().list().stream().map(UserRepresentation::getEmail).toList();
            }
        }
        return Collections.emptyList();
    }
}
