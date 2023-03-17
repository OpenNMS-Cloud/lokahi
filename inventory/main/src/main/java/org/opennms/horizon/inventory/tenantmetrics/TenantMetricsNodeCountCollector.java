/*
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
 *
 */

package org.opennms.horizon.inventory.tenantmetrics;

import io.prometheus.client.Collector;
import lombok.Setter;
import org.opennms.horizon.inventory.model.TenantCount;
import org.opennms.horizon.inventory.repository.NodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TenantMetricsNodeCountCollector extends Collector {

    public static final String NODE_COUNT_METRIC_NAME = "node_count";
    public static final String NODE_COUNT_METRIC_DESCRIPTION = "Count of Nodes";
    public static final String NODE_COUNT_TENANT_LABEL_NAME = "tenant";

    @Autowired
    @Setter     // for testability
    NodeRepository nodeRepository;

    @Override
    public List<MetricFamilySamples> collect() {
        List<TenantCount> tenantCountList = nodeRepository.countNodesByTenant();

        var samplesList =
            tenantCountList.stream()
                .map(this::convertTenantCountToSample)
                .toList();

        MetricFamilySamples metricFamilySamples =
            new MetricFamilySamples(
                NODE_COUNT_METRIC_NAME,
                Type.GAUGE,
                NODE_COUNT_METRIC_DESCRIPTION,
                samplesList
            );

        return List.of(metricFamilySamples);
    }

    private MetricFamilySamples.Sample convertTenantCountToSample(TenantCount tenantCount) {
        return
            new MetricFamilySamples.Sample(
                NODE_COUNT_METRIC_NAME,
                List.of(NODE_COUNT_TENANT_LABEL_NAME),
                List.of(tenantCount.tenantId()),
                tenantCount.count()
            );
    }
}
