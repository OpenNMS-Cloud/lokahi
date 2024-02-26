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
package org.opennms.horizon.notifications.api.keycloak;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opennms.horizon.notifications.GrpcTestBase;

@ExtendWith(MockitoExtension.class)
public class SingleTenantKeyCloakAPITest {
    @InjectMocks
    DefaultTenantKeyCloakAPI keyCloakAPI;

    @Test
    public void canRetrieveEmails() {
        UserRepresentation user = new UserRepresentation();
        user.setFirstName("First");
        user.setLastName("Last");
        user.setEmail("my@email.com");

        try (MockedStatic<Keycloak> mock = Mockito.mockStatic(Keycloak.class, RETURNS_DEEP_STUBS)) {
            mock.when(() -> Keycloak.getInstance(any(), any(), any(), any(), any(String.class))
                            .realm(any())
                            .users()
                            .list())
                    .thenReturn(List.of(user));

            assertEquals(List.of(user.getEmail()), keyCloakAPI.getTenantEmailAddresses(GrpcTestBase.defaultTenant));
        }
    }
}
