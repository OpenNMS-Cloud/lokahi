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

package org.opennms.horizon.metrics.impl;

import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.PushGateway;
import org.opennms.horizon.metrics.api.OnmsMetricsAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public class PrometheusPushGatewayAdapter implements OnmsMetricsAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(PrometheusPushGatewayAdapter.class);
    private final PushGateway pushGateway;
    private final String DEFAULT_JOB_PUSHGATEWAY = "horizon-core";

    public PrometheusPushGatewayAdapter(String pushGatewayUrl) {
        this.pushGateway = new PushGateway(pushGatewayUrl);
    }


    // TODO: rate-limit, or at least prevent concurrent pushes?  Maybe not critical due to groupingKey use
    @Override
    public void pushMetrics(CollectorRegistry registry, Map<String, String> groupingKey) {
        try {
            pushGateway.pushAdd(registry, DEFAULT_JOB_PUSHGATEWAY);
        } catch (IOException e) {
            LOG.error("Exception while pushing metrics for : {}", registry, e);
        }
    }
}
