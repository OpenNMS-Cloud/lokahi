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
package org.opennms.horizon.inventory.tenantmetrics;

import io.prometheus.client.Collector;
import java.util.List;
import lombok.Setter;
import org.opennms.horizon.inventory.model.TenantCount;
import org.opennms.horizon.inventory.repository.NodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TenantMetricsNodeCountCollector extends Collector {

    public static final String NODE_COUNT_METRIC_NAME = "node_count";
    public static final String NODE_COUNT_METRIC_DESCRIPTION = "Count of Nodes";
    public static final String NODE_COUNT_TENANT_LABEL_NAME = "tenant";

    @Autowired
    @Setter // for testability
    NodeRepository nodeRepository;

    @Override
    public List<MetricFamilySamples> collect() {
        List<TenantCount> tenantCountList = nodeRepository.countNodesByTenant();

        var samplesList =
                tenantCountList.stream().map(this::convertTenantCountToSample).toList();

        MetricFamilySamples metricFamilySamples =
                new MetricFamilySamples(NODE_COUNT_METRIC_NAME, Type.GAUGE, NODE_COUNT_METRIC_DESCRIPTION, samplesList);

        return List.of(metricFamilySamples);
    }

    private MetricFamilySamples.Sample convertTenantCountToSample(TenantCount tenantCount) {
        return new MetricFamilySamples.Sample(
                NODE_COUNT_METRIC_NAME,
                List.of(NODE_COUNT_TENANT_LABEL_NAME),
                List.of(tenantCount.tenantId()),
                tenantCount.count());
    }
}
