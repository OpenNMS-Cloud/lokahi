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

import static org.junit.jupiter.api.Assertions.*;

import io.prometheus.client.Collector;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opennms.horizon.inventory.model.TenantCount;
import org.opennms.horizon.inventory.repository.NodeRepository;

class TenantMetricsNodeCountCollectorTest {

    private TenantMetricsNodeCountCollector target;

    private NodeRepository mockNodeRepository;

    @BeforeEach
    void setUp() {
        mockNodeRepository = Mockito.mock(NodeRepository.class);

        target = new TenantMetricsNodeCountCollector();
    }

    @Test
    void testCollect0() {
        //
        // Setup Test Data and Interactions
        //
        List<TenantCount> testSampleList = Collections.EMPTY_LIST;
        Mockito.when(mockNodeRepository.countNodesByTenant()).thenReturn(testSampleList);

        //
        // Execute
        //
        target.setNodeRepository(mockNodeRepository);
        List<Collector.MetricFamilySamples> result = target.collect();

        //
        // Verify the Results
        //
        assertEquals(1, result.size());

        Collector.MetricFamilySamples metricFamilySamples = result.get(0);
        verifyMetricFamilySamplesAttributes(metricFamilySamples, 0);

        assertEquals(0, metricFamilySamples.samples.size());
    }

    @Test
    void testCollect1() {
        //
        // Setup Test Data and Interactions
        //
        List<TenantCount> testSampleList = List.of(new TenantCount("x-tenant-001-x", 1));
        Mockito.when(mockNodeRepository.countNodesByTenant()).thenReturn(testSampleList);

        //
        // Execute
        //
        target.setNodeRepository(mockNodeRepository);
        List<Collector.MetricFamilySamples> result = target.collect();

        //
        // Verify the Results
        //
        assertEquals(1, result.size());

        Collector.MetricFamilySamples metricFamilySamples = result.get(0);
        verifyMetricFamilySamplesAttributes(metricFamilySamples, 1);

        verifySample("x-tenant-001-x", 1, metricFamilySamples.samples.get(0));
    }

    @Test
    void testCollect3() {
        //
        // Setup Test Data and Interactions
        //
        List<TenantCount> testSampleList = List.of(
                new TenantCount("x-tenant-001-x", 1),
                new TenantCount("x-tenant-002-x", 3),
                new TenantCount("x-tenant-003-x", 7));
        Mockito.when(mockNodeRepository.countNodesByTenant()).thenReturn(testSampleList);

        //
        // Execute
        //
        target.setNodeRepository(mockNodeRepository);
        List<Collector.MetricFamilySamples> result = target.collect();

        //
        // Verify the Results
        //
        assertEquals(1, result.size());

        Collector.MetricFamilySamples metricFamilySamples = result.get(0);
        verifyMetricFamilySamplesAttributes(metricFamilySamples, 3);

        verifySample("x-tenant-001-x", 1, metricFamilySamples.samples.get(0));
        verifySample("x-tenant-002-x", 3, metricFamilySamples.samples.get(1));
        verifySample("x-tenant-003-x", 7, metricFamilySamples.samples.get(2));
    }

    // ========================================
    //
    // ----------------------------------------

    private void verifyMetricFamilySamplesAttributes(
            Collector.MetricFamilySamples metricFamilySamples, int expectedSampleCount) {
        assertEquals(expectedSampleCount, metricFamilySamples.samples.size());
        assertEquals(TenantMetricsNodeCountCollector.NODE_COUNT_METRIC_NAME, metricFamilySamples.name);
        assertEquals(Collector.Type.GAUGE, metricFamilySamples.type);
    }

    private void verifySample(String expectedTenantId, int expectedValue, Collector.MetricFamilySamples.Sample sample) {
        assertEquals(1, sample.labelNames.size());
        assertEquals(TenantMetricsNodeCountCollector.NODE_COUNT_TENANT_LABEL_NAME, sample.labelNames.get(0));

        assertEquals(1, sample.labelValues.size());
        assertEquals(expectedTenantId, sample.labelValues.get(0));

        assertEquals(expectedValue, sample.value);
    }
}
