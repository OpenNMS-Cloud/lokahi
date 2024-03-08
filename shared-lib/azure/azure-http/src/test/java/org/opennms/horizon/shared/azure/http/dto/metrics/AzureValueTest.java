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
package org.opennms.horizon.shared.azure.http.dto.metrics;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

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
