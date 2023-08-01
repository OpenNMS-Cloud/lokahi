package org.opennms.horizon.shared.azure.http.dto.metrics;

import org.junit.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class AzureMetricsTest {

    @Test
    public void testCollect() {
        AzureMetrics metrics = getMetrics();

        Map<String, Double> collectedData = metrics.collect();

        assertEquals(1, collectedData.size());
        Map.Entry<String, Double> next = collectedData.entrySet().iterator().next();
        assertEquals("name", next.getKey());
        assertEquals(1234d, next.getValue(), 0d);
    }

    private AzureMetrics getMetrics() {
        AzureMetrics azureMetrics = new AzureMetrics();

        AzureValue azureValue = new AzureValue();
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
        return azureMetrics;
    }
}
