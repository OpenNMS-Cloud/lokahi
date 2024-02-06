package org.opennms.horizon.it;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.restassured.response.Response;
import org.awaitility.Awaitility;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.AccessTokenResponse;
import org.opennms.horizon.utils.TestTrustManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;
import static org.keycloak.OAuth2Constants.PASSWORD;

public class KeycloakTestSteps {

    private static final Logger log = LoggerFactory.getLogger(KeycloakTestSteps.class);

    private String keycloakBaseUrl;
    private String keycloakRealm;
    private String keycloakUsername;
    private String keycloakPassword;
    private String keycloakClientId;

    private Keycloak keycloakClient;
    private Response restResponse;
    private String keycloakAccessToken;

//========================================
// Getters and Setters
//----------------------------------------

    public String getKeycloakAccessToken() {
        return keycloakAccessToken;
    }


//========================================
// Test Step Definitions
//----------------------------------------

    @Given("Keycloak server base url in environment variable {string}")
    public void keycloakServerBaseUrlInEnvironmentVariable(String variableName) {
        keycloakBaseUrl = System.getenv(variableName);

        log.info("KEYCLOAK BASE URL: {}", keycloakBaseUrl);
    }

    @Given("Keycloak realm in environment variable {string}")
    public void keycloakRealmInEnvironmentVariable(String variableName) {
        keycloakRealm = System.getenv(variableName);

        log.info("KEYCLOAK REALM: {}", keycloakRealm);
    }

    @Given("Keycloak username in environment variable {string}")
    public void keycloakUsernameInEnvironmentVariable(String variableName) {
        keycloakUsername = System.getenv(variableName);

        log.info("KEYCLOAK USERNAME: {}", keycloakUsername);
    }

    @Given("Keycloak password in environment variable {string}")
    public void keycloakPasswordInEnvironmentVariable(String variableName) {
        keycloakPassword = System.getenv(variableName);
    }

    @Given("Keycloak client-id in environment variable {string}")
    public void keycloakClientIdInEnvironmentVariable(String variableName) {
        keycloakClientId = System.getenv(variableName);
    }

    @Then("login to Keycloak with timeout {int}ms")
    public void loginToKeycloak(long timeout) {
        Awaitility
            .await()
            .timeout(timeout, TimeUnit.MILLISECONDS)
            .ignoreExceptions()
            .until(this::commonLoginToKeycloak)
            ;
    }

//========================================
// Internals
//----------------------------------------

    private boolean commonLoginToKeycloak() throws NoSuchAlgorithmException, KeyManagementException {
        ResteasyClientBuilder restClientBuilder = new ResteasyClientBuilderImpl();
        restClientBuilder.disableTrustManager();
        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, new TestTrustManager[]{new TestTrustManager()}, new SecureRandom());
        restClientBuilder.sslContext(sslContext);

        keycloakClient =
            KeycloakBuilder.builder()
                .serverUrl(keycloakBaseUrl)
                .grantType(PASSWORD)
                .realm(keycloakRealm)
                .username(keycloakUsername)
                .password(keycloakPassword)
                .clientId(keycloakClientId)
                .resteasyClient(restClientBuilder.build())
                .build()
        ;

        try {
            AccessTokenResponse accessTokenResponse = keycloakClient.tokenManager().getAccessToken();
            keycloakAccessToken = accessTokenResponse.getToken();
        } finally {
            keycloakClient.close();
        }

        assertNotNull(keycloakAccessToken);

        return true;
    }
}
