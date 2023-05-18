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

package org.opennms.horizon.shared.azure.http;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.shared.azure.http.dto.AzureHttpParams;
import org.opennms.horizon.shared.azure.http.dto.error.AzureErrorDescription;
import org.opennms.horizon.shared.azure.http.dto.error.AzureHttpError;
import org.opennms.horizon.shared.azure.http.dto.instanceview.AzureInstanceView;
import org.opennms.horizon.shared.azure.http.dto.login.AzureOAuthToken;
import org.opennms.horizon.shared.azure.http.dto.metrics.AzureMetrics;
import org.opennms.horizon.shared.azure.http.dto.networkinterface.AzureNetworkInterfaces;
import org.opennms.horizon.shared.azure.http.dto.publicipaddresses.AzurePublicIpAddresses;
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
    public static final String NETWORK_INTERFACES_ENDPOINT = "/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Network/networkInterfaces";
    public static final String PUBLIC_IP_ADDRESSES_ENDPOINT = "/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Network/publicIPAddresses";
    public static final String INSTANCE_VIEW_ENDPOINT = "/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Compute/virtualMachines/%s/InstanceView";
    public static final String METRICS_ENDPOINT = "/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Compute/virtualMachines/%s/providers/Microsoft.Insights/metrics";
    public static final String NETWORK_INTERFACE_METRICS_ENDPOINT = "/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Network/networkInterfaces/%s/providers/Microsoft.Insights/metrics";

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

    public AzureOAuthToken login(String directoryId, String clientId, String clientSecret, long timeoutMs, int retries) throws AzureHttpException {
        List<String> parameters = new LinkedList<>();
        parameters.add(LOGIN_GRANT_TYPE_PARAM);
        parameters.add(LOGIN_CLIENT_ID_PARAM + clientId);
        parameters.add(LOGIN_CLIENT_SECRET_PARAM + clientSecret);
        parameters.add("resource=" + params.getBaseManagementUrl() + "/");

        String baseLoginUrl = params.getBaseLoginUrl();
        String versionQueryParam = API_VERSION_PARAM + params.getApiVersion();
        String url = String.format(baseLoginUrl + OAUTH2_TOKEN_ENDPOINT + versionQueryParam, directoryId);
        HttpRequest request = getHttpRequestBuilder(url, timeoutMs)
            .header(CONTENT_TYPE_HEADER, APPLICATION_FORM_URLENCODED_VALUE)
            .POST(HttpRequest.BodyPublishers.ofString(String.join(PARAMETER_DELIMITER, parameters)))
            .build();

        return performRequest(OAUTH2_TOKEN_ENDPOINT, AzureOAuthToken.class, request, retries);
    }

    public AzureSubscription getSubscription(AzureOAuthToken token, String subscriptionId, long timeoutMs, int retries) throws AzureHttpException {
        String versionQueryParam = API_VERSION_PARAM + params.getApiVersion();
        String url = String.format(SUBSCRIPTION_ENDPOINT + versionQueryParam, subscriptionId);
        return get(token, url, timeoutMs, retries, AzureSubscription.class);
    }

    public AzureResourceGroups getResourceGroups(AzureOAuthToken token, String subscriptionId, long timeoutMs, int retries) throws AzureHttpException {
        String versionQueryParam = API_VERSION_PARAM + params.getApiVersion();
        String url = String.format(RESOURCE_GROUPS_ENDPOINT + versionQueryParam, subscriptionId);
        return get(token, url, timeoutMs, retries, AzureResourceGroups.class);
    }

    public AzureResources getResources(AzureOAuthToken token, String subscriptionId, String resourceGroup,
                                       long timeoutMs, int retries) throws AzureHttpException {
        String versionQueryParam = API_VERSION_PARAM + params.getApiVersion();
        String url = String.format(RESOURCES_ENDPOINT + versionQueryParam, subscriptionId, resourceGroup);
        return get(token, url, timeoutMs, retries, AzureResources.class);
    }

    public AzureNetworkInterfaces getNetworkInterfaces(AzureOAuthToken token, String subscriptionId, String resourceGroup,
                                                       long timeoutMs, int retries) throws AzureHttpException {
        String versionQueryParam = API_VERSION_PARAM + params.getApiVersion();
        String url = String.format(NETWORK_INTERFACES_ENDPOINT + versionQueryParam, subscriptionId, resourceGroup);
        return get(token, url, timeoutMs, retries, AzureNetworkInterfaces.class);
    }

    public AzurePublicIpAddresses getPublicIpAddresses(AzureOAuthToken token, String subscriptionId, String resourceGroup, long timeoutMs, int retries) throws AzureHttpException {
        String versionQueryParam = API_VERSION_PARAM + params.getApiVersion();
        String url = String.format(PUBLIC_IP_ADDRESSES_ENDPOINT + versionQueryParam, subscriptionId, resourceGroup);
        return get(token, url, timeoutMs, retries, AzurePublicIpAddresses.class);
    }

    public AzureInstanceView getInstanceView(AzureOAuthToken token, String subscriptionId, String resourceGroup,
                                             String resourceName, long timeoutMs, int retries) throws AzureHttpException {
        String versionQueryParam = API_VERSION_PARAM + params.getApiVersion();
        String url = String.format(INSTANCE_VIEW_ENDPOINT + versionQueryParam, subscriptionId, resourceGroup, resourceName);
        return get(token, url, timeoutMs, retries, AzureInstanceView.class);
    }

    public AzureMetrics getMetrics(AzureOAuthToken token, String subscriptionId, String resourceGroup,
                                   String resourceName, Map<String, String> params, long timeoutMs, int retries) throws AzureHttpException {
        String versionQueryParam = API_VERSION_PARAM + this.params.getMetricsApiVersion();
        String url = String.format(METRICS_ENDPOINT + versionQueryParam, subscriptionId, resourceGroup, resourceName);
        url = addUrlParams(url, params);
        return get(token, url, timeoutMs, retries, AzureMetrics.class);
    }

    public AzureMetrics getNetworkInterfaceMetrics(AzureOAuthToken token, String subscriptionId, String resourceGroup,
                                                   String resourceName, Map<String, String> params, long timeoutMs, int retries) throws AzureHttpException {
        String versionQueryParam = API_VERSION_PARAM + this.params.getMetricsApiVersion();
        String url = String.format(NETWORK_INTERFACE_METRICS_ENDPOINT + versionQueryParam, subscriptionId, resourceGroup, resourceName);
        url = addUrlParams(url, params);
        return get(token, url, timeoutMs, retries, AzureMetrics.class);
    }

    private <T> T get(AzureOAuthToken token, String endpoint, long timeoutMs, int retries, Class<T> clazz) throws AzureHttpException {
        String url = params.getBaseManagementUrl() + endpoint;
        HttpRequest request = buildGetHttpRequest(token, url, timeoutMs);

        return performRequest(endpoint, clazz, request, retries);
    }

    private <T> T performRequest(String endpoint, Class<T> clazz, HttpRequest request, int retries) throws AzureHttpException {
        if (retries < 1) {
            throw new AzureHttpException("Number of retries must be a positive number");
        }

        AzureHttpException exception = null;
        long backoffTime = INITIAL_BACKOFF_TIME_MS;

        for (int retryCount = 1; retryCount <= retries; retryCount++) {
            try {
                HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());

                String httpBody = httpResponse.body();

                if (httpResponse.statusCode() == STATUS_CODE_SUCCESSFUL) {
                    return gson.fromJson(httpBody, clazz);
                }

                AzureHttpError error = gson.fromJson(httpBody, AzureHttpError.class);
                AzureErrorDescription description = error.getError();

                String message = String.format("Failed to get for endpoint: %s, status: %d, body: %s, retry: %d/%d",
                    endpoint, httpResponse.statusCode(), httpResponse.body(), retryCount, retries);
                exception = new AzureHttpException(message, description);

            } catch (IOException | InterruptedException e) {
                String message = String.format("Failed to get for endpoint: %s, retry: %d/%d",
                    endpoint, retryCount, retries);
                exception = new AzureHttpException(message, e);
            }
            if (retryCount != retries) {
                log.warn(exception.getMessage());
                try {
                    Thread.sleep(backoffTime);
                    backoffTime *= EXPONENTIAL_BACKOFF_AMPLIFIER;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    String message = String.format("Failed to wait for exp backoff with time: %d, retry: %d/%d",
                        backoffTime, retryCount, retries);
                    throw new AzureHttpException(message, e);
                }
            }
        }
        Throwable cause = exception.getCause();
        if (cause instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
        throw exception;
    }

    private HttpRequest buildGetHttpRequest(AzureOAuthToken token, String url, long timeoutMs) throws AzureHttpException {
        if (timeoutMs < MIN_TIMEOUT_MS) {
            throw new AzureHttpException("Timeout must be a positive number > " + MIN_TIMEOUT_MS);
        }
        return getHttpRequestBuilder(url, timeoutMs)
            .header(AUTH_HEADER, String.format("%s %s", token.getTokenType(), token.getAccessToken()))
            .GET().build();
    }

    private HttpRequest.Builder getHttpRequestBuilder(String url, long timeoutMs) {
        return HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.of(timeoutMs, ChronoUnit.MILLIS));
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

    protected AzureHttpParams populateParamDefaults(AzureHttpParams params) {
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
