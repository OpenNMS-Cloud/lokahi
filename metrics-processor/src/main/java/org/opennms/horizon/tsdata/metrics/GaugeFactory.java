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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Gauge;

@Component
public class GaugeFactory {
    public static final String METRICS_NAME_ICMP_TRIP = "icmp_round_trip_time";
    public static final String METRICS_NAME_SNMP_TRIP = "snmp_round_trip_time";
    public static final String METRICS_NAME_SNMP_UP = "snmp_uptime_sec";
    public static final String[] LABEL_NAMES = {"instance", "location", "system_id"};
    private Map<String, Gauge> gauges = new ConcurrentHashMap<>();
    private CollectorRegistry collectorRegistry = new CollectorRegistry();

    public GaugeFactory() {
        gauges.put(METRICS_NAME_ICMP_TRIP, Gauge.build()
            .name(METRICS_NAME_ICMP_TRIP)
            .help("ICMP round trip time")
            .unit("msec")
            .labelNames(LABEL_NAMES)
            .register(collectorRegistry));
        gauges.put(METRICS_NAME_SNMP_TRIP, Gauge.build()
            .name(METRICS_NAME_SNMP_TRIP)
            .help("SNMP round trip time")
            .unit("msec")
            .labelNames(LABEL_NAMES)
            .register(collectorRegistry));
        gauges.put(METRICS_NAME_SNMP_UP, Gauge.build()
            .name(METRICS_NAME_SNMP_UP)
            .help("SNMP UP time")
            .unit("sec")
            .labelNames(LABEL_NAMES)
            .register(collectorRegistry));
    }

    public Gauge lookupGauge(String name) {
        Gauge result = gauges.compute(name, (key, gauge) -> {
            if (gauge != null) {
                return gauge;
            }
            return
                Gauge.build()
                    .name(name)
                    .unit("msec")
                    .labelNames(LABEL_NAMES)
                    .register(collectorRegistry);
        });
        return result;
    }

    public CollectorRegistry getCollectorRegistry() {
        return collectorRegistry;
    }
}
