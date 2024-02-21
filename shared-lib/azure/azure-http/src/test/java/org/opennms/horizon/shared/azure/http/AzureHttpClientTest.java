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

import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.opennms.horizon.shared.azure.http.AzureHttpClient.INSTANCE_VIEW_ENDPOINT;
import static org.opennms.horizon.shared.azure.http.AzureHttpClient.METRICS_ENDPOINT;
import static org.opennms.horizon.shared.azure.http.AzureHttpClient.NETWORK_INTERFACES_ENDPOINT;
import static org.opennms.horizon.shared.azure.http.AzureHttpClient.NETWORK_INTERFACE_METRICS_ENDPOINT;
import static org.opennms.horizon.shared.azure.http.AzureHttpClient.OAUTH2_TOKEN_ENDPOINT;
import static org.opennms.horizon.shared.azure.http.AzureHttpClient.PUBLIC_IP_ADDRESSES_ENDPOINT;
import static org.opennms.horizon.shared.azure.http.AzureHttpClient.RESOURCES_ENDPOINT;
import static org.opennms.horizon.shared.azure.http.AzureHttpClient.RESOURCE_GROUPS_ENDPOINT;
import static org.opennms.horizon.shared.azure.http.AzureHttpClient.SUBSCRIPTION_ENDPOINT;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.opennms.horizon.shared.azure.http.dto.AzureHttpParams;
import org.opennms.horizon.shared.azure.http.dto.instanceview.AzureInstanceView;
import org.opennms.horizon.shared.azure.http.dto.instanceview.AzureStatus;
import org.opennms.horizon.shared.azure.http.dto.login.AzureOAuthToken;
import org.opennms.horizon.shared.azure.http.dto.metrics.AzureDatum;
import org.opennms.horizon.shared.azure.http.dto.metrics.AzureMetrics;
import org.opennms.horizon.shared.azure.http.dto.metrics.AzureName;
import org.opennms.horizon.shared.azure.http.dto.metrics.AzureTimeseries;
import org.opennms.horizon.shared.azure.http.dto.networkinterface.AzureNetworkInterface;
import org.opennms.horizon.shared.azure.http.dto.networkinterface.AzureNetworkInterfaces;
import org.opennms.horizon.shared.azure.http.dto.networkinterface.IpConfiguration;
import org.opennms.horizon.shared.azure.http.dto.networkinterface.IpConfigurationProps;
import org.opennms.horizon.shared.azure.http.dto.networkinterface.NetworkInterfaceProps;
import org.opennms.horizon.shared.azure.http.dto.networkinterface.PublicIPAddress;
import org.opennms.horizon.shared.azure.http.dto.networkinterface.VirtualMachine;
import org.opennms.horizon.shared.azure.http.dto.publicipaddresses.AzurePublicIPAddress;
import org.opennms.horizon.shared.azure.http.dto.publicipaddresses.AzurePublicIpAddresses;
import org.opennms.horizon.shared.azure.http.dto.resourcegroup.AzureResourceGroups;
import org.opennms.horizon.shared.azure.http.dto.resourcegroup.AzureValue;
import org.opennms.horizon.shared.azure.http.dto.resources.AzureResources;
import org.opennms.horizon.shared.azure.http.dto.subscription.AzureSubscription;

public class AzureHttpClientTest {
    private static final String TEST_DIRECTORY_ID = "test-directory-id";
    private static final String TEST_CLIENT_ID = "test-client-id";
    private static final String TEST_CLIENT_SECRET = "test-client-secret";
    private static final String TEST_SUBSCRIPTION = "test-subscription";
    private static final String TEST_RESOURCE_GROUP = "test-resource-group";
    private static final String TEST_RESOURCE_NAME = "test-resource-name";
    private static final int TEST_TIMEOUT = 1000;
    private static final int TEST_RETRIES = 2;
    private static final String TEST_AZURE_STATUS_CODE = "PowerState/running";
    private final ObjectMapper snakeCaseMapper;

    @Rule
    public WireMockRule wireMock = new WireMockRule(wireMockConfig().dynamicPort());

    private AzureHttpClient client;
    private AzureHttpParams params;

    public AzureHttpClientTest() {
        this.snakeCaseMapper = new ObjectMapper();
        this.snakeCaseMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    @Before
    public void before() {
        String testBaseUrl = "http://localhost:" + wireMock.port();

        this.params = new AzureHttpParams();
        this.params.setBaseLoginUrl(testBaseUrl);
        this.params.setBaseManagementUrl(testBaseUrl);
        this.params.setApiVersion("1");
        this.params.setMetricsApiVersion("2");

        this.client = new AzureHttpClient(this.params);
    }

    @Test
    public void testEnum() {
        assertEquals("publicIPAddresses", AzureHttpClient.ResourcesType.PUBLIC_IP_ADDRESSES.getMetricName());
        assertEquals(
                AzureHttpClient.ResourcesType.NETWORK_INTERFACES,
                AzureHttpClient.ResourcesType.fromMetricName("networkInterfaces"));
    }

    @Test
    public void testLogin() throws Exception {
        AzureOAuthToken oAuthToken = getAzureOAuthToken();

        String url = String.format(
                OAUTH2_TOKEN_ENDPOINT, "", TEST_DIRECTORY_ID, "?api-version=" + this.params.getApiVersion());

        wireMock.stubFor(post(url)
                .withHeader("Content-Type", new EqualToPattern("application/x-www-form-urlencoded"))
                .willReturn(ResponseDefinitionBuilder.responseDefinition()
                        .withStatus(HttpStatus.SC_OK)
                        .withBody(snakeCaseMapper.writeValueAsString(oAuthToken))));

        AzureOAuthToken token =
                this.client.login(TEST_DIRECTORY_ID, TEST_CLIENT_ID, TEST_CLIENT_SECRET, TEST_TIMEOUT, TEST_RETRIES);

        verify(exactly(1), postRequestedFor(urlEqualTo(url)));

        assertEquals(oAuthToken.getTokenType(), token.getTokenType());
        assertEquals(oAuthToken.getExpiresIn(), token.getExpiresIn());
        assertEquals(oAuthToken.getExtExpiresIn(), token.getExtExpiresIn());
        assertEquals(oAuthToken.getExpiresOn(), token.getExpiresOn());
        assertEquals(oAuthToken.getNotBefore(), token.getNotBefore());
        assertEquals(oAuthToken.getResource(), token.getResource());
        assertEquals(oAuthToken.getAccessToken(), token.getAccessToken());
    }

    @Test
    public void testLoginWithRetriesAndFails() {
        String url = String.format(
                OAUTH2_TOKEN_ENDPOINT, "", TEST_DIRECTORY_ID, "?api-version=" + this.params.getApiVersion());

        wireMock.stubFor(post(url)
                .withHeader("Content-Type", new EqualToPattern("application/x-www-form-urlencoded"))
                .willReturn(ResponseDefinitionBuilder.responseDefinition()
                        .withBody(
                                "{\"error\":\"invalid_client\",\"error_description\":\"AADSTS7000215: Invalid client secret provided. Ensure the secret being sent in the request is the client secret value, not the client secret ID, for a secret added to app 'da5b5bcb-589c-43d3-a7dd-af0900393c20'.\\r\\nTrace ID: d8e73b43-ae4d-40f9-9c4e-2404d5f30300\\r\\nCorrelation ID: fbf5bdb8-ac14-4f77-9941-5a5fb89112fc\\r\\nTimestamp: 2023-07-12 02:48:42Z\",\"error_codes\":[7000215],\"timestamp\":\"2023-07-12 02:48:42Z\",\"trace_id\":\"d8e73b43-ae4d-40f9-9c4e-2404d5f30300\",\"correlation_id\":\"fbf5bdb8-ac14-4f77-9941-5a5fb89112fc\",\"error_uri\":\"https://login.microsoftonline.com/error?code=7000215\"}")
                        .withStatus(HttpStatus.SC_UNAUTHORIZED)));

        AzureHttpException e = assertThrows(AzureHttpException.class, () -> {
            this.client.login(TEST_DIRECTORY_ID, TEST_CLIENT_ID, TEST_CLIENT_SECRET, TEST_TIMEOUT, TEST_RETRIES);
        });

        assertTrue(e.hasHttpError());
        assertEquals("invalid_client", e.getHttpError().getError());

        // should not retry more than once for auth error
        verify(exactly(1), postRequestedFor(urlEqualTo(url)));
    }

    @Test
    public void testLoginWithJsonError() {
        String url = String.format(
                OAUTH2_TOKEN_ENDPOINT, "", TEST_DIRECTORY_ID, "?api-version=" + this.params.getApiVersion());

        wireMock.stubFor(post(url)
                .withHeader("Content-Type", new EqualToPattern("application/x-www-form-urlencoded"))
                .willReturn(ResponseDefinitionBuilder.responseDefinition()
                        .withBody("error")
                        .withStatus(HttpStatus.SC_OK)));

        AzureHttpException e = assertThrows(AzureHttpException.class, () -> {
            this.client.login(TEST_DIRECTORY_ID, TEST_CLIENT_ID, TEST_CLIENT_SECRET, TEST_TIMEOUT, TEST_RETRIES);
        });

        assertFalse(e.hasHttpError());
        assertTrue(e.getMessage().startsWith("Failed to get for endpoint"));

        // should not retry more than once for auth error
        verify(exactly(TEST_RETRIES), postRequestedFor(urlEqualTo(url)));
    }

    @Test
    public void testGetSubscription() throws Exception {
        AzureOAuthToken token = getAzureOAuthToken();

        String url =
                String.format(SUBSCRIPTION_ENDPOINT, TEST_SUBSCRIPTION, "?api-version=" + this.params.getApiVersion());

        AzureSubscription azureSubscription = new AzureSubscription();
        azureSubscription.setSubscriptionId(TEST_SUBSCRIPTION);
        azureSubscription.setState("Enabled");

        wireMock.stubFor(get(url).withHeader("Authorization", new EqualToPattern("Bearer " + token.getAccessToken()))
                .willReturn(ResponseDefinitionBuilder.okForJson(azureSubscription)));

        AzureSubscription subscription =
                this.client.getSubscription(token, TEST_SUBSCRIPTION, TEST_TIMEOUT, TEST_RETRIES);

        verify(exactly(1), getRequestedFor(urlEqualTo(url)));

        assertEquals(azureSubscription.getSubscriptionId(), subscription.getSubscriptionId());
        assertEquals(azureSubscription.getState(), subscription.getState());
    }

    @Test
    public void testGetResourceGroups() throws Exception {
        AzureOAuthToken token = getAzureOAuthToken();

        String url = String.format(
                RESOURCE_GROUPS_ENDPOINT, TEST_SUBSCRIPTION, "?api-version=" + this.params.getApiVersion());

        AzureResourceGroups azureResourceGroups = new AzureResourceGroups();
        AzureValue azureValue = new AzureValue();
        azureValue.setName(TEST_RESOURCE_GROUP);
        azureResourceGroups.setValue(Collections.singletonList(azureValue));

        wireMock.stubFor(get(url).withHeader("Authorization", new EqualToPattern("Bearer " + token.getAccessToken()))
                .willReturn(ResponseDefinitionBuilder.okForJson(azureResourceGroups)));

        AzureResourceGroups resourceGroups =
                this.client.getResourceGroups(token, TEST_SUBSCRIPTION, TEST_TIMEOUT, TEST_RETRIES);

        verify(exactly(1), getRequestedFor(urlEqualTo(url)));

        assertEquals(1, resourceGroups.getValue().size());
        AzureValue value = resourceGroups.getValue().get(0);
        assertEquals(TEST_RESOURCE_GROUP, value.getName());
    }

    @Test
    public void testGetResources() throws Exception {
        AzureOAuthToken token = getAzureOAuthToken();

        String url = String.format(
                RESOURCES_ENDPOINT,
                TEST_SUBSCRIPTION,
                TEST_RESOURCE_GROUP,
                "?api-version=" + this.params.getApiVersion());

        AzureResources azureResources = new AzureResources();
        org.opennms.horizon.shared.azure.http.dto.resources.AzureValue azureValue =
                new org.opennms.horizon.shared.azure.http.dto.resources.AzureValue();
        azureValue.setName(TEST_RESOURCE_NAME);
        azureResources.setValue(Collections.singletonList(azureValue));

        wireMock.stubFor(get(url).withHeader("Authorization", new EqualToPattern("Bearer " + token.getAccessToken()))
                .willReturn(ResponseDefinitionBuilder.okForJson(azureResources)));

        AzureResources resources =
                this.client.getResources(token, TEST_SUBSCRIPTION, TEST_RESOURCE_GROUP, TEST_TIMEOUT, TEST_RETRIES);

        verify(exactly(1), getRequestedFor(urlEqualTo(url)));

        assertEquals(1, resources.getValue().size());
        assertEquals(TEST_RESOURCE_NAME, resources.getValue().get(0).getName());
    }

    @Test
    public void testGetNetworkInterfaces() throws Exception {
        AzureOAuthToken token = getAzureOAuthToken();

        String url = String.format(
                NETWORK_INTERFACES_ENDPOINT,
                TEST_SUBSCRIPTION,
                TEST_RESOURCE_GROUP,
                "?api-version=" + this.params.getApiVersion());

        AzureNetworkInterfaces azureNetworkInterfaces = getAzureNetworkInterfaces();

        wireMock.stubFor(get(url).withHeader("Authorization", new EqualToPattern("Bearer " + token.getAccessToken()))
                .willReturn(ResponseDefinitionBuilder.okForJson(azureNetworkInterfaces)));

        AzureNetworkInterfaces networkInterfaces = this.client.getNetworkInterfaces(
                token, TEST_SUBSCRIPTION, TEST_RESOURCE_GROUP, TEST_TIMEOUT, TEST_RETRIES);

        verify(exactly(1), getRequestedFor(urlEqualTo(url)));

        assertEquals(1, networkInterfaces.getValue().size());
    }

    @Test
    public void testGetPublicIpAddresses() throws Exception {
        AzureOAuthToken token = getAzureOAuthToken();

        String url = String.format(
                PUBLIC_IP_ADDRESSES_ENDPOINT,
                TEST_SUBSCRIPTION,
                TEST_RESOURCE_GROUP,
                "?api-version=" + this.params.getApiVersion());

        AzurePublicIpAddresses azurePublicIpAddresses = new AzurePublicIpAddresses();
        AzurePublicIPAddress azurePublicIPAddress = new AzurePublicIPAddress();
        azurePublicIpAddresses.setValue(Collections.singletonList(azurePublicIPAddress));

        wireMock.stubFor(get(url).withHeader("Authorization", new EqualToPattern("Bearer " + token.getAccessToken()))
                .willReturn(ResponseDefinitionBuilder.okForJson(azurePublicIpAddresses)));

        AzurePublicIpAddresses networkInterfaces = this.client.getPublicIpAddresses(
                token, TEST_SUBSCRIPTION, TEST_RESOURCE_GROUP, TEST_TIMEOUT, TEST_RETRIES);

        verify(exactly(1), getRequestedFor(urlEqualTo(url)));

        assertEquals(1, networkInterfaces.getValue().size());
    }

    @Test
    public void testGetInstanceView() throws Exception {
        AzureOAuthToken token = getAzureOAuthToken();

        String url = String.format(
                INSTANCE_VIEW_ENDPOINT,
                TEST_SUBSCRIPTION,
                TEST_RESOURCE_GROUP,
                TEST_RESOURCE_NAME,
                "?api-version=" + this.params.getApiVersion());

        AzureInstanceView azureInstanceView = new AzureInstanceView();
        AzureStatus azureStatus = new AzureStatus();
        azureStatus.setCode(TEST_AZURE_STATUS_CODE);
        azureInstanceView.setStatuses(Collections.singletonList(azureStatus));

        wireMock.stubFor(get(url).withHeader("Authorization", new EqualToPattern("Bearer " + token.getAccessToken()))
                .willReturn(ResponseDefinitionBuilder.okForJson(azureInstanceView)));

        AzureInstanceView instanceView = this.client.getInstanceView(
                token, TEST_SUBSCRIPTION, TEST_RESOURCE_GROUP, TEST_RESOURCE_NAME, TEST_TIMEOUT, TEST_RETRIES);

        verify(exactly(1), getRequestedFor(urlEqualTo(url)));

        assertEquals(1, instanceView.getStatuses().size());
        assertEquals(TEST_AZURE_STATUS_CODE, instanceView.getStatuses().get(0).getCode());
    }

    @Test
    public void testGetMetrics() throws Exception {
        AzureOAuthToken token = getAzureOAuthToken();

        String url = String.format(
                METRICS_ENDPOINT,
                TEST_SUBSCRIPTION,
                TEST_RESOURCE_GROUP,
                TEST_RESOURCE_NAME,
                "?api-version=" + this.params.getMetricsApiVersion()
                        + "&metricnames=Network+In+Total%2CNetwork+Out+Total"
                        + "&interval=PT1M");

        Instant now = Instant.now();

        wireMock.stubFor(get(url).withHeader("Authorization", new EqualToPattern("Bearer " + token.getAccessToken()))
                .willReturn(ResponseDefinitionBuilder.okForJson(generateAzureMetrics(now))));

        Map<String, String> params = new HashMap<>();
        params.put("metricnames", "Network In Total,Network Out Total");
        params.put("interval", "PT1M");

        AzureMetrics metrics = this.client.getMetrics(
                token, TEST_SUBSCRIPTION, TEST_RESOURCE_GROUP, TEST_RESOURCE_NAME, params, TEST_TIMEOUT, TEST_RETRIES);

        verify(exactly(1), getRequestedFor(urlEqualTo(url)));

        assertNotNull(metrics.getValue());
        assertEquals(1, metrics.getValue().size());

        org.opennms.horizon.shared.azure.http.dto.metrics.AzureValue value =
                metrics.getValue().get(0);
        assertNotNull(value.getName());
        assertEquals("name", value.getName().getValue());
        assertNotNull(value.getTimeseries());
        assertEquals(1, value.getTimeseries().size());

        AzureTimeseries timeseries = value.getTimeseries().get(0);
        assertNotNull(timeseries.getData());
        assertEquals(1, timeseries.getData().size());

        AzureDatum datum = timeseries.getData().get(0);
        assertEquals(1234d, datum.getTotal(), 0d);
        assertEquals(now.toString(), datum.getTimeStamp());
    }

    @Test
    public void testPopulateParamsNullFields() {
        AzureHttpParams result = client.populateParamDefaults(null);
        assertNotNull(result);
        assertNotNull(result.getBaseLoginUrl());
        assertNotNull(result.getBaseManagementUrl());
        assertNotNull(result.getApiVersion());
        assertNotNull(result.getMetricsApiVersion());
    }

    @Test
    public void testGetNetworkInterfaceMetrics() throws AzureHttpException {
        AzureOAuthToken token = getAzureOAuthToken();
        Instant now = Instant.now();

        String url = String.format(
                NETWORK_INTERFACE_METRICS_ENDPOINT,
                TEST_SUBSCRIPTION,
                TEST_RESOURCE_GROUP,
                "publicIPAddresses/PUBLIC_IP_ID",
                "?api-version=" + this.params.getMetricsApiVersion() + "&metricnames=ByteCount&interval=PT1M");

        wireMock.stubFor(get(url).withHeader("Authorization", new EqualToPattern("Bearer " + token.getAccessToken()))
                .willReturn(ResponseDefinitionBuilder.okForJson(generateAzureMetrics(now))));

        Map<String, String> params = new HashMap<>();
        params.put("metricnames", "ByteCount");
        params.put("interval", "PT1M");

        AzureMetrics metrics = this.client.getNetworkInterfaceMetrics(
                token,
                TEST_SUBSCRIPTION,
                TEST_RESOURCE_GROUP,
                "publicIPAddresses/PUBLIC_IP_ID",
                params,
                TEST_TIMEOUT,
                TEST_RETRIES);

        verify(exactly(1), getRequestedFor(urlEqualTo(url)));

        assertNotNull(metrics.getValue());
        assertEquals(1, metrics.getValue().size());

        org.opennms.horizon.shared.azure.http.dto.metrics.AzureValue value =
                metrics.getValue().get(0);
        assertNotNull(value.getName());
        assertEquals("name", value.getName().getValue());
        assertNotNull(value.getTimeseries());
        assertEquals(1, value.getTimeseries().size());

        AzureTimeseries timeseries = value.getTimeseries().get(0);
        assertNotNull(timeseries.getData());
        assertEquals(1, timeseries.getData().size());

        AzureDatum datum = timeseries.getData().get(0);
        assertEquals(1234d, datum.getTotal(), 0d);
        assertEquals(now.toString(), datum.getTimeStamp());
    }

    private AzureMetrics generateAzureMetrics(Instant now) {
        AzureMetrics azureMetrics = new AzureMetrics();

        org.opennms.horizon.shared.azure.http.dto.metrics.AzureValue azureValue =
                new org.opennms.horizon.shared.azure.http.dto.metrics.AzureValue();
        AzureName azureName = new AzureName();
        azureName.setValue("name");
        azureValue.setName(azureName);

        AzureTimeseries azureTimeseries = new AzureTimeseries();
        AzureDatum azureDatum = new AzureDatum();

        azureDatum.setTimeStamp(now.toString());
        azureDatum.setTotal(1234d);

        azureTimeseries.setData(Collections.singletonList(azureDatum));
        azureValue.setTimeseries(Collections.singletonList(azureTimeseries));
        azureMetrics.setValue(Collections.singletonList(azureValue));
        return azureMetrics;
    }

    private static AzureNetworkInterfaces getAzureNetworkInterfaces() {
        AzureNetworkInterfaces azureNetworkInterfaces = new AzureNetworkInterfaces();
        AzureNetworkInterface azureNetworkInterface = new AzureNetworkInterface();
        NetworkInterfaceProps props = new NetworkInterfaceProps();
        IpConfiguration ipConfiguration = new IpConfiguration();
        ipConfiguration.setId("ip-conf-id");
        IpConfigurationProps ipConfProps = new IpConfigurationProps();
        ipConfProps.setPrivateIPAddress("127.0.1.1");
        PublicIPAddress publicIPAddress = new PublicIPAddress();
        publicIPAddress.setId("pub-ip-id");
        ipConfProps.setPublicIPAddress(publicIPAddress);
        ipConfiguration.setProperties(ipConfProps);
        props.setIpConfigurations(Collections.singletonList(ipConfiguration));
        VirtualMachine virtualMachine = new VirtualMachine();
        virtualMachine.setId("vm-id");
        props.setVirtualMachine(virtualMachine);
        azureNetworkInterface.setProperties(props);
        azureNetworkInterfaces.setValue(Collections.singletonList(azureNetworkInterface));
        return azureNetworkInterfaces;
    }

    private AzureOAuthToken getAzureOAuthToken() {
        AzureOAuthToken token = new AzureOAuthToken();
        token.setTokenType("Bearer");
        token.setExpiresIn("3599");
        token.setExtExpiresIn("3599");
        token.setExpiresOn("1673347297");
        token.setNotBefore("1673347297");
        token.setResource(wireMock.baseUrl());
        token.setAccessToken("access-token");
        return token;
    }
}
