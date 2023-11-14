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

import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.server.model.TimeRangeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.opennms.horizon.server.service.metrics.Constants.AVG_RESPONSE_TIME;
import static org.opennms.horizon.server.service.metrics.Constants.AZURE_SCAN_TYPE;
import static org.opennms.horizon.server.service.metrics.Constants.BW_IN_PERCENTAGE;
import static org.opennms.horizon.server.service.metrics.Constants.BW_OUT_PERCENTAGE;
import static org.opennms.horizon.server.service.metrics.Constants.NETWORK_ERRORS_IN;
import static org.opennms.horizon.server.service.metrics.Constants.NETWORK_ERRORS_OUT;
import static org.opennms.horizon.server.service.metrics.Constants.NETWORK_IN_BITS;
import static org.opennms.horizon.server.service.metrics.Constants.NETWORK_OUT_BITS;
import static org.opennms.horizon.server.service.metrics.Constants.QUERY_FOR_AZURE_TOTAL_NETWORK_IN_BITS;
import static org.opennms.horizon.server.service.metrics.Constants.QUERY_FOR_AZURE_TOTAL_NETWORK_OUT_BITS;
import static org.opennms.horizon.server.service.metrics.Constants.QUERY_FOR_BW_IN_UTIL_PERCENTAGE;
import static org.opennms.horizon.server.service.metrics.Constants.QUERY_FOR_BW_OUT_UTIL_PERCENTAGE;
import static org.opennms.horizon.server.service.metrics.Constants.QUERY_FOR_NETWORK_ERRORS_IN;
import static org.opennms.horizon.server.service.metrics.Constants.QUERY_FOR_NETWORK_ERRORS_OUT;
import static org.opennms.horizon.server.service.metrics.Constants.QUERY_FOR_TOTAL_NETWORK_BITS_IN;
import static org.opennms.horizon.server.service.metrics.Constants.QUERY_FOR_TOTAL_NETWORK_BITS_OUT;
import static org.opennms.horizon.server.service.metrics.Constants.QUERY_FOR_TOTAL_NETWORK_IN_BITS;
import static org.opennms.horizon.server.service.metrics.Constants.QUERY_FOR_TOTAL_NETWORK_OUT_BITS;
import static org.opennms.horizon.server.service.metrics.Constants.QUERY_PREFIX;
import static org.opennms.horizon.server.service.metrics.Constants.REACHABILITY_PERCENTAGE;
import static org.opennms.horizon.server.service.metrics.Constants.TOTAL_NETWORK_BITS_IN;
import static org.opennms.horizon.server.service.metrics.Constants.TOTAL_NETWORK_BITS_OUT;

@Component
@RequiredArgsConstructor
public class QueryService {
    private static final Logger LOG = LoggerFactory.getLogger(QueryService.class);
    private static final String OPERATION_NOT_SUPPORTED_FOR_AZURE_NODE = "Operation not supported for Azure node: ";

    public String getQueryString(String metricName, Map<String, String> labels) {
        Map<String, String> queryParams = new HashMap<>(labels);
        queryParams.put(MetricLabelUtils.METRIC_NAME_KEY, metricName);
        return getQueryString(queryParams);
    }

    public boolean isRangeQuery(String metricName) {
        return TOTAL_NETWORK_BITS_IN.equals(metricName) || TOTAL_NETWORK_BITS_OUT.equals(metricName)
            || NETWORK_IN_BITS.equals(metricName) || NETWORK_OUT_BITS.equals(metricName)
            || BW_IN_PERCENTAGE.equals(metricName) || BW_OUT_PERCENTAGE.equals(metricName)
            || NETWORK_ERRORS_IN.equals(metricName) || NETWORK_ERRORS_OUT.equals(metricName);
    }
    
    public String getQueryString(Optional<NodeDTO> node, String metricName, Map<String, String> labels,
                                 Integer timeRange, TimeRangeUnit timeRangeUnit) {
        if (isRangeQuery(metricName)) {
            long end = System.currentTimeMillis() / 1000L;
            long start = end - getDuration(timeRange, timeRangeUnit).orElse(Duration.ofHours(24)).getSeconds();
            String rangeQuerySuffix = "&start=" + start + "&end=" + end +
                "&step=2m";
            switch (metricName) {
                case TOTAL_NETWORK_BITS_IN:
                    return QUERY_PREFIX + encode(QUERY_FOR_TOTAL_NETWORK_BITS_IN) + rangeQuerySuffix;
                case TOTAL_NETWORK_BITS_OUT:
                    return QUERY_PREFIX + encode(QUERY_FOR_TOTAL_NETWORK_BITS_OUT) + rangeQuerySuffix;
                case NETWORK_IN_BITS:
                    if (isAzureNode(node)) {
                        var query = String.format(QUERY_FOR_AZURE_TOTAL_NETWORK_IN_BITS, getLabelsQueryString(labels));
                        return QUERY_PREFIX + query + rangeQuerySuffix;
                    } else {
                        var query = String.format(QUERY_FOR_TOTAL_NETWORK_IN_BITS, getLabelsQueryString(labels));
                        return QUERY_PREFIX + query + rangeQuerySuffix;
                    }
                case NETWORK_OUT_BITS:
                    if (isAzureNode(node)) {
                        var query = String.format(QUERY_FOR_AZURE_TOTAL_NETWORK_OUT_BITS, getLabelsQueryString(labels));
                        return QUERY_PREFIX + query + rangeQuerySuffix;
                    } else {
                        var query = String.format(QUERY_FOR_TOTAL_NETWORK_OUT_BITS, getLabelsQueryString(labels));
                        return QUERY_PREFIX + query + rangeQuerySuffix;
                    }
                case BW_IN_PERCENTAGE:
                    if (isAzureNode(node)) {
                        throw new RuntimeException(OPERATION_NOT_SUPPORTED_FOR_AZURE_NODE + BW_IN_PERCENTAGE);
                    } else {
                        var query = String.format(QUERY_FOR_BW_IN_UTIL_PERCENTAGE, getLabelsQueryString(labels));
                        return QUERY_PREFIX + query + rangeQuerySuffix;
                    }
                case BW_OUT_PERCENTAGE:
                    if (isAzureNode(node)) {
                        throw new RuntimeException(OPERATION_NOT_SUPPORTED_FOR_AZURE_NODE + BW_OUT_PERCENTAGE);
                    } else {
                        var query = String.format(QUERY_FOR_BW_OUT_UTIL_PERCENTAGE, getLabelsQueryString(labels));
                        return QUERY_PREFIX + query + rangeQuerySuffix;
                    }
                case NETWORK_ERRORS_IN:
                    if (isAzureNode(node)) {
                        throw new RuntimeException(OPERATION_NOT_SUPPORTED_FOR_AZURE_NODE + NETWORK_ERRORS_IN);
                    } else {
                        var query = String.format(QUERY_FOR_NETWORK_ERRORS_IN, getLabelsQueryString(labels));
                        return QUERY_PREFIX + query + rangeQuerySuffix;
                    }
                case NETWORK_ERRORS_OUT:
                    if (isAzureNode(node)) {
                        throw new RuntimeException(OPERATION_NOT_SUPPORTED_FOR_AZURE_NODE + NETWORK_ERRORS_OUT);
                    } else {
                        var query = String.format(QUERY_FOR_NETWORK_ERRORS_OUT, getLabelsQueryString(labels));
                        return QUERY_PREFIX + query + rangeQuerySuffix;
                    }
            }
        } else if (REACHABILITY_PERCENTAGE.equals(metricName)) {
                String query = "response_time_msec" + getLabelsQueryString(labels);
                query = addTimeRange(timeRange, timeRangeUnit, query);
                return QUERY_PREFIX + "(" + "count_over_time" + "(" + query + ")" + "/" +
                    numOfMinutesInDuration(timeRange, timeRangeUnit) + ")" + "*100" + " or vector(0)";
        } else if (AVG_RESPONSE_TIME.equals(metricName)) {
            String query = "response_time_msec" + getLabelsQueryString(labels);
            query = addTimeRange(timeRange, timeRangeUnit, query);
            return  QUERY_PREFIX + "avg_over_time" + "(" + query + ")";
        }
        String queryString = getQueryString(metricName, labels);
        return addTimeRange(timeRange, timeRangeUnit, queryString);

    }

    public String getCustomQueryString(String metricName, Map<String, String> labels,
                                 Integer timeRange, TimeRangeUnit timeRangeUnit, Map<String, String> optionalParams) {

        if (REACHABILITY_PERCENTAGE.equals(metricName)) {
            String query = "response_time_msec" + getLabelsQueryString(labels);
            query = addTimeRange(timeRange, timeRangeUnit, query);
            String firstObservationTimeProp = optionalParams.get(Constants.FIRST_OBSERVATION_TIME);
            if (Strings.isNullOrEmpty(firstObservationTimeProp)) {
                throw new IllegalArgumentException("Invalid Observation time");
            } else {
                long firstObservationTime = Long.parseLong(firstObservationTimeProp);
                long totalSamples = getTotalSamples(timeRange, timeRangeUnit, 60, firstObservationTime);
                return QUERY_PREFIX + "(" + "count_over_time" + "(" + query + ")" + "/" +
                    totalSamples + ")" + "*100" + " or vector(0)";
            }
        }
        throw new IllegalArgumentException("Custom query not supported for " + metricName);

    }

    private long getTotalSamples(Integer timeRange, TimeRangeUnit timeRangeUnit, Integer samplingPeriodInSecs, long firstObservationTime) {
        var duration = getDuration(timeRange, timeRangeUnit).orElseThrow();
        var totalDuration = Duration.ofMillis(Instant.now().toEpochMilli() - firstObservationTime);
        if (totalDuration.toMillis() < duration.toMillis()) {
            return totalDuration.toSeconds() / samplingPeriodInSecs;
        } else {
            return duration.toSeconds() / samplingPeriodInSecs;
        }
    }


    public String getQueryString(Map<String, String> queryParams) {
        StringBuilder sb = new StringBuilder(QUERY_PREFIX + "{");

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

    public String getLabelsQueryString(Map<String, String> labels) {
        StringBuilder sb = new StringBuilder("{");

        int index = 0;
        for (Map.Entry<String, String> param : labels.entrySet()) {
            sb.append(String.format("%s=\"%s\"", param.getKey(), param.getValue()));
            if (index != labels.size() - 1) {
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

    public static Optional<Duration> getDuration(Integer timeRange, TimeRangeUnit timeRangeUnit) {
        try {
            if (TimeRangeUnit.DAY.value.equals(timeRangeUnit.value)) {
                return Optional.of(Duration.parse("P" + timeRange + timeRangeUnit.value));
            }
            return Optional.of(Duration.parse("PT" + timeRange + timeRangeUnit.value));
        } catch (Exception e) {
            LOG.warn("Exception while parsing time range with timeRange {} in units {}", timeRange, timeRangeUnit, e);
        }
        return Optional.empty();
    }

    public static long numOfMinutesInDuration(Integer timeRange, TimeRangeUnit timeRangeUnit) {
        var duration = getDuration(timeRange, timeRangeUnit);
        return duration.map(Duration::toMinutes).orElseThrow();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private boolean isAzureNode(Optional<NodeDTO> node){
        return isNodeScanType(node, AZURE_SCAN_TYPE);
    }

    private boolean isNodeScanType(Optional<NodeDTO> node, String scanType) {
        return node.map(NodeDTO::getScanType).orElse("").equals(scanType);
    }

}
