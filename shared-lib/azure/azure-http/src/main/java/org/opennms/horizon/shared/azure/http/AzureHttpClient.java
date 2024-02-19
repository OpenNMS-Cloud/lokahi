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
package org.opennms.horizon.shared.azure.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.shared.azure.http.dto.AzureHttpParams;
import org.opennms.horizon.shared.azure.http.dto.error.AzureHttpError;
import org.opennms.horizon.shared.azure.http.dto.instanceview.AzureInstanceView;
import org.opennms.horizon.shared.azure.http.dto.login.AzureOAuthToken;
import org.opennms.horizon.shared.azure.http.dto.metrics.AzureMetrics;
import org.opennms.horizon.shared.azure.http.dto.networkinterface.AzureNetworkInterfaces;
import org.opennms.horizon.shared.azure.http.dto.publicipaddresses.AzurePublicIpAddresses;
import org.opennms.horizon.shared.azure.http.dto.resourcegroup.AzureResourceGroups;
import org.opennms.horizon.shared.azure.http.dto.resources.AzureResources;
import org.opennms.horizon.shared.azure.http.dto.subscription.AzureSubscription;

@Slf4j
public class AzureHttpClient {
    public enum ResourcesType {
        PUBLIC_IP_ADDRESSES("publicIPAddresses"),
        NETWORK_INTERFACES("networkInterfaces"),
        NODE("node");
        private final String metricName;
        private static final Map<String, ResourcesType> metricNameMap = new HashMap<>();

        static {
            for (ResourcesType value : ResourcesType.values()) {
                metricNameMap.put(value.getMetricName(), value);
            }
        }

        ResourcesType(String metricName) {
            this.metricName = metricName;
        }

        public String getMetricName() {
            return metricName;
        }

        public static ResourcesType fromMetricName(String metricName) {
            var type = metricNameMap.get(metricName);
            if (type != null) {
                return type;
            }
            throw new IllegalArgumentException("Invalid value: " + metricName);
        }
    }

    /*
     * Base URLs
     */
    private static final String DEFAULT_LOGIN_BASE_URL = "https://login.microsoftonline.com";
    private static final String DEFAULT_MANAGEMENT_BASE_URL = "https://management.azure.com";

    /*
     * Endpoints
     */
    public static final String OAUTH2_TOKEN_ENDPOINT = "%s/%s/oauth2/token%s";
    public static final String SUBSCRIPTION_ENDPOINT = "/subscriptions/%s%s";
    public static final String RESOURCE_GROUPS_ENDPOINT = "/subscriptions/%s/resourceGroups%s";
    public static final String RESOURCES_ENDPOINT = "/subscriptions/%s/resourceGroups/%s/resources%s";
    public static final String NETWORK_INTERFACES_ENDPOINT =
            "/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Network/networkInterfaces%s";
    public static final String PUBLIC_IP_ADDRESSES_ENDPOINT =
            "/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Network/publicIPAddresses%s";
    public static final String INSTANCE_VIEW_ENDPOINT =
            "/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Compute/virtualMachines/%s/InstanceView%s";
    public static final String METRICS_ENDPOINT =
            "/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Compute/virtualMachines/%s/providers/Microsoft.Insights/metrics%s";
    public static final String NETWORK_INTERFACE_METRICS_ENDPOINT =
            "/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Network/%s/providers/Microsoft.Insights/metrics%s";

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

    // make sure all region support this version before upgrade
    private static final String DEFAULT_API_VERSION = "2021-04-01";
    private static final String DEFAULT_METRICS_API_VERSION = "2018-01-01";
    private static final String API_VERSION_PARAM = "?api-version=";
    private static final String PARAMETER_DELIMITER = "&";

    /*
     * Misc
     */
    private static final String APPLICATION_FORM_URLENCODED_VALUE = "application/x-www-form-urlencoded";
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
        this.gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss'Z'").create();
    }

    public AzureOAuthToken login(String directoryId, String clientId, String clientSecret, long timeoutMs, int retries)
            throws AzureHttpException {
        StringBuilder postBody = new StringBuilder();
        postBody.append(LOGIN_GRANT_TYPE_PARAM)
                .append(PARAMETER_DELIMITER)
                .append(LOGIN_CLIENT_ID_PARAM)
                .append(Objects.requireNonNull(clientId))
                .append(PARAMETER_DELIMITER)
                .append(LOGIN_CLIENT_SECRET_PARAM)
                .append(Objects.requireNonNull(clientSecret))
                .append(PARAMETER_DELIMITER)
                .append("resource=")
                .append(params.getBaseManagementUrl())
                .append("/");

        String baseLoginUrl = params.getBaseLoginUrl();
        String versionQueryParam = API_VERSION_PARAM + params.getApiVersion();
        String url = String.format(OAUTH2_TOKEN_ENDPOINT, baseLoginUrl, directoryId, versionQueryParam);
        HttpRequest request = getHttpRequestBuilder(url, timeoutMs)
                .header(CONTENT_TYPE_HEADER, APPLICATION_FORM_URLENCODED_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(postBody.toString()))
                .build();

        return performRequest(AzureOAuthToken.class, request, retries);
    }

    public AzureSubscription getSubscription(AzureOAuthToken token, String subscriptionId, long timeoutMs, int retries)
            throws AzureHttpException {
        String versionQueryParam = API_VERSION_PARAM + params.getApiVersion();
        String url = String.format(SUBSCRIPTION_ENDPOINT, subscriptionId, versionQueryParam);
        return get(token, url, timeoutMs, retries, AzureSubscription.class);
    }

    public AzureResourceGroups getResourceGroups(
            AzureOAuthToken token, String subscriptionId, long timeoutMs, int retries) throws AzureHttpException {
        String versionQueryParam = API_VERSION_PARAM + params.getApiVersion();
        String url = String.format(RESOURCE_GROUPS_ENDPOINT, subscriptionId, versionQueryParam);
        return get(token, url, timeoutMs, retries, AzureResourceGroups.class);
    }

    public AzureResources getResources(
            AzureOAuthToken token, String subscriptionId, String resourceGroup, long timeoutMs, int retries)
            throws AzureHttpException {
        String versionQueryParam = API_VERSION_PARAM + params.getApiVersion();
        String url = String.format(RESOURCES_ENDPOINT, subscriptionId, resourceGroup, versionQueryParam);
        return get(token, url, timeoutMs, retries, AzureResources.class);
    }

    public AzureNetworkInterfaces getNetworkInterfaces(
            AzureOAuthToken token, String subscriptionId, String resourceGroup, long timeoutMs, int retries)
            throws AzureHttpException {
        String versionQueryParam = API_VERSION_PARAM + params.getApiVersion();
        String url = String.format(NETWORK_INTERFACES_ENDPOINT, subscriptionId, resourceGroup, versionQueryParam);
        return get(token, url, timeoutMs, retries, AzureNetworkInterfaces.class);
    }

    public AzurePublicIpAddresses getPublicIpAddresses(
            AzureOAuthToken token, String subscriptionId, String resourceGroup, long timeoutMs, int retries)
            throws AzureHttpException {
        String versionQueryParam = API_VERSION_PARAM + params.getApiVersion();
        String url = String.format(PUBLIC_IP_ADDRESSES_ENDPOINT, subscriptionId, resourceGroup, versionQueryParam);
        return get(token, url, timeoutMs, retries, AzurePublicIpAddresses.class);
    }

    public AzureInstanceView getInstanceView(
            AzureOAuthToken token,
            String subscriptionId,
            String resourceGroup,
            String resourceName,
            long timeoutMs,
            int retries)
            throws AzureHttpException {
        String versionQueryParam = API_VERSION_PARAM + params.getApiVersion();
        String url =
                String.format(INSTANCE_VIEW_ENDPOINT, subscriptionId, resourceGroup, resourceName, versionQueryParam);
        return get(token, url, timeoutMs, retries, AzureInstanceView.class);
    }

    public AzureMetrics getMetrics(
            AzureOAuthToken token,
            String subscriptionId,
            String resourceGroup,
            String resourceName,
            Map<String, String> params,
            long timeoutMs,
            int retries)
            throws AzureHttpException {
        String versionQueryParam = API_VERSION_PARAM + this.params.getMetricsApiVersion();
        String url = String.format(METRICS_ENDPOINT, subscriptionId, resourceGroup, resourceName, versionQueryParam);
        url = addUrlParams(url, params);
        return get(token, url, timeoutMs, retries, AzureMetrics.class);
    }

    /**
     * https://learn.microsoft.com/en-us/rest/api/monitor/metrics/list?tabs=HTTP#resulttype
     * @param token
     * @param subscriptionId
     * @param resourceGroup
     * @param resourceUri (simplified version of resourceUri e.g. publicIPAddresses/PUBLIC_IP_ID , networkInterfaces/NETWORK_INTERFACE_ID)
     * @param params (extra parameter e.g. , metricnames, interval)
     * @param timeoutMs
     * @param retries
     * @return AzureMetrics
     * @throws AzureHttpException
     */
    public AzureMetrics getNetworkInterfaceMetrics(
            AzureOAuthToken token,
            String subscriptionId,
            String resourceGroup,
            String resourceUri,
            Map<String, String> params,
            long timeoutMs,
            int retries)
            throws AzureHttpException {
        String versionQueryParam = API_VERSION_PARAM + this.params.getMetricsApiVersion();
        String url = String.format(
                NETWORK_INTERFACE_METRICS_ENDPOINT, subscriptionId, resourceGroup, resourceUri, versionQueryParam);
        url = addUrlParams(url, params);
        return get(token, url, timeoutMs, retries, AzureMetrics.class);
    }

    private <T> T get(AzureOAuthToken token, String endpoint, long timeoutMs, int retries, Class<T> clazz)
            throws AzureHttpException {
        String url = params.getBaseManagementUrl() + endpoint;
        HttpRequest request = buildGetHttpRequest(token, url, timeoutMs);

        return performRequest(clazz, request, retries);
    }

    private <T> T performRequest(Class<T> clazz, HttpRequest request, int retries) throws AzureHttpException {
        if (retries < 1) {
            throw new AzureHttpException("Number of retries must be a positive number");
        }

        // prevent sona null pointer error
        AzureHttpException lastException = new AzureHttpException("null error");
        long backoffTime = INITIAL_BACKOFF_TIME_MS;

        for (int retryCount = 1; retryCount <= retries; retryCount++) {
            try {
                return this.performSingleRequest(clazz, request);
            } catch (AzureHttpException ex) {
                if (ex.getHttpStatusCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    log.error("Request uri: {}, HTTP_UNAUTHORIZED: {}", request.uri(), ex.getMessage(), ex);
                    throw ex;
                }
                lastException = ex;
                log.warn(String.format(
                        "Failed to get for endpoint: %s, retry: %d/%d error: %s",
                        request.uri(), retryCount, retries, lastException));
                try {
                    Thread.sleep(backoffTime);
                    backoffTime *= EXPONENTIAL_BACKOFF_AMPLIFIER;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    String message = String.format(
                            "Failed to wait for exp backoff with time: %d, retry: %d/%d",
                            backoffTime, retryCount, retries);
                    throw new AzureHttpException(message, e);
                }
            }
        }
        throw lastException;
    }

    private <T> T performSingleRequest(Class<T> clazz, HttpRequest request) throws AzureHttpException {
        try {
            HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());

            String httpBody = httpResponse.body();

            log.info("Response statusCode: {}, body: {}", httpResponse.statusCode(), httpBody);

            var statusCode = httpResponse.statusCode();
            if (statusCode == HttpURLConnection.HTTP_OK) {
                return gson.fromJson(httpBody, clazz);
            } else {
                throw toHttpErrorException(httpBody, statusCode);
            }
        } catch (InterruptedException | JsonSyntaxException | IOException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            String message = String.format("Failed to get for endpoint: %s, error: %s", request.uri(), e.getMessage());
            throw new AzureHttpException(message, e);
        }
    }

    private AzureHttpException toHttpErrorException(String message, int httpStatusCode) {
        AzureHttpError error = gson.fromJson(message, AzureHttpError.class);
        return new AzureHttpException(error, httpStatusCode);
    }

    private HttpRequest buildGetHttpRequest(AzureOAuthToken token, String url, long timeoutMs)
            throws AzureHttpException {
        if (timeoutMs < MIN_TIMEOUT_MS) {
            throw new AzureHttpException("Timeout must be a positive number > " + MIN_TIMEOUT_MS);
        }
        return getHttpRequestBuilder(url, timeoutMs)
                .header(AUTH_HEADER, String.format("%s %s", token.getTokenType(), token.getAccessToken()))
                .GET()
                .build();
    }

    private HttpRequest.Builder getHttpRequestBuilder(String url, long timeoutMs) {
        return HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.of(timeoutMs, ChronoUnit.MILLIS));
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
