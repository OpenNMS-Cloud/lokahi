package org.opennms.horizon.shared.azure.http;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.shared.azure.http.dto.AzureHttpParams;
import org.opennms.horizon.shared.azure.http.dto.instanceview.AzureInstanceView;
import org.opennms.horizon.shared.azure.http.dto.login.AzureOAuthToken;
import org.opennms.horizon.shared.azure.http.dto.metrics.AzureMetrics;
import org.opennms.horizon.shared.azure.http.dto.resourcegroup.AzureResourceGroups;
import org.opennms.horizon.shared.azure.http.dto.resources.AzureResources;
import org.opennms.horizon.shared.azure.http.dto.subscription.AzureSubscription;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Slf4j
public class AzureHttpClient {

    /*
     * Base URLs
     */
    private static final String DEFAULT_LOGIN_BASE_URL = "https://login.microsoftonline.com";
    private static final String DEFAULT_MANAGEMENT_BASE_URL = "https://management.azure.com";

    /*
     * Endpoints
     */
    public static final String OAUTH2_TOKEN_ENDPOINT = "/%s/oauth2/token";
    public static final String SUBSCRIPTION_ENDPOINT = "/subscriptions/%s";
    public static final String RESOURCE_GROUPS_ENDPOINT = "/subscriptions/%s/resourceGroups";
    public static final String RESOURCES_ENDPOINT = "/subscriptions/%s/resourceGroups/%s/resources";
    public static final String INSTANCE_VIEW_ENDPOINT = "/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Compute/virtualMachines/%s/InstanceView";
    public static final String METRICS_ENDPOINT = "/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Compute/virtualMachines/%s/providers/Microsoft.Insights/metrics";

    /*
     * Headers
     */
    private static final String AUTH_HEADER = "Authorization";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";

    /*
     * Parameters
     */
    private static final String LOGIN_GRANT_TYPE_PARAM = "grant_type=client_credentials";
    private static final String LOGIN_CLIENT_ID_PARAM = "client_id=";
    private static final String LOGIN_CLIENT_SECRET_PARAM = "client_secret=";

    private static final String DEFAULT_API_VERSION = "2021-04-01";
    private static final String DEFAULT_METRICS_API_VERSION = "2018-01-01";
    private static final String API_VERSION_PARAM = "?api-version=";
    private static final String PARAMETER_DELIMITER = "&";

    /*
     * Misc
     */
    private static final String APPLICATION_FORM_URLENCODED_VALUE = "application/x-www-form-urlencoded";
    private static final int STATUS_CODE_SUCCESSFUL = 200;
    private static final int INITIAL_BACKOFF_TIME_MS = 1000;
    private static final double EXPONENTIAL_BACKOFF_AMPLIFIER = 2.1d;
    private static final int MIN_TIMEOUT_MS = 300;

    private final AzureHttpParams params;
    private final HttpClient client;
    private final Gson gson;

    public AzureHttpClient() {
        this(null);
    }

    public AzureHttpClient(AzureHttpParams params) {
        this.params = populateParamDefaults(params);
        this.client = HttpClient.newHttpClient();
        this.gson = new Gson();
    }

    public AzureOAuthToken login(String directoryId, String clientId, String clientSecret, long timeout, int retries) throws AzureHttpException {
        List<String> parameters = new LinkedList<>();
        parameters.add(LOGIN_GRANT_TYPE_PARAM);
        parameters.add(LOGIN_CLIENT_ID_PARAM + clientId);
        parameters.add(LOGIN_CLIENT_SECRET_PARAM + clientSecret);
        parameters.add("resource=" + params.getBaseManagementUrl() + "/");

        String baseLoginUrl = params.getBaseLoginUrl();
        String versionQueryParam = API_VERSION_PARAM + params.getApiVersion();
        String url = String.format(baseLoginUrl + OAUTH2_TOKEN_ENDPOINT + versionQueryParam, directoryId);
        HttpRequest request = getHttpRequestBuilder(url, timeout)
            .header(CONTENT_TYPE_HEADER, APPLICATION_FORM_URLENCODED_VALUE)
            .POST(HttpRequest.BodyPublishers.ofString(String.join(PARAMETER_DELIMITER, parameters)))
            .build();

        return performRequest(OAUTH2_TOKEN_ENDPOINT, AzureOAuthToken.class, request, retries);
    }

    public AzureSubscription getSubscription(AzureOAuthToken token, String subscriptionId, long timeout, int retries) throws AzureHttpException {
        String versionQueryParam = API_VERSION_PARAM + params.getApiVersion();
        String url = String.format(SUBSCRIPTION_ENDPOINT + versionQueryParam, subscriptionId);
        return get(token, url, timeout, retries, AzureSubscription.class);
    }

    public AzureResourceGroups getResourceGroups(AzureOAuthToken token, String subscriptionId, long timeout, int retries) throws AzureHttpException {
        String versionQueryParam = API_VERSION_PARAM + params.getApiVersion();
        String url = String.format(RESOURCE_GROUPS_ENDPOINT + versionQueryParam, subscriptionId);
        return get(token, url, timeout, retries, AzureResourceGroups.class);
    }

    public AzureResources getResources(AzureOAuthToken token, String subscriptionId, String resourceGroup,
                                       long timeout, int retries) throws AzureHttpException {
        String versionQueryParam = API_VERSION_PARAM + params.getApiVersion();
        String url = String.format(RESOURCES_ENDPOINT + versionQueryParam, subscriptionId, resourceGroup);
        return get(token, url, timeout, retries, AzureResources.class);
    }

    public AzureInstanceView getInstanceView(AzureOAuthToken token, String subscriptionId, String resourceGroup,
                                             String resourceName, long timeout, int retries) throws AzureHttpException {
        String versionQueryParam = API_VERSION_PARAM + params.getApiVersion();
        String url = String.format(INSTANCE_VIEW_ENDPOINT + versionQueryParam, subscriptionId, resourceGroup, resourceName);
        return get(token, url, timeout, retries, AzureInstanceView.class);
    }

    public AzureMetrics getMetrics(AzureOAuthToken token, String subscriptionId, String resourceGroup,
                                   String resourceName, Map<String, String> params, long timeout, int retries) throws AzureHttpException {
        String versionQueryParam = API_VERSION_PARAM + this.params.getMetricsApiVersion();
        String url = String.format(METRICS_ENDPOINT + versionQueryParam, subscriptionId, resourceGroup, resourceName);
        url = addUrlParams(url, params);
        return get(token, url, timeout, retries, AzureMetrics.class);
    }

    private <T> T get(AzureOAuthToken token, String endpoint, long timeout, int retries, Class<T> clazz) throws AzureHttpException {
        String url = params.getBaseManagementUrl() + endpoint;
        HttpRequest request = buildGetHttpRequest(token, url, timeout);

        return performRequest(endpoint, clazz, request, retries);
    }

    private <T> T performRequest(String endpoint, Class<T> clazz, HttpRequest request, int retries) throws AzureHttpException {
        if (retries < 1) {
            throw new AzureHttpException("Retry count must be a positive number");
        }

        AzureHttpException exception = null;
        long backoffTime = INITIAL_BACKOFF_TIME_MS;

        for (int retryCount = 1; retryCount <= retries; retryCount++) {
            try {
                HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (httpResponse.statusCode() == STATUS_CODE_SUCCESSFUL) {

                    String httpBody = httpResponse.body();
                    return gson.fromJson(httpBody, clazz);
                }

                exception = new AzureHttpException("Failed to get for endpoint: "
                    + endpoint + " status: " + httpResponse.statusCode() + " body: " + httpResponse.body() + " retry: " + retryCount);

            } catch (IOException | InterruptedException e) {
                exception = new AzureHttpException("Failed to get for endpoint: " + endpoint + " retry: " + retryCount, e);
            }
            log.warn(exception.getMessage());

            try {
                Thread.sleep(backoffTime);
                backoffTime *= EXPONENTIAL_BACKOFF_AMPLIFIER;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AzureHttpException("Failed to wait for exp backoff with time: " + backoffTime + " retry: " + retryCount, e);
            }
        }
        Throwable cause = exception.getCause();
        if (cause instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
        throw exception;
    }

    private HttpRequest buildGetHttpRequest(AzureOAuthToken token, String url, long timeout) throws AzureHttpException {
        if (timeout < MIN_TIMEOUT_MS) {
            throw new AzureHttpException("Retry count must be a positive number > " + MIN_TIMEOUT_MS);
        }
        return getHttpRequestBuilder(url, timeout)
            .header(AUTH_HEADER, String.format("%s %s", token.getTokenType(), token.getAccessToken()))
            .GET().build();
    }

    private HttpRequest.Builder getHttpRequestBuilder(String url, long timeout) {
        return HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.of(timeout, ChronoUnit.MILLIS));
    }

    private String addUrlParams(String url, Map<String, String> params) {
        StringBuilder urlBuilder = new StringBuilder(url);
        for (Map.Entry<String, String> param : params.entrySet()) {
            urlBuilder.append(PARAMETER_DELIMITER);
            urlBuilder.append(String.format("%s=%s", param.getKey(), encode(param.getValue())));
        }
        return urlBuilder.toString();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private AzureHttpParams populateParamDefaults(AzureHttpParams params) {
        if (params == null) {
            params = new AzureHttpParams();
        }
        if (params.getBaseManagementUrl() == null) {
            params.setBaseManagementUrl(DEFAULT_MANAGEMENT_BASE_URL);
        }
        if (params.getBaseLoginUrl() == null) {
            params.setBaseLoginUrl(DEFAULT_LOGIN_BASE_URL);
        }
        if (params.getApiVersion() == null) {
            params.setApiVersion(DEFAULT_API_VERSION);
        }
        if (params.getMetricsApiVersion() == null) {
            params.setMetricsApiVersion(DEFAULT_METRICS_API_VERSION);
        }
        return params;
    }
}
