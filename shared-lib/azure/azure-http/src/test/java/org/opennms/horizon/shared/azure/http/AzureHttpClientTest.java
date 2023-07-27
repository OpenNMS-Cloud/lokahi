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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
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

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.opennms.horizon.shared.azure.http.AzureHttpClient.INSTANCE_VIEW_ENDPOINT;
import static org.opennms.horizon.shared.azure.http.AzureHttpClient.METRICS_ENDPOINT;
import static org.opennms.horizon.shared.azure.http.AzureHttpClient.NETWORK_INTERFACES_ENDPOINT;
import static org.opennms.horizon.shared.azure.http.AzureHttpClient.OAUTH2_TOKEN_ENDPOINT;
import static org.opennms.horizon.shared.azure.http.AzureHttpClient.PUBLIC_IP_ADDRESSES_ENDPOINT;
import static org.opennms.horizon.shared.azure.http.AzureHttpClient.RESOURCES_ENDPOINT;
import static org.opennms.horizon.shared.azure.http.AzureHttpClient.RESOURCE_GROUPS_ENDPOINT;
import static org.opennms.horizon.shared.azure.http.AzureHttpClient.SUBSCRIPTION_ENDPOINT;

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
    public void testLogin() throws Exception {
        AzureOAuthToken oAuthToken = getAzureOAuthToken();

        String url = String.format(OAUTH2_TOKEN_ENDPOINT, "", TEST_DIRECTORY_ID
            ,"?api-version=" + this.params.getApiVersion());

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
        String url = String.format(OAUTH2_TOKEN_ENDPOINT, "", TEST_DIRECTORY_ID
            , "?api-version=" + this.params.getApiVersion());

        wireMock.stubFor(post(url)
            .withHeader("Content-Type", new EqualToPattern("application/x-www-form-urlencoded"))
            .willReturn(ResponseDefinitionBuilder.responseDefinition()
                .withBody("{\"error\":{\"code\":\"AuthorizationFailed\",\"message\":\"authorization failed message\"}}")
                .withStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR)));

        AzureHttpException e = assertThrows(AzureHttpException.class, () -> {
            this.client.login(TEST_DIRECTORY_ID, TEST_CLIENT_ID, TEST_CLIENT_SECRET, TEST_TIMEOUT, TEST_RETRIES);
        });

        assertTrue(e.hasDescription());
        assertEquals("AuthorizationFailed: authorization failed message", e.getDescription().toString());

        verify(exactly(TEST_RETRIES), postRequestedFor(urlEqualTo(url)));
        assertEquals("Failed to get for endpoint: %s/%s/oauth2/token%s, status: 500, body: {\"error\":{\"code\":\"AuthorizationFailed\",\"message\":\"authorization failed message\"}}, retry: 2/2", e.getMessage());
    }

    @Test
    public void testGetSubscription() throws Exception {
        AzureOAuthToken token = getAzureOAuthToken();

        String url = String.format(SUBSCRIPTION_ENDPOINT, TEST_SUBSCRIPTION
            , "?api-version=" + this.params.getApiVersion());

        AzureSubscription azureSubscription = new AzureSubscription();
        azureSubscription.setSubscriptionId(TEST_SUBSCRIPTION);
        azureSubscription.setState("Enabled");

        wireMock.stubFor(get(url)
            .withHeader("Authorization", new EqualToPattern("Bearer " + token.getAccessToken()))
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

        String url = String.format(RESOURCE_GROUPS_ENDPOINT, TEST_SUBSCRIPTION
            , "?api-version=" + this.params.getApiVersion());

        AzureResourceGroups azureResourceGroups = new AzureResourceGroups();
        AzureValue azureValue = new AzureValue();
        azureValue.setName(TEST_RESOURCE_GROUP);
        azureResourceGroups.setValue(Collections.singletonList(azureValue));

        wireMock.stubFor(get(url)
            .withHeader("Authorization", new EqualToPattern("Bearer " + token.getAccessToken()))
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

        String url = String.format(RESOURCES_ENDPOINT, TEST_SUBSCRIPTION, TEST_RESOURCE_GROUP
            , "?api-version=" + this.params.getApiVersion());

        AzureResources azureResources = new AzureResources();
        org.opennms.horizon.shared.azure.http.dto.resources.AzureValue azureValue
            = new org.opennms.horizon.shared.azure.http.dto.resources.AzureValue();
        azureValue.setName(TEST_RESOURCE_NAME);
        azureResources.setValue(Collections.singletonList(azureValue));

        wireMock.stubFor(get(url)
            .withHeader("Authorization", new EqualToPattern("Bearer " + token.getAccessToken()))
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

        String url = String.format(NETWORK_INTERFACES_ENDPOINT, TEST_SUBSCRIPTION, TEST_RESOURCE_GROUP
            , "?api-version=" + this.params.getApiVersion());

        AzureNetworkInterfaces azureNetworkInterfaces = getAzureNetworkInterfaces();

        wireMock.stubFor(get(url)
            .withHeader("Authorization", new EqualToPattern("Bearer " + token.getAccessToken()))
            .willReturn(ResponseDefinitionBuilder.okForJson(azureNetworkInterfaces)));

        AzureNetworkInterfaces networkInterfaces =
            this.client.getNetworkInterfaces(token, TEST_SUBSCRIPTION, TEST_RESOURCE_GROUP, TEST_TIMEOUT, TEST_RETRIES);

        verify(exactly(1), getRequestedFor(urlEqualTo(url)));

        assertEquals(1, networkInterfaces.getValue().size());
    }

    @Test
    public void testGetPublicIpAddresses() throws Exception {
        AzureOAuthToken token = getAzureOAuthToken();

        String url = String.format(PUBLIC_IP_ADDRESSES_ENDPOINT, TEST_SUBSCRIPTION, TEST_RESOURCE_GROUP
            , "?api-version=" + this.params.getApiVersion());

        AzurePublicIpAddresses azurePublicIpAddresses = new AzurePublicIpAddresses();
        AzurePublicIPAddress azurePublicIPAddress = new AzurePublicIPAddress();
        azurePublicIpAddresses.setValue(Collections.singletonList(azurePublicIPAddress));

        wireMock.stubFor(get(url)
            .withHeader("Authorization", new EqualToPattern("Bearer " + token.getAccessToken()))
            .willReturn(ResponseDefinitionBuilder.okForJson(azurePublicIpAddresses)));

        AzurePublicIpAddresses networkInterfaces =
            this.client.getPublicIpAddresses(token, TEST_SUBSCRIPTION, TEST_RESOURCE_GROUP, TEST_TIMEOUT, TEST_RETRIES);

        verify(exactly(1), getRequestedFor(urlEqualTo(url)));

        assertEquals(1, networkInterfaces.getValue().size());
    }

    @Test
    public void testGetInstanceView() throws Exception {
        AzureOAuthToken token = getAzureOAuthToken();

        String url = String.format(INSTANCE_VIEW_ENDPOINT, TEST_SUBSCRIPTION, TEST_RESOURCE_GROUP, TEST_RESOURCE_NAME,
            "?api-version=" + this.params.getApiVersion());

        AzureInstanceView azureInstanceView = new AzureInstanceView();
        AzureStatus azureStatus = new AzureStatus();
        azureStatus.setCode(TEST_AZURE_STATUS_CODE);
        azureInstanceView.setStatuses(Collections.singletonList(azureStatus));

        wireMock.stubFor(get(url)
            .withHeader("Authorization", new EqualToPattern("Bearer " + token.getAccessToken()))
            .willReturn(ResponseDefinitionBuilder.okForJson(azureInstanceView)));

        AzureInstanceView instanceView =
            this.client.getInstanceView(token, TEST_SUBSCRIPTION, TEST_RESOURCE_GROUP, TEST_RESOURCE_NAME, TEST_TIMEOUT, TEST_RETRIES);

        verify(exactly(1), getRequestedFor(urlEqualTo(url)));

        assertEquals(1, instanceView.getStatuses().size());
        assertEquals(TEST_AZURE_STATUS_CODE, instanceView.getStatuses().get(0).getCode());
    }

    @Test
    public void testGetMetrics() throws Exception {
        AzureOAuthToken token = getAzureOAuthToken();

        String url = String.format(METRICS_ENDPOINT, TEST_SUBSCRIPTION, TEST_RESOURCE_GROUP, TEST_RESOURCE_NAME,
             "?api-version=" + this.params.getMetricsApiVersion()
            + "&metricnames=Network+In+Total%2CNetwork+Out+Total"
            + "&interval=PT1M");

        AzureMetrics azureMetrics = new AzureMetrics();

        org.opennms.horizon.shared.azure.http.dto.metrics.AzureValue azureValue
            = new org.opennms.horizon.shared.azure.http.dto.metrics.AzureValue();
        AzureName azureName = new AzureName();
        azureName.setValue("name");
        azureValue.setName(azureName);

        AzureTimeseries azureTimeseries = new AzureTimeseries();
        AzureDatum azureDatum = new AzureDatum();
        Instant now = Instant.now();
        azureDatum.setTimeStamp(now.toString());
        azureDatum.setTotal(1234d);

        azureTimeseries.setData(Collections.singletonList(azureDatum));
        azureValue.setTimeseries(Collections.singletonList(azureTimeseries));
        azureMetrics.setValue(Collections.singletonList(azureValue));

        wireMock.stubFor(get(url)
            .withHeader("Authorization", new EqualToPattern("Bearer " + token.getAccessToken()))
            .willReturn(ResponseDefinitionBuilder.okForJson(azureMetrics)));

        Map<String, String> params = new HashMap<>();
        params.put("metricnames", "Network In Total,Network Out Total");
        params.put("interval", "PT1M");

        AzureMetrics metrics =
            this.client.getMetrics(token, TEST_SUBSCRIPTION, TEST_RESOURCE_GROUP, TEST_RESOURCE_NAME, params, TEST_TIMEOUT, TEST_RETRIES);

        verify(exactly(1), getRequestedFor(urlEqualTo(url)));

        assertNotNull(metrics.getValue());
        assertEquals(1, metrics.getValue().size());

        org.opennms.horizon.shared.azure.http.dto.metrics.AzureValue value = metrics.getValue().get(0);
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
