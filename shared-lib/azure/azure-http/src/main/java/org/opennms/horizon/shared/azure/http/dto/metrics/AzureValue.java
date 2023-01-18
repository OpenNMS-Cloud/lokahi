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

package org.opennms.horizon.shared.azure.http.dto.metrics;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

            //sanity check - may not actually need to sort here
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
