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

package org.opennms.horizon.tsdata.metrics;

import java.io.IOException;
import java.util.Map;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.PushGateway;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PrometheusMetricsPushAdapter implements MetricsPushAdapter {
    private final PushGateway pushGateway;
    private final String PUSH_JOB_NAME = "horizon-core";

    public PrometheusMetricsPushAdapter(String pushURL) {
        pushGateway = new PushGateway(pushURL);
    }


    @Override
    public void pushMetrics(CollectorRegistry registry, Map<String, String> groupingKey) {
        try {
            pushGateway.pushAdd(registry, PUSH_JOB_NAME);
        } catch (IOException e) {
            log.error("Exception while pushing metrics for : {}", registry, e);
        }
    }
}
