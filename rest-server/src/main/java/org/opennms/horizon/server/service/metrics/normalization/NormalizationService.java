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

package org.opennms.horizon.server.service.metrics.normalization;

import lombok.RequiredArgsConstructor;
import org.opennms.horizon.inventory.dto.NodeDTO;
import org.opennms.horizon.server.model.TSData;
import org.opennms.horizon.server.model.TSResult;
import org.opennms.horizon.server.model.TimeSeriesQueryResult;
import org.opennms.horizon.server.service.metrics.MetricLabelUtils;
import org.opennms.horizon.server.service.metrics.normalization.snmp.SnmpNormalizationService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.opennms.horizon.server.service.metrics.normalization.Constants.DISCOVERY_SCAN;
import static org.opennms.horizon.server.service.metrics.normalization.Constants.NODE_SCAN;
import static org.opennms.horizon.server.service.metrics.normalization.Constants.SNMP_MONITOR_TYPE;

@Component
@RequiredArgsConstructor
public class NormalizationService {
    private final MetricLabelUtils metricLabelUtils;
    private final SnmpNormalizationService snmpNormalizationService;

    public String getQueryMetricRegex(NodeDTO node, String metricName, Map<String, String> metricLabels) {

        if (DISCOVERY_SCAN.equals(node.getScanType()) || NODE_SCAN.equals(node.getScanType())) {
            String monitor = metricLabelUtils.getMonitorType(metricLabels);

            if (SNMP_MONITOR_TYPE.equals(monitor)) {
                Optional<String> metricRegexOpt =
                    snmpNormalizationService.getQueryMetricRegex(metricName);

                if (metricRegexOpt.isPresent()) {
                    return metricRegexOpt.get();
                }
            }
        }
        return metricName;
    }

    public TimeSeriesQueryResult normalizeResults(String commonName, TimeSeriesQueryResult queryResult) {

        TSData data = queryResult.getData();
        List<TSResult> tsResults = data.getResult();

        if (tsResults.isEmpty()) {
            return queryResult;
        }

        String monitorType = getMonitorType(tsResults);
        if (SNMP_MONITOR_TYPE.equals(monitorType)) {
            return snmpNormalizationService.normalizeResults(commonName, queryResult);
        }

        return queryResult;
    }

    public String getMonitorType(List<TSResult> tsResults) {
        TSResult tsResult = tsResults.get(0);
        Map<String, String> metricLabels = tsResult.getMetric();
        return metricLabelUtils.getMonitorType(metricLabels);
    }
}
