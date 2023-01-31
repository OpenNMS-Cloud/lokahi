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

package org.opennms.horizon.server.service.metrics.normalization.snmp;

import lombok.RequiredArgsConstructor;
import org.opennms.horizon.server.model.TSData;
import org.opennms.horizon.server.model.TSResult;
import org.opennms.horizon.server.model.TimeSeriesQueryResult;
import org.opennms.horizon.server.service.metrics.MetricLabelUtils;
import org.opennms.horizon.server.service.metrics.normalization.snmp.dto.SnmpMetricQueryInfoDTO;
import org.opennms.horizon.server.service.metrics.normalization.snmp.dto.SnmpWalkDataDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static org.opennms.horizon.server.service.metrics.normalization.Constants.IF_IN_OCTETS;
import static org.opennms.horizon.server.service.metrics.normalization.Constants.IF_OUT_OCTETS;
import static org.opennms.horizon.server.service.metrics.normalization.Constants.NETWORK_IN_TOTAL_BYTES;
import static org.opennms.horizon.server.service.metrics.normalization.Constants.NETWORK_OUT_TOTAL_BYTES;
import static org.opennms.horizon.server.service.metrics.normalization.Constants.SYS_UP_TIME;

@Component
@RequiredArgsConstructor
public class SnmpNormalizationService {
    private final MetricLabelUtils metricLabelUtils;

    // Mapping Common name to SNMP Query Information
    private static final Map<String, SnmpMetricQueryInfoDTO> SNMP_COMMON_TO_QUERY_INFO = new HashMap<>();

    static {
        SNMP_COMMON_TO_QUERY_INFO.put(NETWORK_IN_TOTAL_BYTES, SnmpMetricQueryInfoDTO.builder().metric(IF_IN_OCTETS).dependents(List.of(SYS_UP_TIME)).build());
        SNMP_COMMON_TO_QUERY_INFO.put(NETWORK_OUT_TOTAL_BYTES, SnmpMetricQueryInfoDTO.builder().metric(IF_OUT_OCTETS).dependents(List.of(SYS_UP_TIME)).build());
    }

    public Optional<String> getQueryMetricRegex(String metricName) {
        if (SNMP_COMMON_TO_QUERY_INFO.containsKey(metricName)) {
            SnmpMetricQueryInfoDTO queryInfo = SNMP_COMMON_TO_QUERY_INFO.get(metricName);
            return Optional.of(queryInfo.getNamesOrRegex());
        }
        return Optional.empty();
    }

    public TimeSeriesQueryResult normalizeResults(String commonName, TimeSeriesQueryResult queryResult) {
        if (NETWORK_IN_TOTAL_BYTES.equals(commonName)) {
            calculateNetworkInOut(queryResult, commonName, IF_IN_OCTETS);
        } else if (NETWORK_OUT_TOTAL_BYTES.equals(commonName)) {
            calculateNetworkInOut(queryResult, commonName, IF_OUT_OCTETS);
        }
        return queryResult;
    }

    private void calculateNetworkInOut(TimeSeriesQueryResult queryResult, String commonName, String octetMetricName) {
        TSData data = queryResult.getData();
        List<TSResult> results = data.getResult();

        if (results.size() != 2) {
            throw new RuntimeException("Failed to get both required metrics for " + octetMetricName);
        }

        TSResult octetResult = findResult(results, octetMetricName);
        TSResult sysUpTimeResult = findResult(results, SYS_UP_TIME);

        Map<Double, SnmpWalkDataDTO> transformMap = transform(octetResult, sysUpTimeResult);

        if (transformMap.size() < 2) {
            data.setResult(Collections.emptyList());
            return;
        }

        List<List<Double>> collectedData = new ArrayList<>();

        List<Double> timeKeyList = new ArrayList<>(transformMap.keySet());
        for (int index = 1; index < timeKeyList.size(); index++) {
            SnmpWalkDataDTO data1 = transformMap.get(timeKeyList.get(index - 1));

            Double time2 = timeKeyList.get(index);
            SnmpWalkDataDTO data2 = transformMap.get(time2);

            List<Double> tsData = new ArrayList<>();
            tsData.add(time2);
            tsData.add(calculateThroughputBytesPerSec(data1, data2));
            collectedData.add(tsData);
        }

        List<TSResult> calculatedResultList = new ArrayList<>();
        TSResult calculatedResult = new TSResult();
        Map<String, String> labels = octetResult.getMetric();
        labels.put(MetricLabelUtils.METRIC_NAME_KEY, commonName);

        calculatedResult.setMetric(labels);
        calculatedResult.setValue(octetResult.getValue());
        calculatedResult.setValues(collectedData);

        calculatedResultList.add(calculatedResult);
        data.setResult(calculatedResultList);
    }

    private Map<Double, SnmpWalkDataDTO> transform(TSResult octetResult,
                                                   TSResult sysUpTimeResult) {

        Map<Double, SnmpWalkDataDTO> transformMap = new TreeMap<>();

        for (List<Double> octetValue : octetResult.getValues()) {
            Double time = octetValue.get(0);
            Double octet = octetValue.get(1);

            if (transformMap.containsKey(time)) {
                transformMap.get(time).setOctet(octet);
            } else {
                SnmpWalkDataDTO walkDataDto = new SnmpWalkDataDTO();
                walkDataDto.setOctet(octet);
                transformMap.put(time, walkDataDto);
            }
        }

        for (List<Double> sysUpTimeValue : sysUpTimeResult.getValues()) {
            Double time = sysUpTimeValue.get(0);
            Double sysUpTime = sysUpTimeValue.get(1);

            if (transformMap.containsKey(time)) {
                transformMap.get(time).setSysUpTime(sysUpTime);
            }
        }

        return transformMap;
    }

    private double calculateThroughputBytesPerSec(SnmpWalkDataDTO data1, SnmpWalkDataDTO data2) {
        double octetDelta = data1.getOctetDelta(data2.getOctet());
        double sysUpTimeDelta = data1.getSysUpTimeDelta(data2.getSysUpTime());

        if (sysUpTimeDelta == 0) {
            return 0d;
        } else {
            return octetDelta / sysUpTimeDelta;
        }
    }

    private TSResult findResult(List<TSResult> results, String name) {
        for (TSResult result : results) {
            if (metricLabelUtils.getMetricName(result.getMetric()).equals(name)) {
                return result;
            }
        }
        throw new RuntimeException("Failed to find metric in results: " + name);
    }
}
