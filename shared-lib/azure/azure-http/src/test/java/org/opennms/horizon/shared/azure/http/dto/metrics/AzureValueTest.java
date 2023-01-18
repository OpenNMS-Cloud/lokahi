package org.opennms.horizon.shared.azure.http.dto.metrics;

import org.junit.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class AzureValueTest {

    @Test
    public void testCollect() {
        AzureValue azureValue = getAzureValue();
        Map<String, Double> collectedData = new HashMap<>();
        azureValue.collect(collectedData);

        assertEquals(1, collectedData.size());
        Map.Entry<String, Double> next = collectedData.entrySet().iterator().next();
        assertEquals("name", next.getKey());
        assertEquals(1234d, next.getValue(), 0d);
    }

    private AzureValue getAzureValue() {
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

        return azureValue;
    }

}
