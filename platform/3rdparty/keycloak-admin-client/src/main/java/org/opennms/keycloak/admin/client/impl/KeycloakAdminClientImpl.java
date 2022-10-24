package org.opennms.keycloak.admin.client.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.keycloak.representations.AccessTokenResponse;
import org.opennms.keycloak.admin.client.KeycloakAdminClient;
import org.opennms.keycloak.admin.client.KeycloakAdminClientSession;
import org.opennms.keycloak.admin.client.exc.KeycloakAuthenticationException;
import org.opennms.keycloak.admin.client.util.SurprisinglyHardToFindUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

public class KeycloakAdminClientImpl implements KeycloakAdminClient {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(KeycloakAdminClientImpl.class);

    private Logger log = DEFAULT_LOGGER;

    public static final String DEFAULT_CLIENT_ID = "admin-cli";
    public static final String DEFAULT_SCOPE = "openid";

    private String clientId = DEFAULT_CLIENT_ID;
    private String scope = DEFAULT_SCOPE;
    private String baseUrl;

    private ObjectMapper objectMapper;
    private HttpClient httpClient;

    private KeycloakResponseUtil keycloakResponseUtil = KeycloakResponseUtil.INSTANCE;
    private SurprisinglyHardToFindUtils surprisinglyHardToFindUtils = SurprisinglyHardToFindUtils.INSTANCE;

//========================================
// Lifecycle
//----------------------------------------

    public void init() {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
        }

        if (httpClient == null) {

            int timeout = 5;
            RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(timeout * 1000)
                .setConnectionRequestTimeout(timeout * 1000)
                .setSocketTimeout(timeout * 1000).build();

            httpClient =
                    HttpClientBuilder.create()
                            .setDefaultRequestConfig(config)
                            .build();
        }
    }

//========================================
// Getters and Setters
//----------------------------------------

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public KeycloakResponseUtil getKeycloakResponseUtil() {
        return keycloakResponseUtil;
    }

    public void setKeycloakResponseUtil(KeycloakResponseUtil keycloakResponseUtil) {
        this.keycloakResponseUtil = keycloakResponseUtil;
    }

    public SurprisinglyHardToFindUtils getSurprisinglyHardToFindUtils() {
        return surprisinglyHardToFindUtils;
    }

    public void setSurprisinglyHardToFindUtils(SurprisinglyHardToFindUtils surprisinglyHardToFindUtils) {
        this.surprisinglyHardToFindUtils = surprisinglyHardToFindUtils;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

//========================================
// API
//----------------------------------------

    /**
     * // URL="http://localhost:9000/realms/${REALM}/protocol/openid-connect/token"
     *
     * @param realm
     * @param user
     * @param password
     * @return
     * @throws MalformedURLException
     */
    @Override
    public KeycloakAdminClientSession login(String realm, String user, String password) throws IOException, URISyntaxException, KeycloakAuthenticationException {
        log.debug("login keycloak at {} with admin realm [{}], admin user [{}]", this.getBaseUrl(), realm, user);

        //
        // Format the URL
        //
        String encodedRealm = surprisinglyHardToFindUtils.encodeUrlPathSegment(realm);
        String path = "realms/" + encodedRealm + "/protocol/openid-connect/token";
        URL fullUrl = this.formatUrl(path);

        HttpEntity httpEntity = prepareLoginRequestEntity(user, password);

        HttpPost tokenPostRequest = new HttpPost();
        tokenPostRequest.setHeader(HTTP.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType());
        tokenPostRequest.setURI(fullUrl.toURI());
        tokenPostRequest.setEntity(httpEntity);

        HttpResponse httpResponse = null;
        AccessTokenResponse accessTokenResponse = null;

        try {
            httpResponse = httpClient.execute(tokenPostRequest);
            accessTokenResponse = keycloakResponseUtil.parseAccessTokenResponse(httpResponse);
        } finally {
            if (httpResponse != null) {
                EntityUtils.consumeQuietly(httpResponse.getEntity());  //make sure the connection is closed
            }
        }

        KeycloakAdminClientSessionImpl result = new KeycloakAdminClientSessionImpl();
        result.setClientId(clientId);
        result.setScope(scope);
        result.setHttpClient(httpClient);
        result.setObjectMapper(objectMapper);
        result.setBaseUrl(baseUrl);
        result.setAdminRealm(realm);
        result.setInitialAccessToken(accessTokenResponse.getToken());
        result.setInitialRefreshToken(accessTokenResponse.getRefreshToken());
        result.init();

        return result;
    }

//========================================
// Internals
//----------------------------------------

    private URL formatUrl(String path) throws MalformedURLException {
        URL base = new URL(baseUrl);
        URL fullUrl = new URL(base, path);

        return fullUrl;
    }

    private HttpEntity prepareLoginRequestEntity(String username, String password) throws UnsupportedEncodingException {
        List<NameValuePair> params = new LinkedList<>();
        params.add(new BasicNameValuePair("username", username));
        params.add(new BasicNameValuePair("password", password));
        params.add(new BasicNameValuePair("grant_type", "password"));
        params.add(new BasicNameValuePair("client_id", clientId));
        params.add(new BasicNameValuePair("scope", scope));

        UrlEncodedFormEntity urlEncodedFormEntity = new UrlEncodedFormEntity(params);

        return urlEncodedFormEntity;
    }
}
