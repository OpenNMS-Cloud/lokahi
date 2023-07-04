/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2018 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2018 The OpenNMS Group, Inc.
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

package org.opennms.horizon.minion.flows.parser.factory;

import java.util.Objects;

import org.opennms.horizon.minion.flows.listeners.Parser;
import org.opennms.horizon.minion.flows.parser.IpfixUdpParser;
import org.opennms.horizon.minion.flows.parser.TelemetryRegistry;
import org.opennms.horizon.shared.ipc.rpc.IpcIdentity;
import org.opennms.sink.flows.contract.ParserConfig;

import com.codahale.metrics.MetricRegistry;

public class IpfixUdpParserFactory implements ParserFactory {

    private final TelemetryRegistry telemetryRegistry;
    private final DnsResolver dnsResolver;
    private final IpcIdentity identity;
    private final MetricRegistry metricRegistry;

    public IpfixUdpParserFactory(final TelemetryRegistry telemetryRegistry,
                                 final IpcIdentity identity,
                                 final DnsResolver dnsResolver,
                                 final MetricRegistry metricRegistry) {
        this.telemetryRegistry = Objects.requireNonNull(telemetryRegistry);
        this.identity = Objects.requireNonNull(identity);
        this.dnsResolver = Objects.requireNonNull(dnsResolver);
        this.metricRegistry = Objects.requireNonNull(metricRegistry);
    }

    @Override
    public Class<? extends Parser> getParserClass() {
        return IpfixUdpParser.class;
    }

    @Override
    public Parser create(ParserConfig parserConfig) {
        final var dispatcher = telemetryRegistry.getDispatcher();

        return new IpfixUdpParser(
            parserConfig.getName(),
            dispatcher,
            this.identity,
            this.dnsResolver,
            metricRegistry
        );
    }
}
