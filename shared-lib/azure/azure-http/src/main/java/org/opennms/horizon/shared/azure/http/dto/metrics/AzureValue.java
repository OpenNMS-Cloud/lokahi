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

import com.google.gson.annotations.SerializedName;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
public class AzureValue {
    @SerializedName("name")
    private AzureName name;

    @SerializedName("timeseries")
    private List<AzureTimeseries> timeseries = new ArrayList<>();

    public void collect(Map<String, Double> collectedData) {
        String metricName = name.getValue();

        if (timeseries.isEmpty()) {
            collectedData.put(metricName, 0d);
        } else {

            AzureTimeseries firstTimeseries = timeseries.get(0);
            List<AzureDatum> data = firstTimeseries.getData();

            // sanity check - may not actually need to sort here
            data.sort((o1, o2) -> {
                Instant t1 = Instant.parse(o1.getTimeStamp());
                Instant t2 = Instant.parse(o2.getTimeStamp());
                return t1.compareTo(t2);
            });

            // for now getting last value as it is most recent
            AzureDatum datum = data.get(data.size() - 1);

            Double value = datum.getValue();
            if (value == null) {
                value = 0d;
            }
            collectedData.put(metricName, value);
        }
    }
}
