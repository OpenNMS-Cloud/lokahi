package org.opennms.horizon.notifications.api.keycloak;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opennms.horizon.shared.constants.GrpcConstants;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;

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
            mock.when(() -> Keycloak.getInstance(any(), any(), any(), any(), any(String.class)).realm(any()).users().list()).thenReturn(List.of(user));

            assertEquals(List.of(user.getEmail()), keyCloakAPI.getTenantEmailAddresses(GrpcConstants.DEFAULT_TENANT_ID));
        }
    }

    @Test
    public void onlySupportsDefaultTenant() {
        UserRepresentation user = new UserRepresentation();
        user.setFirstName("First");
        user.setLastName("Last");
        user.setEmail("my@email.com");

        try (MockedStatic<Keycloak> mock = Mockito.mockStatic(Keycloak.class, RETURNS_DEEP_STUBS)) {
            mock.when(() -> Keycloak.getInstance(any(), any(), any(), any(), any(String.class)).realm(any()).users().list()).thenReturn(List.of(user));

            assertEquals(Collections.emptyList(), keyCloakAPI.getTenantEmailAddresses("AnyOtherTenantHere"));
        }
    }


}
