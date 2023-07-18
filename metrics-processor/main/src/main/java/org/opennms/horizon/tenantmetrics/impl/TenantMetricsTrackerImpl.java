/*******************************************************************************
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
 *******************************************************************************/

package org.opennms.horizon.tenantmetrics.impl;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import org.opennms.horizon.tenantmetrics.TenantMetricsTracker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

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
        Counter counter =
            meterRegistry.counter(METRIC_SAMPLE_COUNT_NAME,
                List.of(
                    Tag.of(SAMPLE_COUNT_TENANT_LABEL_NAME, tenant)
                ));

        counter.increment(count);
    }

    @Override
    public void addTenantFlowReceviedCount(String tenant, int count) {
        Counter counter =
            meterRegistry.counter(FLOW_RECEIVED_COUNT_NAME,
                List.of(
                    Tag.of(SAMPLE_COUNT_TENANT_LABEL_NAME, tenant)
                ));

        counter.increment(count);
    }

    @Override
    public void addTenantFlowCompletedCount(String tenant, int count) {
        Counter counter =
            meterRegistry.counter(FLOW_COMPLETED_COUNT_NAME,
                List.of(
                    Tag.of(SAMPLE_COUNT_TENANT_LABEL_NAME, tenant)
                ));

        counter.increment(count);
    }
}
