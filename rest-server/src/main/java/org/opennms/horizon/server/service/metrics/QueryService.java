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

package org.opennms.horizon.server.service.metrics;

import org.opennms.horizon.server.model.TimeRangeUnit;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class QueryService {

    public String getQueryString(String metricName, Map<String, String> labels) {
        Map<String, String> queryParams = new HashMap<>(labels);
        queryParams.put(MetricLabelUtils.METRIC_NAME_KEY, metricName);
        return getQueryString(queryParams);
    }

    public String getQueryString(String metricName, Map<String, String> labels,
                                 Integer timeRange, TimeRangeUnit timeRangeUnit) {
        String queryString = getQueryString(metricName, labels);
        return addTimeRange(timeRange, timeRangeUnit, queryString);
    }

    public String getQueryString(Map<String, String> queryParams) {
        StringBuilder sb = new StringBuilder("query={");

        int index = 0;
        for (Map.Entry<String, String> param : queryParams.entrySet()) {
            // tilde will treat the string as a regex
            sb.append(String.format("%s=~\"%s\"", param.getKey(), param.getValue()));
            if (index != queryParams.size() - 1) {
                sb.append(",");
            }
            index++;
        }

        sb.append("}");
        return sb.toString();
    }

    private String addTimeRange(Integer timeRange, TimeRangeUnit timeRangeUnit, String queryString) {
        if (timeRange != null && timeRangeUnit != null) {
            return queryString + "[" + timeRange + timeRangeUnit.value + "]";
        }
        return queryString;
    }
}
