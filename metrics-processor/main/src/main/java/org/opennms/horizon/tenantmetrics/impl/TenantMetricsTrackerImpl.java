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
package org.opennms.horizon.tenantmetrics.impl;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import java.util.List;
import org.opennms.horizon.tenantmetrics.TenantMetricsTracker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TenantMetricsTrackerImpl implements TenantMetricsTracker {

    public static final String METRIC_SAMPLE_COUNT_NAME = "metric_sample_count";
    public static final String FLOW_RECEIVED_COUNT_NAME = "flow_received_count";

    public static final String FLOW_COMPLETED_COUNT_NAME = "flow_completed_count";
    public static final String SAMPLE_COUNT_TENANT_LABEL_NAME = "tenant";

    @Autowired
    private MeterRegistry meterRegistry;

    @Override
    public void addTenantMetricSampleCount(String tenant, int count) {
        Counter counter = meterRegistry.counter(
                METRIC_SAMPLE_COUNT_NAME, List.of(Tag.of(SAMPLE_COUNT_TENANT_LABEL_NAME, tenant)));

        counter.increment(count);
    }

    @Override
    public void addTenantFlowReceviedCount(String tenant, int count) {
        Counter counter = meterRegistry.counter(
                FLOW_RECEIVED_COUNT_NAME, List.of(Tag.of(SAMPLE_COUNT_TENANT_LABEL_NAME, tenant)));

        counter.increment(count);
    }

    @Override
    public void addTenantFlowCompletedCount(String tenant, int count) {
        Counter counter = meterRegistry.counter(
                FLOW_COMPLETED_COUNT_NAME, List.of(Tag.of(SAMPLE_COUNT_TENANT_LABEL_NAME, tenant)));

        counter.increment(count);
    }
}
