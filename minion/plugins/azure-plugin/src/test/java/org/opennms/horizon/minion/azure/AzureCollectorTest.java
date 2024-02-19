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
package org.opennms.horizon.minion.azure;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.protobuf.Any;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opennms.azure.contract.AzureCollectorRequest;
import org.opennms.azure.contract.AzureCollectorResourcesRequest;
import org.opennms.horizon.azure.api.AzureResponseMetric;
import org.opennms.horizon.minion.plugin.api.CollectionRequest;
import org.opennms.horizon.minion.plugin.api.CollectorRequestImpl;
import org.opennms.horizon.shared.azure.http.AzureHttpClient;
import org.opennms.horizon.shared.azure.http.AzureHttpException;
import org.opennms.horizon.shared.azure.http.dto.instanceview.AzureInstanceView;
import org.opennms.horizon.shared.azure.http.dto.instanceview.AzureStatus;
import org.opennms.horizon.shared.azure.http.dto.instanceview.VmAgent;
import org.opennms.horizon.shared.azure.http.dto.login.AzureOAuthToken;
import org.opennms.horizon.shared.azure.http.dto.metrics.AzureDatum;
import org.opennms.horizon.shared.azure.http.dto.metrics.AzureMetrics;
import org.opennms.horizon.shared.azure.http.dto.metrics.AzureName;
import org.opennms.horizon.shared.azure.http.dto.metrics.AzureTimeseries;
import org.opennms.horizon.shared.azure.http.dto.metrics.AzureValue;
import org.opennms.taskset.contract.MonitorType;

public class AzureCollectorTest {
    private final AzureHttpClient client = mock(AzureHttpClient.class);

    final String clientId = "clientId";

    final String failClientId = "failClientId";
    final String clientSecret = "clientSecret";
    final String subscriptionId = "subscriptionId";
    final String directoryId = "directoryId";

    final String resourceGroup = "resourceGroup";

    final String resourceName = "resourceName";

    final String resourceNameNotReady = "resourceNameNotReady";

    final long timeout = 1000L;
    final int rety = 2;

    @Before
    public void setup() throws AzureHttpException {
        AzureOAuthToken token = new AzureOAuthToken();
        token.setAccessToken("token");
        when(client.login(directoryId, clientId, clientSecret, timeout, rety)).thenReturn(token);
        when(client.login(directoryId, failClientId, clientSecret, timeout, rety))
                .thenThrow(new AzureHttpException("Fail login"));

        AzureInstanceView view = new AzureInstanceView();
        AzureStatus status = new AzureStatus();
        status.setCode("PowerState/running");
        view.setStatuses(Collections.singletonList(status));

        List<AzureStatus> readyStatuses = new ArrayList<>();
        AzureStatus readyStatus = new AzureStatus();
        readyStatuses.add(readyStatus);
        readyStatus.setCode("ProvisioningState/succeeded");
        VmAgent vmAgent = new VmAgent();
        vmAgent.setStatuses(readyStatuses);
        view.setVmAgent(vmAgent);

        when(client.getInstanceView(token, subscriptionId, resourceGroup, resourceName, timeout, rety))
                .thenReturn(view);
        // empty instance view to simulate not ready
        when(client.getInstanceView(token, subscriptionId, resourceGroup, resourceNameNotReady, timeout, rety))
                .thenReturn(new AzureInstanceView());

        when(client.getMetrics(
                        eq(token),
                        eq(subscriptionId),
                        eq(resourceGroup),
                        eq(resourceName),
                        any(),
                        eq(timeout),
                        eq(rety)))
                .thenReturn(generateMetrics(Arrays.asList("Network In Total", "Network Out Total")));

        when(client.getNetworkInterfaceMetrics(
                        eq(token),
                        eq(subscriptionId),
                        eq(resourceGroup),
                        eq(AzureHttpClient.ResourcesType.NETWORK_INTERFACES.getMetricName() + "/" + "interface"),
                        any(),
                        eq(timeout),
                        eq(rety)))
                .thenReturn(generateMetrics(Arrays.asList("BytesReceivedRate", "BytesSentRate")));

        when(client.getNetworkInterfaceMetrics(
                        eq(token),
                        eq(subscriptionId),
                        eq(resourceGroup),
                        eq(AzureHttpClient.ResourcesType.PUBLIC_IP_ADDRESSES.getMetricName() + "/publicIp"),
                        any(),
                        eq(timeout),
                        eq(rety)))
                .thenReturn(generateMetrics(Arrays.asList("ByteCount")));

        when(client.getNetworkInterfaceMetrics(
                        eq(token),
                        eq(subscriptionId),
                        eq(resourceGroup),
                        eq(AzureHttpClient.ResourcesType.PUBLIC_IP_ADDRESSES.getMetricName() + "/publicIpException"),
                        any(),
                        eq(timeout),
                        eq(rety)))
                .thenThrow(new AzureHttpException("testing"));
    }

    private AzureMetrics generateMetrics(List<String> metricsNames) {
        AzureMetrics metrics = new AzureMetrics();
        List<AzureValue> values = new ArrayList<>();
        metricsNames.forEach(metricsName -> {
            AzureValue value = new AzureValue();
            AzureName name = new AzureName();
            AzureTimeseries timeseries = new AzureTimeseries();
            AzureDatum data = new AzureDatum();
            data.setAverage(1.1d);
            data.setTotal(2.0d);
            timeseries.setData(Collections.singletonList(data));
            name.setValue(metricsName);
            value.setName(name);
            value.setTimeseries(Collections.singletonList(timeseries));
            values.add(value);
        });
        metrics.setValue(values);
        return metrics;
    }

    @Test
    public void testCollect() {
        var collectorRequest = AzureCollectorRequest.newBuilder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setSubscriptionId(subscriptionId)
                .setDirectoryId(directoryId)
                .setResourceGroup(resourceGroup)
                .setResource(resourceName)
                .setTimeoutMs(timeout)
                .setRetries(rety)
                .addCollectorResources(AzureCollectorResourcesRequest.newBuilder()
                        .setType(AzureHttpClient.ResourcesType.NETWORK_INTERFACES.getMetricName())
                        .setResource("interface")
                        .build())
                .addCollectorResources(AzureCollectorResourcesRequest.newBuilder()
                        .setType(AzureHttpClient.ResourcesType.PUBLIC_IP_ADDRESSES.getMetricName())
                        .setResource("publicIp")
                        .build())
                .addCollectorResources(AzureCollectorResourcesRequest.newBuilder()
                        .setType(AzureHttpClient.ResourcesType.PUBLIC_IP_ADDRESSES.getMetricName())
                        .setResource("publicIpException")
                        .build())
                .build();
        var config = Any.pack(collectorRequest);
        CollectionRequest request = CollectorRequestImpl.builder().build();

        AzureCollector collector = new AzureCollector(client);

        var future = collector.collect(request, config);
        var response = future.join();

        AzureResponseMetric results = (AzureResponseMetric) response.getResults();
        Assert.assertTrue(response.getStatus());
        Assert.assertEquals("azure-node-0", response.getIpAddress());
        Assert.assertEquals(MonitorType.AZURE, response.getMonitorType());
        Assert.assertEquals(5, results.getResultsCount());
        Assert.assertEquals(
                2,
                results.getResultsList().stream()
                        .filter(r -> r.getResourceName().equals("resourceName"))
                        .count());
        Assert.assertEquals(
                2,
                results.getResultsList().stream()
                        .filter(r -> r.getResourceName().equals("interface"))
                        .count());
        Assert.assertEquals(
                1,
                results.getResultsList().stream()
                        .filter(r -> r.getResourceName().equals("publicIp"))
                        .count());
    }

    @Test
    public void testNotReadyCollect() {
        var collectorRequest = AzureCollectorRequest.newBuilder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setSubscriptionId(subscriptionId)
                .setDirectoryId(directoryId)
                .setResourceGroup(resourceGroup)
                .setResource(resourceNameNotReady)
                .setTimeoutMs(timeout)
                .setRetries(rety)
                .build();
        var config = Any.pack(collectorRequest);
        CollectionRequest request = CollectorRequestImpl.builder().build();

        AzureCollector collector = new AzureCollector(client);

        var future = collector.collect(request, config);
        var response = future.join();

        Assert.assertFalse(response.getStatus());
        Assert.assertEquals("azure-node-0", response.getIpAddress());
        Assert.assertEquals(MonitorType.AZURE, response.getMonitorType());
        Assert.assertNull(response.getResults());
    }

    @Test
    public void testFailLogin() {
        var collectorRequest = AzureCollectorRequest.newBuilder()
                .setClientId(failClientId)
                .setClientSecret(clientSecret)
                .setSubscriptionId(subscriptionId)
                .setDirectoryId(directoryId)
                .setTimeoutMs(timeout)
                .setRetries(rety)
                .build();
        var config = Any.pack(collectorRequest);
        CollectionRequest request = CollectorRequestImpl.builder().build();

        AzureCollector collector = new AzureCollector(client);

        var future = collector.collect(request, config);
        var response = future.join();

        Assert.assertFalse(response.getStatus());
        Assert.assertEquals("azure-node-0", response.getIpAddress());
        Assert.assertEquals(MonitorType.AZURE, response.getMonitorType());
        Assert.assertNull(response.getResults());
    }
}
