package org.opennms.horizon.minion.azure.http;

import com.google.common.net.HttpHeaders;
import com.google.gson.Gson;
import org.opennms.horizon.minion.azure.http.dto.AzureOAuthToken;
import org.opennms.horizon.minion.azure.http.dto.AzureResources;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.List;

public class AzureHttpClient {

    /*
     * Base URLs
     */
    private static final String LOGIN_BASE_URL = "https://login.microsoftonline.com";
    private static final String MANAGEMENT_BASE_URL = "https://management.azure.com";

    /*
     * Endpoints
     */
    private static final String OAUTH2_TOKEN_ENDPOINT = "/%s/oauth2/token";
    private static final String RESOURCES_ENDPOINT = "/subscriptions/%s/resourceGroups/%s/resources";

    /*
     * Parameters
     */
    private static final String LOGIN_GRANT_TYPE_PARAM = "grant_type=client_credentials";
    private static final String LOGIN_CLIENT_ID_PARAM = "client_id=";
    private static final String LOGIN_CLIENT_SECRET_PARAM = "client_secret=";
    private static final String LOGIN_RESOURCE_PARAM = "resource=" + MANAGEMENT_BASE_URL + "/";

    /*
     * Misc
     */
    private static final String APPLICATION_FORM_URLENCODED_VALUE = "application/x-www-form-urlencoded";
    private static final String PARAMETER_DELIMITER = "&";
    private static final int STATUS_CODE_SUCCESSFUL = 200;

    private final HttpClient client;
    private final Gson gson;

    public AzureHttpClient() {
        this.client = HttpClient.newHttpClient();
        this.gson = new Gson();
    }

    public AzureOAuthToken login(String directoryId, String clientId, String clientSecret, long timeout) throws AzureHttpException {
        List<String> parameters = new LinkedList<>();
        parameters.add(LOGIN_GRANT_TYPE_PARAM);
        parameters.add(LOGIN_CLIENT_ID_PARAM + clientId);
        parameters.add(LOGIN_CLIENT_SECRET_PARAM + clientSecret);
        parameters.add(LOGIN_RESOURCE_PARAM);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(String.format(LOGIN_BASE_URL + OAUTH2_TOKEN_ENDPOINT, directoryId)))
            .timeout(Duration.of(timeout, ChronoUnit.MILLIS))
            .header(HttpHeaders.CONTENT_TYPE, APPLICATION_FORM_URLENCODED_VALUE)
            .POST(HttpRequest.BodyPublishers.ofString(String.join(PARAMETER_DELIMITER, parameters)))
            .build();

        return performRequest(OAUTH2_TOKEN_ENDPOINT, AzureOAuthToken.class, request);
    }

    public AzureResources getResources(AzureOAuthToken token, String subscriptionId, String resourceGroup, long timeout) throws AzureHttpException {
        String url = String.format(RESOURCES_ENDPOINT + "?api-version=2021-04-01", subscriptionId, resourceGroup);
        return get(token, url, timeout, AzureResources.class);
    }

    public <T> T get(AzureOAuthToken token, String endpoint, long timeout, Class<T> clazz) throws AzureHttpException {
        String url = MANAGEMENT_BASE_URL + endpoint;
        HttpRequest request = buildGetHttpRequest(token, url, timeout);

        return performRequest(endpoint, clazz, request);
    }

    private <T> T performRequest(String endpoint, Class<T> clazz, HttpRequest request) throws AzureHttpException {
        try {
            HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (httpResponse.statusCode() == STATUS_CODE_SUCCESSFUL) {
                return gson.fromJson(httpResponse.body(), clazz);
            }

            throw new AzureHttpException("Failed to get for endpoint: "
                + endpoint + " status: " + httpResponse.statusCode() + " body: " + httpResponse.body());

        } catch (IOException e) {
            throw new AzureHttpException("Failed to get for endpoint: " + endpoint, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AzureHttpException("Failed to get for endpoint: " + endpoint, e);
        }
    }

    private HttpRequest buildGetHttpRequest(AzureOAuthToken token, String url, long timeout) {
        return HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.of(timeout, ChronoUnit.MILLIS))
            .header(HttpHeaders.AUTHORIZATION, String.format("%s %s", token.getTokenType(), token.getAccessToken()))
            .GET().build();
    }
}
