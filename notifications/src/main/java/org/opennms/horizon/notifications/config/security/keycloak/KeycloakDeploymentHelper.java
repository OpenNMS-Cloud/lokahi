package org.opennms.horizon.notifications.config.security.keycloak;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.rotation.HardcodedPublicKeyLocator;
import org.keycloak.adapters.rotation.JWKPublicKeyLocator;
import org.keycloak.adapters.rotation.PublicKeyLocator;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

@Component
public class KeycloakDeploymentHelper {
    private KeycloakDeployment keycloakDeployment;

    public static final String PEM_KEY_BEGIN_MARKER = "-----BEGIN PUBLIC KEY-----";
    public static final String PEM_KEY_END_MARKER   = "-----END PUBLIC KEY-----";

    @Value("${horizon.keycloak.realm}")
    String keycloakRealm;

    @Value("${horizon.keycloak.use-static-pubkey}")
    boolean tokenSignatureUseStaticKey;

    @Value("${horizon.keycloak.base-url}")
    String keycloakServerUrl;

    @Value("${horizon.keycloak.keycloakRequireSsl}")
    boolean keycloakRequireSsl;

    @Value("${horizon.keycloak.static-pubkey-path}")
    String tokenSignaturePublicKeyPath;

    public KeycloakDeployment getKeycloakDeployment() {
        return keycloakDeployment;
    }

    @PostConstruct
    private void initKeycloak() {
        try {
            AdapterConfig adapterConfig = configureKeycloak();

            keycloakDeployment = new KeycloakDeployment();
            keycloakDeployment.setAuthServerBaseUrl(adapterConfig);
            keycloakDeployment.setRealm(keycloakRealm);

            // Create the HTTP Client that the Keycloak library will use to call out for Public Key lookup
            HttpClient httpClient = HttpClientBuilder.create().build();
            keycloakDeployment.setClient(httpClient);

            //
            // Configure the Public Key Locator for Keycloak signature validation
            //
            if (tokenSignatureUseStaticKey) {
                configureStaticPublicKeyFile();
            } else {
                configureKeycloakServerPublicKeyLookup();
            }
        } catch (Exception exc) {
            throw new RuntimeException("Failed to initialize the JWT Public Key Signature Verification", exc);
        }
    }

    private AdapterConfig configureKeycloak() {
        AdapterConfig adapterConfig = new AdapterConfig();

        adapterConfig.setAllowAnyHostname(true);
        adapterConfig.setAuthServerUrl(keycloakServerUrl);
        adapterConfig.setRealm(keycloakRealm);
        adapterConfig.setUseResourceRoleMappings(false);
        adapterConfig.setPrincipalAttribute("preferred_username");
        adapterConfig.setSslRequired(Boolean.toString(keycloakRequireSsl));

        return adapterConfig;
    }

    private void configureKeycloakServerPublicKeyLookup() {
        JWKPublicKeyLocator jwkPublicKeyLocator = new JWKPublicKeyLocator();

        keycloakDeployment.setPublicKeyLocator(jwkPublicKeyLocator);
        keycloakDeployment.setPublicKeyCacheTtl(3600);
    }

    private void configureStaticPublicKeyFile () {
        PublicKey publicKey = loadPemPublicKeyFile(tokenSignaturePublicKeyPath);

        PublicKeyLocator publicKeyLocator = new HardcodedPublicKeyLocator(publicKey);
        keycloakDeployment.setPublicKeyLocator(publicKeyLocator);
    }

    private PublicKey loadPemPublicKeyFile(String path) {
        try {

            byte[] bytes = Files.readAllBytes(Path.of(path));
            String fileContentText = new String(bytes, StandardCharsets.UTF_8);

            String extractedKeyText =
                    fileContentText
                            .replace(PEM_KEY_BEGIN_MARKER, "")
                            .replace(PEM_KEY_END_MARKER, "")
                            .trim()
                    ;

            byte[] publicKeyBinary = Base64.decodeBase64(extractedKeyText);

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBinary);

            PublicKey result = keyFactory.generatePublic(keySpec);

            return result;
        } catch (Exception exc) {
            throw new RuntimeException("Failed to load PublicKey from file " + path, exc);
        }
    }
}
