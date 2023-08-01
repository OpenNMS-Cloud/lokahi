/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
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

package org.opennms.horizon.minion.azure;

import com.google.protobuf.Any;
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
import org.opennms.horizon.shared.azure.http.dto.login.AzureOAuthToken;
import org.opennms.horizon.shared.azure.http.dto.metrics.AzureDatum;
import org.opennms.horizon.shared.azure.http.dto.metrics.AzureMetrics;
import org.opennms.horizon.shared.azure.http.dto.metrics.AzureName;
import org.opennms.horizon.shared.azure.http.dto.metrics.AzureTimeseries;
import org.opennms.horizon.shared.azure.http.dto.metrics.AzureValue;
import org.opennms.taskset.contract.MonitorType;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AzureCollectorTest {
    final private AzureHttpClient client = mock(AzureHttpClient.class);

    final String clientId = "clientId";
    final String clientSecret = "clientSecret";
    final String subscriptionId = "subscriptionId";
    final String directoryId = "directoryId";

    final String resourceGroup = "resourceGroup";

    final String resourceName = "resourceName";

    final long timeout = 1000L;
    final int rety = 2;

    @Before
    public void setup() throws AzureHttpException {
        AzureOAuthToken token = new AzureOAuthToken();
        token.setAccessToken("token");
        when(client.login(directoryId, clientId, clientSecret, timeout, rety)).thenReturn(token);

        AzureInstanceView view = new AzureInstanceView();
        AzureStatus status = new AzureStatus();
        status.setCode("PowerState/running");
        view.setStatuses(Collections.singletonList(status));
        when(client.getInstanceView(token, subscriptionId, resourceGroup, resourceName, timeout, rety)).thenReturn(view);

        when(client.getMetrics(eq(token), eq(subscriptionId), eq(resourceGroup), eq(resourceName), any(),
            eq(timeout), eq(rety))).thenReturn(generateMetrics(Arrays.asList("Network In Total", "Network Out Total")));


        when(client.getNetworkInterfaceMetrics(eq(token), eq(subscriptionId), eq(resourceGroup),
            eq(AzureHttpClient.ResourcesType.NETWORK_INTERFACES.getMetricName() + "/" + "interface"), any(),
            eq(timeout), eq(rety))).thenReturn(generateMetrics(Arrays.asList("BytesReceivedRate", "BytesSentRate")));

        when(client.getNetworkInterfaceMetrics(eq(token), eq(subscriptionId), eq(resourceGroup),
            eq(AzureHttpClient.ResourcesType.PUBLIC_IP_ADDRESSES.getMetricName() + "/" + "publicIp"), any(),
            eq(timeout), eq(rety))).thenReturn(generateMetrics(Arrays.asList("ByteCount")));
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
            .addCollectorResources(AzureCollectorResourcesRequest.newBuilder().setType(AzureHttpClient.ResourcesType.NETWORK_INTERFACES.getMetricName()).setResource("interface").build())
            .addCollectorResources(AzureCollectorResourcesRequest.newBuilder().setType(AzureHttpClient.ResourcesType.PUBLIC_IP_ADDRESSES.getMetricName()).setResource("publicIp").build())
            .build();
        var config = Any.pack(collectorRequest);
        CollectionRequest request = CollectorRequestImpl.builder()
            .build();

        AzureCollector collector = new AzureCollector(client);

        var future = collector.collect(request, config);
        var response = future.join();

        AzureResponseMetric results = (AzureResponseMetric) response.getResults();
        Assert.assertTrue(response.getStatus());
        Assert.assertEquals("azure-node-0", response.getIpAddress());
        Assert.assertEquals(MonitorType.AZURE, response.getMonitorType());
        Assert.assertEquals(5, results.getResultsCount());
        Assert.assertEquals(2, results.getResultsList().stream().filter(r -> r.getResourceName().equals("resourceName")).count());
        Assert.assertEquals(2, results.getResultsList().stream().filter(r -> r.getResourceName().equals("interface")).count());
        Assert.assertEquals(1, results.getResultsList().stream().filter(r -> r.getResourceName().equals("publicIp")).count());
    }
}
