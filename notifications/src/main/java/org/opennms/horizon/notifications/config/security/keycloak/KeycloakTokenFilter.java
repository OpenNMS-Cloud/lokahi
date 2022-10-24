package org.opennms.horizon.notifications.config.security.keycloak;

import org.keycloak.TokenVerifier;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.rotation.AdapterTokenVerifier;
import org.keycloak.representations.AccessToken;
import org.keycloak.util.TokenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import static org.apache.logging.log4j.util.Strings.isEmpty;
import static org.keycloak.TokenVerifier.IS_ACTIVE;
import static org.keycloak.TokenVerifier.SUBJECT_EXISTS_CHECK;

@Component
public class KeycloakTokenFilter extends OncePerRequestFilter {
    private final Logger LOG = LoggerFactory.getLogger(KeycloakTokenFilter.class);

    KeycloakTokenFilter(KeycloakDeploymentHelper keycloakDeploymentHelper, KeycloakRolesHelper keycloakRolesHelper) {
        this.keycloakRolesHelper = keycloakRolesHelper;
        this.keycloakDeploymentHelper = keycloakDeploymentHelper;
    }

    KeycloakDeploymentHelper keycloakDeploymentHelper;

    KeycloakRolesHelper keycloakRolesHelper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // Get authorization header and validate
        final String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (isEmpty(header) || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Get jwt token and validate
        final String token = header.split(" ")[1].trim();
        AccessToken accessToken = validate(token);

        if (accessToken == null) {
            filterChain.doFilter(request, response);
            return;
        }

        UserDetails userDetails = getUserDetails(accessToken.getPreferredUsername());

        UsernamePasswordAuthenticationToken
                authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null,
                userDetails == null ?
                        List.of() : userDetails.getAuthorities()
        );

        authentication.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request)
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    private UserDetails getUserDetails(String username) {
        List<String> roles = keycloakRolesHelper.getRoles(username);

        UserDetails user = User.withUsername(username)
            .password("unknown")
            .roles(roles.toArray(new String[0]))
            .build();

        return user;
    }

    private AccessToken validate(String tokenString) {
        try {
            //
            // Copied from withDefaultChecks, removing the always-fails RealmUrlCheck(null).  Did not replace it with
            //  one that may pass because configuring the "Issuer URL" can be a challenge, and the value of this check
            //  after already verifying the public key is likely low.
            //
            // Note also that there are some unfortunate choices in the Keycloak library regarding API public and
            //  non-public methods leading to more code here.  Without the Realm URL Check problem, the following line
            //  does all of the same work:
            //
            //     AccessToken result = AdapterTokenVerifier.verifyToken(tokenString, keycloakDeployment);
            //

            // Create the verifier using the Keycloak library.
            KeycloakDeployment deployment = keycloakDeploymentHelper.getKeycloakDeployment();
            TokenVerifier<AccessToken> verifier =
                    AdapterTokenVerifier.createVerifier(tokenString, deployment,
                            false, AccessToken.class);

            // Update the checks used by the verifier using the same verifiers as the defaults, with the fix mentioned above.
            verifier.withChecks(
                    // new TokenVerifier.RealmUrlCheck(issuerUrl),
                    SUBJECT_EXISTS_CHECK,
                    new TokenVerifier.TokenTypeCheck(TokenUtil.TOKEN_TYPE_BEARER),
                    IS_ACTIVE
            );

            // Verify now
            verifier.verify();

            return verifier.getToken();
        } catch (Exception exc) {
            LOG.error("JWT authentication failure", exc);
            return null;
        }
    }
}
